package playground.common.authserver

import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes

internal fun JwtException.toInvalidTokenException(prependMessage: String? = null): OAuth2AuthenticationException {
    val description = prependMessage?.let { "$it$message" } ?: message
    return OAuth2AuthenticationException(
        OAuth2Error(BearerTokenErrorCodes.INVALID_TOKEN, description, null),
        description,
        this
    )
}
