package playground.common.messaging.config

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.MicrometerConsumerListener
import org.springframework.kafka.listener.CompositeRecordInterceptor
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor
import org.springframework.kafka.listener.RecordInterceptor
import org.springframework.kafka.support.KafkaHeaderMapper
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.transaction.KafkaTransactionManager
import org.springframework.retry.support.RetryTemplate
import org.springframework.util.backoff.ExponentialBackOff
import playground.common.exception.IdempotentInputConflict
import playground.common.exception.RecordNotExistException
import playground.common.exception.SupportException
import playground.common.exception.ValidationException
import playground.common.messaging.consumer.HeaderPreservingDeadLetterPublishingRecoverer
import playground.common.messaging.consumer.MessageListenerContainerStartHelper
import playground.common.messaging.converter.*
import playground.common.messaging.logging.*
import playground.common.observability.logging.BuildInfoToMdc
import java.time.Duration

@Configuration
@Import(
    MoxMessageConverter::class,
    BuildInfoToMdc::class,
    FailedDeserializationInfoInterceptor::class
)
@EnableConfigurationProperties(KafkaConsumerRetryProperties::class)
internal class KafkaConsumerConfig {

    @Value("\${playground.messaging.bootstrap.servers}")
    lateinit var bootstrapServers: String
    @Value("\${playground.messaging.consumer.max_poll_records:250}")
    lateinit var maxPollRecords: String

    @Autowired
    lateinit var kafkaConsumerRetryProperties: KafkaConsumerRetryProperties

    @Autowired
    lateinit var dragonMessagingProperties: DragonMessagingProperties

