package uz.minibank.transfer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import uz.minibank.TestcontainersConfiguration;
import uz.minibank.account.Account;
import uz.minibank.account.AccountRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TransferServiceTest {

    // Счёт с таким владельцем Postgres откажется обновлять (см. installCreditFailureTrigger)
    private static final String CREDIT_FAILS = "CREDIT_FAILS";

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void dropCreditFailureTrigger() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_credit ON accounts");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS fail_credit()");
    }

    @Test
    void transferMovesMoneyBetweenAccounts() {
        Account from = accountRepository.save(new Account("Alice", 1_000_000_00L));
        Account to = accountRepository.save(new Account("Bob", 500_000_00L));

        transferService.transfer(UUID.randomUUID(), from.getId(), to.getId(), 200_000_00L);

        assertThat(balanceOf(from)).isEqualTo(800_000_00L);
        assertThat(balanceOf(to)).isEqualTo(700_000_00L);
    }

    @Test
    void failedCreditDoesNotLoseMoney() {
        Account from = accountRepository.save(new Account("Alice", 1_000_000_00L));
        Account to = accountRepository.save(new Account(CREDIT_FAILS, 500_000_00L));
        long totalBefore = balanceOf(from) + balanceOf(to);

        installCreditFailureTrigger();

        // Списание проходит, зачисление падает в Postgres
        assertThatThrownBy(() -> transferService.transfer(UUID.randomUUID(), from.getId(), to.getId(), 200_000_00L))
                .rootCause()
                .hasMessageContaining("simulated credit failure");

        // Инвариант: деньги не появляются и не исчезают
        assertThat(balanceOf(from) + balanceOf(to)).isEqualTo(totalBefore);
        assertThat(balanceOf(from)).isEqualTo(1_000_000_00L);
    }

    private long balanceOf(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow().getBalance();
    }

    private void installCreditFailureTrigger() {
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION fail_credit() RETURNS trigger AS $$
                BEGIN
                    IF NEW.owner_name = '%s' THEN
                        RAISE EXCEPTION 'simulated credit failure';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """.formatted(CREDIT_FAILS));
        jdbcTemplate.execute("""
                CREATE TRIGGER fail_credit
                BEFORE UPDATE ON accounts
                FOR EACH ROW EXECUTE FUNCTION fail_credit()
                """);
    }
}
