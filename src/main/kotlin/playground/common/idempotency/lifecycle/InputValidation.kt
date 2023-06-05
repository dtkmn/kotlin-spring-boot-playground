package playground.common.idempotency.lifecycle

import com.fasterxml.jackson.databind.JsonMappingException
import playground.common.exception.IdempotentInputConflict
import playground.common.idempotency.IDENTIFIER_REGEX
import playground.common.idempotency.IdempotencyContext
import playground.common.idempotency.logging.LOG_EVENT_IDEMPOTENCY_INPUT_DIFFER_FROM_CAHCED
import playground.common.idempotency.phase
import playground.common.idempotency.validateIdentifier
import org.slf4j.LoggerFactory
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv

object InputValidation
private val log = LoggerFactory.getLogger(InputValidation::class.java)!!

/**
 * See [validateProcessInputs]
 *
 * Note that this is v2 as we have changed the serialization format.
 */
const val INPUT_PHASE_IDENTIFIER = "_input_v2"

/** See [validateSingleProcessInput] */
const val SINGLE_INPUT_KEY = "theInput"

/**
 * Ensures that inputs of idempotent process are same when the process is retried.
 *
 * Multiple inputs can be specified, but this is to combine business inputs from different sources like
 * body, path parameters or headers. If you have only single input (e.g. kafka message), use [validateSingleProcessInput].
 *
 * @param inputs Map of unique input ids matching [IDENTIFIER_REGEX] to any Jackson serializable values.
 * @throws IdempotentInputConflict when specified inputs do not match cached inputs from previous execution.
 *
 * To support code evolution it is allowed to add or remove new inputs. This only logs warning but does not throw.
 *
 * Internally uses [IdempotencyContext.phaseData] with identifier [INPUT_PHASE_IDENTIFIER].
 *
 * Comparision uses serialized json nodes, not the actual values.
 */
fun IdempotencyContext.validateProcessInputs(inputs: Map<String, Any?>) {
    inputs.keys.forEach { validateIdentifier("inputId", it) }

    // We need to persist generic values, so they can be deserialized back.
    // Using JsonNode looses distinction between doubles and decimal,
    // therefore we use serialized strings, introducing double serialization.
    val dataToSave = inputs.mapValues {
        objectMapper.writeValueAsString(it.value)
    }
    val savedData = phase(INPUT_PHASE_IDENTIFIER, false) { dataToSave }

    if (inputs.keys != savedData.keys) {
        log.warn(
            "Set of validated inputs differs from cached values. " +
                "Added input ids: ${dataToSave.keys - savedData.keys}. " +
                "Removed input ids: ${savedData.keys - dataToSave.keys}.",
            kv(MDC_KEY_LOG_EVENT, LOG_EVENT_IDEMPOTENCY_INPUT_DIFFER_FROM_CAHCED)
        )
    }

    // To compare, we deserialize. This way squads have control over
    val suppressedExceptions = mutableSetOf<Throwable>()
    val conflictingKeys = mutableSetOf<String>()

    (inputs.keys intersect savedData.keys).forEach { k ->
        try {
            val inputValue = inputs[k]
            val savedValue = objectMapper.readValue(savedData[k], getDeserializationClass(inputValue))
            if (inputValue != savedValue) {
                conflictingKeys.add(k)
            }
        } catch (e: JsonMappingException) {
            // If there is a difference in structure we consider it to be a conflict
            conflictingKeys.add("$k (${e.message})")
            suppressedExceptions.add(e)
        }
    }

    if (conflictingKeys.isNotEmpty()) {
        throw IdempotentInputConflict(conflictingKeys).apply {
            suppressedExceptions.forEach { addSuppressed(it) }
        }
    }
}

internal fun getDeserializationClass(instance: Any?): Class<*> {
    return when (instance) {
        null -> Any::class.java
        is Map<*, *> -> Map::class.java
        is Set<*> -> Set::class.java
        is Iterable<*> -> List::class.java
        else -> instance.javaClass
    }
}

/**
 * Convenience version of [validateProcessInputs] using [SINGLE_INPUT_KEY] as key for single [input]
 */
fun IdempotencyContext.validateSingleProcessInput(input: Any?) = validateProcessInputs(mapOf(SINGLE_INPUT_KEY to input))
