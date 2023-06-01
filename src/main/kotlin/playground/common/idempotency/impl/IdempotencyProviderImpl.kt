package playground.common.idempotency.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import playground.common.exception.SupportException
import playground.common.exception.error.ERR_SYS_SERVER_ERROR
import playground.common.idempotency.logging.LOG_EVENT_IDEMPOTENCY_MULTIPLE_EXECUTION_IN_PARALLEL
import playground.common.idempotency.logging.LOG_EVENT_IDEMPOTENCY_SKIP_PHASE_DUE_TO_RESULT_STORED
import playground.common.idempotency.logging.LOG_EVENT_RELEASE_IDEMPOTENCY_LOCK_FAILED
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import playground.common.idempotency.*
import playground.common.idempotency.validateIdentifier
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Implementation of [IdempotencyProvider] interface that is agnostic to underlying stores.
 *
 * To use this implementation with the specific stores
 * you need to implement [IdempotencyLockAdapter] and [IdempotencyStoreAdapter] and provide it in the constructor.
 */
class IdempotencyProviderImpl(
    val objectMapper: ObjectMapper,
    val lockAdapter: IdempotencyLockAdapter,
    val storeAdapter: IdempotencyStoreAdapter,
    private val interceptors: List<IdempotencyInterceptor> = emptyList()
) : IdempotencyProvider {

    val log: Logger = LoggerFactory.getLogger(IdempotencyProviderImpl::class.java)

    private val phaseExecutor = interceptors.foldRight(BasePhaseExecutor() as IdempotencyInterceptor.PhaseExecutor) {
        interceptor, base ->
        interceptor.wrapPhaseExecutor(base)
    }

    inner class Context(
        override val processId: String,
        override val idempotencyKey: String,
        private val lock: IdempotencyLockAdapter.Lock,
        override val settings: List<IdempotencyContext.Setting>
    ) : IdempotencyContext {

        inner class Phase(
            override val id: String,
            override val final: Boolean,
            override val transactional: Boolean,
            override val settings: List<IdempotencyContext.PhaseData.Setting>
        ) : IdempotencyContext.PhaseData {
            override val context: IdempotencyContext = this@Context
            override val genericStore: ConcurrentMap<String, Any?> = ConcurrentHashMap()
        }

        override val objectMapper: ObjectMapper
            get() = this@IdempotencyProviderImpl.objectMapper

        override val genericStore: ConcurrentMap<String, Any?> = ConcurrentHashMap()

        override val phaseData: IdempotencyContext.PhaseData?
            get() = currentPhaseRef.get()

        override var wasFinalPhase: Boolean = false

        private val currentPhaseRef: AtomicReference<Phase?> = AtomicReference(null)

        private val visitedPhases: AtomicReference<Set<String>> = AtomicReference(emptySet())

        override fun <Output> internalPhase(
            phaseId: String,
            outputType: TypeReference<Output>,
            final: Boolean,
            transactional: Boolean,
            settings: List<IdempotencyContext.PhaseData.Setting>,
            body: () -> Output
        ): Output {
            validateIdentifier("phaseId", phaseId)
            val data = Phase(phaseId, final, transactional, settings)
            return try {
                enterPhase(data)
                phaseExecutor.execute(data, outputType, body)
            } finally {
                leavePhase(data)
            }
        }

        private fun enterPhase(phase: Phase) {
            if (wasFinalPhase) throw IdempotencyAssertionError(
                "Process $processId: You can not enter phase ${phase.id}, because there was already a final phase."
            )

            visitedPhases.getAndUpdate { it + phase.id }.let {
                // `it` represents visited set immediately before update
                if (phase.id in it) throw IdempotencyAssertionError(
                    "Process $processId: Duplicate phaseId: ${phase.id}."
                )
            }

            currentPhaseRef.getAndUpdate { it ?: phase }.let {
                if (it != null) throw IdempotencyAssertionError(
                    "Process $processId: You can not enter phase ${phase.id}, because you are already in phase ${it.id}."
                )
            }
        }

        private fun leavePhase(phase: Phase) {
            if (phase.final) wasFinalPhase = true
            currentPhaseRef.set(null)
        }

        override fun close() {
            val suppressed = mutableListOf<Throwable>()
            interceptors.asReversed().forEach { interceptor ->
                try {
                    interceptor.onContextClosed(this)
                } catch (e: Throwable) {
                    suppressed.add(e)
                }
            }
            try {
                lock.close()
            } catch (e: Throwable) {
                // We are not throwing exception since this should not intercept with successful processing
                log.error(
                    "Idempotency: Attempt to release idempotency lock has failed. " +
                        "It will be released implicitly after specified timeout.",
                    kv(MDC_KEY_LOG_EVENT, LOG_EVENT_RELEASE_IDEMPOTENCY_LOCK_FAILED),
                    e
                )
            }
            if (suppressed.isNotEmpty()) {
                throw SupportException(
                    "Exceptions occurred when closing idempotency context",
                    ERR_SYS_SERVER_ERROR
                ).also { parent -> suppressed.forEach { child -> parent.addSuppressed(child) } }
            }
        }
    }

    override fun unsafeStartIdempotentProcess(
        idempotencyKey: String,
        processId: String,
        lockTimeout: Duration,
        settings: List<IdempotencyContext.Setting>
    ): IdempotencyContext {
        validateIdentifier("processId", processId)
        val inKey = QualifiedIdempotencyKey(processId, idempotencyKey, PROCESS_LOCK_IDENTIFIER)
        val now = Instant.now()

        return when (val decision = lockAdapter.obtainLock(inKey, lockTimeout, now)) {
            is IdempotencyLockAdapter.Result.Obtained -> {
                val ctx = Context(processId, idempotencyKey, decision.lock, settings)
                interceptors.forEach { it.onContextCreated(ctx) }
                ctx
            }
            is IdempotencyLockAdapter.Result.Locked ->
                throw IdempotencyProvider.ProcessLockedException(decision.tryAt, processId, idempotencyKey)
        }
    }

    private inner class BasePhaseExecutor : IdempotencyInterceptor.PhaseExecutor {
        override fun <Output> execute(data: IdempotencyContext.PhaseData, type: TypeReference<Output>, body: () -> Output): Output {
            val key = QualifiedIdempotencyKey(data.context.processId, data.context.idempotencyKey, data.id)
            val existingSerializedOutput = storeAdapter.getOrNull(key)
            return if (existingSerializedOutput != null) {
                log.debug(
                    "Idempotency: Skipping phase, because the result is already stored.",
                    kv(MDC_KEY_LOG_EVENT, LOG_EVENT_IDEMPOTENCY_SKIP_PHASE_DUE_TO_RESULT_STORED)
                )
                objectMapper.readValue(existingSerializedOutput, type)
            } else {
                try {
                    if (data.transactional) {
                        storeAdapter.executeInTransaction { ops ->
                            val output = body()
                            ops.insert(key, objectMapper.writeValueAsString(output), data.final)
                            output
                        }
                    } else {
                        val output = body()
                        storeAdapter.executeInTransaction { ops ->
                            ops.insert(key, objectMapper.writeValueAsString(output), data.final)
                        }
                        output
                    }
                } catch (e: Exception) {
                    if (storeAdapter.mayBeDuplicateRecordException(e)) {
                        val concurrentlySerializedOutput = storeAdapter.getOrNull(key)
                        if (concurrentlySerializedOutput != null) {
                            log.warn(
                                "Idempotency: There were multiple executions of the same phase in parallel. " +
                                    "Making sure the same result was returned in both.",
                                kv(MDC_KEY_LOG_EVENT, LOG_EVENT_IDEMPOTENCY_MULTIPLE_EXECUTION_IN_PARALLEL)
                            )
                            objectMapper.readValue(concurrentlySerializedOutput, type)
                        } else throw e
                    } else throw e
                }
            }
        }
    }

    companion object {
        const val PROCESS_LOCK_IDENTIFIER = "_PROCESS_LOCK"
    }
}
