package playground.common.messaging.converter

import org.apache.kafka.common.header.Headers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.stereotype.Component

@Component
class JsonTopics(
    @Value("\${dragon.messaging.jsonTopics:}")
    val jsonTopics: Set<String>
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)!!

    init {
        logger.info("Json topics: \n  ${jsonTopics.joinToString(",\n  ")}")
    }

    fun isJsonTopic(topic: String, headers: Headers): Boolean {
        return jsonTopics.contains(topic) ||
            headers.headers(KafkaHeaders.DLT_ORIGINAL_TOPIC)
                .any { jsonTopics.contains(String(it.value())) }
    }
}
