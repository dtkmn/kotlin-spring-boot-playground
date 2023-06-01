package playground.common.messaging.listener

import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import playground.common.messaging.logging.LOG_EVENT_PRODUCER_SENT_NOT_IN_IO_THREAD
import playground.common.messaging.logging.LOG_EVENT_PRODUCER_ERROR
import playground.common.messaging.logging.MessagePayloadLogger
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.errors.ApiException
import org.apache.kafka.common.utils.KafkaThread
import org.slf4j.LoggerFactory
import org.springframework.kafka.support.ProducerListener
import java.lang.Exception

/**
 * Logs error on a message send failure.
 * Re-throws kafka ApiExceptions in addition to logging errors - this is only done when exception occur not in kafka io thread.
 * This feature is needed to rollback transaction when send fail inside KafkaProducer.send()
 * method with ApiException which is returned in Future and not used by KafkaTemplate at all.
 * https://projectdrgn.atlassian.net/browse/APS-1045
 *
 * This probably can be removed if original kafka error is solved: https://issues.apache.org/jira/browse/KAFKA-9279
 */
class MoxProducerListener<K, V>(private val messagePayloadLogger: MessagePayloadLogger) : ProducerListener<K, V> {

    val log = LoggerFactory.getLogger(javaClass)!!

    override fun onError(
        producerRecord: ProducerRecord<K, V>,
        recordMetadata: RecordMetadata?,
        exception: Exception?
    ) {
        log.error(
            "Exception thrown when sending a message to the topic ${recordMetadata?.topic()}, message key='${producerRecord.key()}', " +
                "value ${messagePayloadLogger.logOutputFor(producerRecord.value())}",
            kv(MDC_KEY_LOG_EVENT, LOG_EVENT_PRODUCER_ERROR),
            exception
        )

        if (exception is ApiException && Thread.currentThread() !is KafkaThread) {
            log.error(
                "Error on KafkaProducer.sent in not io thread - rethrowing exception to rollback transaction",
                kv(MDC_KEY_LOG_EVENT, LOG_EVENT_PRODUCER_SENT_NOT_IN_IO_THREAD)
            )
            throw exception
        }
    }
}
