package uz.minibank.outbox;

import java.util.concurrent.CompletableFuture;

/**
 * Граница между OutboxRelay и конкретным способом доставки. OutboxRelay не должен
 * знать про KafkaTemplate вообще — только "отправь топик+ключ+готовый JSON".
 *
 * Отдельный интерфейс — не блажь: если завязаться на bean типа KafkaTemplate<String,String>
 * напрямую, Spring Boot решит, что "бин KafkaTemplate уже есть" (дженерики стираются
 * при проверке @ConditionalOnMissingBean) и не создаст СТАНДАРТНЫЙ KafkaTemplate<String,Object>,
 * которым пользуется TransferEventPublisher. Свой интерфейс убирает этот конфликт
 * и заодно делает релей тривиально подменяемым в тестах.
 */
public interface OutboxPublisher {
    CompletableFuture<?> send(String topic, String key, String payload);
}
