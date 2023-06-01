package playground.common.authserver

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.getProperty
import org.springframework.core.type.AnnotatedTypeMetadata
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Duration

@ConfigurationProperties("dragon.security.oauth2")
data class DragonOAuth2ConfigurationProperties(
    var enabled: Boolean = false,
    var forgeRock: ProviderConfigurationProperties? = null,
    var okta: ProviderConfigurationProperties? = null
)

data class ProviderConfigurationProperties(
    var issuerURI: String? = null,
    var publicKeyLocation: RSAPublicKey? = null,
    var privateKeyLocation: RSAPrivateKey? = null, // WARN: won't work if issuerURI is used
    var jwkURI: String? = null,
    var jwkSetCacheTtl: Duration = Duration.ofMinutes(30)
)

class ForgeRockCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val enabled: Boolean = context["enabled"] ?: false
        val jwkURI: String? = context["forge-rock.jwk-uri"]
        val issuerURI: String? = context["forge-rock.issuer-uri"]
        val publicKey: String? = context["forge-rock.public-key-location"]
        return enabled && (!jwkURI.isNullOrBlank() || !issuerURI.isNullOrBlank() || publicKey != null)
    }
}

class OktaCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val enabled: Boolean = context["enabled"] ?: false
        val issuerURI: String? = context["okta.issuer-uri"]
        val publicKey: String? = context["okta.public-key-location"]
        return enabled && (!issuerURI.isNullOrBlank() || publicKey != null)
    }
}

private inline operator fun <reified T> ConditionContext.get(suffix: String): T? =
    environment.getProperty<T>("dragon.security.oauth2.$suffix")
