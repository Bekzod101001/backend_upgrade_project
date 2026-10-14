package uz.minibank.payment.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Контракт события. Как и TransferCompletedEvent — публичный API для внешних
 * консьюмеров, несовместимые изменения полей не делаются.
 *
 * @param eventId идентификатор САМОГО события (не попытки доставки) — ключ
 *                идемпотентности на стороне consumer'а (Day 5).
 */
public record PaymentCompletedEvent(
        UUID eventId,
        UUID paymentId,
        Long accountId,
        String providerCode,
        String providerAccountNumber,
        Long amount,
        Instant occurredAt
) {
}
