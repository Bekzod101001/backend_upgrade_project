package uz.minibank.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)                 // ← «читай с for update»
    @Query("select a from Account a where a.id = :id")    // обычный запрос по id
    Optional<Account> findByIdForUpdate(Long id);
}
