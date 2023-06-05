package playground.common.rest.config

import playground.common.rest.shutdown.TomcatGracefulShutdown
import playground.common.rest.shutdown.TomcatPreShutdown
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConditionalOnProperty("dragon.graceful-shutdown.enabled")
class GracefulShutdownConfiguration(
    @Value("\${dragon.graceful-shutdown.grace-period}")
    val shutdownDelay: Duration
) {

    @Bean
    fun tomcatPreShutdown() = TomcatPreShutdown()

    @Bean
    fun tomcatGracefulShutdown() = TomcatGracefulShutdown(shutdownDelay)
}
