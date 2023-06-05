package playground.common.messaging.converter

import playground.common.messaging.MoxHeaders
import playground.common.messaging.config.MESSAGING_JSON_TOPICS
import playground.common.messaging.config.MESSAGING_SERIALIZER_RETRY_TEMPLATE
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.retry.support.RetryTemplate

class MoxDeserializer : Deserializer<Any> {

    private val logger = LoggerFactory.getLogger(javaClass)!!
    private val stringDeserializer = StringDeserializer()
    val avroDeserializer = KafkaAvroDeserializer()
    private lateinit var jsonTopics: JsonTopics
    private lateinit var retryTemplate: RetryTemplate

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        stringDeserializer.configure(configs, isKey)
        avroDeserializer.configure(configs, isKey)
        jsonTopics = configs[MESSAGING_JSON_TOPICS] as JsonTopics
        retryTemplate = configs[MESSAGING_SERIALIZER_RETRY_TEMPLATE] as RetryTemplate
    }

    override fun deserialize(topic: String?, data: ByteArray?): Any? {
        throw NotImplementedError("Need headers to deserialize")
    }

    override fun deserialize(topic: String, headers: Headers, data: ByteArray?): Any? {
        headers.add(MoxHeaders.RECORD_BYTE_VALUE, data)
        return if (jsonTopics.isJsonTopic(topic, headers)) {
            logger.debug("Deserialize to String on $topic")
            stringDeserializer.deserialize(topic, headers, data)
        } else {
            logger.debug("Deserialize to AVRO on $topic")
            return retryTemplate.execute<Any?, RuntimeException> {
                logger.debug("Deserialize (retry: ${it.retryCount})")
                avroDeserializer.deserialize(topic, data)
            }
        }
    }

    override fun close() {
        stringDeserializer.close()
        avroDeserializer.close()
    }
}
