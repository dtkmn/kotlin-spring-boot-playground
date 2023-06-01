package playground.common.observability.observability

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfiguration {

    @Bean
    fun metricsConfig(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry: MeterRegistry ->
            registry.config().meterFilter(CustomRenameFilter())
        }
    }

    /**
     * Some metrics changed name after updating micrometer, to not break working monitors metrics are renamed to old named.
     */
    class CustomRenameFilter : MeterFilter {
        override fun map(id: Meter.Id): Meter.Id {
            if (id.name.startsWith("kafka.consumer.fetch.manager")) {
                return id.withName(id.name.replaceFirst("kafka.consumer.fetch.manager", "kafka.consumer"))
            }
            return super.map(id)
        }
    }
}
