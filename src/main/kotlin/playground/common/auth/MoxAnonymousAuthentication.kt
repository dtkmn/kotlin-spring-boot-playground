package playground.common.auth

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils

class MoxAnonymousAuthentication :
    AbstractAuthenticationToken(AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_ANONYMOUS")) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any = "anonymousUser for service to service temporarily"
    override fun getPrincipal(): Any = "anonymousUser for service to service temporarily"
}
