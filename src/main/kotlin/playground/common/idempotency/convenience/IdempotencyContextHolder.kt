package playground.common.idempotency.convenience

import playground.common.idempotency.logging.LOG_EVENT_IDEMPOTENCY_OLD_CONTEXT_NOT_REMOVED
import playground.common.idempotency.IdempotencyContext
import playground.common.idempotency.IdempotencyInterceptor
import org.slf4j.LoggerFactory
import playground.common.idempotency.IdempotencyAssertionError
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv

/**
 * Stores current [IdempotencyContextHolder] in the thread local variable
 * so it is accessible without explicit passing as an argument.
 *
 * Needs [Interceptor] to be configured. It is done in the default configuration.
 */
object IdempotencyContextHolder {

    private val log = LoggerFactory.getLogger(IdempotencyContextHolder::class.java)
    private val state: ThreadLocal<IdempotencyContext?> = ThreadLocal()

    /** Get [IdempotencyContext] stored in thread local variable or null if not present **/
    fun getOrNull(): IdempotencyContext? = state.get()

    /** Get [IdempotencyContext] stored in thread local variable or throw if not present **/
    fun get(): IdempotencyContext = getOrNull() ?: throw IdempotencyAssertionError(
        "There is no Idempotency Context in the thread local store:\n" +
            " - Are you running this code inside `runIdempotentProcess` or `@IdempotentHandler`?" +
            " - Is it in the same thread?" +
            " - Do you have IdempotencyContextHolder.Interceptor configured? (It is in the default configuration.)"
    )

    internal fun removeLocal() {
        state.remove()
    }

    internal fun setLocal(context: IdempotencyContext) {
        if (state.get() != null) {
            // Not throwing exception as that would prevent processing of request when the previous messed cleanup
            log.error(
                "New idempotency context is being stored in the local store, while the old was not removed.",
                kv(MDC_KEY_LOG_EVENT, LOG_EVENT_IDEMPOTENCY_OLD_CONTEXT_NOT_REMOVED)
            )
        }
        state.set(context)
    }

    /** Stores [IdempotencyContext] in [IdempotencyContextHolder] so it is accessible without explicit passing **/
    object Interceptor : IdempotencyInterceptor {
        override fun onContextCreated(context: IdempotencyContext) {
            setLocal(context)
        }

        override fun onContextClosed(context: IdempotencyContext) {
            removeLocal()
        }
    }
}
