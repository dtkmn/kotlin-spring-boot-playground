package playground.common.exception

import playground.common.exception.error.ERR_SYS_RESOURCE_NOT_FOUND
import org.springframework.http.HttpStatus

/**
 * A RecordNotExistException should be thrown when entity for requested Id is not found.
 */
class RecordNotExistException(
    override val message: String,
    override val errorCode: String,
    override val cause: Throwable? = null
) : DragonException(message, errorCode, HttpStatus.NOT_FOUND, cause) {

    @Deprecated("Constructor with default errorCode will be removed.")
    constructor(
        message: String,
        cause: Throwable? = null
    ) : this(message, ERR_SYS_RESOURCE_NOT_FOUND, cause)
}
