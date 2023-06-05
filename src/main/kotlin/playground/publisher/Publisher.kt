package playground.publisher

import org.apache.avro.generic.GenericContainer
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import playground.common.messaging.MessageMetadata

/**
 * Topic Publisher
 *
 * Use this if you prefer having topics wired in your config class
 */
@Component
class Publisher<TYPE : Any>(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
//    @Qualifier("kafkaTemplate") private val kafkaTemplate: KafkaTemplate<Any, Any>,
//    @Qualifier("producerOnlyKafkaTemplate") private val producerOnlyKafkaTemplate: KafkaTemplate<Any, Any>,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Uses the transaction provided by Kafka Container
     *
     * To be used when invoked within a @KafkaListener processor.
     **/
    open fun publish(topic: String, key: String, payload: TYPE, metadata: MessageMetadata? = null) {
        kafkaTemplate.send(message(topic, key, payload, metadata))
        logger.info("Sent: $payload")

    }

    open fun publish(topic: String, key: String, payload: String, metadata: MessageMetadata? = null) {
        kafkaTemplate.send(topic, payload)
    }

    /**
     * Publishes message in a new transaction
     *
     * To be used when invoked from a REST Controller or Integration Test.
     *
     * When used in @KafkaListener processor, message will be published in a separate transaction
     **/
//    @Transactional(PRODUCER_ONLY_KAFKA_TRANSACTION_MANAGER)
//    open fun publishInTransaction(topic: String, key: String, payload: TYPE, metadata: MessageMetadata? = null) {
//        producerOnlyKafkaTemplate.executeInTransaction {
//            producerOnlyKafkaTemplate.send(message(topic, key, payload, metadata))
//        }
//    }
}

fun <T : Any> message(topic: String, key: String, message: T, metadata: MessageMetadata?): Message<T> {

    if (message is GenericContainer && metadata == null) {
        throw IllegalArgumentException("Metadata required for AVRO messages (key=$key, payloadType=${message::class.simpleName}")
    }

    val messageBuilder = MessageBuilder
        .withPayload(message)
        .setHeader(KafkaHeaders.TOPIC, topic)
        .setHeader(KafkaHeaders.KEY, key)

//    metadata?.let {
//        messageBuilder.setHeader(MoxHeaders.IDEMPOTENCY_KEY, metadata.idempotencyKey)
//        messageBuilder.setHeader(MoxHeaders.TIMESTAMP, metadata.timestamp)
//        metadata.customerId?.let { messageBuilder.setHeader(MoxHeaders.CUSTOMER_ID, it) }
//    }

    return messageBuilder.build()
}

const val KAFKA_TRANSACTION_MANAGER = "kafkaTransactionManager"
const val PRODUCER_ONLY_KAFKA_TRANSACTION_MANAGER = "producerOnly-kafkaTransactionManager"