package playground.common.authserver

import playground.common.auth.MoxAuthentication
import playground.common.auth.MoxPreAuthenticationToken
import playground.common.auth.MoxPrincipal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

@Component
@Conditional(OktaCondition::class)
class StaffAuthenticationManager(
    private val oktaJwtDecoder: JwtDecoder,
    private val forgeRockJwtDecoder: JwtDecoder? = null
) : AuthenticationManager {

    private val log: Logger = LoggerFactory.getLogger(javaClass)!!

    init {
        if (log.isDebugEnabled) {
            log.debug(
                "Initialising StaffAuthenticationManager with [forgeRockJwtDecoder: {}, oktaJwtDecoder: {}]",
                forgeRockJwtDecoder,
                oktaJwtDecoder
            )
        }
    }

    override fun authenticate(auth: Authentication): Authentication =
        if (auth is MoxPreAuthenticationToken) {
            val idToken = try {
                oktaJwtDecoder.decode(auth.staffIdToken)
            } catch (failed: JwtException) {
                throw failed.toInvalidTokenException("staffIdToken: ")
            }
            val customerIdToken = auth.customerIdToken?.let { tokenStr ->
                try {
                    forgeRockJwtDecoder?.decode(tokenStr)
                } catch (failed: JwtException) {
                    throw failed.toInvalidTokenException("customerIdToken: ")
                }
            }

            if (customerIdToken != null)
                validateCustomerId(auth.customerIdInHeader, customerIdToken.subject)

            MoxAuthentication(
                staffIdToken = idToken,
                customerIdToken = customerIdToken,
                principal = MoxPrincipal.MoxStaff(idToken)
            )
        } else auth

    private fun validateCustomerId(customerIdInHeader: String?, customerIdInIdToken: String?) {
        customerIdInHeader
            ?.takeIf { customerIdInIdToken != it }
            ?.run {
                throw JwtException("Invalid ID Token mismatching with customerId in headers").toInvalidTokenException()
            }
    }
}
