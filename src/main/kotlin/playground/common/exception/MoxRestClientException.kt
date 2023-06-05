package playground.common.exception

import org.springframework.http.HttpStatus

class MoxRestClientException(
    override val statusCode: HttpStatus,
    override val errorCode: String,
    val debugMessage: String,
    val validationErrors: Map<String, Any>? = null
) : DragonException(debugMessage, errorCode, statusCode, null)
