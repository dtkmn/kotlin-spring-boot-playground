package playground.common.rest.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain


@Configuration
@EnableWebSecurity
//@EnableConfigurationProperties(RestBasicAuthProperties::class)
//@Import(
////    MoxBasicAuthSecurityConfigurer::class,
////    MoxOAuth2SecurityConfigurer::class,
////    JwtConfig::class,
////    MoxAuthenticationManagerConfig::class
//)
class SecurityConfig {
    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain? {
        http
            .csrf().disable()
            .authorizeHttpRequests().anyRequest().permitAll()
        return http.build()
    }
}
