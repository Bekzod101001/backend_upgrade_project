package uz.minibank.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Единственное место, которое знает имена топиков и их устройство.
 * Имя топика — это маршрут, а не бизнес-правило, поэтому живёт в инфраструктуре,
 * а не внутри модуля transfer.
 */
@Configuration
public class KafkaTopics {

    public static final String TRANSFER_COMPLETED = "transfer.completed";
    public static final String PAYMENT_COMPLETED = "payment.completed";

    /**
     * Kafka умеет создавать топики автоматически, но тогда число партиций —
     * молчаливый дефолт брокера. Объявляем явно: партиций 3, реплика 1 (кластер из одной ноды).
     * Партиции можно только добавить, убавить нельзя — и добавление ломает привязку ключ→партиция.
     */
    @Bean
    NewTopic transferCompletedTopic() {
        return TopicBuilder.name(TRANSFER_COMPLETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(PAYMENT_COMPLETED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
