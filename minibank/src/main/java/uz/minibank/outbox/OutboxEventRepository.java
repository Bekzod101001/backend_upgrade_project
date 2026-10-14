package uz.minibank.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * FOR UPDATE SKIP LOCKED: если строку уже держит другой инстанс воркера
     * (второй под, параллельный тик), эта строка просто пропускается, а не ждёт lock.
     * Ровно то, что делает Laravel database-драйвер очередей под капотом.
     *
     * jakarta.persistence.lock.timeout = -2 — это SKIP_LOCKED в терминах Hibernate/JDBC.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select o from OutboxEvent o where o.processedAt is null order by o.id")
    List<OutboxEvent> findBatchForPublishing(Limit limit);
}
