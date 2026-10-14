package uz.minibank.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.KafkaListener;
import uz.minibank.TestcontainersConfiguration;
import uz.minibank.account.Account;
import uz.minibank.account.AccountRepository;
import uz.minibank.kafka.KafkaTopics;
import uz.minibank.transfer.TransferService;
import uz.minibank.transfer.event.TransferCompletedEvent;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ровно тот сценарий, который убивал вариант B: воркер (или процесс) падает
 * между "перевод закоммичен" и "событие ушло в Kafka".
 *
 * Разница с вариантом B — здесь падение НЕ означает потерю. Строка в outbox
 * остаётся processedAt = null, и следующий тик @Scheduled (раз в секунду)
 * подхватывает её снова. Клиент в этот момент давно получил успешный ответ
 * и никогда не узнает, что первая попытка публикации не удалась —
 * и ему не нужно об этом знать.
 */
@Import({TestcontainersConfiguration.class, OutboxRelayRecoveryTest.Fixture.class})
@SpringBootTest
class OutboxRelayRecoveryTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private Fixture fixture;

    @Test
    void relayRetriesAfterTransientFailureAndEventuallyDelivers() throws Exception {
        Account from = accountRepository.save(new Account("Alice", 1_000_000_00L));
        Account to = accountRepository.save(new Account("Bob", 500_000_00L));

        // "Брокер недоступен" именно в момент, когда релей пытается отправить —
        // деньги при этом переводятся штатно, метод transfer() про это вообще не знает
        fixture.brokerDown.set(true);
        transferService.transfer(UUID.randomUUID(), from.getId(), to.getId(), 200_000_00L);

        // Пока брокер лежит — несколько тиков релея проходят вхолостую, строка остаётся неотправленной
        Thread.sleep(2500);
        long unprocessedRows = outboxEventRepository.findAll().stream()
                .filter(e -> e.getProcessedAt() == null)
                .count();
        TransferCompletedEvent whileDown = fixture.received.poll(500, TimeUnit.MILLISECONDS);

        // Брокер снова жив. Мы НИЧЕГО не делаем руками — просто ждём следующий тик
        fixture.brokerDown.set(false);
        TransferCompletedEvent delivered = fixture.received.poll(10, TimeUnit.SECONDS);

        System.out.printf("""
                        === outbox relay: восстановление после падения ===
                        деньги переведены сразу:      Alice %d, Bob %d
                        пока брокер лежал:             событие в Kafka = %s, неотправленных строк = %d
                        после восстановления брокера:  событие в Kafka = %s
                        %n""",
                balanceOf(from), balanceOf(to),
                whileDown == null ? "нет" : "ЕСТЬ (не должно было)", unprocessedRows,
                delivered == null ? "НЕТ" : "ЕСТЬ, transferId=" + delivered.transferId());

        // Деньги двинулись сразу же, независимо от судьбы Kafka
        assertThat(balanceOf(from)).isEqualTo(800_000_00L);
        assertThat(balanceOf(to)).isEqualTo(700_000_00L);

        // Пока брокер лежал, событие честно не доставлено — но и не потеряно
        assertThat(whileDown).isNull();
        assertThat(unprocessedRows).isEqualTo(1);

        // ...а как только брокер поднялся, следующий же тик релея его доставил.
        // Никакого ретрая от клиента, никакого нового idempotency key — просто подождали.
        assertThat(delivered)
                .as("релей сам переотправил строку из outbox после восстановления брокера")
                .isNotNull();
        assertThat(delivered.fromAccountId()).isEqualTo(from.getId());
    }

    private long balanceOf(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow().getBalance();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Fixture {

        final AtomicBoolean brokerDown = new AtomicBoolean(false);
        final BlockingQueue<TransferCompletedEvent> received = new LinkedBlockingQueue<>();

        /**
         * Подменяем ровно тот бин, которым пользуется OutboxRelay (OutboxPublisher),
         * а не основной KafkaTemplate<String,Object> — тот трогать незачем.
         */
        @Bean
        @Primary
        OutboxPublisher flakyOutboxPublisher(KafkaOutboxPublisher real) {
            return (topic, key, payload) -> {
                if (brokerDown.get()) {
                    return CompletableFuture.failedFuture(new IllegalStateException("broker unreachable"));
                }
                return real.send(topic, key, payload);
            };
        }

        @KafkaListener(topics = KafkaTopics.TRANSFER_COMPLETED, groupId = "test-outbox-recovery")
        void onEvent(TransferCompletedEvent event) {
            received.add(event);
        }
    }
}
