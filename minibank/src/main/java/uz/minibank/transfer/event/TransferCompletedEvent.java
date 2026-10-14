package uz.minibank.transfer.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Контракт события. Всё, что попадает в топик, — публичный API:
 * читатели могут быть в других сервисах и на других языках,
 * поэтому менять поля несовместимо нельзя.
 *
 * @param eventId идентификатор САМОГО СОБЫТИЯ, не попытки его доставить.
 *                Именно он будет ключом идемпотентности на стороне consumer'а (Day 5).
 */
public record TransferCompletedEvent(
        UUID eventId,
        Long transferId,
        Long fromAccountId,
        Long toAccountId,
        Long amount,
        Instant occurredAt
) {
}
