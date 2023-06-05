package playground.common.messaging.idempotency

import playground.common.idempotency.IdempotencyContext
import playground.common.idempotency.impl.GenericProperty
import playground.common.messaging.MessageMetadata
import playground.common.messaging.publisher.message
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.Message
import playground.common.idempotency.IdempotencyAssertionError

/**
 * With this [PublishStrategy] all publishing happens at the end of the final phase,
 * where a standalone Kafka transaction is created. Its best suited for REST handlers.
 *
 * When the final phase completes successfully, messages are not republished, no matter the retries.
 * However, in the very unlikely scenario where kafka transaction succeeds, but database transaction does not,
 * it may happen that the messages are published multiple times. Consumers should be able to handle that.
 *
 * To improve code readability, it is also possible call [IdempotencyContext.publish] anywhere outside phase
 * (and before final phase). Doing so, will put the message into queue (stored on the context instance) and
 * automatically publish at the end of final phase.
 *
 * To sum up, [IdempotencyContext.publish] can NOT be called:
 * - inside a phase that is not final
 * - after a final phase
 */
class PublishingInFinalPhase(
    private val producerOnlyKafkaTemplate: KafkaTemplate<Any, Any>
) : PublishStrategy {

    private val IdempotencyContext.queuedRequests: MutableList<Message<*>> by GenericProperty(PROPERTY_NAMESPACE) {
        mutableListOf()
    }

    override fun <T : Any> publish(
        context: IdempotencyContext,
        topicName: String,
        key: String,
        payload: T,
        metadata: MessageMetadata?
    ) {
        if (context.wasFinalPhase) throw IdempotencyAssertionError(
            "It is not possible to publish after final phase"
        )
        if (context.phaseData != null && !context.phaseData!!.final) throw IdempotencyAssertionError(
            "It is not possible to publish inside non final phase"
        )
        validateDestinationIfTombstone(payload, topicName)
        context.queuedRequests.add(
            message(topicName, key, payload, metadata)
        )
    }

    override fun <R> wrapBody(data: IdempotencyContext.PhaseData, body: () -> R): () -> R =
        if (data.final) {
            {
                val res = body()
                if (data.context.queuedRequests.isNotEmpty()) {
                    producerOnlyKafkaTemplate.executeInTransaction {
                        data.context.queuedRequests.forEach {
                            producerOnlyKafkaTemplate.send(it)
                        }
                    }
                }
                res
            }
        } else body
}
