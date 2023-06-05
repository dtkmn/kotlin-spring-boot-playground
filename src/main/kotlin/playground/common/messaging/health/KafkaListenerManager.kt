package playground.common.messaging.health

import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import playground.common.messaging.consumer.MessageListenerContainerStartHelper
import playground.common.messaging.logging.LOG_EVENT_KAFKA_LISTENER_MANAGER_INVALID_HEALTH_CHECK_NAME
import playground.common.messaging.logging.LOG_EVENT_KAFKA_LISTENER_STARTED_DUE_TO_RECOVERED_CONTRIBUTOR
import playground.common.messaging.logging.LOG_EVENT_KAFKA_LISTENER_STOPPED_DUE_TO_UNHEALTHY_CONTRIBUTOR
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.HealthContributorRegistry
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.Status
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.SmartLifecycle
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(value = ["dragon.messaging.healthcheck.suspend"], havingValue = "enabled", matchIfMissing = false)
class KafkaListenerManager(
    private val healthContributorRegistry: HealthContributorRegistry,
    private val kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry,
    private val messageListenerContainerStartHelper: MessageListenerContainerStartHelper
) : SmartLifecycle {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var running = false

    override fun isRunning(): Boolean {
        return running
    }

    override fun start() {
        this.running = true
        Thread(::run, "KafkaListenerManagerThread").start()
    }

    override fun stop() {
        this.running = false
    }

    private fun run() {
        while (isRunning) {
            check()
            Thread.sleep(10000)
        }
    }

    fun check() {
        log.trace("Checking app health for kafka")
        kafkaListenerEndpointRegistry.listenerContainers.forEach { container ->
            val unhealthyContributors = unhealthyContributorsFor(container)
            if (unhealthyContributors.isNotEmpty()) {
                stopListener(container, unhealthyContributors)
            } else {
                startListener(container)
            }
        }
    }

    private fun parseHealthChecksFromProperties(container: MessageListenerContainer): Set<String> {
        val value = container.containerProperties.kafkaConsumerProperties["healthchecks"]
        return if (value is String && value.isNotBlank()) {
            val healthChecks = value.split(',').toSet()
            checkInvalidNames(healthChecks)
            healthChecks
        } else {
            emptySet()
        }
    }

    fun checkInvalidNames(healthChecks: Set<String>): Set<String> {
        val invalidNames = healthChecks - (healthContributorRegistry.map { it.name } - "kafka")
        if (invalidNames.isNotEmpty()) {
            log.warn("Invalid health contributor names: $invalidNames", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_KAFKA_LISTENER_MANAGER_INVALID_HEALTH_CHECK_NAME))
        }
        return invalidNames
    }

    private fun unhealthyContributorsFor(container: MessageListenerContainer): List<String> {
        val healthChecks = parseHealthChecksFromProperties(container)
        return healthContributorRegistry
            // never depend on kafka health
            .filter { it.name != "kafka" }
            // isEmpty means check all
            .filter { healthChecks.isEmpty() || healthChecks.contains(it.name) }
            .filter {
                val contributor = it.contributor
                contributor is HealthIndicator && contributor.health().status in setOf(Status.DOWN, Status.OUT_OF_SERVICE)
            }
            .map { it.name }
    }

    private fun stopListener(listenerContainer: MessageListenerContainer, unhealthyContributors: List<String>) {
        if (listenerContainer.isRunning) {
            log.warn(
                "Stopping listener ${listenerContainer.listenerId} because of the health of $unhealthyContributors",
                kv(MDC_KEY_LOG_EVENT, LOG_EVENT_KAFKA_LISTENER_STOPPED_DUE_TO_UNHEALTHY_CONTRIBUTOR)
            )
            listenerContainer.stop()
        }
    }

    private fun startListener(listenerContainer: MessageListenerContainer) {
        if (!listenerContainer.isRunning) {
            log.info(
                "Starting listener ${listenerContainer.listenerId}",
                kv(MDC_KEY_LOG_EVENT, LOG_EVENT_KAFKA_LISTENER_STARTED_DUE_TO_RECOVERED_CONTRIBUTOR)
            )
            messageListenerContainerStartHelper.startContainer(listenerContainer)
        }
    }
}
