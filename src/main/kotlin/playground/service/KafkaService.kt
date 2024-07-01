package playground.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import playground.aspect.Idempotent

@Service
class KafkaService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    data class KafkaMessage(
        val messageId: String,
        val content: String
    )

    fun sendMessage(topic: String, message: String) {
        kafkaTemplate.executeInTransaction { template ->
            template.send(topic, message)
        }
    }

    private fun extractKafkaMessage(message: String): KafkaMessage {
        return objectMapper.readValue(message, KafkaMessage::class.java)
    }

    @KafkaListener(topics = ["my-topic"], groupId = "group_id")
    @Retryable(value = [Exception::class], maxAttempts = 5, backoff = Backoff(delay = 1000, multiplier = 2.0))
    @Idempotent
    fun listen(record: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        // Log the metadata
        println("Received message with offset: ${record.offset()}, partition: ${record.partition()}, timestamp: ${record.timestamp()}")
        val kafkaMessage = extractKafkaMessage(record.value())
        println("Received message: ${kafkaMessage.content}")
    }

}