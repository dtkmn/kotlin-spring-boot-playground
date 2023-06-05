package playground.common.authserver

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.security.authentication.AuthenticationManager

//@Import(
//    MoxAuthenticationManagerResolver::class,
//    CustomerAuthenticationManager::class,
//    StaffAuthenticationManager::class
//)
@Configuration
class MoxAuthenticationManagerConfig {

    /**
     * To fulfil HttpSecurityConfiguration introduced since 5.4.0 and it's only use case is to expose HttpSecurity bean
     * which is not used anyway.
     */
    @Primary
    @Bean
    fun defaultAuthenticationManager(): AuthenticationManager? = null
}
