package playground.common.idempotency.impl

import java.time.Duration
import java.time.Instant

/**
 * TODO(APS-680): Document
 */
interface IdempotencyLockAdapter {

    fun obtainLock(key: QualifiedIdempotencyKey, timeout: Duration, now: Instant): Result

    interface Lock : AutoCloseable

    sealed class Result {
        data class Obtained(val lock: Lock) : Result()
        data class Locked(val tryAt: Instant) : Result()
    }
}
