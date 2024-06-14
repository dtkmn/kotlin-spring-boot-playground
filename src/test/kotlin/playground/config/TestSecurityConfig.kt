package playground.config


//@TestConfiguration
//@EnableWebSecurity
class TestSecurityConfig {

//    @Bean
//    @Primary
//    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
//        http
//            .csrf { it.disable() }
//            .cors { it.disable() }
//            .authorizeHttpRequests { it.anyRequest().permitAll() }
//            .httpBasic { it.disable() }
//            .formLogin { it.disable() }
//            .headers { it.disable() }
//        println("*** TestConfig loaded and injected!!!!!")
//        return http.build()
//    }

//    @Bean
//    fun webSecurityCustomizer(): WebSecurityCustomizer {
//        return WebSecurityCustomizer { web -> web.ignoring().anyRequest() }
//    }

}