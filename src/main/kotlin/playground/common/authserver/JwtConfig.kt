package playground.common.authserver

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

@Configuration
@EnableConfigurationProperties(DragonOAuth2ConfigurationProperties::class)
class JwtConfig(private val dragonOAuth2ConfigurationProperties: DragonOAuth2ConfigurationProperties) {
    @Bean
    @Conditional(ForgeRockCondition::class)
    fun forgeRockJwtDecoder(): JwtDecoder = dragonOAuth2ConfigurationProperties.forgeRock!!
        .run {
            when {
                !jwkURI.isNullOrBlank() ->
                    NimbusJwtDecoder
                        .withJwkSetUri(jwkURI)
                        .let { builder ->
                            jwkSetCacheTtl
                                .let { ttl ->
                                    val cache: Cache<Any, Any> = Caffeine.newBuilder().expireAfterWrite(ttl).build()
                                    builder.cache(CaffeineCache("jwk-set", cache))
                                } ?: builder
                        }
                        .build()
                !issuerURI.isNullOrBlank() -> JwtDecoders.fromOidcIssuerLocation(issuerURI)
                else ->
                    NimbusJwtDecoder
                        .withPublicKey(publicKeyLocation)
                        .signatureAlgorithm(RS256)
                        .build()
            }
        }

    @Bean
    @Conditional(OktaCondition::class)
    fun oktaJwtDecoder(): JwtDecoder = dragonOAuth2ConfigurationProperties.okta!!
        .run {
            if (!issuerURI.isNullOrBlank()) JwtDecoders.fromOidcIssuerLocation(issuerURI)
            else NimbusJwtDecoder.withPublicKey(publicKeyLocation).signatureAlgorithm(RS256).build()
        }
}
