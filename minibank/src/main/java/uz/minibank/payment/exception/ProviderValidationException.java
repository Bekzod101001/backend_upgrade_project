package uz.minibank.payment.exception;

public class ProviderValidationException extends RuntimeException {

    public ProviderValidationException(String message) {
        super(message);
    }
}
