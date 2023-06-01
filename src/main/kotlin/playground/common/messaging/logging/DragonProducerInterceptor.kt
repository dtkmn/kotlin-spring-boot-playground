package playground.common.messaging.logging

import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import playground.common.messaging.MoxHeaders
import playground.common.messaging.config.MESSAGE_PAYLOAD_LOGGER_BEAN
import org.apache.kafka.clients.producer.ProducerInterceptor
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DragonProducerInterceptor : ProducerInterceptor<String, String> {

    private val log: Logger = LoggerFactory.getLogger(javaClass)!!

    lateinit var payloadDebugLogger: MessagePayloadLogger

    lateinit var publishedBy: String

    lateinit var publishedByHostname: String

    override fun configure(configs: Map<String, *>) {
        payloadDebugLogger = configs[MESSAGE_PAYLOAD_LOGGER_BEAN] as MessagePayloadLogger
        publishedBy = configs[MoxHeaders.PUBLISHED_BY] as String
        publishedByHostname = configs[MoxHeaders.PUBLISHED_BY_HOSTNAME] as String
    }

    override fun onSend(record: ProducerRecord<String, String>): ProducerRecord<String, String> {
        val updatedRecord = record.apply {
            headers()
                .add(MoxHeaders.PUBLISHED_BY, publishedBy.toByteArray())
                .add(MoxHeaders.PUBLISHED_BY_HOSTNAME, publishedByHostname.toByteArray())
        }
        val logEvent =
            if (updatedRecord.topic().endsWith(".DLT")) LOG_EVENT_MESSAGE_PUBLISHED_DLT else LOG_EVENT_MESSAGE_PUBLISHED

        if (log.isInfoEnabled) log.info(
            "Publishing Record - [topic: {}, partition: {}, key: {}, payloadType: {}, headers: {}, payload: {}]",
            updatedRecord.topic(),
            updatedRecord.partition(),
            updatedRecord.key(),
            payloadType(updatedRecord),
            payloadDebugLogger.logOutputForHeaders(updatedRecord.headers()),
            kv(MDC_KEY_ANY_PAYLOAD, payloadDebugLogger.logOutputFor(updatedRecord.value())),
            kv(MDC_KEY_LOG_EVENT, logEvent)
        )

        return updatedRecord
    }

    fun payloadType(record: ProducerRecord<String, *>): String {
        val value = record.value()
        return when (value) {
            is ByteArray -> "ByteArray" // DLT only?
            else -> value?.javaClass?.name ?: "null"
        }
    }

    override fun onAcknowledgement(metadata: RecordMetadata, exception: Exception?) {}

    override fun close() {}
}
