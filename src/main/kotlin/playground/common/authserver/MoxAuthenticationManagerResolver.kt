package playground.common.authserver

import playground.common.auth.MoxAnonymousAuthentication
import playground.common.auth.MoxPreAuthenticationToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.server.resource.BearerTokenError
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes
import org.springframework.stereotype.Component

@Component
class MoxAuthenticationManagerResolver : AuthenticationManagerResolver<MoxPreAuthenticationToken> {

    private val log: Logger = LoggerFactory.getLogger(javaClass)!!

    @Autowired(required = false)
    var customerAuthenticationManager: CustomerAuthenticationManager? = null

    @Autowired(required = false)
    var staffAuthenticationManager: StaffAuthenticationManager? = null

    init {
        if (log.isDebugEnabled) {
            log.debug("Initialising MoxAuthenticationManagerResolver")
        }
    }

    override fun resolve(preAuth: MoxPreAuthenticationToken): AuthenticationManager = preAuth.run {
        if (log.isDebugEnabled) {
            log.debug(
                "Resolving Mox Auth with [CustomerAuthenticationManager: {}, StaffAuthenticationManager: {}]",
                customerAuthenticationManager,
                staffAuthenticationManager
            )
        }
        when {
            staffIdToken != null ->
                staffAuthenticationManager
                    ?: throw InternalAuthenticationServiceException("Okta JWT configuration missing")
            customerAccessToken == null && customerIdToken == null ->
                AuthenticationManager { MoxAnonymousAuthentication() }
            customerAccessToken != null && customerIdToken != null ->
                customerAuthenticationManager
                    ?: throw InternalAuthenticationServiceException("ForgeRock JWT configuration missing")
            customerAccessToken == null -> throw invalidAuthorizationRequestException("Missing customer access token but customer id token found")
            customerIdToken == null -> throw invalidAuthorizationRequestException("Missing customer id token but customer access token found")
            else -> throw invalidAuthorizationRequestException(
                "Unknown token combination: " +
                    "customerAccessToken=$customerAccessToken, customerIdToken=$customerIdToken, staffIdToken=$staffIdToken"
            )
        }
    }

    private fun invalidAuthorizationRequestException(msg: String) = OAuth2AuthenticationException(
        BearerTokenError(BearerTokenErrorCodes.INVALID_REQUEST, HttpStatus.UNAUTHORIZED, msg, null)
    )
}
