package playground.common.exception

import org.springframework.http.HttpStatus

abstract class DragonException(
    override val message: String,
    open val errorCode: String,
    open val statusCode: HttpStatus,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)
