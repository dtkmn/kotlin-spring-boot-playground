package playground.common.messaging.publisher

import playground.common.messaging.MessageMetadata
import playground.common.messaging.config.PRODUCER_ONLY_KAFKA_TRANSACTION_MANAGER
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.annotation.Transactional

/**
 * Topic Publisher
 *
 * Use this if you prefer having topics wired in your config class
 */
open class TopicPublisher<TYPE : Any>(
    @Qualifier("kafkaTemplate") private val kafkaTemplate: KafkaTemplate<Any, Any>,
    @Qualifier("producerOnlyKafkaTemplate") private val producerOnlyKafkaTemplate: KafkaTemplate<Any, Any>,
    private val topic: String
) {

    /**
     * Uses the transaction provided by Kafka Container
     *
     * To be used when invoked within a @KafkaListener processor.
     **/
    open fun publish(key: String, payload: TYPE, metadata: MessageMetadata? = null) {

        kafkaTemplate.send(message(topic, key, payload, metadata))
    }

    /**
     * Publishes message in a new transaction
     *
     * To be used when invoked from a REST Controller or Integration Test.
     *
     * When used in @KafkaListener processor, message will be published in a separate transaction
     **/
    @Transactional(PRODUCER_ONLY_KAFKA_TRANSACTION_MANAGER)
    open fun publishInTransaction(key: String, payload: TYPE, metadata: MessageMetadata? = null) {
        producerOnlyKafkaTemplate.executeInTransaction {
            producerOnlyKafkaTemplate.send(message(topic, key, payload, metadata))
        }
    }
}
