package playground.common.auth.exception

import playground.common.auth.error.ERR_AUTH_ACTION_BLOCKED
import org.springframework.http.HttpStatus
import playground.common.exception.DragonException

class ActionBlockedException(
    override val message: String = "Access Denied"
) : DragonException(message, ERR_AUTH_ACTION_BLOCKED, HttpStatus.FORBIDDEN, null)
