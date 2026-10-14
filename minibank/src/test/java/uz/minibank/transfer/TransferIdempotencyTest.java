package uz.minibank.transfer;

import uz.minibank.transfer.exception.IdempotencyKeyConflictException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import uz.minibank.TestcontainersConfiguration;
import uz.minibank.account.Account;
import uz.minibank.account.AccountRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TransferIdempotencyTest {

    private static final long INITIAL_BALANCE = 1_000_000_00L;
    private static final long AMOUNT = 200_000_00L;

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    /**
     * Сценарий A: клиент отправил перевод, ответ не дошёл (обрыв сети), клиент сделал retry.
     * Одно намерение пользователя доехало до сервера дважды — деньги должны двинуться один раз.
     */
    @Test
    void retriedTransferIsAppliedOnce() {
        Account alice = accountRepository.save(new Account("Alice", INITIAL_BALANCE));
        Account bob = accountRepository.save(new Account("Bob", 0L));

        UUID key = UUID.randomUUID();

        transferService.transfer(key, alice.getId(), bob.getId(), AMOUNT);
        // Тот же ключ = тот же перевод. Повтор не должен двигать деньги и не должен падать:
        // клиент не сделал ничего плохого, для него операция уже успешна.
        transferService.transfer(key, alice.getId(), bob.getId(), AMOUNT);

        System.out.printf("""
                        === retried transfer (same key) ===
                        Alice: %d (ожидание: %d)
                        Bob:   %d (ожидание: %d)
                        %n""",
                balanceOf(alice), INITIAL_BALANCE - AMOUNT, balanceOf(bob), AMOUNT);

        assertThat(balanceOf(alice)).isEqualTo(INITIAL_BALANCE - AMOUNT);
        assertThat(balanceOf(bob)).isEqualTo(AMOUNT);
    }

    /**
     * Сценарий B: Алиса честно перевела Бобу дважды подряд одну и ту же сумму.
     * Payload идентичен сценарию A, но намерения два — значит и переводов должно быть два.
     * Этот тест зелёный уже сейчас; он страхует от «фикса», который просто режет дубли по содержимому.
     */
    @Test
    void differentKeysWithSamePayloadAreBothApplied() {
        Account alice = accountRepository.save(new Account("Alice", INITIAL_BALANCE));
        Account bob = accountRepository.save(new Account("Bob", 0L));

        transferService.transfer(UUID.randomUUID(), alice.getId(), bob.getId(), AMOUNT);
        transferService.transfer(UUID.randomUUID(), alice.getId(), bob.getId(), AMOUNT);

        assertThat(balanceOf(alice)).isEqualTo(INITIAL_BALANCE - 2 * AMOUNT);
        assertThat(balanceOf(bob)).isEqualTo(2 * AMOUNT);
    }

    /**
     * Тот же ключ с другим payload — это не retry: клиент переиспользовал ключ (баг)
     * либо кто-то пытается подменить сумму. Ни провести, ни промолчать нельзя.
     */
    @Test
    void sameKeyWithDifferentPayloadIsRejected() {
        Account alice = accountRepository.save(new Account("Alice", INITIAL_BALANCE));
        Account bob = accountRepository.save(new Account("Bob", 0L));

        UUID key = UUID.randomUUID();
        transferService.transfer(key, alice.getId(), bob.getId(), AMOUNT);

        assertThatThrownBy(() -> transferService.transfer(key, alice.getId(), bob.getId(), 900_000_00L))
                .isInstanceOf(IdempotencyKeyConflictException.class);

        assertThat(balanceOf(alice)).isEqualTo(INITIAL_BALANCE - AMOUNT);
        assertThat(balanceOf(bob)).isEqualTo(AMOUNT);
    }

    /**
     * Двойной тап: обе попытки летят одновременно, первая ещё не закоммичена, когда приходит вторая.
     * Последовательный тест этого не ловит — там вторая попытка видит уже закоммиченный ключ.
     */
    @Test
    void concurrentDuplicatesAreAppliedOnce() throws Exception {
        int threads = 5;
        Account alice = accountRepository.save(new Account("Alice", INITIAL_BALANCE));
        Account bob = accountRepository.save(new Account("Bob", 0L));

        UUID key = UUID.randomUUID();
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        transferService.transfer(key, alice.getId(), bob.getId(), AMOUNT);
                    } catch (Throwable e) {
                        errors.add(e);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        }

        System.out.printf("""
                        === %d параллельных попыток с одним ключом ===
                        errors:          %d %s
                        строк в transfers: %d
                        Alice: %d (ожидание: %d)
                        Bob:   %d (ожидание: %d)
                        %n""",
                threads, errors.size(), errors.stream().map(e -> e.getClass().getSimpleName()).distinct().toList(),
                transferRepository.countByFromAccountId(alice.getId()),
                balanceOf(alice), INITIAL_BALANCE - AMOUNT, balanceOf(bob), AMOUNT);

        // Ни одна попытка не должна упасть: для клиента это один и тот же перевод
        assertThat(errors).isEmpty();
        // Деньги двинулись ровно один раз
        assertThat(balanceOf(alice)).isEqualTo(INITIAL_BALANCE - AMOUNT);
        assertThat(balanceOf(bob)).isEqualTo(AMOUNT);
        // И в истории ровно одна запись
        assertThat(transferRepository.findByIdempotencyKey(key)).isPresent();
        assertThat(transferRepository.countByFromAccountId(alice.getId())).isEqualTo(1);
    }

    private long balanceOf(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow().getBalance();
    }
}
