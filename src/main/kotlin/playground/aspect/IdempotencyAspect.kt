package playground.aspect

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import playground.service.DynamoDBService
import playground.service.KafkaService

@Aspect
@Component
class IdempotencyAspect(
    private val dynamoDBService: DynamoDBService,
    private val objectMapper: ObjectMapper
) {

    @Around("@annotation(playground.aspect.Idempotent) && args(record, acknowledgment,..)")
    @Throws(Throwable::class)
    fun checkIdempotency(joinPoint: ProceedingJoinPoint, record: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        val kafkaMessage = objectMapper.readValue(record.value(), KafkaService.KafkaMessage::class.java)
        val messageId = kafkaMessage.messageId
        println("Inside Aspect: $messageId")
        if (dynamoDBService.isAlreadyProcessed(messageId)) {
//            throw IllegalArgumentException("Message with ID $messageId has already been processed")
            acknowledgment.acknowledge()
            println("Message with ID $messageId has already been processed. Skipping.")
            return
        }
        try {
            joinPoint.proceed()
            dynamoDBService.markAsProcessed(messageId)
            acknowledgment.acknowledge()
        } catch (e: Throwable) {
            throw e
        }
    }

}