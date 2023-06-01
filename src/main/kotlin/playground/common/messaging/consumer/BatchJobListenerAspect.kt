package playground.common.messaging.consumer

import playground.common.messaging.logging.MessageProcessingLoggingAspect
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.kafka.support.Acknowledgment

/**
 * Wraps MessageProcessingLoggingAspect, gets Acknowledgment from arguments and acknowledge() - commit message before processing.
 */
@Aspect
class BatchJobListenerAspect(private val messageProcessingLoggingAspect: MessageProcessingLoggingAspect) {

    @Around("@annotation(playground.common.messaging.consumer.BatchJobListener)")
    fun beforeProcessingBatchJobListener(joinPoint: ProceedingJoinPoint) {
        val acknowledgment = joinPoint.args.find { it is Acknowledgment } as Acknowledgment?
            ?: throw UnsupportedOperationException("Missing Acknowledgment parameter in process method signature")
        acknowledgment.acknowledge()
        messageProcessingLoggingAspect.beforeProcessing(joinPoint)
    }
}
