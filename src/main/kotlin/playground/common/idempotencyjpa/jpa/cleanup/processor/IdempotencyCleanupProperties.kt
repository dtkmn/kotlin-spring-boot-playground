package playground.common.idempotencyjpa.jpa.cleanup.processor

import org.springframework.boot.context.properties.ConfigurationProperties
import playground.common.batch.annotation.EnableOnBatchJob

//import org.springframework.boot.context.properties.ConstructorBinding

//@ConstructorBinding
@ConfigurationProperties("dragon.idempotency.cleanup", ignoreInvalidFields = true)
@EnableOnBatchJob
data class IdempotencyCleanupProperties(
    val statusTopic: String,
    val retentionInDays: Int = 30,
    val maxRecordsToRemove: Int = 1000000,
    val largeObjectBatchSize: Int = 10000,
    val idempotencyRecordsBatchSize: Int = 10000
)
