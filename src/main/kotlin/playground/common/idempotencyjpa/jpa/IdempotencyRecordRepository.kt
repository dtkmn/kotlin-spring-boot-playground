package playground.common.idempotencyjpa.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import playground.common.idempotencyjpa.jpa.IdempotencyPK
import playground.common.idempotencyjpa.jpa.IdempotencyRecord
import java.time.Instant

interface IdempotencyRecordRepository : JpaRepository<IdempotencyRecord, IdempotencyPK> {

    @Transactional
    @Modifying
    @Query(
        nativeQuery = true,
        value = "delete from commons_idempotency_record where (process_id, idempotency_key, record_id) in " +
            "(select process_id, idempotency_key, record_id from commons_idempotency_record where created_at < :deleteThreshold limit :limit)"
    )
    fun deleteWhereCreatedAtBefore(deleteThreshold: Instant, limit: Int): Int

    @Transactional
    @Query(
        nativeQuery = true,
        value = "SELECT lo_unlink(lom.oid) FROM pg_largeobject_metadata lom " +
            "left join commons_idempotency_record ir on lom.oid = ir.data\\:\\:oid where ir.created_at < :deleteThreshold limit :limit"
    )
    fun cleanIdempotencyLargeObjects(deleteThreshold: Instant, limit: Int): List<Int>

    @Query(
        nativeQuery = true,
        value = "select max(created_at) from " +
            "(select created_at from commons_idempotency_record order by created_at limit :nth) as sub"
    )
    fun getNthIdempotencyTimestamp(nth: Int): Instant
}
