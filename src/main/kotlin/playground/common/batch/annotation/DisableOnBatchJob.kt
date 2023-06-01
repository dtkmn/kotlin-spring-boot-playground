package playground.common.batch.annotation

import playground.common.batch.common.BatchConstants
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ConditionalOnProperty(name = [BatchConstants.BATCH_JOB_SERVICE], havingValue = "false", matchIfMissing = true)
annotation class DisableOnBatchJob
