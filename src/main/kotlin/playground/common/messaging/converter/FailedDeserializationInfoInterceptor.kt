package playground.common.messaging.converter

import org.apache.kafka.clients.consumer.Consumer
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import playground.common.messaging.logging.LOG_EVENT_AVRO_IGNORING_UNKNOWN_SCHEMA
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.SerializationException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.listener.RecordInterceptor
import org.springframework.kafka.support.serializer.FailedDeserializationInfo
import org.springframework.stereotype.Component

@Component
class FailedDeserializationInfoInterceptor(
    @Value("\${dragon.messaging.ignoreUnknownSchemas:}")
    private val ignoreUnknownSchemas: List<String>
) : RecordInterceptor<Any, Any> {
    private val pattern = Regex("Could not find class (\\S+) specified in writer's schema whilst finding reader's schema for a SpecificRecord\\.")

    private val logger = LoggerFactory.getLogger(javaClass)!!
    override fun intercept(record: ConsumerRecord<Any, Any>, consumer: Consumer<Any, Any>): ConsumerRecord<Any, Any>? {
        val payload = record.value()
        return if (payload is FailedDeserializationInfo) {
            if (isUnhandledAvroType(payload)) {
                // TODO: log this as DEBUG?
                logger.info("Ignoring unknown avro schema", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_AVRO_IGNORING_UNKNOWN_SCHEMA))
                MDC.clear()
                null
            } else {
                throw payload.exception
            }
        } else {
            record
        }
    }

//    override fun intercept(record: ConsumerRecord<Any, Any>): ConsumerRecord<Any, Any>? {
//
//        val payload = record.value()
//        return if (payload is FailedDeserializationInfo) {
//            if (isUnhandledAvroType(payload)) {
//                // TODO: log this as DEBUG?
//                logger.info("Ignoring unknown avro schema", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_AVRO_IGNORING_UNKNOWN_SCHEMA))
//                MDC.clear()
//                null
//            } else {
//                throw payload.exception
//            }
//        } else {
//            record
//        }
//    }

    private fun isUnhandledAvroType(failedDeserializationInfo: FailedDeserializationInfo): Boolean {
        if (failedDeserializationInfo.exception is SerializationException) {
            val className = extractClassName(failedDeserializationInfo.exception as SerializationException)
            return ignoreUnknownSchemas.contains(className)
        }
        return false
    }

    private fun extractClassName(e: SerializationException) = pattern.matchEntire(e.message!!)?.let { it.groups[1]?.value }
}
