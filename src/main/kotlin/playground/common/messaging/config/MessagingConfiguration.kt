package playground.common.messaging.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import playground.common.observability.health.HealthCheckConfiguration
import playground.common.messaging.MoxHeaders
import playground.common.messaging.health.KafkaConsumerHealthMeterBinder
import playground.common.messaging.health.KafkaHealthIndicator
import playground.common.messaging.health.KafkaListenerManager
import playground.common.messaging.logging.MessagePayloadLogger
import playground.common.observability.sentry.SentryConfiguration
import playground.common.observability.observability.MetricsConfiguration
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import java.io.IOException
import java.time.Duration
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClientConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.support.DefaultKafkaHeaderMapper
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import playground.common.messaging.config.*
import playground.common.restclient.RestClientConfiguration
import java.util.*

@Configuration
@EnableKafka
@EnableConfigurationProperties(DragonMessagingProperties::class)
@Import(
    KafkaProducerConfig::class,
    KafkaConsumerConfig::class,
    KafkaBatchListenerConsumerConfig::class,
    SentryConfiguration::class,
    RestClientConfiguration::class,
    HealthCheckConfiguration::class,
    MetricsConfiguration::class,
    KafkaListenerManager::class,
    KafkaConsumerHealthMeterBinder::class
)
class MessagingConfiguration {

    @Bean("kafkaObjectMapper")
    fun kafkaObjectMapper(): ObjectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @Bean
    fun headerMapper(
        @Qualifier("kafkaObjectMapper") kafkaObjectMapper: ObjectMapper
    ): DefaultKafkaHeaderMapper = DefaultKafkaHeaderMapper(kafkaObjectMapper).apply {
        addTrustedPackages("com.projectdrgn.common.messaging")
        addTrustedPackages("java.time")
        setRawMappedHeaders(
            mapOf(
                MoxHeaders.PUBLISHED_BY to true,
                MoxHeaders.PUBLISHED_BY_HOSTNAME to true
            )
        )
    }

    @Bean
    fun messagePayloadLogger(): MessagePayloadLogger = MessagePayloadLogger()

    @Bean
    fun kafkaAdmin(
        @Value("\${playground.messaging.bootstrap.servers}")
        bootstrapServers: String,
        dragonMessagingProperties: DragonMessagingProperties
    ) = KafkaAdmin(
        mapOf(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            AdminClientConfig.SECURITY_PROTOCOL_CONFIG to when {
                dragonMessagingProperties.auth -> "SASL_SSL"
                dragonMessagingProperties.ssl -> "SSL"
                else -> CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL
            }
        ) + saslProperties(dragonMessagingProperties)
    )

    @Bean
    @ConditionalOnProperty(value = ["dragon.messaging.healthcheck.indicator"], havingValue = "enabled", matchIfMissing = false)
    fun kafkaHealthIndicator(
        kafkaAdmin: KafkaAdmin,
        @Value("\${dragon.messaging.healthcheck.response-timeout:100ms}")
        responseTimeout: Duration,
        @Value("\${dragon.messaging.healthcheck.down:disabled}")
        down: String?
    ): KafkaHealthIndicator {
        val isDown = down?.trim()?.lowercase(Locale.getDefault()) == "enabled"
        return KafkaHealthIndicator(kafkaAdmin, responseTimeout, isDown)
    }

    // retry on transient (de)serialization errors, e.g. avro schema registry HTTP hickups
    @Bean
    fun serializerRetryTemplate() = RetryTemplate().apply {
        setBackOffPolicy(
            ExponentialBackOffPolicy().apply {
                initialInterval = 200L
            }
        )
        setRetryPolicy(
            SimpleRetryPolicy(
                5,
                mapOf(
                    IOException::class.java to true,
                    RestClientException::class.java to true
                ),
                true
            )
        )
    }
}
