package playground.common.messaging.converter

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Import
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaderMapper
import org.springframework.kafka.support.converter.MessagingMessageConverter
import org.springframework.kafka.support.converter.RecordMessageConverter
import org.springframework.kafka.support.converter.StringJsonMessageConverter
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import java.lang.reflect.Type

/*
  MessageConverter that handles both avro and json messages.

  Avro payload is converted in kafka serializer layer, while json is converted here.

  Header mapping is done for both types.
 */
@Component
@Import(JsonTopics::class)
class MoxMessageConverter(
    @Qualifier("kafkaObjectMapper")
    val objectMapper: ObjectMapper,
    val headerMapper: KafkaHeaderMapper,
    private val jsonTopics: JsonTopics
) : RecordMessageConverter {

    private val logger = LoggerFactory.getLogger(javaClass)!!
    private val jsonMessageConverter = StringJsonMessageConverter(objectMapper).apply {
        setHeaderMapper(headerMapper)
    }
    private val avroMessageConverter = MessagingMessageConverter().apply {
        setHeaderMapper(headerMapper)
    }

    override fun toMessage(
        record: ConsumerRecord<*, *>,
        acknowledgment: Acknowledgment?,
        consumer: Consumer<*, *>?,
        type: Type?
    ): Message<*> {
        return if (jsonTopics.isJsonTopic(record.topic(), record.headers())) {
            jsonMessageConverter.toMessage(record, acknowledgment, consumer, type)
        } else {
            avroMessageConverter.toMessage(record, acknowledgment, consumer, type)
        }
    }

    override fun fromMessage(
        message: Message<*>,
        defaultTopic: String?
    ): ProducerRecord<*, *> {
        // this seems like the simplest way to determine topic, without duplicating logic from MessageConverter
        val avroRecord = avroMessageConverter.fromMessage(message, defaultTopic)
        return if (jsonTopics.isJsonTopic(avroRecord.topic(), avroRecord.headers())) {
            jsonMessageConverter.fromMessage(message, defaultTopic)
        } else {
            avroRecord
        }
    }
}

fun String.toHex() = toByteArray().joinToString(",") { "%02x".format(it) }
