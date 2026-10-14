package uz.minibank.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import uz.minibank.TestcontainersConfiguration;
import uz.minibank.kafka.KafkaTopics;
import uz.minibank.transfer.event.TransferCompletedEvent;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * kafkaTemplate.send() ВЫГЛЯДИТ синхронным вызовом, но им не является:
 * он кладёт запись в буфер в памяти и возвращает CompletableFuture.
 * Настоящую сетевую отправку делает фоновый поток продюсера.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class KafkaSendIsAsyncTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void sendReturnsBeforeBrokerConfirms() throws Exception {
        TransferCompletedEvent event = new TransferCompletedEvent(
                UUID.randomUUID(), 1L, 1L, 2L, 100L, Instant.now());

        // Прогреваем: первый send тянет метаданные топика и может заблокироваться
        kafkaTemplate.send(KafkaTopics.TRANSFER_COMPLETED, "1", event).get(30, TimeUnit.SECONDS);

        String callerThread = Thread.currentThread().getName();
        AtomicReference<String> completionThread = new AtomicReference<>();

        long startNanos = System.nanoTime();
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaTopics.TRANSFER_COMPLETED, "1", event);
        long returnedAfterMicros = (System.nanoTime() - startNanos) / 1_000;

        // Успел ли брокер подтвердить к моменту, когда send() уже вернул управление?
        boolean doneImmediately = future.isDone();

        future.whenComplete((result, error) -> completionThread.set(Thread.currentThread().getName()));
        future.get(30, TimeUnit.SECONDS);

        System.out.printf("""
                        === kafkaTemplate.send() ===
                        вызван из потока:         %s
                        вернул управление через:  %d мкс
                        брокер подтвердил к тому моменту: %s
                        подтверждение пришло в потоке:    %s
                        %n""",
                callerThread, returnedAfterMicros, doneImmediately ? "ДА" : "НЕТ", completionThread.get());

        // send() вернулся до того, как брокер что-либо подтвердил
        assertThat(doneImmediately)
                .as("send() не ждёт брокера — он кладёт запись в буфер и возвращается")
                .isFalse();

        // Сетевую работу делает не тот поток, который звал send()
        assertThat(completionThread.get())
                .as("подтверждение приходит на фоновом потоке продюсера, а не на вызывающем")
                .isNotEqualTo(callerThread)
                .contains("producer");
    }
}
