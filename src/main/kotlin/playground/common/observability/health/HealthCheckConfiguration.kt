package playground.common.observability.health

import playground.common.observability.observability.ManagementCustomEndpointValidator
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

@Configuration
@Import(ManagementCustomEndpointValidator::class)
@PropertySource("classpath:/common/health/healthcheck.properties")
class HealthCheckConfiguration
