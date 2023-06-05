package playground.common.messaging.health

import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import playground.common.messaging.logging.LOG_EVENT_KAFKA_BROKER_UNHEALTHY_NOT_ENOUGH_NODES
import playground.common.messaging.logging.LOG_EVENT_KAFKA_BROKER_UNHEALTHY
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.DescribeClusterOptions
import org.apache.kafka.common.config.ConfigResource
import org.apache.kafka.common.errors.TimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import java.time.Duration
import java.util.concurrent.ExecutionException
import kotlin.math.max

// based on https://github.com/spring-projects/spring-boot/pull/11515
class KafkaHealthIndicator(
    private val kafkaAdmin: KafkaAdmin,
    responseTimeout: Duration,
    private val down: Boolean
) : DisposableBean, AbstractHealthIndicator() {

    private val log: Logger = LoggerFactory.getLogger(javaClass)!!
    private val describeOptions = DescribeClusterOptions().timeoutMs(responseTimeout.toMillis().toInt())
    private val adminClient = AdminClient.create(kafkaAdmin.configurationProperties)

    @Throws(Exception::class)
    override fun doHealthCheck(builder: Health.Builder) {
        try {
            retry(5) {
                val result = adminClient.describeCluster(describeOptions)
                val brokerId = result.controller().get().idString()
                val minReplicas = minReplicas(brokerId, adminClient)
                val nodes = result.nodes().get().size
                val clusterId = result.clusterId().get()
                if (minReplicas <= nodes) {
                    builder.up()
                } else {
                    log.warn(
                        "Kafka broker is unhealthy with not enough nodes: $nodes / $minReplicas",
                        kv(MDC_KEY_LOG_EVENT, LOG_EVENT_KAFKA_BROKER_UNHEALTHY_NOT_ENOUGH_NODES)
                    )
                    if (down) {
                        builder.down()
                    } else {
                        builder.up()
                    }
                }
                builder.withDetail("clusterId", clusterId)
                builder.withDetail("brokerId", brokerId)
                builder.withDetail("minReplicas", minReplicas)
                builder.withDetail("nodes", nodes)
            }
        } catch (e: Throwable) {
            log.warn(
                "Kafka broker is unhealthy with error: ${e.message}",
                kv(MDC_KEY_LOG_EVENT, LOG_EVENT_KAFKA_BROKER_UNHEALTHY)
            )
            if (down) {
                builder.down(e)
            } else {
                builder.up()
            }
        }
    }

    private fun <T> retry(maxAttempts: Int, action: (Any) -> T): T = RetryTemplate().apply {
        setBackOffPolicy(FixedBackOffPolicy().apply { backOffPeriod = 200 })
        setRetryPolicy(
            SimpleRetryPolicy(
                maxAttempts,
                mapOf(TimeoutException::class.java to true),
                true
            )
        )
    }.execute<T, Exception>(action)

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun minReplicas(brokerId: String, adminClient: AdminClient): Int {
        val configResource = ConfigResource(ConfigResource.Type.BROKER, brokerId)
        val kafkaConfig = adminClient.describeConfigs(listOf(configResource)).all().get()
        val brokerConfig = kafkaConfig[configResource]!!
        val minIsr = brokerConfig["transaction.state.log.min.isr"].value().toInt()
        val minInsyncReplicas = brokerConfig["min.insync.replicas"].value().toInt()
        return max(minIsr, minInsyncReplicas)
    }

    override fun destroy() {
        adminClient.close()
    }
}
