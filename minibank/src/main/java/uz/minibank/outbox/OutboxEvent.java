package uz.minibank.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Строка = «намерение опубликовать событие», пережившее коммит бизнес-транзакции.
 * Payload уже сериализован в JSON вызывающей стороной — эта таблица не знает
 * и не должна знать про TransferCompletedEvent или любой другой конкретный тип события.
 */
@Entity
@Table(name = "outbox")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String topic;

    @Column(name = "partition_key", nullable = false, updatable = false)
    private String partitionKey;

    // JSONB-колонка: Hibernate пишет Java String как есть, без лишнего экранирования
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String topic, String partitionKey, String payload) {
        this.topic = topic;
        this.partitionKey = partitionKey;
        this.payload = payload;
    }

    public void markProcessed() {
        this.processedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
