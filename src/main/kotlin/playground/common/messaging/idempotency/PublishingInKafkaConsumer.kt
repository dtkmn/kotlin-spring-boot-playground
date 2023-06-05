package playground.common.messaging.idempotency

import playground.common.exception.SupportException
import playground.common.exception.error.ERR_SYS_CANNOT_SEND_TOMBSTONE_TO_NOT_SNAPSHOT_TOPIC
import playground.common.idempotency.IdempotencyContext
import playground.common.messaging.MessageMetadata
import playground.common.messaging.publisher.message
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaNull
import playground.common.idempotency.IdempotencyAssertionError

/**
 * With this [PublishStrategy] all publishing is part of Kafka consumer transaction that wraps
 * all the idempotency related code. This means, that messages are published
 * if and only if the input message is consumed. Consequently, when there are
 * several copies of input message, several copies output messages are published.
 *
 * It is not possible to call [IdempotencyContext.publish] inside phase, but you can and should use the phases
 * to construct the messages and then publish them outside phase. Note that in the moment Avro generated classes
 * are not Jackson serializable and therefore conversion must happen also outside the phase.
 */
class PublishingInKafkaConsumer(
    private val kafkaTemplate: KafkaTemplate<Any, Any>
) : PublishStrategy {

    override fun <T : Any> publish(
        context: IdempotencyContext,
        topicName: String,
        key: String,
        payload: T,
        metadata: MessageMetadata?
    ) {
        if (context.phaseData != null) throw IdempotencyAssertionError(
            "It is not possible to publish inside phase"
        )
        validateDestinationIfTombstone(payload, topicName)
        kafkaTemplate.send(message(topicName, key, payload, metadata))
    }
}

fun <T : Any> validateDestinationIfTombstone(payload: T, topicName: String) {
    if (payload == KafkaNull.INSTANCE && topicName.split('.').lastOrNull() != "snapshot") {
        throw SupportException(
            "Cannot send tombstone to other than snapshot topic: $topicName",
            ERR_SYS_CANNOT_SEND_TOMBSTONE_TO_NOT_SNAPSHOT_TOPIC
        )
    }
}
