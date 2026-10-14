package uz.minibank.outbox;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * payload в outbox УЖЕ сериализован в JSON — если пропустить его через основной
 * KafkaTemplate<String, Object> (JacksonJsonSerializer), строка сериализуется ЕЩЁ РАЗ
 * и превратится в JSON-строку-в-кавычках. Поэтому здесь собственный producer
 * с обычным StringSerializer: значение уходит в Kafka как есть, байт в байт.
 *
 * Конфиг НЕ собирается заново из KafkaProperties.buildProducerProperties(): в тестах
 * Testcontainers подменяет bootstrap-servers через KafkaConnectionDetails, а не через
 * сырое свойство spring.kafka.bootstrap-servers — buildProducerProperties() эту
 * подмену не видит и стучится в несуществующий localhost:9092. Правильный адрес
 * уже знает автоконфигурированный ProducerFactory<Object,Object> — конфиг берём у него.
 */
@Component
class KafkaOutboxPublisher implements OutboxPublisher {

    private final KafkaTemplate<String, String> delegate;

    KafkaOutboxPublisher(ProducerFactory<Object, Object> defaultProducerFactory) {
        Map<String, Object> props = new HashMap<>(defaultProducerFactory.getConfigurationProperties());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        this.delegate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Override
    public CompletableFuture<SendResult<String, String>> send(String topic, String key, String payload) {
        return delegate.send(topic, key, payload);
    }
}
