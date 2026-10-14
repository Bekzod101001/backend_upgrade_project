package uz.minibank.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import uz.minibank.TestcontainersConfiguration;
import uz.minibank.kafka.KafkaTopics;
import uz.minibank.transfer.event.TransferCompletedEvent;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяем саму проводку: producer → брокер → consumer.
 * Заодно — fan-out: две независимые группы читают ОДНО сообщение,
 * каждая со своим offset'ом, и друг о друге не знают.
 */
@Import({TestcontainersConfiguration.class, TransferEventPublishingTest.TestListeners.class})
@SpringBootTest
class TransferEventPublishingTest {

    @Autowired
    private TransferEventPublisher publisher;

    @Autowired
    private TestListeners listeners;

    @Test
    void eventReachesEveryConsumerGroup() throws Exception {
        TransferCompletedEvent event = new TransferCompletedEvent(
                UUID.randomUUID(), 42L, 1L, 2L, 200_000_00L, Instant.now());

        publisher.publish(event);

        TransferCompletedEvent bySms = listeners.sms.poll(30, TimeUnit.SECONDS);
        TransferCompletedEvent byAntifraud = listeners.antifraud.poll(30, TimeUnit.SECONDS);

        System.out.printf("""
                        === fan-out ===
                        группа sms:       %s
                        группа antifraud: %s
                        %n""",
                bySms == null ? "НЕ ПОЛУЧИЛА" : "получила eventId=" + bySms.eventId(),
                byAntifraud == null ? "НЕ ПОЛУЧИЛА" : "получила eventId=" + byAntifraud.eventId());

        // Один publish — две доставки. Producer не знает ни об одной из групп
        assertThat(bySms).isEqualTo(event);
        assertThat(byAntifraud).isEqualTo(event);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestListeners {

        final BlockingQueue<TransferCompletedEvent> sms = new LinkedBlockingQueue<>();
        final BlockingQueue<TransferCompletedEvent> antifraud = new LinkedBlockingQueue<>();

        @KafkaListener(topics = KafkaTopics.TRANSFER_COMPLETED, groupId = "test-sms")
        void onSms(TransferCompletedEvent event) {
            sms.add(event);
        }

        @KafkaListener(topics = KafkaTopics.TRANSFER_COMPLETED, groupId = "test-antifraud")
        void onAntifraud(TransferCompletedEvent event) {
            antifraud.add(event);
        }
    }
}
