package playground.common.idempotency

import playground.common.exception.DragonException
import playground.common.exception.error.ERR_SYS_IDEMPOTENCY_ASSERTION_ERROR
import org.springframework.http.HttpStatus

/**
 * Thrown when the idempotency API is used in the improper way.
 *
 * Should be only encountered in the tests.
 *
 * @property message is targeted to developer to help identify the issue.
 */
class IdempotencyAssertionError(message: String) : DragonException(
    message,
    ERR_SYS_IDEMPOTENCY_ASSERTION_ERROR,
    HttpStatus.INTERNAL_SERVER_ERROR
)
