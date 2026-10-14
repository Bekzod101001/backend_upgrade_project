package uz.minibank.transfer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import uz.minibank.TestcontainersConfiguration;
import uz.minibank.account.Account;
import uz.minibank.account.AccountRepository;
import uz.minibank.kafka.KafkaTopics;
import uz.minibank.transfer.event.TransferCompletedEvent;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Postgres и Kafka — две разные системы, атомарно записать в обе нельзя.
 *
 * История этого теста: сначала публикация жила ВНУТРИ @Transactional (вариант A) —
 * откат транзакции не откатывал Kafka, консьюмер получал событие о переводе,
 * которого не было. Потом публикацию вынесли ПОСЛЕ commit (вариант B) — фантом ушёл,
 * но появилось окно тихой потери между commit и publish.
 *
 * OUTBOX: событие пишется В ТУ ЖЕ транзакцию, что и деньги. Откат теперь откатывает
 * и деньги, и строку в outbox одним и тем же ROLLBACK — публиковать физически нечего,
 * фантом исключён на уровне БД, а не дисциплиной кода.
 */
@Import({TestcontainersConfiguration.class, TransferDualWriteTest.EventSpy.class})
@SpringBootTest
class TransferDualWriteTest {

    private static final String CREDIT_FAILS = "CREDIT_FAILS";

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EventSpy events;

    @Autowired
    private uz.minibank.outbox.OutboxEventRepository outboxEventRepository;

    @AfterEach
    void dropCreditFailureTrigger() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_credit ON accounts");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS fail_credit()");
    }

    @Test
    void rollbackLeavesNoOutboxRowAndNoEvent() throws Exception {
        Account from = accountRepository.save(new Account("Alice", 1_000_000_00L));
        Account to = accountRepository.save(new Account(CREDIT_FAILS, 500_000_00L));

        installCreditFailureTrigger();

        // Перевод падает на commit: UPDATE балансов отбивает триггер
        assertThatThrownBy(() -> transferService.transfer(
                UUID.randomUUID(), from.getId(), to.getId(), 200_000_00L))
                .rootCause()
                .hasMessageContaining("simulated credit failure");

        long fromBalance = balanceOf(from);
        long toBalance = balanceOf(to);
        long outboxRows = outboxEventRepository.count();
        TransferCompletedEvent delivered = events.received.poll(5, TimeUnit.SECONDS);

        System.out.printf("""
                        === outbox: откат = откат ===
                        перевод:           УПАЛ, транзакция откачена
                        Alice: %d (было 100000000)
                        Bob:   %d (было 50000000)
                        строк в outbox:    %d
                        событие в Kafka:   %s
                        %n""",
                fromBalance, toBalance, outboxRows,
                delivered == null ? "нет — как и должно быть" : "ЕСТЬ (ФАНТОМ), transferId=" + delivered.transferId());

        // Деньги не двигались — Postgres отработал честно
        assertThat(fromBalance).isEqualTo(1_000_000_00L);
        assertThat(toBalance).isEqualTo(500_000_00L);

        // INSERT в outbox откатился вместе с UPDATE'ами балансов — той же транзакцией
        assertThat(outboxRows).isZero();

        // Публиковать нечего — фантом исключён физически, а не дисциплиной кода
        assertThat(delivered)
                .as("фантомное событие: транзакция откачена, а консьюмер уведомлён")
                .isNull();
    }

    private long balanceOf(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow().getBalance();
    }

    private void installCreditFailureTrigger() {
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION fail_credit() RETURNS trigger AS $$
                BEGIN
                    IF NEW.owner_name = '%s' THEN
                        RAISE EXCEPTION 'simulated credit failure';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """.formatted(CREDIT_FAILS));
        jdbcTemplate.execute("""
                CREATE TRIGGER fail_credit
                BEFORE UPDATE ON accounts
                FOR EACH ROW EXECUTE FUNCTION fail_credit()
                """);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EventSpy {

        final BlockingQueue<TransferCompletedEvent> received = new LinkedBlockingQueue<>();

        @KafkaListener(topics = KafkaTopics.TRANSFER_COMPLETED, groupId = "test-dual-write")
        void onEvent(TransferCompletedEvent event) {
            received.add(event);
        }
    }
}
