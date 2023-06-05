package playground.common.auth

import org.springframework.security.authentication.AbstractAuthenticationToken

data class MoxPreAuthenticationToken(
    val customerAccessToken: String? = null,
    val customerIdToken: String? = null,
    val staffIdToken: String? = null,
    val customerIdInHeader: String? = null
) : AbstractAuthenticationToken(emptyList()) {
    override fun getCredentials() = toString()
    override fun getPrincipal() = toString()
}
