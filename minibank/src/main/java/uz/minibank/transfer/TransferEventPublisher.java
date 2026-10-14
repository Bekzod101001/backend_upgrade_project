package uz.minibank.transfer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import uz.minibank.kafka.KafkaTopics;
import uz.minibank.transfer.event.TransferCompletedEvent;

@Component
public class TransferEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransferEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TransferCompletedEvent event) {
        // Ключ определяет партицию: hash(key) % partitions.
        // Берём счёт-источник, чтобы события одного счёта лежали в одной партиции
        // и приходили consumer'у строго по порядку.
        String key = String.valueOf(event.fromAccountId());
        kafkaTemplate.send(KafkaTopics.TRANSFER_COMPLETED, key, event);
    }
}
