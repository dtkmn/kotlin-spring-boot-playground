package playground.common.messaging

import java.time.Instant
import java.util.UUID

data class MessageMetadata(
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val timestamp: Instant = Instant.now(),
    val customerId: String? = null
)
