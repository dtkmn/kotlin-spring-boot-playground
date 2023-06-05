package playground.common.rest.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order


@Order(1)
@ConditionalOnProperty(
    prefix = "playground",
    name = ["rest.basic-auth.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class MoxBasicAuthSecurityConfigurer(
    private val restEndpointBasicAuthProperties: RestBasicAuthProperties,
    @Value("\${management.endpoints.web.exposure.include}")
    private val actuatorEndpoints: List<String>
) {

    private val actuatorEndpointsArray = actuatorEndpoints.map { "/$it" }.toTypedArray()

//    @Bean
//    fun springSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
//        http
//            .csrf().disable() // Disable Cross Site Forgery Attacks prevention.
//            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
//            .authorizeHttpRequests()
//            .requestMatchers(*actuatorEndpointsArray).permitAll()
//            .anyRequest().authenticated()
//            .and()
//            .httpBasic()
//
//        return http.build()
//    }
//
//    @Bean
//    fun inMemoryUserDetailsManager(): InMemoryUserDetailsManager {
//        val userDetails: UserDetails = User.withUsername(restEndpointBasicAuthProperties.username)
//            .passwordEncoder { password -> password }
//            .password(restEndpointBasicAuthProperties.password)
//            .authorities(listOf())
//            .build()
//        return InMemoryUserDetailsManager(userDetails)
//    }

//    override fun configure(http: HttpSecurity) {
//        http.csrf().disable() // Disable Cross Site Forgery Attacks prevention.
//            .sessionManagement { it.sessionCreationPolicy(STATELESS) }
//            .requestMatcher(antMatchersMatch(restEndpointBasicAuthProperties.matchers))
//            .authorizeRequests().apply {
//                antMatchers(*actuatorEndpointsArray).permitAll()
//                anyRequest().authenticated().and().httpBasic()
//            }
//    }
//
//    override fun configure(auth: AuthenticationManagerBuilder) {
//        auth.inMemoryAuthentication()
//            .withUser(restEndpointBasicAuthProperties.username)
//            .password(restEndpointBasicAuthProperties.password)
//            .authorities(listOf())
//    }
//
//    private fun antMatchersMatch(patterns: Array<String>): RequestMatcher {
//        val antPathMatcher = AntPathMatcher()
//        return RequestMatcher {
//            patterns.fold(false) { acc, pattern ->
//                acc || antPathMatcher.match(pattern, it.requestURI)
//            }
//        }
//    }
}
