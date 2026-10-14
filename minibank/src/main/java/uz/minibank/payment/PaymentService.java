package uz.minibank.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import uz.minibank.account.Account;
import uz.minibank.account.AccountRepository;
import uz.minibank.api.model.PaymentInitRequest;
import uz.minibank.api.model.PaymentIntentResponse;
import uz.minibank.kafka.KafkaTopics;
import uz.minibank.outbox.OutboxEvent;
import uz.minibank.outbox.OutboxEventRepository;
import uz.minibank.payment.event.PaymentCompletedEvent;
import uz.minibank.payment.exception.InsufficientBalanceException;
import uz.minibank.payment.exception.PaymentNotFoundException;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO здесь — сгенерированные из openapi/payments.yaml (uz.minibank.api.model),
 * не ручные record'ы: спека — единственный источник истины для формы запроса/ответа (Day 18, SDD).
 */
@Service
public class PaymentService {

    private final PaymentIntentRepository paymentIntentRepository;
    private final AccountRepository accountRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProviderClient providerClient;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentIntentRepository paymentIntentRepository,
                           AccountRepository accountRepository,
                           OutboxEventRepository outboxEventRepository,
                           ProviderClient providerClient,
                           ObjectMapper objectMapper) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.accountRepository = accountRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.providerClient = providerClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Шаг 1. Сетевой вызов к провайдеру — до сохранения и вне транзакции:
     * задержка провайдера не должна держать коннекшн из пула (Day 17).
     * save() не обёрнут явной @Transactional: ровно одна DB-операция,
     * SimpleJpaRepository уже транзакционен сам по себе на каждый свой метод.
     */
    public PaymentIntentResponse createIntent(PaymentInitRequest request) {
        ProviderAccountInfo providerAccount = providerClient.lookup(
                request.getProviderCode(), request.getProviderAccountNumber());

        PaymentIntent intent = paymentIntentRepository.save(new PaymentIntent(
                request.getAccountId(),
                request.getProviderCode(),
                request.getProviderAccountNumber(),
                providerAccount.displayName()));

        return toResponse(intent);
    }

    /**
     * Шаг 2. Explicit-транзакция: больше одной DB-операции (статус intent +
     * баланс счёта + outbox), которые обязаны коммититься/откатываться вместе.
     *
     * Row lock на PaymentIntent защищает от двойного тапа той же природы, что
     * lost update на балансе (Day 2-4): без lock'а два конкурентных confirm
     * одного intent оба прочитали бы статус CREATED и оба бы списали деньги.
     * После лока второй вызов видит уже COMPLETED и возвращается идемпотентным
     * no-op, не трогая баланс второй раз — PaymentIntent.id играет ту же роль,
     * что idempotency_key у Transfer (Day 5), только не требует unique constraint,
     * потому что id уже сам по себе первичный ключ, выданный сервером на шаге 1.
     */
    @Transactional
    public PaymentIntentResponse confirmPayment(UUID paymentId, Long amount) {
        PaymentIntent intent = paymentIntentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment intent not found: " + paymentId));

        if (intent.getStatus() == PaymentStatus.COMPLETED) {
            return toResponse(intent);
        }

        Account account = accountRepository.findByIdForUpdate(intent.getAccountId()).orElseThrow();
        if (account.getBalance() < amount) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
        account.setBalance(account.getBalance() - amount);
        intent.confirm(amount);

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                intent.getId(),
                intent.getAccountId(),
                intent.getProviderCode(),
                intent.getProviderAccountNumber(),
                amount,
                Instant.now());

        outboxEventRepository.save(new OutboxEvent(
                KafkaTopics.PAYMENT_COMPLETED,
                String.valueOf(intent.getAccountId()),
                objectMapper.writeValueAsString(event)));

        return toResponse(intent);
    }

    // Статус домена (PaymentStatus) и статус контракта (PaymentIntentResponse.StatusEnum) —
    // сознательно разные enum'ы: спека не должна меняться каждый раз, когда меняется домен,
    // и наоборот. Имена сейчас совпадают, valueOf(name()) — явная, а не случайная связь.
    private PaymentIntentResponse toResponse(PaymentIntent intent) {
        return new PaymentIntentResponse(intent.getId(), PaymentIntentResponse.StatusEnum.valueOf(intent.getStatus().name()))
                .providerDisplayName(intent.getProviderDisplayName())
                .amount(intent.getAmount());
    }
}
