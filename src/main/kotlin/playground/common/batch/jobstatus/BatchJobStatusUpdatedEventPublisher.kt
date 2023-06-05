package playground.common.batch.jobstatus

import com.mox.shared.contract.avro.batchjobstatusupdatedevent.v1.BatchJobStatusUpdatedEvent
import com.mox.shared.contract.avro.batchjobstatusupdatedevent.v1.JobStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.Message
import playground.common.messaging.MessageMetadata
import playground.common.messaging.publisher.message
import java.time.Instant.now

class BatchJobStatusUpdatedEventPublisher(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val producerOnlyKafkaTemplate: KafkaTemplate<Any, Any>
) {
    private fun KafkaTemplate<Any, Any>.send(message: Message<*>, needsNewTransaction: Boolean) {
        if (needsNewTransaction) {
            this.executeInTransaction {
                this.send(message)
            }
        } else {
            this.send(message)
        }
    }

    fun publishStatusUpdate(
        jobId: String,
        status: JobStatus,
        details: String,
        batchJobStatusUpdatedEventTopic: String
    ) {
        val inKafkaTransaction = kafkaTemplate.inTransaction()
        val template = if (inKafkaTransaction) kafkaTemplate else producerOnlyKafkaTemplate
//        val msg = message(
//            key = jobId,
//            topic = batchJobStatusUpdatedEventTopic,
//            message = BatchJobStatusUpdatedEvent(
//                jobId = jobId,
//                time = now(),
//                status = status,
//                details = details
//            ),
//            metadata = MessageMetadata()
//        )
//        template.send(
//            message(
//                key = jobId,
//                topic = batchJobStatusUpdatedEventTopic,
//                message = BatchJobStatusUpdatedEvent(
//                    jobId = jobId,
//                    time = now(),
//                    status = status,
//                    details = details
//                ),
//                metadata = MessageMetadata()
//            ),
//            !inKafkaTransaction
//        )
    }
}
