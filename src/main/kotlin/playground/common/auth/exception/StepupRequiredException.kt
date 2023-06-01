package playground.common.auth.exception

import playground.common.auth.error.ERR_AUTH_STEPUP_REQUIRED
import org.springframework.http.HttpStatus
import playground.common.exception.DragonException

class StepupRequiredException(
    override val message: String = "Access Denied",
    val userActionCode: String,
    val userActionReferenceId: String,
    val allowedAuthFactors: List<String>
) : DragonException(message, ERR_AUTH_STEPUP_REQUIRED, HttpStatus.FORBIDDEN, null)
