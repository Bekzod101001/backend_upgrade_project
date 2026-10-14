package uz.minibank.transfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByIdempotencyKey(UUID idempotencyKey);

    long countByFromAccountId(Long fromAccountId);
}
