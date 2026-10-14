package uz.minibank.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Якорь между шагом 1 (credentials провайдера) и шагом 2 (сумма + подтверждение) —
 * то самое "между шагами" человеческое время, на которое нельзя держать DB-транзакцию (Day 17).
 * id генерируется здесь же, до первого save, и становится естественным idempotency key
 * для confirm: повторный confirm с тем же id ловится блокировкой строки и проверкой статуса,
 * а не отдельным UUID-ключом от клиента.
 */
@Entity
@Table(name = "payment_intents")
public class PaymentIntent {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private Long accountId;

    @Column(name = "provider_code", nullable = false, updatable = false)
    private String providerCode;

    @Column(name = "provider_account_number", nullable = false, updatable = false)
    private String providerAccountNumber;

    @Column(name = "provider_display_name", updatable = false)
    private String providerDisplayName;

    @Column
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // Значение ставит БД (DEFAULT now())
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected PaymentIntent() {
    }

    public PaymentIntent(Long accountId, String providerCode, String providerAccountNumber, String providerDisplayName) {
        this.id = UUID.randomUUID();
        this.accountId = accountId;
        this.providerCode = providerCode;
        this.providerAccountNumber = providerAccountNumber;
        this.providerDisplayName = providerDisplayName;
        this.status = PaymentStatus.CREATED;
    }

    public void confirm(Long amount) {
        this.amount = amount;
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getProviderAccountNumber() {
        return providerAccountNumber;
    }

    public String getProviderDisplayName() {
        return providerDisplayName;
    }

    public Long getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
