package playground.common.messaging.converter

import org.apache.kafka.common.header.Headers
import org.springframework.kafka.support.KafkaHeaderMapper
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.MessageHeaders
import java.nio.ByteBuffer

class DeadLetterHeaderMapper : KafkaHeaderMapper {

    override fun toHeaders(source: Headers, target: MutableMap<String, Any>) {
        source.forEach {
            val key = it.key()
            val value = it.value()
            when (key) {
                KafkaHeaders.DLT_ORIGINAL_TOPIC -> target[key] = value.asString()
                KafkaHeaders.DLT_ORIGINAL_PARTITION -> target[key] = value.asInt()
                KafkaHeaders.DLT_ORIGINAL_OFFSET -> target[key] = value.asLong()
                KafkaHeaders.DLT_ORIGINAL_TIMESTAMP -> target[key] = value.asLong()
                KafkaHeaders.DLT_ORIGINAL_TIMESTAMP_TYPE -> target[key] = value.asString()
                KafkaHeaders.DLT_EXCEPTION_FQCN -> target[key] = value.asString()
                KafkaHeaders.DLT_EXCEPTION_MESSAGE -> target[key] = value.asString()
                KafkaHeaders.DLT_EXCEPTION_STACKTRACE -> target[key] = value.asString()
            }
        }
    }

    fun ByteArray.asString() = toString(Charsets.UTF_8)
    fun ByteArray.asInt() = ByteBuffer.wrap(this).int
    fun ByteArray.asLong() = ByteBuffer.wrap(this).long

    // see org.springframework.kafka.listener.DeadLetterPublishingRecoverer.enhanceHeaders()
    override fun fromHeaders(headers: MessageHeaders?, target: Headers?) {
        TODO("not implemented")
    }
}
