package playground.common.exception.error

import datadog.trace.api.CorrelationIdentifier

/**
 * Error response to returned to the front-end from:
 * - REST API Controller
 */
open class DragonErrorResponse(
    open val errorCode: String,
    open val debugMessage: String? = null,
    open val validationErrors: Map<String, Any>? = null
) {
    val traceId: String? = CorrelationIdentifier.getTraceId()
    val spanId: String? = CorrelationIdentifier.getSpanId()
}
