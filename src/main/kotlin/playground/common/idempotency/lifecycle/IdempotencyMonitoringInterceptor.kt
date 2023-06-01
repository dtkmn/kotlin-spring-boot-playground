package playground.common.idempotency.lifecycle

import com.fasterxml.jackson.core.type.TypeReference
import playground.common.idempotency.IdempotencyContext
import playground.common.idempotency.IdempotencyInterceptor
import playground.common.idempotency.logging.LOG_EVENT_CONTEXT_CLOSED
import playground.common.idempotency.logging.LOG_EVENT_CONTEXT_CREATED
import playground.common.idempotency.logging.LOG_EVENT_ENTERING_NEW_PHASE
import playground.common.idempotency.logging.LOG_EVENT_IDEMPOTENCY_PHASE_BODY_FINISHED_EXCEPTIONALLY
import playground.common.idempotency.logging.LOG_EVENT_IDEMPOTENCY_PHASE_EXECUTOR_ERROR
import playground.common.idempotency.logging.LOG_EVENT_PHASE_FINISHED
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import playground.common.observability.logging.*

object IdempotencyMonitoringInterceptor : IdempotencyInterceptor {

    val log: Logger = LoggerFactory.getLogger(IdempotencyMonitoringInterceptor::class.java)

    override fun onContextCreated(context: IdempotencyContext) {
        MDC.put(MDC_KEY_IDEMPOTENCY_PROCESS_ID, context.processId)
        MDC.put(MDC_KEY_IDEMPOTENCY_KEY, context.idempotencyKey)
        log.debug("Idempotency: Entering new process.", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_CONTEXT_CREATED))
    }

    override fun onContextClosed(context: IdempotencyContext) {
        log.debug("Idempotency: Closing the process.", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_CONTEXT_CLOSED))
        MDC.remove(MDC_KEY_IDEMPOTENCY_PROCESS_ID)
        MDC.remove(MDC_KEY_IDEMPOTENCY_KEY)
    }

    override fun wrapPhaseExecutor(next: IdempotencyInterceptor.PhaseExecutor): IdempotencyInterceptor.PhaseExecutor =
        object : IdempotencyInterceptor.PhaseExecutor {
            override fun <R> execute(data: IdempotencyContext.PhaseData, type: TypeReference<R>, body: () -> R): R {
                return try {
                    MDC.put(MDC_KEY_IDEMPOTENCY_PHASE_ID, data.id)
                    log.debug("Idempotency: Entering new phase.", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_ENTERING_NEW_PHASE))
                    if (data.final) log.debug("Idempotency: This is the final phase.")
                    val res = next.execute(data, type, body = {
                        try {
                            body()
                        } catch (e: Throwable) {
                            throw PhaseBodyException(e)
                        }
                    })
                    log.debug(
                        "Idempotency: Phase finished successfully and will not be executed on retry.",
                        kv(MDC_KEY_LOG_EVENT, LOG_EVENT_PHASE_FINISHED)
                    )
                    res
                } catch (e: PhaseBodyException) {
                    log.warn(
                        "Idempotency: Phase body throws an exception typed in ${e.cause::class.qualifiedName}. Database transaction has been rolled back.",
                        kv(MDC_KEY_LOG_EVENT, LOG_EVENT_IDEMPOTENCY_PHASE_BODY_FINISHED_EXCEPTIONALLY)
                    )
                    throw e.cause
                } catch (e: Throwable) {
                    log.error(
                        "Idempotency: Phase finished exceptionally. Database transaction has been rolled back.",
                        kv(MDC_KEY_LOG_EVENT, LOG_EVENT_IDEMPOTENCY_PHASE_EXECUTOR_ERROR)
                    )
                    throw e
                } finally {
                    MDC.remove(MDC_KEY_IDEMPOTENCY_PHASE_ID)
                }
            }
        }

    class PhaseBodyException(override val cause: Throwable) : RuntimeException(cause)
}
