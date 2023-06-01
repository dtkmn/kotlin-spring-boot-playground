package playground.common.exception

import playground.common.exception.error.ERR_SYS_VALIDATION_ERROR
import org.springframework.http.HttpStatus

/**
 * A ValidationException should be thrown when validation of request data fails.
 */
class ValidationException(
    override val message: String,
    override val errorCode: String,
    override val cause: Throwable? = null,
    val validationErrors: Map<String, Any>? = null
) : DragonException(message, errorCode, HttpStatus.UNPROCESSABLE_ENTITY, cause) {

    @Deprecated("Constructor with default errorCode will be removed.")
    constructor(
        message: String,
        cause: Throwable? = null
    ) : this(message, ERR_SYS_VALIDATION_ERROR, cause)
}
