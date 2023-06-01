package playground.common.idempotency

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import playground.common.idempotency.impl.GenericProperty

/**
 * Provides information and tools to avoid the duplication of side effects and state changes.
 *
 * Should be only obtained using [IdempotencyProvider] or some of the derived convenience helpers.
 *
 * Implements [GenericProperty.Store] so it is possible to extend the stored data.
 */
interface IdempotencyContext : AutoCloseable, GenericProperty.Store {

    /**
     * Executes the [body] and memorizes the output, so it is not successfully executed multiple times.
     *
     * The execution is in the separate database transaction that is only committed when the [body] is
     * successfully executed for the first time. This also ensures database atomicity.
     *
     * The most common use cases are either atomic database transaction
     * or storing the data before making external side effect.
     *
     * @param phaseId: See [PhaseData.id].
     * @param outputType: Explicit type information so we can deserialize
     * @param final: See [PhaseData.final].
     * @param settings: See [PhaseData.settings].
     * @param body: The implementation of the process. Will be successfully executed only once.
     *
     * There are several validations performed potentially throwing [IdempotencyAssertionError]:
     *  - same [phaseId] cannot be used twice,
     *  - this function cannot be called inside the [body] of another phase,
     *  - this function cannot be called after [final] phase was invoked.
     */
    fun <Output> internalPhase(
        phaseId: String,
        outputType: TypeReference<Output>,
        final: Boolean = false,
        transactional: Boolean = true,
        settings: List<PhaseData.Setting>,
        body: () -> Output
    ): Output

    /**
     * Inside the body of a phase contains instance of [PhaseData] associated with that phase.
     * Outside a phase its null. Can be also used do determine, whether this context is within a phase.
     */
    val phaseData: PhaseData?

    /**
     * The idempotency key provided by the initiator of the process.
     *
     * Two process executions should share the same idempotency key if and only if one is the retry of another.
     *
     * In theory there might occur same idempotency keys in different processes having different [processId],
     * but in practice the idempotency key is either randomly generated or determined from another keys
     * so it should not happen.
     */
    val idempotencyKey: String

    /**
     * The globally unique identifier of the process. Should be same for different instances of the same process.
     *
     * Must match [IDENTIFIER_REGEX].
     *
     * If we want to change the phone number of both, Mike and Nick,
     * there will be the same [processId] but different [idempotencyKey].
     * If we want to change Mike's phone and Nick's mail,
     * there will be different [processId] and nobody will care to compare [idempotencyKey].
     *
     * The field is usually populated as a constant value when calling [IdempotencyProvider]
     * or some convenience helper to start an idempotent process.
     */
    val processId: String

    /**
     * Set to true after final phase was invoked. See [PhaseData.final] for more info.
     */
    val wasFinalPhase: Boolean

    /**
     * Jackson object mapper this context uses to serialize phase results.
     */
    val objectMapper: ObjectMapper

    /**
     * Provides a way to extend context creation API. Examples are TODO().
     */
    val settings: List<Setting>

    /**
     * Encapsulates data related to the lifecycle of single idempotent phase.
     *
     * Should be only created by [IdempotencyContext.phase] and accessed by [IdempotencyContext.phaseData]
     *
     * Implements [GenericProperty.Store] so it is possible to extend the stored data.
     */
    interface PhaseData : GenericProperty.Store {

        /**
         * Identifier of the phase that is unique within the process.
         *
         * Must match [IDENTIFIER_REGEX].
         */
        val id: String

        /**
         * Mark of the final phase. Every idempotent process should end with a final phase.
         *
         * In the future, there will be tooling to discover idempotent processes that started but did not finish,
         * because there was not enough retrying. To recognize failed processes we need to mark the successful ones.
         *
         * Convenience method [runIdempotentProcess] invokes final phase automatically if it did not happen and
         * there was no exception. But you can also do it yourself, which gives you more control, even in situations,
         * where you throw exceptions.
         *
         * It is invalid to invoke another phase, after final phase was invoked.
         */
        val final: Boolean

        /**
         * Mark phase as transactional.
         *
         * For transactional phase it will be executed inside tranasction, if there is some problem with phase body
         * execution or saving result wrapping transaction will be rolled back.
         *
         * For non transactional phase its body is not executed inside transaction - such phases should be used for
         * example for calling rest calls for which there is no point of hacing transactions (holding db connection).
         */
        val transactional: Boolean

        /**
         * Provides a way to extend phase creation API. Examples are TODO().
         */
        val settings: List<Setting>

        /** Link to parent [IdempotencyContext]. */
        val context: IdempotencyContext

        /** Marker interface for implementing custom [settings] */
        interface Setting
    }

    /** Marker interface for implementing custom [settings] */
    interface Setting {
        /**
         * Some settings may conflict with another settings of same type.
         * If that is the case, this value should specify the set of classes or superclasses of all conflicting options.
         */
        val conflictsWithClasses: Set<Class<out Setting>>
    }
}

inline fun <reified Output> IdempotencyContext.phase(
    phaseIdentifier: String,
    final: Boolean = false,
    transactional: Boolean = true,
    vararg settings: IdempotencyContext.PhaseData.Setting,
    noinline body: () -> Output
) = internalPhase(
    phaseIdentifier,
    object : TypeReference<Output>() {},
    final,
    transactional,
    settings.toList(),
    body = body
)
