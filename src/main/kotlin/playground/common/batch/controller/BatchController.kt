package playground.common.batch.controller


import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import playground.common.batch.annotation.EnableOnBatchJob
import playground.common.batch.configuration.BatchProperties

@RestController
@RequestMapping("/jobs")
@EnableOnBatchJob
class BatchController(
    val producerOnlyKafkaTemplate: KafkaTemplate<Any, Any>,
    val batchProperties: BatchProperties
) {
//    @PostMapping("/start")
//    fun start(@RequestBody request: BatchJobRequest): ResponseEntity<Any> {
//        if (!MoxSecurity.isService) {
//            throw AccessDeniedException("Access is denied")
//        }
//        batchProperties.jobCommandTopics[request.jobName]?.run {
//            producerOnlyKafkaTemplate.executeInTransaction {
//                producerOnlyKafkaTemplate.send(
//                    message(
//                        topic = this,
//                        key = request.jobId,
//                        message = StartBatchJobCommand(
//                            jobId = request.jobId,
//                            jobName = request.jobName,
//                            scheduledTimestamp = request.scheduledTimestamp
//                        ),
//                        metadata = MessageMetadata()
//                    )
//                )
//            }
//        } ?: throw SupportException("Job name [${request.jobName}] not found.", ERR_BATCH_JOB_NAME_NOT_FOUND)
//        return ResponseEntity.accepted().build()
//    }
}
