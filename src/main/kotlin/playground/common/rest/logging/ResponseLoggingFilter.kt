package playground.common.rest.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper
import playground.common.observability.logging.*
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant
import kotlin.math.min

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // to be executed after RequestLoggingFilter which is setting MDCs
class ResponseLoggingFilter(
    @Value("\${dragon.logging.response-body.size-limit}")
    private val responseBodySizeLimit: Int,
    @Value("\${spring.profiles.active:}")
    private val activeProfile: String
) : OncePerRequestFilter() {
    private val log: Logger = LoggerFactory.getLogger(javaClass)!!

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val wrappedResponse = ContentCachingResponseWrapper(response)
        try {
            MDC.put(MDC_KEY_EVENT_CHANNEL, LOG_EVENT_HTTP)
            val start = Instant.now()
            filterChain.doFilter(request, wrappedResponse)
            val finish = Instant.now()
            val elapsedTime: Long = Duration.between(start, finish).toMillis()

            val logArgumentList = mutableListOf(
                kv(MDC_KEY_RESPONSE_CODE, response.status),
                kv(MDC_KEY_ELAPSED_TIME, elapsedTime),
                kv(MDC_KEY_LOG_EVENT, LOG_EVENT_HTTP_RESPONSE)
            ).apply {
                if (activeProfile.contains("rest-response-body-debug-logging")) {
                    add(kv(MDC_KEY_RESPONSE_BODY, getResponsePayload(wrappedResponse)))
                }
            }

            if (wrappedResponse.contentType == MediaType.APPLICATION_JSON_VALUE) {
                log.info(
                    "HTTP response: ${request.method} ${request.requestURI}",
                    *logArgumentList.toTypedArray()
                )
            }
        } finally {
            wrappedResponse.copyBodyToResponse()
        }
    }

    private fun getResponsePayload(response: ContentCachingResponseWrapper): String {
        val buf = response.contentAsByteArray
        if (buf.isNotEmpty()) {
            val length = min(buf.size, responseBodySizeLimit)
            return try {
                String(buf, 0, length, Charset.forName(response.characterEncoding))
            } catch (ex: UnsupportedEncodingException) {
                "[cannot read response]"
            }
        }
        return "[empty]"
    }
}
