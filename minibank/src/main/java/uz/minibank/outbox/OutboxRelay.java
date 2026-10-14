package uz.minibank.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Воркер очереди: то же самое, что делает `php artisan queue:work` для Laravel-джобы,
 * только читает не Redis, а Postgres.
 *
 * Транзакция держит PESSIMISTIC_WRITE lock на выбранных строках всё время,
 * пока идёт отправка в Kafka — второй инстанс воркера (второй под) в этот момент
 * пропустит эти строки (SKIP LOCKED) и возьмёт следующие.
 *
 * Если процесс умрёт МЕЖДУ успешным send() и markProcessed()/commit — lock отпустится
 * вместе со смертью соединения, строка останется processedAt = null,
 * следующий тик отправит её СНОВА. Это at-least-once, и это осознанный выбор:
 * дубль в Kafka дешевле, чем потерянное навсегда событие.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository repository;
    private final OutboxPublisher publisher;

    public OutboxRelay(OutboxEventRepository repository, OutboxPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = repository.findBatchForPublishing(Limit.of(BATCH_SIZE));
        for (OutboxEvent event : batch) {
            try {
                // .get() здесь НЕ проблема, в отличие от денежной операции:
                // это фоновый воркер, а не запрос клиента, держать поток секунду можно
                publisher.send(event.getTopic(), event.getPartitionKey(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);
                event.markProcessed();
            } catch (Exception e) {
                // Не глотаем весь батч из-за одной проблемной строки: она останется
                // необработанной и будет подхвачена на следующем тике — как раз то,
                // ради чего эта таблица существует
                log.error("Не удалось опубликовать outbox-событие id={}, повторим на следующем тике",
                        event.getId(), e);
            }
        }
    }
}
