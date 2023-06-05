package playground.common.messaging.logging

import com.fasterxml.jackson.databind.ObjectMapper
import playground.common.messaging.DltHeaders
import playground.common.messaging.MoxHeaders
import playground.common.messaging.converter.DeadLetterHeaderMapper
import org.apache.avro.generic.GenericContainer
import org.apache.kafka.common.header.Headers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.kafka.support.KafkaHeaderMapper
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.support.mapping.AbstractJavaTypeMapper

const val masked = "######"

class MessagePayloadLogger {

    @Autowired
    lateinit var env: Environment

    @Autowired
    lateinit var kafkaObjectMapper: ObjectMapper

    @Autowired
    lateinit var headerMapper: KafkaHeaderMapper

    private val deadLetterHeaderMapper = DeadLetterHeaderMapper()

    // only these headers will be logged -- we use whitelist to be sure no PII is leaked
    // add "blacklisted" headers as commented out,
    // so they are not added accidentally later
    private val headerWhitelist = setOf<String>(
        MoxHeaders.PUBLISHED_BY,
        MoxHeaders.PUBLISHED_BY_HOSTNAME,
        MoxHeaders.DATADOG_TRACE_ID,
        DltHeaders.DLT_ORIGINAL_PUBLISHED_BY,
        DltHeaders.DLT_ORIGINAL_PUBLISHED_BY_HOSTNAME,
        DltHeaders.DLT_ORIGINAL_DATADOG_TRACE_ID,
        DltHeaders.DLT_DEAD_LETTER_ID,
        MoxHeaders.CUSTOMER_ID,
        MoxHeaders.TIMESTAMP,
        MoxHeaders.IDEMPOTENCY_KEY,
        KafkaHeaders.DLT_ORIGINAL_TOPIC,
        KafkaHeaders.DLT_ORIGINAL_PARTITION,
        KafkaHeaders.DLT_ORIGINAL_OFFSET,
        KafkaHeaders.DLT_ORIGINAL_TIMESTAMP,
        KafkaHeaders.DLT_ORIGINAL_TIMESTAMP_TYPE,
        KafkaHeaders.DLT_EXCEPTION_FQCN,
        // KafkaHeaders.DLT_EXCEPTION_MESSAGE, message can have PII
        // KafkaHeaders.DLT_EXCEPTION_STACKTRACE, stacktrace also has message
        KafkaHeaders.CORRELATION_ID,
        KafkaHeaders.CONSUMER,
        KafkaHeaders.GROUP_ID,
        KafkaHeaders.KEY,
        KafkaHeaders.OFFSET,
        KafkaHeaders.PARTITION,
        KafkaHeaders.RECEIVED_KEY,
        KafkaHeaders.RECEIVED_PARTITION,
        KafkaHeaders.RECEIVED_TIMESTAMP,
        KafkaHeaders.RECEIVED_TOPIC,
        KafkaHeaders.TIMESTAMP,
        KafkaHeaders.TIMESTAMP_TYPE,
        KafkaHeaders.TOPIC,
        KafkaHeaders.REPLY_PARTITION,
        KafkaHeaders.REPLY_TOPIC,
        AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
        // not serializable, only used to propagate error from serializer
        // ErrorHandlingDeserializer.VALUE_DESERIALIZER_EXCEPTION_HEADER
        "x-datadog-parent-id",
        "x-datadog-sampling-priority"
    )

    private fun isMessageDebugLoggingEnabled(): Boolean {
        return env.activeProfiles.contains("message-debug-logging")
    }

    fun logOutputFor(messagePayload: Any?): String {
        return if (this.isMessageDebugLoggingEnabled()) {
            if (messagePayload is GenericContainer) {
                messagePayload.toString()
            } else {
                kafkaObjectMapper.writeValueAsString(messagePayload)
            }
        } else {
            masked
        }
    }

    fun logOutputForHeaders(headers: Headers): String {
        val mappedHeaders = mutableMapOf<String, Any>()
        headerMapper.toHeaders(headers, mappedHeaders)
        // just overwrite values, could be more elegant
        deadLetterHeaderMapper.toHeaders(headers, mappedHeaders)

        // other hacks
        mappedHeaders.entries.forEach {
            val value = it.value

            if (it.key !in headerWhitelist) {
                it.setValue(masked)

                // fall back to string conversion
            } else if (value is ByteArray) {
                it.setValue(value.toString(Charsets.UTF_8))
            }
        }

        return mappedHeaders.entries.joinToString(prefix = "[", postfix = "]") { (k, v) -> "$k: $v" }
    }
}
