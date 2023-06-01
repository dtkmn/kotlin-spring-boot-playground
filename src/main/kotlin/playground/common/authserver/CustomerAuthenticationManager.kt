package playground.common.authserver

import playground.common.auth.MoxAuthentication
import playground.common.auth.MoxPreAuthenticationToken
import playground.common.auth.MoxPrincipal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.server.resource.BearerTokenError
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes
import org.springframework.stereotype.Component

@Component
@Conditional(ForgeRockCondition::class)
class CustomerAuthenticationManager(private val forgeRockJwtDecoder: JwtDecoder) : AuthenticationManager {

    private val log: Logger = LoggerFactory.getLogger(javaClass)!!

    init {
        if (log.isDebugEnabled) {
            log.debug(
                "Initialising CustomerAuthenticationManager with [forgeRockJwtDecoder: {}]",
                forgeRockJwtDecoder
            )
        }
    }

    override fun authenticate(auth: Authentication): Authentication =
        if (auth is MoxPreAuthenticationToken) {
            val accessToken = try {
                forgeRockJwtDecoder.decode(auth.customerAccessToken)
            } catch (failed: JwtException) {
                throw failed.toInvalidTokenException("accessToken: ")
            }
            val idToken = try {
                forgeRockJwtDecoder.decode(auth.customerIdToken)
            } catch (failed: JwtException) {
                throw failed.toInvalidTokenException("idToken: ")
            }

            checkIfSubjectsMatched(accessToken.subject, idToken.subject)

            if (accessToken.subject == MoxPrincipal.UnboundDevice.SUBJECT) {
                MoxAuthentication(
                    customerAccessToken = accessToken,
                    customerIdToken = idToken,
                    principal = MoxPrincipal.UnboundDevice()
                )
            } else {
                validateCustomerId(auth.customerIdInHeader, idToken.subject)
                MoxAuthentication(
                    customerAccessToken = accessToken,
                    customerIdToken = idToken,
                    principal = MoxPrincipal.MoxCustomer(idToken)
                )
            }
        } else auth

    private fun checkIfSubjectsMatched(subjectOfAccessToken: String, subjectOfIdToken: String) {
        if (subjectOfAccessToken != subjectOfIdToken) {
            val err = BearerTokenError(
                BearerTokenErrorCodes.INVALID_TOKEN,
                HttpStatus.UNAUTHORIZED,
                "CustomerId (subject) mismatched: the one of accessToken: $subjectOfAccessToken while that of idToken: $subjectOfIdToken",
                null
            )
            throw OAuth2AuthenticationException(err)
        }
    }

    private fun validateCustomerId(customerIdInHeader: String?, customerIdInIdToken: String) {
        customerIdInHeader
            ?.takeIf { customerIdInIdToken != it }
            ?.run {
                throw JwtException("Invalid ID Token mismatching with customerId in headers").toInvalidTokenException()
            }
    }
}
