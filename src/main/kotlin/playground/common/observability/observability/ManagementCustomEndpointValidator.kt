package playground.common.observability.observability

import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Simple validation if dragon.observability.management.custom.endpoints starts from a comma
 */
@Component
class ManagementCustomEndpointValidator(
    @Value("\${dragon.observability.management.custom.endpoints:}")
    private val actuatorEndpoints: String?
) : InitializingBean {

    override fun afterPropertiesSet() {
        if (actuatorEndpoints != null && actuatorEndpoints.isNotEmpty() && !actuatorEndpoints.startsWith(",")) {
            throw RuntimeException("dragon.observability.management.custom.endpoints should start from a comma if there are specified endpoints")
        }
    }
}
