package playground.common.exception

import playground.common.exception.error.ERR_SYS_IDEMPOTENT_INPUT_DOES_NOT_MATCH
import org.springframework.http.HttpStatus

/** See [validateInputs] */
class IdempotentInputConflict(val conflictingInputIds: Set<String>) : DragonException(
    "Some inputs of an idempotent operation differ from previously used inputs with the same idempotency key. " +
        "Conflicting input ids: ${conflictingInputIds.joinToString(separator = ", ")}",

    ERR_SYS_IDEMPOTENT_INPUT_DOES_NOT_MATCH,

    // HTTP Spec: Indicates a request conflict with current state of the server
    // Our stored idempotency data is indeed our state and request data is in conflict with it
    HttpStatus.CONFLICT
)
