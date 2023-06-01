package playground.common.messaging.publisher

import playground.common.messaging.MessageMetadata
import playground.common.messaging.MoxHeaders
import playground.common.messaging.config.PRODUCER_ONLY_KAFKA_TRANSACTION_MANAGER
import org.apache.avro.generic.GenericContainer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.transaction.annotation.Transactional

/**
 * Publisher
 *
 * Use this if you prefer having topics wired in your processor/controller
 */
open class Publisher(
    @Qualifier("kafkaTemplate") private val kafkaTemplate: KafkaTemplate<Any, Any>,
    @Qualifier("producerOnlyKafkaTemplate") private val producerOnlyKafkaTemplate: KafkaTemplate<Any, Any>
) {

    /**
     * Uses the transaction provided by Kafka Container
     *
     * To be used when invoked within a @KafkaListener processor.
     **/
    open fun publish(topic: String, key: String, message: Any, metadata: MessageMetadata? = null) {
        kafkaTemplate.send(message(topic, key, message, metadata))
    }

    /**
     * Publishes message in a new transaction
     *
     * To be used when invoked from a REST Controller or Integration Test.
     *
     * When used in @KafkaListerner processor, message will be published in a seperate transaction
     **/
    @Transactional(PRODUCER_ONLY_KAFKA_TRANSACTION_MANAGER)
    open fun publishInTransaction(topic: String, key: String, message: Any, metadata: MessageMetadata? = null) {
        producerOnlyKafkaTemplate.executeInTransaction {
            producerOnlyKafkaTemplate.send(message(topic, key, message, metadata))
        }
    }
}

fun <T : Any> message(topic: String, key: String, message: T, metadata: MessageMetadata?): Message<T> {

    if (message is GenericContainer && metadata == null) {
        throw IllegalArgumentException("Metadata required for AVRO messages (key=$key, payloadType=${message::class.simpleName}")
    }

    val messageBuilder = MessageBuilder
        .withPayload(message)
        .setHeader(KafkaHeaders.TOPIC, topic)
        .setHeader(KafkaHeaders.KEY, key)

    metadata?.let {
        messageBuilder.setHeader(MoxHeaders.IDEMPOTENCY_KEY, metadata.idempotencyKey)
        messageBuilder.setHeader(MoxHeaders.TIMESTAMP, metadata.timestamp)
        metadata.customerId?.let { messageBuilder.setHeader(MoxHeaders.CUSTOMER_ID, it) }
    }

    return messageBuilder.build()
}

fun <T : Any> messageWithStringReturn(topic: String, key: String, message: T, metadata: MessageMetadata?): String {

    if (message is GenericContainer && metadata == null) {
        throw IllegalArgumentException("Metadata required for AVRO messages (key=$key, payloadType=${message::class.simpleName}")
    }

    val messageBuilder = MessageBuilder
        .withPayload(message)
        .setHeader(KafkaHeaders.TOPIC, topic)
        .setHeader(KafkaHeaders.KEY, key)

    metadata?.let {
        messageBuilder.setHeader(MoxHeaders.IDEMPOTENCY_KEY, metadata.idempotencyKey)
        messageBuilder.setHeader(MoxHeaders.TIMESTAMP, metadata.timestamp)
        metadata.customerId?.let { messageBuilder.setHeader(MoxHeaders.CUSTOMER_ID, it) }
    }

    return messageBuilder.build().toString()
}
