package playground.common.rest.security

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import playground.common.auth.MoxHeaders.HEADER_ID_TOKEN
import playground.common.auth.MoxHeaders.HEADER_STAFF_ID_TOKEN
import playground.common.auth.MoxPreAuthenticationToken
import playground.common.auth.MoxPrincipal
import playground.common.auth.MoxSecurity
import playground.common.authserver.MoxAuthenticationManagerResolver
import com.projectdrgn.common.rest.HEADER_CUSTOMER_ID
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import playground.common.rest.logging.LOG_EVENT_OAUTH2_AUTHENTICATION_FAILED
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.HeaderBearerTokenResolver
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import playground.common.exception.error.DragonErrorResponse
import playground.common.exception.error.ERR_SYS_ACCESS_DENIED
import playground.common.observability.logging.*

class MoxOAuth2Filter(
    requestMatcher: RequestMatcher,
    private val moxAuthenticationManagerResolver: MoxAuthenticationManagerResolver
) : AbstractAuthenticationProcessingFilter(requestMatcher) {
    val log: Logger = LoggerFactory.getLogger(MoxOAuth2Filter::class.java)

    private val accessTokenResolver: BearerTokenResolver = DefaultBearerTokenResolver()
    private val idTokenResolver = HeaderBearerTokenResolver(HEADER_ID_TOKEN)
    private val staffIdTokenResolver = HeaderBearerTokenResolver(HEADER_STAFF_ID_TOKEN)
    private val authenticationEntryPoint: AuthenticationEntryPoint = BearerTokenAuthenticationEntryPoint()

    override fun attemptAuthentication(request: HttpServletRequest, response: HttpServletResponse): Authentication? =
        arrayOf(accessTokenResolver, idTokenResolver, staffIdTokenResolver)
            .map { it.resolve(request) }
            .let { (customerAccessToken, customerIdToken, staffIdToken) ->
                val preAuth = MoxPreAuthenticationToken(
                    customerAccessToken = customerAccessToken,
                    customerIdToken = customerIdToken,
                    staffIdToken = staffIdToken,
                    customerIdInHeader = request.getHeader(HEADER_CUSTOMER_ID)
                )
                moxAuthenticationManagerResolver.resolve(preAuth).authenticate(preAuth)
            }

    private fun addMDCInfo() {
        MoxSecurity.authentication?.let { auth ->
            MDC.put(MDC_KEY_AUTHENTICATION_TYPE, auth::class.java.name)
            auth.principal?.let { MDC.put(MDC_KEY_PRINCIPAL_TYPE, it::class.java.name) }
        }
        if (MoxSecurity.isStaff) MDC.put(MDC_KEY_STAFF_NAME, (MoxSecurity.principal as MoxPrincipal.MoxStaff).name)
        MoxSecurity.principal?.customerId?.let { MDC.put(MDC_KEY_OAUTH2_CUSTOMER_ID, it) }
        MoxSecurity.acrLevel?.let { MDC.put(MDC_KEY_OAUTH2_ACR_LEVEL, it.value) }
        MoxSecurity.amr?.let { MDC.put(MDC_KEY_OAUTH2_AMR, it) }
        MoxSecurity.transactionId?.let { MDC.put(MDC_KEY_OAUTH2_TRANSACTION_ID, it) }
    }

    private fun removeMDCInfo() {
        MDC.remove(MDC_KEY_AUTHENTICATION_TYPE)
        MDC.remove(MDC_KEY_OAUTH2_CUSTOMER_ID)
        MDC.remove(MDC_KEY_STAFF_NAME)
        MDC.remove(MDC_KEY_PRINCIPAL_TYPE)
        MDC.remove(MDC_KEY_OAUTH2_ACR_LEVEL)
        MDC.remove(MDC_KEY_OAUTH2_AMR)
        MDC.remove(MDC_KEY_OAUTH2_TRANSACTION_ID)
    }

    override fun successfulAuthentication(
        request: HttpServletRequest,
        response: HttpServletResponse?,
        chain: FilterChain,
        authResult: Authentication
    ) {
        SecurityContextHolder.getContext().authentication = authResult

        addMDCInfo()

        try {
            chain.doFilter(request, response)
        } finally {
            removeMDCInfo()
        }
    }

    override fun unsuccessfulAuthentication(
        request: HttpServletRequest,
        response: HttpServletResponse?,
        failed: AuthenticationException
    ) {
        val dragonErrorResponse = DragonErrorResponse(ERR_SYS_ACCESS_DENIED, failed.message)

        log.warn(failed.message, dragonErrorResponse, kv(MDC_KEY_LOG_EVENT, LOG_EVENT_OAUTH2_AUTHENTICATION_FAILED))

        SecurityContextHolder.clearContext()
        authenticationEntryPoint.commence(request, response, failed)

        response?.writer?.print(jacksonObjectMapper().writeValueAsString(dragonErrorResponse))
    }
}
