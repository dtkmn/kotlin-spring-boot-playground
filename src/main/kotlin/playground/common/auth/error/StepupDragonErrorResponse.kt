package playground.common.auth.error

import playground.common.exception.error.DragonErrorResponse

/**
 * Step up error response to returned to the front-end from:
 * - REST API Controller
 */
class StepupDragonErrorResponse(
    override val errorCode: String,
    override val debugMessage: String? = null,
    override val validationErrors: Map<String, Any>? = null,
    // generic reference id for current user step up action. E.g : transaction Id if user perform payment step up
    val userActionReferenceId: String? = null,
    // user step up action identifier
    val userActionCode: String? = null,
    // allowed auth factors for user authentication
    val allowedAuthFactors: List<String>
) : DragonErrorResponse(errorCode, debugMessage, validationErrors)
