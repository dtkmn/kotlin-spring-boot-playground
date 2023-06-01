package playground.common.messaging.idempotency

import playground.common.idempotency.IdempotencyAssertionError
import playground.common.idempotency.IdempotencyContext
import playground.common.messaging.MessageMetadata

/**
 * You should use this [PublishStrategy] to explicitly state you do not plan to publish in the process.
 * Every call to [IdempotencyContext.publish] will throw [IdempotencyAssertionError].
 */
class NoPublishing : PublishStrategy {

    override fun <T : Any> publish(context: IdempotencyContext, topicName: String, key: String, payload: T, metadata: MessageMetadata?) {
        throw IdempotencyAssertionError(
            "It is not possible to publish with NoPublishing strategy"
        )
    }
}
