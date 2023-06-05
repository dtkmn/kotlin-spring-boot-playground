package playground.common.messaging.logging

import playground.common.exception.DragonException
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.Payload
import playground.common.messaging.logging.*

@Aspect
class MessageProcessingLoggingAspect {

    val log: Logger = LoggerFactory.getLogger(MessageProcessingLoggingAspect::class.java)

    @Autowired
    lateinit var payloadDebugLogger: MessagePayloadLogger

    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    fun beforeProcessing(joinPoint: ProceedingJoinPoint) {

        try {
            val payload = joinPoint.payload()
            if (log.isInfoEnabled) log.info(
                "Processing message - {}, {}, {}",
                kv(MDC_KEY_ANY_PAYLOAD_TYPE, payload?.javaClass?.name ?: "null"),
                kv(MDC_KEY_ANY_PAYLOAD, payloadDebugLogger.logOutputFor(payload)),
                kv(MDC_KEY_LOG_EVENT, LOG_EVENT_PROCESSING_START)
            )
            joinPoint.proceed()
        } catch (e: Throwable) {
            if (e is DragonException && e.statusCode.is4xxClientError) {
                log.warn(
                    "Error while processing message: ${e.message}",
                    kv(MDC_KEY_LOG_EVENT, LOG_EVENT_PROCESSING_ERROR),
                    e
                )
            } else {
                log.error(
                    "Error while processing message: ${e.message}",
                    kv(MDC_KEY_LOG_EVENT, LOG_EVENT_PROCESSING_ERROR),
                    e
                )
            }
            throw e
        } finally {
            log.debug("Finished processing message", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_PROCESSING_END))
            MDC.clear()
        }
    }

    private fun JoinPoint.payload(): Any? {
        val parameterAnnotations = (signature as MethodSignature).method.parameterAnnotations
        val index =
            parameterAnnotations.indexOfFirst { annotations -> annotations.firstOrNull { it is Payload } != null }
        return if (index < 0) null else args[index]
    }
}
