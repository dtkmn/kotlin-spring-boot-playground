package playground.common.rest.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.util.AntPathMatcher
import playground.common.authserver.DragonOAuth2ConfigurationProperties
import playground.common.authserver.MoxAuthenticationManagerResolver

@Order(2)
class MoxOAuth2SecurityConfigurer(
    private val restEndpointBasicAuthProperties: RestBasicAuthProperties,
    private val moxAuthenticationManagerResolver: MoxAuthenticationManagerResolver,
    private val dragonOAuth2ConfigurationProperties: DragonOAuth2ConfigurationProperties,
    private val isActuator: (HttpServletRequest) -> Boolean
) {

    private val isOAuth2Enabled = dragonOAuth2ConfigurationProperties.enabled
    private val customizedFilter: MoxOAuth2Filter? = moxAuthenticationManagerResolver
        .takeIf { isOAuth2Enabled }
        ?.let { resolver ->
            val notActuatorMatcher = RequestMatcher { req -> !isActuator(req) }
            val customizedFilterMatcher =
                if (restEndpointBasicAuthProperties.isEnabled())
                    notActuatorMatcher and antMatchersNotMatch(restEndpointBasicAuthProperties.matchers)
                else notActuatorMatcher
            MoxOAuth2Filter(customizedFilterMatcher, resolver)
        }

    //TODO: Review later
//    @Bean
//    fun springSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
//        http
//            .csrf().disable() // Disable Cross Site Forgery Attacks prevention.
//            .sessionManagement { it.sessionCreationPolicy(STATELESS) }
//            .apply(block = {
//                customizedFilter?.let { f -> addFilterAt(f, AbstractPreAuthenticatedProcessingFilter::class.java) }
//            })
//            .authorizeHttpRequests().apply {
//                requestMatchers(RequestMatcher { req -> isActuator(req) }).permitAll()
//                anyRequest().apply {
//                    if (isOAuth2Enabled) authenticated() else permitAll()
//                }
//            }
//
//        return http.build()
//    }

//    fun configure(http: HttpSecurity) {
//        http
//            .csrf().disable() // Disable Cross Site Forgery Attacks prevention.
//            .sessionManagement { it.sessionCreationPolicy(STATELESS) }
//            .apply(block = {
//                customizedFilter?.let { f -> addFilterAt(f, AbstractPreAuthenticatedProcessingFilter::class.java) }
//            })
//            .authorizeRequests().apply {
//                requestMatchers(RequestMatcher { req -> isActuator(req) }).permitAll()
//                anyRequest().apply {
//                    if (isOAuth2Enabled) authenticated() else permitAll()
//                }
//            }
//    }

//    override fun matches(request: HttpServletRequest): Boolean {
//        // Implement your logic here to decide whether this security chain should apply to the incoming request.
//        // This is a simple example and may need to be adjusted based on your needs.
//        return true
//    }

    private fun antMatchersNotMatch(patterns: Array<String>): RequestMatcher {
        val antPathMatcher = AntPathMatcher()
        return RequestMatcher {
            !patterns.fold(false) { acc, pattern ->
                acc || antPathMatcher.match(pattern, it.requestURI)
            }
        }
    }

    private infix fun RequestMatcher.and(other: RequestMatcher) =
        RequestMatcher { req -> this.matches(req) && other.matches(req) }

//    override fun configure(http: HttpSecurity) {
//        http
//            .csrf().disable() // Disable Cross Site Forgery Attacks prevention.
//            .sessionManagement { it.sessionCreationPolicy(STATELESS) }
//            .apply(block = {
//                customizedFilter?.let { f -> addFilterAt(f, AbstractPreAuthenticatedProcessingFilter::class.java) }
//            })
//            .authorizeRequests().apply {
//                requestMatchers(RequestMatcher { req -> isActuator(req) }).permitAll()
//                anyRequest().apply {
//                    if (isOAuth2Enabled) authenticated() else permitAll()
//                }
//            }
//    }
//
//    override fun configure(auth: AuthenticationManagerBuilder) {
//        // No AuthenticationManagerBuilder Config
//        // We need an empty method so spring won't try to autowire an AuthenticationManager
//    }
//
//    private fun antMatchersNotMatch(patterns: Array<String>): RequestMatcher {
//        val antPathMatcher = AntPathMatcher()
//        return RequestMatcher {
//            !patterns.fold(false) { acc, pattern ->
//                acc || antPathMatcher.match(pattern, it.requestURI)
//            }
//        }
//    }
//
//    private infix fun RequestMatcher.and(other: RequestMatcher) =
//        RequestMatcher { req -> this.matches(req) && other.matches(req) }
}
