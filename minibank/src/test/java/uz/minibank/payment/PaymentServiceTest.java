package uz.minibank.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import uz.minibank.TestcontainersConfiguration;
import uz.minibank.account.Account;
import uz.minibank.account.AccountRepository;
import uz.minibank.api.model.PaymentInitRequest;
import uz.minibank.api.model.PaymentIntentResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void confirmDebitsAccountAndCompletesIntent() {
        Account account = accountRepository.save(new Account("Alice", 1_000_000_00L));

        PaymentIntentResponse intent = paymentService.createIntent(
                new PaymentInitRequest(account.getId(), "ELECTRICITY", "123456789"));

        assertThat(intent.getStatus()).isEqualTo(PaymentIntentResponse.StatusEnum.CREATED);
        assertThat(intent.getProviderDisplayName().get()).isEqualTo("Customer #123456789");

        PaymentIntentResponse confirmed = paymentService.confirmPayment(intent.getPaymentId(), 200_000_00L);

        assertThat(confirmed.getStatus()).isEqualTo(PaymentIntentResponse.StatusEnum.COMPLETED);
        assertThat(balanceOf(account)).isEqualTo(800_000_00L);
    }

    @Test
    void secondConfirmOnSameIntentIsIdempotentNoOp() {
        Account account = accountRepository.save(new Account("Bob", 1_000_000_00L));

        PaymentIntentResponse intent = paymentService.createIntent(
                new PaymentInitRequest(account.getId(), "MOBILE", "987654321"));

        paymentService.confirmPayment(intent.getPaymentId(), 300_000_00L);
        // Повторный confirm того же intent (двойной тап) не должен списать второй раз
        paymentService.confirmPayment(intent.getPaymentId(), 300_000_00L);

        assertThat(balanceOf(account)).isEqualTo(700_000_00L);
    }

    @Test
    void invalidProviderAccountNumberIsRejectedBeforeAnyDbWrite() {
        Account account = accountRepository.save(new Account("Carol", 1_000_000_00L));

        assertThatThrownBy(() -> paymentService.createIntent(
                new PaymentInitRequest(account.getId(), "ELECTRICITY", "not-a-number")))
                .hasMessageContaining("Unknown ELECTRICITY account");
    }

    @Test
    void confirmWithInsufficientBalanceLeavesIntentRetryable() {
        Account account = accountRepository.save(new Account("Dave", 100_00L));

        PaymentIntentResponse intent = paymentService.createIntent(
                new PaymentInitRequest(account.getId(), "ELECTRICITY", "111222333"));

        assertThatThrownBy(() -> paymentService.confirmPayment(intent.getPaymentId(), 200_000_00L))
                .hasMessageContaining("Insufficient balance");

        assertThat(balanceOf(account)).isEqualTo(100_00L);
    }

    private long balanceOf(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow().getBalance();
    }
}
