package playground.common.rest.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("playground.rest.basic-auth")
class RestBasicAuthProperties {
    var enabled: Boolean = false
    lateinit var username: String
    lateinit var password: String
    var matchers: Array<String> = emptyArray()

    fun isEnabled() = enabled && !username.isBlank() && !password.isBlank() && matchers.isNotEmpty()
}
