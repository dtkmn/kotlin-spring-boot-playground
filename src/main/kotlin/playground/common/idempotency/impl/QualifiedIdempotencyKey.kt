package playground.common.idempotency.impl

data class QualifiedIdempotencyKey(
    val processId: String,
    val idempotencyKey: String,
    val recordId: String
)
