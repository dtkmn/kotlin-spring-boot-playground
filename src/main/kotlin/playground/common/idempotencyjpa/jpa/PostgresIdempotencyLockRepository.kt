package playground.common.idempotencyjpa.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import playground.common.idempotencyjpa.jpa.IdempotencyLock
import playground.common.idempotencyjpa.jpa.IdempotencyPK
import java.time.Instant

/**
 * Provides Postgres specific, single query operations for implementing [PostgresIdempotencyLockAdapter].
 */
interface PostgresIdempotencyLockRepository : JpaRepository<IdempotencyLock, IdempotencyPK> {

    @Transactional
    @Modifying
    @Query(
        nativeQuery = true,
        value = """
        INSERT INTO commons_idempotency_lock(process_id,idempotency_key,record_id,operation_id,locked_until)
        VALUES (:process_id, :idempotency_key, :record_id, :operation_id, :until)
        ON CONFLICT ON CONSTRAINT commons_idempotency_lock_pk
        DO UPDATE SET locked_until=:until, operation_id=:operation_id
        WHERE commons_idempotency_lock.locked_until < :now
    """
    )
    fun obtainLock(
        @Param("process_id") processId: String,
        @Param("idempotency_key") idempotencyKey: String,
        @Param("record_id") recordId: String,
        @Param("operation_id") operationId: String,
        @Param("now") now: Instant,
        @Param("until") until: Instant
    ): Int

    @Transactional
    @Modifying
    @Query(
        nativeQuery = true,
        value = """
        DELETE FROM commons_idempotency_lock
        WHERE commons_idempotency_lock.process_id = :process_id
        AND commons_idempotency_lock.idempotency_key = :idempotency_key
        AND commons_idempotency_lock.record_id = :record_id
        AND commons_idempotency_lock.operation_id = :operation_id
    """
    )
    fun releaseLock(
        @Param("process_id") processId: String,
        @Param("idempotency_key") idempotencyKey: String,
        @Param("record_id") recordId: String,
        @Param("operation_id") operationId: String
    ): Int

    @Transactional
    @Modifying
    @Query("delete from IdempotencyLock where lockedUntil < :deleteThreshold")
    fun deleteWhereLockedUntilBefore(deleteThreshold: Instant): Int
}
