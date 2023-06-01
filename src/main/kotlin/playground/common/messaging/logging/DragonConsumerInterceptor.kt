package playground.common.messaging.logging

import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import playground.common.messaging.config.MESSAGE_PAYLOAD_LOGGER_BEAN
import org.apache.kafka.clients.consumer.ConsumerInterceptor
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class DragonConsumerInterceptor : ConsumerInterceptor<String, String> {

    private val log: Logger = LoggerFactory.getLogger(javaClass)!!
    lateinit var payloadDebugLogger: MessagePayloadLogger

    override fun configure(configs: Map<String, *>) {
        payloadDebugLogger = configs[MESSAGE_PAYLOAD_LOGGER_BEAN] as MessagePayloadLogger
    }

    override fun onConsume(records: ConsumerRecords<String, String>): ConsumerRecords<String, String> {
        MDC.clear()
        log.info(
            "Polled Records: count=${records.count()}, partitions=${records.partitions()}",
            kv(MDC_KEY_LOG_EVENT, LOG_EVENT_CONSUMER_POLL),
        )
        return records
    }

    override fun onCommit(offsets: Map<TopicPartition, OffsetAndMetadata>) {
        if (log.isDebugEnabled) {
            val offsetsString = offsets
                .map { "[topic: ${it.key.topic()}, partition: ${it.key.partition()}, offset: ${it.value.offset()}]" }
                .joinToString(", ")

            log.debug("Committing Record Offsets - $offsetsString", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_COMMITTING_OFFSETS))
        }
    }

    override fun close() {}
}
