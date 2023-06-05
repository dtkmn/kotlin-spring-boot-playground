package playground.common.restclient

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.client.ClientHttpRequestInterceptor
import playground.common.auth.MoxHeaders
import playground.common.auth.MoxSecurity

/**
 * Used by openapi codegen Java client ApiClientConfig only
 */
class StaticRestClientHelper4J {
    companion object {
        fun withMoxAuthPropagation(restTemplateBuilder: RestTemplateBuilder): RestTemplateBuilder = restTemplateBuilder.additionalInterceptors(
            ClientHttpRequestInterceptor { request, body, execution ->
                (
                    MoxSecurity.moxAuthentication
                        ?.let { (customerAccessToken, customerIdToken, staffIdToken) ->
                            request.apply {
                                customerAccessToken?.let { headers.setBearerAuth(it.tokenValue) }
                                customerIdToken?.let { headers[MoxHeaders.HEADER_ID_TOKEN] = it.tokenValue }
                                staffIdToken?.let { headers[MoxHeaders.HEADER_STAFF_ID_TOKEN] = it.tokenValue }
                            }
                        }
                        ?: request
                    )
                    .let { execution.execute(it, body) }
            }
        )
    }
}
