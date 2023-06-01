package playground.common.idempotency

import com.fasterxml.jackson.core.type.TypeReference

interface IdempotencyInterceptor {

    /** See [wrapPhaseExecutor] */
    interface PhaseExecutor {
        fun <R> execute(data: IdempotencyContext.PhaseData, type: TypeReference<R>, body: () -> R): R
    }

    /**
     * Invoked every time new [IdempotencyContext] is created.
     *
     * Default implementation does nothing.
     */
    fun onContextCreated(context: IdempotencyContext) {}

    /**
     * Invoked every time [IdempotencyContext.close] is called.
     *
     * Default implementation does nothing.
     *
     * The order of execution for multiple interceptors is reversed to the order of execution for [onContextCreated].
     *
     * If this method throws an exception, it is suppressed to another exception.
     */
    fun onContextClosed(context: IdempotencyContext) {}

    /**
     * Invoked ONCE to extend the behavior of phase execution.
     *
     * You must call underlying implementation provided by [next] at most once,
     * you may wrap the body and change the return type, but you can not change phase data.
     *
     * Default implementation returns [next] unchanged.
     *
     * The order of execution for multiple interceptors is same as the order of execution for [onContextCreated].
     */
    fun wrapPhaseExecutor(next: PhaseExecutor): PhaseExecutor = next
}
