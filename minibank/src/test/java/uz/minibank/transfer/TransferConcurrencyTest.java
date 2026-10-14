package uz.minibank.transfer;

import uz.minibank.transfer.exception.InsufficientBalanceException;

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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TransferConcurrencyTest {

    private static final int THREADS = 10;
    private static final long INITIAL_BALANCE = 1_000_000_00L;
    private static final long AMOUNT = 200_000_00L;

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void concurrentTransfersDoNotSpendSameBalanceTwice() throws Exception {
        Account from = accountRepository.save(new Account("Alice", INITIAL_BALANCE));
        Account to = accountRepository.save(new Account("Bob", 0L));

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger insufficientBalance = new AtomicInteger();
        List<Throwable> otherErrors = new CopyOnWriteArrayList<>();

        // Все потоки ждут на start и стартуют одновременно
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);

        try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < THREADS; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        transferService.transfer(UUID.randomUUID(), from.getId(), to.getId(), AMOUNT);
                        succeeded.incrementAndGet();
                    } catch (InsufficientBalanceException e) {
                        insufficientBalance.incrementAndGet();
                    } catch (Throwable e) {
                        otherErrors.add(e);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        }

        long fromBalance = balanceOf(from);
        long toBalance = balanceOf(to);

        System.out.printf("""
                        === concurrent transfers ===
                        succeeded:            %d
                        insufficient balance: %d
                        other errors:         %d %s
                        Alice: %d, Bob: %d, total: %d
                        %n""",
                succeeded.get(), insufficientBalance.get(), otherErrors.size(),
                otherErrors.stream().map(e -> e.getClass().getSimpleName()).toList(),
                fromBalance, toBalance, fromBalance + toBalance);

        // Больше 5 переводов по 200 000 с 1 000 000 пройти не может
        assertThat(succeeded.get()).isLessThanOrEqualTo(5);
        // Каждый успешный перевод должен реально отразиться на обоих счетах
        assertThat(fromBalance).isEqualTo(INITIAL_BALANCE - succeeded.get() * AMOUNT);
        assertThat(toBalance).isEqualTo(succeeded.get() * AMOUNT);
        assertThat(fromBalance).isGreaterThanOrEqualTo(0);
    }

    @Test
    void oppositeTransfersDoNotDeadlock() throws Exception {
        int perDirection = 10;
        long amount = 10_000_00L;
        // Денег хватает на все переводы, InsufficientBalanceException не ожидается
        Account alice = accountRepository.save(new Account("Alice", INITIAL_BALANCE));
        Account bob = accountRepository.save(new Account("Bob", INITIAL_BALANCE));

        AtomicInteger aliceToBob = new AtomicInteger();
        AtomicInteger bobToAlice = new AtomicInteger();
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(perDirection * 2);

        try (ExecutorService executor = Executors.newFixedThreadPool(perDirection * 2)) {
            for (int i = 0; i < perDirection; i++) {
                // Чередуем направления, чтобы встречные переводы стартовали вперемешку
                executor.submit(() -> {
                    try {
                        start.await();
                        transferService.transfer(UUID.randomUUID(), alice.getId(), bob.getId(), amount);
                        aliceToBob.incrementAndGet();
                    } catch (Throwable e) {
                        errors.add(e);
                    } finally {
                        done.countDown();
                    }
                });
                executor.submit(() -> {
                    try {
                        start.await();
                        transferService.transfer(UUID.randomUUID(), bob.getId(), alice.getId(), amount);
                        bobToAlice.incrementAndGet();
                    } catch (Throwable e) {
                        errors.add(e);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        }

        long aliceBalance = balanceOf(alice);
        long bobBalance = balanceOf(bob);

        System.out.printf("""
                        === opposite transfers ===
                        Alice -> Bob succeeded: %d
                        Bob -> Alice succeeded: %d
                        errors:                 %d
                        %s
                        Alice: %d, Bob: %d, total: %d
                        %n""",
                aliceToBob.get(), bobToAlice.get(), errors.size(),
                errors.stream().map(TransferConcurrencyTest::describe).distinct().toList(),
                aliceBalance, bobBalance, aliceBalance + bobBalance);

        // Каждый успешный перевод отражён на обоих счетах, деньги не появились и не исчезли
        long net = (bobToAlice.get() - aliceToBob.get()) * amount;
        assertThat(aliceBalance).isEqualTo(INITIAL_BALANCE + net);
        assertThat(bobBalance).isEqualTo(INITIAL_BALANCE - net);
        // Денег хватает на все переводы: любой отказ здесь — ошибка конкурентности
        assertThat(errors).isEmpty();
    }

    // Тип исключения + сообщение исходной ошибки из Postgres
    private static String describe(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return e.getClass().getSimpleName() + " <- " + root.getMessage().lines().findFirst().orElse("");
    }

    private long balanceOf(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow().getBalance();
    }
}
