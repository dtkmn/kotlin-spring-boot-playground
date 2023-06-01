package playground.common.idempotencyjpa.jpa

import playground.common.idempotencyjpa.logging.LOG_EVENT_UNABLE_OBTAIN_LOCK
import playground.common.idempotency.impl.IdempotencyLockAdapter
import playground.common.idempotency.impl.QualifiedIdempotencyKey
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * [IdempotencyLockAdapter] that depends on Spring JPA with PostgresSQL database.
 */
class PostgresIdempotencyLockAdapter(
    val repository: PostgresIdempotencyLockRepository
) : IdempotencyLockAdapter {

    private val log: Logger = LoggerFactory.getLogger(PostgresIdempotencyLockAdapter::class.java)

    override fun obtainLock(
        key: QualifiedIdempotencyKey,
        timeout: Duration,
        now: Instant
    ): IdempotencyLockAdapter.Result {
        /** HACK: There are several retries, see [tryObtainLock] for more details */
        for (retry in 1..3) {
            tryObtainLock(key, timeout, now)?.let { return it }
        }
        log.error(
            "We were unable to obtain lock nor retrieve lock object in three attempts." +
                "This should not be happening. Please report to commons library owner.",
            kv(MDC_KEY_LOG_EVENT, LOG_EVENT_UNABLE_OBTAIN_LOCK)
        )
        // We do not fail on purpose and advice upstream to repeat immediately
        return IdempotencyLockAdapter.Result.Locked(now)
    }

    /**
     * Tries to obtain lock as specified in [IdempotencyLockAdapter.obtainLock],
     * but in some rare circumstances we are unable to get a proper response.
     * This happens when the old lock was removed shortly before or during an attempt to obtain a new one.
     * In that case we return `null` and advise caller to retry.
     */
    private fun tryObtainLock(
        key: QualifiedIdempotencyKey,
        timeout: Duration,
        now: Instant
    ): IdempotencyLockAdapter.Result? {
        val operationId = UUID.randomUUID()
        val updatedLines = repository.obtainLock(
            key.processId, key.idempotencyKey, key.recordId,
            operationId.toString(), now, now + timeout
        )
        return if (updatedLines == 1) {
            IdempotencyLockAdapter.Result.Obtained(Lock(operationId, key))
        } else {
            repository.findById(IdempotencyPK(key)).map {
                IdempotencyLockAdapter.Result.Locked(it.lockedUntil)
            }.orElse(null)
        }
    }

    inner class Lock(val operationId: UUID, val key: QualifiedIdempotencyKey) : IdempotencyLockAdapter.Lock {
        override fun close() {
            repository.releaseLock(
                key.processId, key.idempotencyKey, key.recordId, operationId.toString()
            )
        }
    }
}
