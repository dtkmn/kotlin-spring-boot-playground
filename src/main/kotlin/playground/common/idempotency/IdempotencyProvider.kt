package playground.common.idempotency

import playground.common.idempotency.IdempotencyProvider.ProcessLockedException
import org.springframework.http.HttpStatus
import playground.common.exception.DragonException
import playground.common.exception.error.ERR_SYS_IDEMPOTENT_PROCESS_LOCKED
import java.time.Duration
import java.time.Instant

/**
 * Used to match `processId`, `phaseId` or other unique idempotency identifiers.
 *
 * Identifiers starting with underscore are reserved for commons library
 */
val IDENTIFIER_REGEX = "[_a-zA-Z][-.\\w]*".toRegex()

/**
 * This interface is an entry point for any idempotency functionality.
 * It gathers all the dependencies and data necessary for idempotent process
 * and encapsulates them to the [IdempotencyContext] for every instance of every process.
 *
 * Also takes care of preventing same instances
 *
 * There are several possibilities to introduce idempotent process:
 *
 *   - the high level API, that is Kotlin friendly: [runIdempotentProcess]
 *   - the convenience APIs for the most common cases: IdempotentHandler in commons-rest
 *   - the low level API, provided by implementors and designed in a way it can be used from a Spring interceptor.
 */
interface IdempotencyProvider {

    /**
     * WARNING: This is part of low-level API. You should not use this unless you know what you are doing.
     * See the [IdempotencyProvider] documentation for more info.
     *
     * Creates an [IdempotencyContext] instance based on provided parameters.
     * Caller takes the responsibility to eventually call [AutoCloseable.close]
     *
     * @param idempotencyKey: See [IdempotencyContext.idempotencyKey].
     * @param processId: See [IdempotencyContext.processId].
     * @param lockTimeout: Specifies for how long should the retries be blocked.
     * @param settings: See [IdempotencyContext.settings]
     * @throws ProcessLockedException: When the same process instance was executed recently
     *                                 and there is an active temporal lock.
     */
    fun unsafeStartIdempotentProcess(
        idempotencyKey: String,
        processId: String,
        lockTimeout: Duration,
        settings: List<IdempotencyContext.Setting>
    ): IdempotencyContext

    /**
     * Thrown when the instance of idempotent process should not start
     * because there was same instance executed recently and there is an active temporal lock.
     *
     * @param tryAt: Time when the lock expires.
     * @param idempotencyKey: See [IdempotencyContext.idempotencyKey].
     * @param processId: See [IdempotencyContext.processId].
     */
    class ProcessLockedException(
        val tryAt: Instant,
        val processId: String,
        val idempotencyKey: String
    ) : DragonException(
        "There is an active lock of idempotent process $processId:$idempotencyKey. Retry at $tryAt",

        ERR_SYS_IDEMPOTENT_PROCESS_LOCKED,

        // We are changing semantics of 423 from the original RFC-4918,
        // but the use case is documented in Confluence and it was agreed with architecture.
        HttpStatus.LOCKED
    )
}

/**
 * Creates an instance of idempotent process with available managed [IdempotencyContext].
 * High level API of [IdempotencyProvider].
 *
 * If there was not a final phase during execution of body and there was not exception,
 * final phase is invoked automatically.
 *
 * @param idempotencyKey: See [IdempotencyContext.idempotencyKey].
 * @param processId: See [IdempotencyContext.processId].
 * @param lockTimeout: Specifies for how long should the retries be blocked.
 * @param body: The implementation of the process.
 *
 * @return Return value of the [body].
 * @throws ProcessLockedException: When the same process instance was executed recently
 *                                 and there is an active temporal lock.
 */
fun <ReturnValue> IdempotencyProvider.runIdempotentProcess(
    idempotencyKey: String,
    processId: String,
    lockTimeout: Duration,
    vararg settings: IdempotencyContext.Setting,
    body: (IdempotencyContext) -> ReturnValue
): ReturnValue = unsafeStartIdempotentProcess(idempotencyKey, processId, lockTimeout, settings.toList()).use { ctx ->
    val result = body(ctx)
    if (!ctx.wasFinalPhase) ctx.phase("_autoFinal", final = true) {}
    result
}

internal fun validateIdentifier(type: String, id: String) {
    IDENTIFIER_REGEX.matchEntire(id) ?: throw IdempotencyAssertionError(
        "Invalid identifier $type: $id. Must match ${IDENTIFIER_REGEX.pattern}."
    )
}
