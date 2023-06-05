package playground.common.batch.model

import java.time.Instant

data class BatchJobRequest(
    val jobId: String,
    val jobName: String,
    val scheduledTimestamp: Instant
)
