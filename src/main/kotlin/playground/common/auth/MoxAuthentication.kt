package playground.common.auth

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import playground.common.auth.MoxPrincipal

data class MoxAuthentication(
    val customerAccessToken: Jwt? = null,
    val customerIdToken: Jwt? = null,
    val staffIdToken: Jwt? = null,
    val principal: MoxPrincipal
) : AbstractAuthenticationToken(emptyList()) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials() = principal
    override fun getPrincipal(): Any? = principal
}
