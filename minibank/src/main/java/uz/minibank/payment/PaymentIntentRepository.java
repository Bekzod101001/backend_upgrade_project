package uz.minibank.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)                 // ← «читай с for update», та же защита, что у AccountRepository
    @Query("select p from PaymentIntent p where p.id = :id")
    Optional<PaymentIntent> findByIdForUpdate(UUID id);
}
