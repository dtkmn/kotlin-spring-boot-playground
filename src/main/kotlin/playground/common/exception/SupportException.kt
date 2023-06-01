package playground.common.exception

import playground.common.exception.error.ERR_SYS_SUPPORT_ERROR
import org.springframework.http.HttpStatus

/**
 * A SupportException should be thrown when the system appears to be operational
 * but the retrieved data indicates that something the setup or state is wrong.
 */
class SupportException(
    override val message: String,
    override val errorCode: String,
    override val cause: Throwable? = null
) : DragonException(message, errorCode, HttpStatus.CONFLICT, cause) {

    @Deprecated("Constructor with default errorCode will be removed.")
    constructor(
        message: String,
        cause: Throwable? = null
    ) : this(message, ERR_SYS_SUPPORT_ERROR, cause)
}
