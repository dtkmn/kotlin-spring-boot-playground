package playground.common.messaging.consumer

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeaders
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaOperations
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.lang.Nullable
import playground.common.messaging.DltHeaders
import playground.common.messaging.MoxHeaders
import playground.common.messaging.logging.LOG_EVENT_DEAD_LETTER_PUBLICATION_FAILED
import playground.common.messaging.logging.LOG_EVENT_DEAD_LETTER_PUBLICATION_SUCCEEDED
import playground.common.messaging.logging.LOG_EVENT_RECORD_HAS_MULTIPLE_HEADERS
import playground.common.messaging.logging.MessagePayloadLogger
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import java.util.function.BiFunction


class HeaderPreservingDeadLetterPublishingRecoverer(
    kafkaTemplate: KafkaOperations<Any, Any>,
    private val messageDebugLogger: MessagePayloadLogger
) : DeadLetterPublishingRecoverer(kafkaTemplate, BiFunction { cr, _ -> TopicPartition(cr.topic() + ".DLT", -1) }) {

    private val log = LoggerFactory.getLogger(HeaderPreservingDeadLetterPublishingRecoverer::class.java)

    override fun createProducerRecord(
        record: ConsumerRecord<*, *>,
        topicPartition: TopicPartition,
        headers: Headers,
        @Nullable key: ByteArray?,
        @Nullable value: ByteArray?
    ): ProducerRecord<Any, Any> {
        val preserveOriginalHeaders = headers.preserveOriginalHeaders(
            MoxHeaders.PUBLISHED_BY,
            MoxHeaders.PUBLISHED_BY_HOSTNAME,
            MoxHeaders.DATADOG_TRACE_ID
        )
        val dltKey = key ?: record.key()
        val dltValue = value ?: (headers.firstOrNull { it.key() == MoxHeaders.RECORD_BYTE_VALUE }?.value() ?: record.value())
        val dltHeaders = preserveOriginalHeaders.remove(MoxHeaders.RECORD_BYTE_VALUE) as RecordHeaders
        if (log.isInfoEnabled) {
            log.info(
                "Converting consumer record to Dead Letter (producer) record - [DLT: {}, key:{}, producerRecordHeaders: {}]",
                topicPartition.topic(),
                record.key(),
                messageDebugLogger.logOutputForHeaders(dltHeaders)
            )
        }
        return ProducerRecord(
            topicPartition.topic(),
            if (topicPartition.partition() < 0) null else topicPartition.partition(),
            dltKey,
            dltValue,
            dltHeaders
        )
    }

    private fun Headers.preserveOriginalHeaders(vararg keys: String): Headers {
        keys.forEach { key ->
            val existingHeaders = headers(key).toList()
            if (existingHeaders.size > 1) {
                log.warn("Record has already multiple header values", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_RECORD_HAS_MULTIPLE_HEADERS))
            }
            existingHeaders.lastOrNull()
                ?.value()
                ?.let { v -> this.remove(key).add("${DltHeaders.DLT_ORIGINAL_PREFIX}-$key", v) }
        }
        return this
    }

    override fun publish(outRecord: ProducerRecord<Any, Any>, kafkaTemplate: KafkaOperations<Any, Any>, inRecord: ConsumerRecord<*, *>) {
            kafkaTemplate.send(outRecord)
            .thenAccept { result ->
                log.debug(
                    "Successful dead-letter publication for record with key ${result?.producerRecord?.key()}",
                    kv(MDC_KEY_LOG_EVENT, LOG_EVENT_DEAD_LETTER_PUBLICATION_SUCCEEDED)
                )
            }
            .exceptionally { ex ->
                log.error(
                    "Dead-letter publication failed for record with key ${outRecord.key()}",
                    kv(MDC_KEY_LOG_EVENT, LOG_EVENT_DEAD_LETTER_PUBLICATION_FAILED),
                    ex
                )
                null
            }

//        try {
//
//            kafkaTemplate.send(outRecord).addCallback(
//                { result: SendResult<Any?, Any?>? ->
//                    log.debug(
//                        "Successful dead-letter publication for record with key ${result?.producerRecord?.key()}",
//                        kv(MDC_KEY_LOG_EVENT, LOG_EVENT_DEAD_LETTER_PUBLICATION_SUCCEEDED)
//                    )
//                },
//                { ex: Throwable? ->
//                    log.error(
//                        "Dead-letter publication failed for record with key ${outRecord.key()}",
//                        kv(MDC_KEY_LOG_EVENT, LOG_EVENT_DEAD_LETTER_PUBLICATION_FAILED),
//                        ex
//                    )
//                }
//            )
//        } catch (e: Exception) {
//            log.error(
//                "Dead-letter publication failed for record with key ${outRecord.key()}",
//                kv(MDC_KEY_LOG_EVENT, LOG_EVENT_DEAD_LETTER_PUBLICATION_FAILED),
//                e
//            )
//        }
    }

}
