package playground.common.idempotencyjpa.jpa

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "commons_idempotency_lock")
data class IdempotencyLock(
    @EmbeddedId
    val key: IdempotencyPK,

    val operationId: String,

    val lockedUntil: Instant
)
