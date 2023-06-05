package playground.common.idempotencyjpa.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import playground.common.batch.annotation.EnableOnBatchJob
import playground.common.batch.configuration.BatchConfiguration
import playground.common.idempotencyjpa.jpa.cleanup.processor.IdempotencyCleanupProcessor

@ConditionalOnProperty(prefix = "playground.idempotency.cleanup", value = ["enabled"], havingValue = "true")
@Configuration
@Import(
    IdempotencyCleanupProcessor::class,
    BatchConfiguration::class
)
@EnableOnBatchJob
class PostgresIdempotencyCleanupConfiguration