    @Bean
    fun kafkaListenerContainerFactory(
        @Qualifier(KAFKA_TRANSACTION_MANAGER) kafkaTransactionManager: KafkaTransactionManager<Any, Any>,
        moxMessageConverter: MoxMessageConverter,
        defaultAfterRollbackProcessor: DefaultAfterRollbackProcessor<Any, Any>,
        dragonMessagingErrorHandler: DragonMessagingErrorHandler,
        consumerConfig: Map<String, Any>,
        dragonRecordInterceptor: RecordInterceptor<Any, Any>,
        meterRegistry: MeterRegistry
    ): ConcurrentKafkaListenerContainerFactory<Any, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
        factory.consumerFactory = consumerFactory(consumerConfig, meterRegistry)
//        factory.setMessageConverter(moxMessageConverter)
        factory.setRecordMessageConverter(moxMessageConverter) // Since 2.9.6
        factory.setAfterRollbackProcessor(defaultAfterRollbackProcessor)
        factory.containerProperties.transactionManager = kafkaTransactionManager
        factory.containerProperties.assignmentCommitOption = ContainerProperties.AssignmentCommitOption.NEVER
        factory.setRecordInterceptor(dragonRecordInterceptor)
//        factory.setErrorHandler(dragonMessagingErrorHandler)
        factory.setCommonErrorHandler(dragonMessagingErrorHandler)
        // TODO: try to leave this as true,
        //  see http://devdoc.net/javaweb/spring/spring-kafka-docs-2.2.3/reference/htmlsingle/
        //  see https://stackoverflow.com/questions/58206543/springboot-kafka-consumer-failed-to-start-bean-internalkafkalistenerendpointreg/58207320#58207320
        factory.setMissingTopicsFatal(false)
        factory.setContainerCustomizer { it.setInterceptBeforeTx(false) }
        return factory
    }

    @Bean
    fun defaultAfterRollbackProcessor(
        deadLetterPublishingRecoverer: HeaderPreservingDeadLetterPublishingRecoverer,
        kafkaTemplate: KafkaTemplate<Any, Any>
    ): DefaultAfterRollbackProcessor<Any, Any> {
        return DefaultAfterRollbackProcessor<Any, Any>(
            deadLetterPublishingRecoverer,
            ExponentialBackOff().apply {
                initialInterval = kafkaConsumerRetryProperties.initialInterval.toMillis()
                multiplier = kafkaConsumerRetryProperties.multiplier
                maxInterval = kafkaConsumerRetryProperties.maxInterval.toMillis()
                maxElapsedTime = kafkaConsumerRetryProperties.maxElapsedTime.toMillis()
            },
            kafkaTemplate,
            true
        ).apply {
            addNotRetryableExceptions(SerializationException::class.java, SupportException::class.java, RecordNotExistException::class.java, ValidationException::class.java, IdempotentInputConflict::class.java)
        }
    }

    @Bean
    fun dragonRecordInterceptor(
        headerMapper: KafkaHeaderMapper,
        buildInfoToMdc: BuildInfoToMdc,
        failedDeserializationInfoInterceptor: FailedDeserializationInfoInterceptor
    ): RecordInterceptor<Any, Any> {
        return CompositeRecordInterceptor(
            DragonRecordInterceptor(headerMapper, buildInfoToMdc),
            failedDeserializationInfoInterceptor
        )
    }

    @Bean
    fun messageListenerContainerStartHelper() = MessageListenerContainerStartHelper()

    @Bean
    fun dragonMessagingErrorHandler(messageListenerContainerStartHelper: MessageListenerContainerStartHelper) =
        DragonMessagingErrorHandler(messageListenerContainerStartHelper)

    @Bean
    fun deadLetterPublishingRecoverer(
        kafkaTemplate: KafkaTemplate<Any, Any>,
        messagePayloadLogger: MessagePayloadLogger
    ) = HeaderPreservingDeadLetterPublishingRecoverer(kafkaTemplate, messagePayloadLogger)

    @Bean
    fun messageProcessingLoggingAspect(): MessageProcessingLoggingAspect =
        MessageProcessingLoggingAspect()

    // used also by other kafka listener container factories (TM)
    @Bean
    fun consumerFactory(
        consumerConfig: Map<String, Any>,
        meterRegistry: MeterRegistry
    ): ConsumerFactory<Any, Any> {
        val consumerFactory = DefaultKafkaConsumerFactory<Any, Any>(consumerConfig)
        // not setting micrometer listeners for tests
        if (!dragonMessagingProperties.inTestContext) {
            consumerFactory.addListener(MicrometerConsumerListener(meterRegistry, listOf()))
        }
        return consumerFactory
    }

    @Bean
    fun consumerConfig(
        messagePayloadLogger: MessagePayloadLogger,
        @Value("\${playground.messaging.schema.registry.url}")
        schemaRegistryUrl: String,
        @Value("\${playground.messaging.auto.register.schemas:false}")
        autoRegisterSchemas: Boolean,
        jsonTopics: JsonTopics,
        @Qualifier("serializerRetryTemplate")
        serializerRetryTemplate: RetryTemplate
    ): Map<String, Any> {
        return mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,

            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java.name,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java.name,
            ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS to StringDeserializer::class.java.name,
            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to MoxDeserializer::class.java.name,
            ErrorHandlingDeserializer.VALUE_FUNCTION to FailedDeserializationFunction::class.java.name,

            ConsumerConfig.ISOLATION_LEVEL_CONFIG to "read_committed",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to maxPollRecords.toInt(),
            ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG to Duration.ofSeconds(2).toMillis().toInt(),

            ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG to DragonConsumerInterceptor::class.java.name,
            MESSAGE_PAYLOAD_LOGGER_BEAN to messagePayloadLogger,
            MESSAGING_JSON_TOPICS to jsonTopics,
            MESSAGING_SERIALIZER_RETRY_TEMPLATE to serializerRetryTemplate,
            AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl,
            AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS to autoRegisterSchemas,
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
            KafkaAvroDeserializerConfig.VALUE_SUBJECT_NAME_STRATEGY to RecordNameStrategy::class.java.name,
            JsonDeserializer.USE_TYPE_INFO_HEADERS to false,
            JsonDeserializer.TRUSTED_PACKAGES to "*",
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to
                (
                    if (dragonMessagingProperties.auth) "SASL_SSL" else (
                        if (dragonMessagingProperties.ssl) "SSL"
                        else CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL
                        )
                    )
        ) + saslProperties(dragonMessagingProperties)
    }
}
