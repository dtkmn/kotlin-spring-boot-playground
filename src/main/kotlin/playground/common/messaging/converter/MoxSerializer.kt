package playground.common.messaging.converter

import com.fasterxml.jackson.databind.ObjectMapper
import playground.common.messaging.config.MESSAGING_JSON_TOPICS
import playground.common.messaging.config.MESSAGING_OBJECT_MAPPER
import playground.common.messaging.config.MESSAGING_SERIALIZER_RETRY_TEMPLATE
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.retry.support.RetryTemplate

class MoxSerializer : Serializer<Any> {

    override fun serialize(topic: String?, data: Any?): ByteArray {
        throw NotImplementedError("Need headers to serialize")
    }

    private val logger = LoggerFactory.getLogger(javaClass)!!
    private val stringSerializer = StringSerializer()
    private lateinit var jsonSerializer: JsonSerializer<Any>
    private val avroSerializer = KafkaAvroSerializer()
    private lateinit var jsonTopics: JsonTopics
    private lateinit var retryTemplate: RetryTemplate

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        stringSerializer.configure(configs, isKey)
        jsonSerializer = JsonSerializer(configs[MESSAGING_OBJECT_MAPPER] as ObjectMapper)
        jsonSerializer.configure(configs, isKey)
        avroSerializer.configure(configs, isKey)
        jsonTopics = configs[MESSAGING_JSON_TOPICS] as JsonTopics
        retryTemplate = configs[MESSAGING_SERIALIZER_RETRY_TEMPLATE] as RetryTemplate
    }

    override fun serialize(topic: String, headers: Headers, data: Any?): ByteArray? {
        if (data == null) {
            return null
        }
        if (data is ByteArray) {
            // means we couldn't deserialize and are sending raw byte[] to DLT
            logger.debug("Serialize raw ByteArray on $topic")
            return data
        }
        return if (jsonTopics.isJsonTopic(topic, headers)) {
            when (data) {
                is String -> {
                    logger.debug("Serialize String on $topic")
                    stringSerializer.serialize(topic, headers, data)
                }
                else -> {
                    logger.debug("Serialize JSON on $topic")
                    jsonSerializer.serialize(topic, headers, data)
                }
            }
        } else {
            logger.debug("Serialize AVRO on $topic")
            return retryTemplate.execute<ByteArray, RuntimeException> {
                logger.debug("Serialize (retry: ${it.retryCount})")
                avroSerializer.serialize(topic, data)
            }
        }
    }

    override fun close() {
        stringSerializer.close()
        jsonSerializer.close()
        avroSerializer.close()
    }
}
