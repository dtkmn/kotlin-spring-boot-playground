package playground.common.messaging.idempotency

import playground.common.idempotency.IdempotencyContext
import playground.common.idempotency.impl.GenericProperty
import playground.common.messaging.MessageMetadata
import playground.common.messaging.publisher.message
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import playground.common.idempotency.IdempotencyAssertionError

/**
 * Similar to PublishingInFinalPhase but sends messages at the end of current phase.
 * Groups all messages published with publish method and send all in one transaction at the end of current phase.
 */
@Component
class PublishingAtTheEndOfCurrentPhase(
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
        if (context.phaseData == null) throw IdempotencyAssertionError(
            "It is not possible to publish outside phase"
        )
        validateDestinationIfTombstone(payload, topicName)
        context.queuedRequests.add(
            message(topicName, key, payload, metadata)
        )
    }

    override fun <R> wrapBody(data: IdempotencyContext.PhaseData, body: () -> R): () -> R = {
        val res = body()
        if (data.context.queuedRequests.isNotEmpty()) {
            producerOnlyKafkaTemplate.executeInTransaction {
                data.context.queuedRequests.forEach {
                    producerOnlyKafkaTemplate.send(it)
                }
            }
            data.context.queuedRequests.clear()
        }
        res
    }
}
