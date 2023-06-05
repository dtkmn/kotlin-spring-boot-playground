package playground.common.messaging.logging

import org.apache.kafka.clients.consumer.Consumer
import playground.common.observability.logging.BuildInfoToMdc
import playground.common.observability.logging.MDC_KEY_CUSTOMER_ID
import playground.common.observability.logging.MDC_KEY_EVENT_CHANNEL
import playground.common.observability.logging.MDC_KEY_PUBLISHED_BY
import playground.common.observability.logging.MDC_KEY_PUBLISHED_BY_HOSTNAME
import playground.common.observability.logging.MDC_KEY_REQUEST_ID
import playground.common.messaging.MoxHeaders
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.MDC
import org.springframework.kafka.listener.RecordInterceptor
import org.springframework.kafka.support.KafkaHeaderMapper

class DragonRecordInterceptor<K, V>(
    private val headerMapper: KafkaHeaderMapper,
    private val buildInfoToMdc: BuildInfoToMdc
) : RecordInterceptor<K, V> {

//    override fun intercept(record: ConsumerRecord<K, V>): ConsumerRecord<K, V>? {
//
//        // build info
//        buildInfoToMdc.putAll()
//
//        // kafka headers
//        mdcPut(MDC_KEY_PROCESSING_FROM_TOPIC, record.topic())
//        mdcPut(MDC_KEY_PROCESSING_FROM_PARTITION, record.partition())
//        mdcPut(MDC_KEY_PROCESSING_FROM_OFFSET, record.offset())
//        mdcPut(MDC_KEY_PROCESSING_FROM_MESSAGE_KEY, record.key())
//        mdcPut(MDC_KEY_PROCESSING_FROM_KAFKA_TIMESTAMP, record.timestamp()) // TODO: format?
//
//        val headers = mutableMapOf<String, Any>()
//        headerMapper.toHeaders(record.headers(), headers)
//
//        // message metadata
//        mdcPut(MDC_KEY_MESSAGE_HEADER_IDEMPOTENCY_KEY, headers[MoxHeaders.IDEMPOTENCY_KEY])
//        mdcPut(MDC_KEY_MESSAGE_HEADER_TIMESTAMP, headers[MoxHeaders.TIMESTAMP])
//        mdcPut(MDC_KEY_MESSAGE_HEADER_CUSTOMER_ID, headers[MoxHeaders.CUSTOMER_ID])
//
//        // also log under legacy keys
//        mdcPut(MDC_KEY_REQUEST_ID, headers[MoxHeaders.IDEMPOTENCY_KEY])
//        mdcPut(MDC_KEY_CUSTOMER_ID, headers[MoxHeaders.CUSTOMER_ID])
//        mdcPut(MDC_KEY_PUBLISHED_BY, headers[MoxHeaders.PUBLISHED_BY])
//        mdcPut(MDC_KEY_PUBLISHED_BY_HOSTNAME, headers[MoxHeaders.PUBLISHED_BY_HOSTNAME])
//
//        mdcPut(MDC_KEY_EVENT_CHANNEL, LOG_EVENT_KAFKA)
//
//        return record
//    }

    private fun mdcPut(key: String, value: Any?) = value?.let { MDC.put(key, value.toString()) }
    private fun mdcPut(key: String, value: String?) = value?.let { MDC.put(key, value) }
    override fun intercept(record: ConsumerRecord<K, V>, consumer: Consumer<K, V>): ConsumerRecord<K, V>? {
        // build info
        buildInfoToMdc.putAll()

        // kafka headers
        mdcPut(MDC_KEY_PROCESSING_FROM_TOPIC, record.topic())
        mdcPut(MDC_KEY_PROCESSING_FROM_PARTITION, record.partition())
        mdcPut(MDC_KEY_PROCESSING_FROM_OFFSET, record.offset())
        mdcPut(MDC_KEY_PROCESSING_FROM_MESSAGE_KEY, record.key())
        mdcPut(MDC_KEY_PROCESSING_FROM_KAFKA_TIMESTAMP, record.timestamp()) // TODO: format?

        val headers = mutableMapOf<String, Any>()
        headerMapper.toHeaders(record.headers(), headers)

        // message metadata
        mdcPut(MDC_KEY_MESSAGE_HEADER_IDEMPOTENCY_KEY, headers[MoxHeaders.IDEMPOTENCY_KEY])
        mdcPut(MDC_KEY_MESSAGE_HEADER_TIMESTAMP, headers[MoxHeaders.TIMESTAMP])
        mdcPut(MDC_KEY_MESSAGE_HEADER_CUSTOMER_ID, headers[MoxHeaders.CUSTOMER_ID])

        // also log under legacy keys
        mdcPut(MDC_KEY_REQUEST_ID, headers[MoxHeaders.IDEMPOTENCY_KEY])
        mdcPut(MDC_KEY_CUSTOMER_ID, headers[MoxHeaders.CUSTOMER_ID])
        mdcPut(MDC_KEY_PUBLISHED_BY, headers[MoxHeaders.PUBLISHED_BY])
        mdcPut(MDC_KEY_PUBLISHED_BY_HOSTNAME, headers[MoxHeaders.PUBLISHED_BY_HOSTNAME])

        mdcPut(MDC_KEY_EVENT_CHANNEL, LOG_EVENT_KAFKA)

        return record
    }
}
