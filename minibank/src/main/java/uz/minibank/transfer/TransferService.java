package uz.minibank.transfer;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import uz.minibank.transfer.exception.IdempotencyKeyConflictException;

import java.util.UUID;

@Service
public class TransferService {

    private final TransferExecutor transferExecutor;
    private final TransferRepository transferRepository;

    public TransferService(TransferExecutor transferExecutor, TransferRepository transferRepository) {
        this.transferExecutor = transferExecutor;
        this.transferRepository = transferRepository;
    }

    /**
     * Повторная доставка того же перевода (retry клиента, двойной тап) не двигает деньги второй раз.
     * Метод НЕ транзакционный: транзакция начинается и заканчивается внутри transferExecutor.
     *
     * Публикация события в Kafka здесь больше НЕ упоминается вообще — это осознанно.
     * transferExecutor уже записал намерение опубликовать событие в outbox, в той же
     * транзакции, что и деньги. Сама публикация — отдельный процесс (OutboxRelay),
     * развязанный по времени с этим методом. Раньше именно здесь жил dual write:
     * либо публикация внутри транзакции (фантомные события при откате),
     * либо публикация после commit (тихая потеря, если упасть в этом окне).
     * Outbox убирает саму возможность у этого метода дотянуться до Kafka напрямую.
     */
    public void transfer(UUID idempotencyKey, Long fromId, Long toId, Long amount) {
        try {
            transferExecutor.execute(idempotencyKey, fromId, toId, amount);
        } catch (DataIntegrityViolationException e) {
            // Ключ уже занят => транзакция откатилась целиком, деньги не двигались.
            // Читаем запись первой попытки уже в новой транзакции.
            Transfer existing = transferRepository.findByIdempotencyKey(idempotencyKey)
                    // записи нет => нарушен другой constraint (например, FK на несуществующий счёт)
                    .orElseThrow(() -> e);

            requireSamePayload(existing, fromId, toId, amount);
            // Дубль: перевод уже проведён первой попыткой, повторять нечего
        }
    }

    private void requireSamePayload(Transfer existing, Long fromId, Long toId, Long amount) {
        if (!existing.getFromAccountId().equals(fromId)
                || !existing.getToAccountId().equals(toId)
                || !existing.getAmount().equals(amount)) {
            throw new IdempotencyKeyConflictException(
                    "Idempotency key %s already used for a different transfer".formatted(existing.getIdempotencyKey()));
        }
    }
}
