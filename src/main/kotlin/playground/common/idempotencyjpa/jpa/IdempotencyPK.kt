package playground.common.idempotencyjpa.jpa

import jakarta.persistence.Embeddable
import playground.common.idempotency.impl.QualifiedIdempotencyKey
import java.io.Serializable

@Embeddable
data class IdempotencyPK(
    val processId: String,
    val idempotencyKey: String,
    val recordId: String
) : Serializable {
    constructor(key: QualifiedIdempotencyKey) : this(key.processId, key.idempotencyKey, key.recordId)
}
