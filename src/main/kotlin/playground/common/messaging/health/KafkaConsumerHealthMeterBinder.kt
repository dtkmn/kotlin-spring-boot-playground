package playground.common.messaging.health

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.stereotype.Component

@Component
class KafkaConsumerHealthMeterBinder(
    private val kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry
) : MeterBinder {

    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder(
            "kafka.listenercontainer.status",
            kafkaListenerEndpointRegistry,
            { kler -> kler.listenerContainers.count { it.isRunning }.toDouble() }
        )
            .tags("status", "running")
            .description("Number of running KafkaListenerContainers")
            .register(registry)
        Gauge.builder(
            "kafka.listenercontainer.status",
            kafkaListenerEndpointRegistry,
            { kler -> kler.listenerContainers.count { !it.isRunning }.toDouble() }
        )
            .tags("status", "stopped")
            .description("Number of stopped KafkaListenerContainers")
            .register(registry)
    }
}
