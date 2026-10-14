package uz.minibank.payment;

import org.springframework.stereotype.Component;
import uz.minibank.payment.exception.ProviderValidationException;

/**
 * Заглушка вместо реального HTTP-клиента к провайдеру. Нужна только чтобы
 * граница "сетевой вызов снаружи транзакции" была реальной архитектурной
 * границей уже сейчас — настоящий HTTP-клиент встанет на это же место,
 * не трогая PaymentService.
 */
@Component
public class InMemoryProviderClient implements ProviderClient {

    @Override
    public ProviderAccountInfo lookup(String providerCode, String providerAccountNumber) {
        if (providerAccountNumber == null || !providerAccountNumber.matches("\\d{6,12}")) {
            throw new ProviderValidationException(
                    "Unknown %s account: %s".formatted(providerCode, providerAccountNumber));
        }
        return new ProviderAccountInfo("Customer #" + providerAccountNumber);
    }
}
