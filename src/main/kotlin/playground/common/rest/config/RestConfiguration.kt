package playground.common.rest.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import playground.common.idempotency.IdempotencyContext
import playground.common.idempotency.IdempotencyProvider
import playground.common.observability.health.HealthCheckConfiguration
import playground.common.observability.logging.BuildInfoToMdc
import playground.common.observability.observability.MetricsConfiguration
import playground.common.observability.sentry.SentryConfiguration
import playground.common.rest.exception.DragonRestExceptionHandler
import playground.common.rest.exception.SpringWebExceptionHandler
import playground.common.rest.idempotency.IdempotencyProvidingHandlerAspect
import playground.common.rest.logging.RequestLoggingFilter
import playground.common.rest.logging.ResponseLoggingFilter
import playground.common.rest.security.SecurityConfig
import playground.common.restclient.RestClientConfiguration
import java.util.*

@Configuration
@Import(
    RequestLoggingFilter::class,
    ResponseLoggingFilter::class,
    SentryConfiguration::class,
    HealthCheckConfiguration::class,
    MetricsConfiguration::class,
    SecurityConfig::class,
    RestClientConfiguration::class,
    BuildInfoToMdc::class,
    GracefulShutdownConfiguration::class
)
@PropertySource("classpath:/common/rest/rest.properties")
class RestConfiguration : WebMvcConfigurer {

    @Bean
    fun dragonRestExceptionHandler() = DragonRestExceptionHandler()

    @Bean
    fun springWebExceptionHandler() = SpringWebExceptionHandler()

    @Bean
    fun idempotencyProvidingHandlerAspect(
        idempotencyProvider: Optional<IdempotencyProvider>,
        applicationContext: ApplicationContext,
        // This is hack to provide smart interoperability of `rest` and `messaging` modules
        @Qualifier("publishingInFinalPhase")
        defaultPublishingForRest: Optional<IdempotencyContext.Setting>
    ) = IdempotencyProvidingHandlerAspect(idempotencyProvider, applicationContext, defaultPublishingForRest)

    @Bean
    fun isActuator(
        @Value("\${management.endpoints.web.exposure.include}")
        actuatorEndpoints: List<String>
    ): (HttpServletRequest) -> Boolean = { request ->
        actuatorEndpoints.any { actuator ->
            request.requestURI.startsWith("/$actuator")
        }
    }
}
