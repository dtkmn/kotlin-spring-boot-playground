package playground.common.messaging.publisher

import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import playground.common.messaging.logging.LOG_EVENT_NOT_MOX_KAFKA_PRODUCER_FACTORY
import playground.common.messaging.logging.LOG_EVENT_RETRYING_TRANSACTION
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.errors.InvalidPidMappingException
import org.apache.kafka.common.errors.UnknownProducerIdException
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaOperations
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import playground.common.messaging.producer.MoxProducerOnlyKafkaProducerFactory

/**
 * Wrapper to KafkaTemplate with retry on executeInTransaction when failing on producer transient exceptions
 * UnknownProducerIdException or InvalidPidMappingException.
 * Additionally, it will also retry for KafkaException("The producer closed forcefully")
 * as from spring-kafka 2.3.8.RELEASE UnknownProducerIdException is caught in producer.send() and producer is closed.
 * Number of retries is based on remaining producers size in cache (which can also fail).
 * Important: This class should be used only for producer only kafka templates.
 */
open class MoxProducerOnlyKafkaTemplate<K, V>(producerFactory: ProducerFactory<K, V>) :
    KafkaTemplate<K, V>(producerFactory) {

    private val log = LoggerFactory.getLogger(javaClass)!!

    override fun <T : Any> executeInTransaction(callback: KafkaOperations.OperationsCallback<K, V, T>): T {
        try {
            return super.executeInTransaction(callback)
        } catch (e: KafkaException) {
            handleException(e)
        }
        return doRetries(callback)
    }

    private fun <T : Any?> doRetries(
        callback: KafkaOperations.OperationsCallback<K, V, T>
    ): T {
        // there can be other producers in cache which may fail
        // setting retry count to size + 2 - just to be sure we handle case when one additional producer is returned to cache which will fail
        var lastException: KafkaException? = null
        val retryCount = if (producerFactory is MoxProducerOnlyKafkaProducerFactory) {
            (producerFactory as MoxProducerOnlyKafkaProducerFactory<K, V>).getCacheCountForTransactionIdPrefix() + 2
        } else {
            log.error(
                "ProducerFactory is not MoxKafkaProducerFactory. Setting retry count to arbitrary value 10",
                kv(MDC_KEY_LOG_EVENT, LOG_EVENT_NOT_MOX_KAFKA_PRODUCER_FACTORY)
            )
            10
        }
        log.info("Will retry transaction max: $retryCount times")

        for (retry in 1..retryCount) {
            try {
                log.warn(
                    "Retrying transaction because of transient producer exception $lastException",
                    kv(MDC_KEY_LOG_EVENT, LOG_EVENT_RETRYING_TRANSACTION)
                )
                return super.executeInTransaction(callback)
            } catch (e: KafkaException) {
                lastException = handleException(e)
            }
        }
        throw lastException!!
    }

    private fun handleException(e: KafkaException): KafkaException? {
        return when {
            e is UnknownProducerIdException -> e
            e.cause is UnknownProducerIdException -> e
            e is InvalidPidMappingException -> e
            e.cause is InvalidPidMappingException -> e
            e.message == "The producer closed forcefully" -> e
            else -> throw e
        }
    }
}
