package playground.common.rest.logging

import com.projectdrgn.common.rest.HEADER_CUSTOMER_ID
import com.projectdrgn.common.rest.HEADER_REQUEST_ID
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.AbstractRequestLoggingFilter
import playground.common.observability.logging.*

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter(
    private val env: Environment,
    private val isActuator: (HttpServletRequest) -> Boolean,
    @Value("\${dragon.logging.body.size-limit}")
    private val requesBodySizeLimit: Int
) : AbstractRequestLoggingFilter() {
    private val log: Logger = LoggerFactory.getLogger(javaClass)!!

    @Autowired
    lateinit var buildInfoToMdc: BuildInfoToMdc

    override fun shouldLog(request: HttpServletRequest) = !isActuator(request)

    override fun afterRequest(request: HttpServletRequest, message: String) {
        MDC.clear()
    }

    override fun beforeRequest(request: HttpServletRequest, message: String) {
        val payload = when {
            request is BufferedWrapper -> redactIfNeeded(request.body())
            isRequestBodySizeOverLimit(request) -> "Request body size is over limit for logging ($requesBodySizeLimit bytes)"
            else -> "#####"
        }

        log.info(
            "HTTP request: ${request.method} ${request.requestURI}",
            kv(MDC_KEY_REQUEST_BODY, payload),
            kv(MDC_KEY_LOG_EVENT, LOG_EVENT_HTTP_REQUEST)
        )
    }

    fun redactIfNeeded(content: String): String = content.takeIf { isMessageDebugLoggingEnabled() } ?: "######"

    private fun isMessageDebugLoggingEnabled(): Boolean = env.activeProfiles.contains(PROFILE_REST_BODY_DEBUG_LOGGING)

    private fun isRequestBodySizeOverLimit(request: HttpServletRequest) = request.contentLength > requesBodySizeLimit

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        MDC.clear()

        val requestId: String? = request.getHeader(HEADER_REQUEST_ID)
        val customerId: String? = request.getHeader(HEADER_CUSTOMER_ID)

        MDC.put(MDC_KEY_CUSTOMER_ID, customerId)
        MDC.put(MDC_KEY_REQUEST_ID, requestId)
        MDC.put(MDC_KEY_REQUEST_URI, request.requestURI)
        MDC.put(MDC_KEY_REQUEST_METHOD, request.method)
        MDC.put(MDC_KEY_REQUEST_HEADER_CONTENT_TYPE, request.getHeader(HttpHeaders.CONTENT_TYPE))
        MDC.put(MDC_KEY_REQUEST_HEADER_CONTENT_LENGTH, request.getHeader(HttpHeaders.CONTENT_LENGTH))

        buildInfoToMdc.putAll()

        // We want to use BufferedWrapper only when we are sure that we will log request body
        // if we do not read body, fetching input stream will fail in downstream
        if (isMessageDebugLoggingEnabled() && !isRequestBodySizeOverLimit(request) &&
            request.contentType == MediaType.APPLICATION_JSON_VALUE
        ) {
            super.doFilterInternal(BufferedWrapper(request), response, filterChain)
        } else {
            super.doFilterInternal(request, response, filterChain)
        }
    }
}
