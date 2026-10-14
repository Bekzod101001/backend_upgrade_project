package uz.minibank.transfer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import uz.minibank.account.Account;
import uz.minibank.account.AccountRepository;
import uz.minibank.kafka.KafkaTopics;
import uz.minibank.outbox.OutboxEvent;
import uz.minibank.outbox.OutboxEventRepository;
import uz.minibank.transfer.event.TransferCompletedEvent;
import uz.minibank.transfer.exception.InsufficientBalanceException;

import java.time.Instant;
import java.util.UUID;

/**
 * Одна транзакция: запись перевода + движение денег + намерение опубликовать событие.
 * Живёт отдельным бином, потому что обработка повтора обязана быть СНАРУЖИ этой транзакции:
 * после unique_violation транзакция помечена rollback-only и читать в ней уже нельзя.
 */
@Service
public class TransferExecutor {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public TransferExecutor(AccountRepository accountRepository,
                            TransferRepository transferRepository,
                            OutboxEventRepository outboxEventRepository,
                            ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void execute(UUID idempotencyKey, Long fromId, Long toId, Long amount) {
        // INSERT идёт первым: дубль отваливается на самом дешёвом шаге,
        // не успев взять lock'и на счета и подержать их до конца транзакции.
        // GenerationType.IDENTITY заставляет Hibernate выполнить INSERT сразу, а не отложить до commit.
        Transfer transfer = transferRepository.save(new Transfer(idempotencyKey, fromId, toId, amount));

        // Единый порядок захвата по id — защита от deadlock на встречных переводах (Day 4)
        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);

        Account firstAccount = accountRepository.findByIdForUpdate(firstId).orElseThrow();
        Account secondAccount = accountRepository.findByIdForUpdate(secondId).orElseThrow();

        Account fromAccount = firstId.equals(fromId) ? firstAccount : secondAccount;
        Account toAccount = firstId.equals(fromId) ? secondAccount : firstAccount;

        if (fromAccount.getBalance() < amount) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + amount);

        // OUTBOX: событие пишем В ТУ ЖЕ транзакцию, что и деньги — INSERT в outbox
        // либо закоммитится вместе с UPDATE'ами балансов, либо откатится вместе с ними.
        // Дальше событие живёт в БД, а не в памяти процесса: публикация — забота
        // отдельного воркера (OutboxRelay), и ему для этого не нужна открытая транзакция.
        //
        // eventId генерируется ОДИН РАЗ здесь и уходит в JSON. Сколько бы раз воркер
        // ни переотправлял эту строку, eventId будет тем же самым — консьюмер сможет
        // отличить повторную доставку от нового события.
        TransferCompletedEvent event = new TransferCompletedEvent(
                UUID.randomUUID(),
                transfer.getId(),
                fromId,
                toId,
                amount,
                Instant.now());

        outboxEventRepository.save(new OutboxEvent(
                KafkaTopics.TRANSFER_COMPLETED,
                String.valueOf(fromId),
                objectMapper.writeValueAsString(event)));
    }
}
