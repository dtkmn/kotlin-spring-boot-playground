package playground.common.messaging.config

import com.fasterxml.jackson.databind.ObjectMapper
import playground.common.messaging.MoxHeaders
import playground.common.messaging.converter.JsonTopics
import playground.common.messaging.converter.MoxSerializer
import playground.common.messaging.idempotency.PublishStrategyInterceptor
import playground.common.messaging.idempotency.NoPublishing
import playground.common.messaging.idempotency.PublishingInKafkaConsumer
import playground.common.messaging.idempotency.PublishingInFinalPhase
import playground.common.messaging.idempotency.PublishingAtTheEndOfCurrentPhase
import playground.common.messaging.listener.MoxProducerListener
import playground.common.messaging.logging.DragonProducerInterceptor
import playground.common.messaging.logging.MessagePayloadLogger
import playground.common.messaging.producer.MoxProducerOnlyKafkaProducerFactory
import playground.common.messaging.publisher.MoxProducerOnlyKafkaTemplate
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.MicrometerProducerListener
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.KafkaHeaderMapper
import org.springframework.kafka.support.converter.MessagingMessageConverter
import org.springframework.kafka.transaction.KafkaTransactionManager
import org.springframework.retry.support.RetryTemplate
import java.net.InetAddress
import java.time.Duration

@Configuration
internal class KafkaProducerConfig {

    @Value("\${service.name}")
    private lateinit var serviceName: String

    // value should be less or equal to broker config transactional.id.expiration.ms
    @Value("\${playground.messaging.producer-max-age:7d}")
    private lateinit var producerMaxAge: Duration

    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var dragonMessagingProperties: DragonMessagingProperties

    // READ-PROCESS-WRITE (@KafkaListener)
    @Bean
    fun kafkaProducerFactory(
        producerConfig: Map<String, Any>,
        meterRegistry: MeterRegistry
    ): ProducerFactory<Any, Any> {
        val factory = DefaultKafkaProducerFactory<Any, Any>(producerConfig)
        factory.setTransactionIdPrefix(dragonMessagingProperties.transactionIdPrefix)
        factory.setMaxAge(producerMaxAge)

        factory.addListener(MicrometerProducerListener(meterRegistry, listOf()))
        // not setting MicrometerProducerListener - using producer metrics from broker
        return factory
    }

    @Bean
    fun kafkaTemplate(
        @Qualifier("kafkaProducerFactory") producerFactory: ProducerFactory<Any, Any>,
        headerMapper: KafkaHeaderMapper,
        messagePayloadLogger: MessagePayloadLogger
    ): KafkaTemplate<Any, Any> {
        return KafkaTemplate(producerFactory).apply {
            setHeaderMapper(headerMapper)
            setProducerListener(MoxProducerListener(messagePayloadLogger))
        }
    }

    @Bean(KAFKA_TRANSACTION_MANAGER)
    fun kafkaTransactionManager(@Qualifier("kafkaProducerFactory") producerFactory: ProducerFactory<Any, Any>): KafkaTransactionManager<Any, Any> {
        return KafkaTransactionManager<Any, Any>(producerFactory)
    }

    // PRODUCER ONLY (Controllers)
    @Bean
    fun producerOnlyKafkaProducerFactory(
        producerConfig: Map<String, Any>,
        meterRegistry: MeterRegistry
    ): ProducerFactory<Any, Any> {
        val factory = MoxProducerOnlyKafkaProducerFactory<Any, Any>(producerConfig)
        factory.setTransactionIdPrefix(producerOnlyTransactionIdPrefix())
        factory.addListener(MicrometerProducerListener(meterRegistry, listOf()))
        factory.setMaxAge(producerMaxAge)
        // not setting MicrometerProducerListener - using producer metrics from broker
        return factory
    }

    private fun producerOnlyTransactionIdPrefix(): String {
        val hostnameEnvVar = hostName()
        return if (!hostnameEnvVar.isNullOrBlank())
            "$hostnameEnvVar-tx-"
        else
            dragonMessagingProperties.transactionIdPrefix
    }

    private fun hostName() = environment.getProperty("HOSTNAME") ?: InetAddress.getLocalHost().getHostName()

    @Bean
    fun producerOnlyKafkaTemplate(
        @Qualifier("producerOnlyKafkaProducerFactory") producerFactory: ProducerFactory<Any, Any>,
        headerMapper: KafkaHeaderMapper,
        messagePayloadLogger: MessagePayloadLogger
    ): KafkaTemplate<Any, Any> {
        return MoxProducerOnlyKafkaTemplate(producerFactory).apply {
            setHeaderMapper(headerMapper)
            setProducerListener(MoxProducerListener(messagePayloadLogger))
        }
    }

