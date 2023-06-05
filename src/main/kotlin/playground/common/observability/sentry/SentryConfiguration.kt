package playground.common.observability.sentry

import io.sentry.IHub
import io.sentry.Sentry
import io.sentry.SentryOptions
import io.sentry.spring.jakarta.SentryExceptionResolver
import io.sentry.spring.jakarta.tracing.TransactionNameProvider
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.HandlerExceptionResolver

@Configuration
@ConditionalOnProperty(name = ["sentry.enabled"], havingValue = "true")
class SentryConfiguration {

    @Bean
    fun initializeSentry(
        @Value("\${sentry.project.dsn}") dsn: String,
        @Value("\${sentry.project.environment}") environment: String,
        @Value("\${service.name:unnamed-service}") serviceName: String
    ): HandlerExceptionResolver {
//        Sentry.init(dsn)
//        Sentry.getStoredClient().environment = environment
//        Sentry.getStoredClient().addTag("service", serviceName)
        Sentry.init { options: SentryOptions ->
            options.environment = environment
            options.setTag("service", serviceName)
            options.dsn = dsn
        }

        val hub: IHub = Sentry.getCurrentHub()
        val transactionNameProvider = TransactionNameProvider { request: HttpServletRequest ->
            // Example: Use the request URI as the transaction name.
            request.requestURI
        }
        val order = Ordered.HIGHEST_PRECEDENCE  // High precedence.

        return SentryExceptionResolver(hub, transactionNameProvider, order)
    }
}
