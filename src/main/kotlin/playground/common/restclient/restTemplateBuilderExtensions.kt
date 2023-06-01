package playground.common.restclient

import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.util.TimeValue
import playground.common.restclient.logging.LOG_EVENT_REST_REQUEST
import playground.common.restclient.logging.MDC_KEY_REST_BODY
import playground.common.restclient.logging.MDC_KEY_REST_REQUEST_CONTENT_TYPE
import playground.common.restclient.logging.MDC_KEY_REST_REQUEST_LENGTH
import playground.common.restclient.logging.MDC_KEY_REST_REQUEST_METHOD
import playground.common.restclient.logging.MDC_KEY_REST_REQUEST_URI
import playground.common.restclient.logging.PROFILE_REST_BODY_DEBUG_LOGGING
import playground.common.restclient.logging.PROFILE_REST_CLIENT_DEBUG_LOGGING
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.env.Environment
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.client.*
import org.springframework.stereotype.Component
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import java.io.UnsupportedEncodingException
import java.util.function.Supplier
import kotlin.math.min

fun RestTemplateBuilder.withMoxAuthPropagation(): RestTemplateBuilder =
    StaticRestClientHelper4J.withMoxAuthPropagation(this)

/**
 * Use standard apache [HttpClient] connection pool configured by system properties.
 *
 * There is only one difference from standard behavior. If the server does not set keep alive header,
 * the connection is not kept forever but rather capped to [MOX_CONNECTION_KEEP_ALIVE_DEFAULT].
 */
fun RestTemplateBuilder.withMoxConnectionPool(maxConnectionsPerRoute: Int, maxConnectionsTotal: Int): RestTemplateBuilder =
    this
        .requestFactory(Supplier<ClientHttpRequestFactory> {
            val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnPerRoute(maxConnectionsPerRoute)
                .setMaxConnTotal(maxConnectionsTotal)
                .build()

            val defaultStrategy = DefaultConnectionKeepAliveStrategy()

            val httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .useSystemProperties()
                .setKeepAliveStrategy { response, context ->
                    val result = defaultStrategy.getKeepAliveDuration(response, context)
                    if (result > TimeValue.ZERO_MILLISECONDS) result else TimeValue.ofMilliseconds(MOX_CONNECTION_KEEP_ALIVE_DEFAULT)
                }
                .build()

            HttpComponentsClientHttpRequestFactory(httpClient)
        })


//fun RestTemplateBuilder.withMoxConnectionPool(maxConnectionsPerRoute: Int, maxConnectionsTotal: Int): RestTemplateBuilder =
//    this
//        .requestFactory {
//            HttpComponentsClientHttpRequestFactory(
//                HttpClientBuilder.create().apply {
//                    useSystemProperties()
//                    setMaxConnPerRoute(maxConnectionsPerRoute)
//                    setMaxConnTotal(maxConnectionsTotal)
//
//                    val defaultStrategy = DefaultConnectionKeepAliveStrategy()
//                    setKeepAliveStrategy { response, context ->
//                        val result = defaultStrategy.getKeepAliveDuration(response, context)
//                        if (result > 0) result else MOX_CONNECTION_KEEP_ALIVE_DEFAULT
//                    }
//                }.build() as HttpClient
//            )
//        }

/**
 * Value in milliseconds to keep connection in pool alive in case server does not set the header.
 * There is no data to justify this value, its random. Feel free to change when you know, what you do.
 */
const val MOX_CONNECTION_KEEP_ALIVE_DEFAULT = 20000L

@Component
class MoxRestClientLoggingInterceptor(
    @Value("\${dragon.logging.rest-body.size-limit:5120}")
    private val requestBodySizeLimit: Int,
    private val env: Environment
) : ClientHttpRequestInterceptor {
    private val log = LoggerFactory.getLogger(MoxRestClientLoggingInterceptor::class.java)

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        if (isRestClientLoggingIsEnabled() && MediaType.APPLICATION_JSON == request.headers.contentType) {
            log.info(
                "Rest Request: ${request.method} ${request.uri}",
                kv(MDC_KEY_REST_BODY, getRequestPayload(request, body)),
                kv(MDC_KEY_REST_REQUEST_CONTENT_TYPE, request.headers.contentType.toString()),
                kv(MDC_KEY_REST_REQUEST_LENGTH, body.size),
                kv(MDC_KEY_REST_REQUEST_METHOD, request.method!!),
                kv(MDC_KEY_REST_REQUEST_URI, request.uri),
                kv(MDC_KEY_LOG_EVENT, LOG_EVENT_REST_REQUEST)
            )
        }
        return execution.execute(request, body)
    }

    private fun getRequestPayload(request: HttpRequest, body: ByteArray): String {
        if (body.isNotEmpty()) {
            val length = min(body.size, requestBodySizeLimit)
            return try {
                String(body, 0, length).takeIf { isRestBodyLoggingEnabled() } ?: "#####"
            } catch (ex: UnsupportedEncodingException) {
                "[cannot read request body]"
            }
        }
        return "[empty]"
    }

    private fun isRestBodyLoggingEnabled() = env.activeProfiles.contains(PROFILE_REST_BODY_DEBUG_LOGGING)

    private fun isRestClientLoggingIsEnabled() = env.activeProfiles.contains(PROFILE_REST_CLIENT_DEBUG_LOGGING)
}