    @Bean(PRODUCER_ONLY_KAFKA_TRANSACTION_MANAGER)
    fun producerOnlyKafkaTransactionManager(@Qualifier("producerOnlyKafkaProducerFactory") producerFactory: ProducerFactory<Any, Any>): KafkaTransactionManager<Any, Any> {
        return KafkaTransactionManager<Any, Any>(producerFactory)
    }

    @Bean
    fun producerConfig(
        messagePayloadLogger: MessagePayloadLogger,
        @Value("\${playground.messaging.bootstrap.servers}")
        bootstrapServers: String,
        @Value("\${playground.messaging.schema.registry.url}")
        schemaRegistryUrl: String,
        @Value("\${playground.messaging.auto.register.schemas:true}")
        autoRegisterSchemas: Boolean,
        jsonTopics: JsonTopics,
        @Qualifier("kafkaObjectMapper") kafkaObjectMapper: ObjectMapper,
        @Qualifier("serializerRetryTemplate") serializerRetryTemplate: RetryTemplate
    ): Map<String, Any> {
        return mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to MoxSerializer::class.java.name,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            // setting in flight requests per connection for test to 2 to avoid errors in tests with one broker
            // please remove when using 2.7 client when https://issues.apache.org/jira/browse/KAFKA-10520 is fixed
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to if (dragonMessagingProperties.inTestContext) 2 else 1,
            ProducerConfig.MAX_BLOCK_MS_CONFIG to 30000,
            ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG to 30000,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.BATCH_SIZE_CONFIG to 16384,
            ProducerConfig.LINGER_MS_CONFIG to 1,
            ProducerConfig.BUFFER_MEMORY_CONFIG to 33554432,
            ProducerConfig.MAX_REQUEST_SIZE_CONFIG to 4194304,
            ProducerConfig.INTERCEPTOR_CLASSES_CONFIG to DragonProducerInterceptor::class.java.name,
            MESSAGE_PAYLOAD_LOGGER_BEAN to messagePayloadLogger,
            MESSAGING_OBJECT_MAPPER to kafkaObjectMapper,
            MESSAGING_JSON_TOPICS to jsonTopics,
            MESSAGING_SERIALIZER_RETRY_TEMPLATE to serializerRetryTemplate,
            MoxHeaders.PUBLISHED_BY to serviceName,
            MoxHeaders.PUBLISHED_BY_HOSTNAME to (hostName() ?: "unknown"),
            AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl,
            AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS to autoRegisterSchemas,
            KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY to RecordNameStrategy::class.java.name,
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to when {
                dragonMessagingProperties.auth -> "SASL_SSL"
                dragonMessagingProperties.ssl -> "SSL"
                else -> CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL
            }
        ) + saslProperties(dragonMessagingProperties)
    }

    // -----------
    // IDEMPOTENCY
    // -----------

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun publishStrategyInterceptor() = PublishStrategyInterceptor()

    @Bean
    fun noPublishing() = NoPublishing()

    @Bean
    fun publishingInKafkaConsumer(
        kafkaTemplate: KafkaTemplate<Any, Any>
    ) = PublishingInKafkaConsumer(kafkaTemplate)

    @Bean
    fun publishingInFinalPhase(
        producerOnlyKafkaTemplate: KafkaTemplate<Any, Any>
    ) = PublishingInFinalPhase(producerOnlyKafkaTemplate)

    @Bean
    fun publishingAtEndOfCurrentPhase(
        producerOnlyKafkaTemplate: KafkaTemplate<Any, Any>
    ) = PublishingAtTheEndOfCurrentPhase(producerOnlyKafkaTemplate)
}

fun saslProperties(dragonMessagingProperties: DragonMessagingProperties): Map<String, Any> {
    return if (dragonMessagingProperties.auth) {
        mapOf(
            "sasl.jaas.config" to "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username='${dragonMessagingProperties.username}' password='${dragonMessagingProperties.password}';",
            "sasl.mechanism" to "PLAIN"
        )
    } else {
        mapOf()
    }
}

fun <K, V> KafkaTemplate<K, V>.setHeaderMapper(headerMapper: KafkaHeaderMapper) {
    val messagingMessageConverter = messageConverter as MessagingMessageConverter
    messagingMessageConverter.setHeaderMapper(headerMapper)
}
