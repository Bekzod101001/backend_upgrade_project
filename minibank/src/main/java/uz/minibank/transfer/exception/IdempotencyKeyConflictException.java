package uz.minibank.transfer.exception;

/**
 * Тот же ключ пришёл с другим payload. Это не retry, а баг клиента
 * (переиспользовал ключ) или попытка подмены — молча отвечать «успех» нельзя.
 */
public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException(String message) {
        super(message);
    }
}
