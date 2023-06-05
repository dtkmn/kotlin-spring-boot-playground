package playground.common.idempotencyjpa.jpa.cleanup.processor

import com.mox.shared.contract.avro.batchjobstatusupdatedevent.v1.JobStatus
import com.mox.shared.contract.avro.startbatchjobcommand.v1.StartBatchJobCommand
import playground.common.idempotencyjpa.jpa.IdempotencyRecordRepository
import playground.common.idempotencyjpa.jpa.PostgresIdempotencyLockRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import playground.common.batch.annotation.EnableOnBatchJob
import playground.common.batch.jobstatus.BatchJobStatusUpdatedEventPublisher
import playground.common.messaging.consumer.BatchJobListener
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import java.time.Instant
import java.time.temporal.ChronoUnit

const val LOG_EVENT_IDEMPOTENCY_CLEANUP_JOB_STARTED = "idempotency-cleanup-job-started"
const val LOG_EVENT_IDEMPOTENCY_CLEANUP_JOB_FINISHED = "idempotency-cleanup-job-finished"
const val LOG_EVENT_IDEMPOTENCY_CLEANUP_LARGE_OBJECTS = "idempotency-cleanup-large-objects"
const val LOG_EVENT_IDEMPOTENCY_CLEANUP_MAX_RECORDS_REACHED = "idempotency-cleanup-max-records-reached"
const val LOG_EVENT_IDEMPOTENCY_CLEANUP_IDEMPOTENCY_RECORD = "idempotency-cleanup-idempotency_records"
const val LOG_EVENT_IDEMPOTENCY_CLEANUP_JOB_FAILED = "idempotency-cleanup-job-failed"

@ConditionalOnProperty(prefix = "dragon.idempotency.cleanup", value = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(IdempotencyCleanupProperties::class)
@Component
@EnableOnBatchJob
class IdempotencyCleanupProcessor(
    private val batchJobStatusUpdatedEventPublisher: BatchJobStatusUpdatedEventPublisher,
    private val idempotencyRecordRepository: IdempotencyRecordRepository,
    private val idempotencyLockRepository: PostgresIdempotencyLockRepository,
    private val idempotencyCleanupProperties: IdempotencyCleanupProperties
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @BatchJobListener(
        id = "\${service.name}-idempotency-cleanup-processor",
        clientIdPrefix = "\${service.name}-idempotency-cleanup-processor",
        topics = ["\${dragon.idempotency.cleanup.topic:}"],
        properties = ["max.poll.records=1", "max.poll.interval.ms=28800000"]
    )
    fun process(@Payload payload: StartBatchJobCommand, acknowledgment: Acknowledgment) {
        val (details: String, status: JobStatus) = if (idempotencyRecordRepository.count()> 0) {
            doCleanup()
        } else {
            "No idempotency records to clean" to JobStatus.SUCCESS
        }

        batchJobStatusUpdatedEventPublisher.publishStatusUpdate(
            jobId = payload.jobId,
            status = status,
            details = details,
            batchJobStatusUpdatedEventTopic = idempotencyCleanupProperties.statusTopic
        )
    }

    private fun doCleanup(): Pair<String, JobStatus> {
        var details: String
        var status: JobStatus

        try {
            val nthIdempotencyTimestamp =
                idempotencyRecordRepository.getNthIdempotencyTimestamp(idempotencyCleanupProperties.maxRecordsToRemove + 1)
            val retentionThreshold = Instant.now().minus(idempotencyCleanupProperties.retentionInDays.toLong(), ChronoUnit.DAYS)
            val deleteThreshold =
                if (nthIdempotencyTimestamp.isBefore(retentionThreshold)) {
                    log.warn(
                        "Removing ${idempotencyCleanupProperties.maxRecordsToRemove} records which is max records to remove in one batch job execution.",
                        kv(MDC_KEY_LOG_EVENT, LOG_EVENT_IDEMPOTENCY_CLEANUP_MAX_RECORDS_REACHED)
                    )
                    nthIdempotencyTimestamp
                } else {
                    retentionThreshold
                }

            log.info(
                "Removing old records from IdempotencyRecord table (with createdAt before $deleteThreshold)",
                kv(
                    MDC_KEY_LOG_EVENT,
                    LOG_EVENT_IDEMPOTENCY_CLEANUP_JOB_STARTED
                )
            )

            var removedLargeObjects = 0
            do {
                log.info(
                    "Removing large objects in ${idempotencyCleanupProperties.largeObjectBatchSize} batch",
                    kv(MDC_KEY_LOG_EVENT, LOG_EVENT_IDEMPOTENCY_CLEANUP_LARGE_OBJECTS)
                )
                val removedLargeObjectOids =
                    idempotencyRecordRepository.cleanIdempotencyLargeObjects(deleteThreshold, idempotencyCleanupProperties.largeObjectBatchSize)
                removedLargeObjects += removedLargeObjectOids.size
            } while (removedLargeObjectOids.size == idempotencyCleanupProperties.largeObjectBatchSize)
            log.info(
                "Removed $removedLargeObjects large objects",
                kv(MDC_KEY_LOG_EVENT, LOG_EVENT_IDEMPOTENCY_CLEANUP_LARGE_OBJECTS)
            )

            var removedRecords = 0
            do {
                log.info(
                    "Removing idempotency records in ${idempotencyCleanupProperties.idempotencyRecordsBatchSize} batch",
                    kv(MDC_KEY_LOG_EVENT, LOG_EVENT_IDEMPOTENCY_CLEANUP_IDEMPOTENCY_RECORD)
                )
                val removedIdempotencyRecords =
                    idempotencyRecordRepository.deleteWhereCreatedAtBefore(deleteThreshold, idempotencyCleanupProperties.idempotencyRecordsBatchSize)
                removedRecords += removedIdempotencyRecords
            } while (removedIdempotencyRecords == idempotencyCleanupProperties.idempotencyRecordsBatchSize)

            val removedLocks = idempotencyLockRepository.deleteWhereLockedUntilBefore(deleteThreshold)
            details =
                "Removed $removedRecords records from IdempotencyRecord table and $removedLocks records from IdempotencyLock " +
                "table (with createdAt/lockedUntil before $deleteThreshold)"
            status = JobStatus.SUCCESS
            log.info(
                details,
                kv(
                    MDC_KEY_LOG_EVENT,
                    LOG_EVENT_IDEMPOTENCY_CLEANUP_JOB_FINISHED
                )
            )
        } catch (ex: Exception) {
            details = "Error: ${ex.message}"
            status = JobStatus.FAILED
            log.error(
                "Failed to remove old records from IdempotencyRecord table",
                kv(MDC_KEY_LOG_EVENT, LOG_EVENT_IDEMPOTENCY_CLEANUP_JOB_FAILED),
                ex
            )
        }
        return Pair(details, status)
    }
}
