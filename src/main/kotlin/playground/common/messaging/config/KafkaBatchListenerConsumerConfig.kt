package playground.common.messaging.config

import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.MicrometerConsumerListener
import org.springframework.kafka.listener.*
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter
import org.springframework.util.backoff.ExponentialBackOff
import playground.common.messaging.consumer.BatchJobListenerAspect
import playground.common.messaging.consumer.BatchJobListenerBeanPostProcessor
import playground.common.messaging.consumer.HeaderPreservingDeadLetterPublishingRecoverer
import playground.common.messaging.converter.MoxMessageConverter
import playground.common.messaging.logging.MessagePayloadLogger
import playground.common.messaging.logging.MessageProcessingLoggingAspect

@Configuration
class KafkaBatchListenerConsumerConfig(
    val kafkaConsumerRetryProperties: KafkaConsumerRetryProperties,
    val dragonMessagingProperties: DragonMessagingProperties
) {

    /**
     * Listener container factory used only for @BatchJobListener.
     * It does not use kafka transaction - and offset is committed manually with Acknowledgment.acknowledge().
     * Additionaly sets max.poll.records to 1 and max.poll.interval.ms to 30 minutes.
     * Should not be used by normal listeners (when using @KafkaListener).
     */
    @Bean(NO_TX_BATCH_JOB_LISTENER_CONTAINER_FACTORY)
    fun noTxBatchJobListenerContainerFactory(
        moxMessageConverter: MoxMessageConverter,
        defaultAfterRollbackProcessor: DefaultAfterRollbackProcessor<Any, Any>,
        @Qualifier(NO_TX_MESSAGING_ERROR_HANDLER)
        noTxMessagingErrorHandler: DefaultErrorHandler,
        @Qualifier(NO_TX_BATCH_JOB_LISTENER_CONSUMER_FACTORY)
        noTxBatchJobConsumerFactory: ConsumerFactory<Any, Any>,
        dragonRecordInterceptor: RecordInterceptor<Any, Any>,
        meterRegistry: MeterRegistry
    ): ConcurrentKafkaListenerContainerFactory<Any, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
        factory.consumerFactory = noTxBatchJobConsumerFactory
//        factory.setMessageConverter(moxMessageConverter)
        factory.setBatchMessageConverter(BatchMessagingMessageConverter(moxMessageConverter))
        // manual immediate ack mode - commit will be done when acknowledge (Acknowledgment.acknowledge()) is done manually
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.containerProperties.assignmentCommitOption = ContainerProperties.AssignmentCommitOption.NEVER
        factory.setRecordInterceptor(dragonRecordInterceptor)
        factory.setCommonErrorHandler(noTxMessagingErrorHandler)
        return factory
    }

    @Bean(NO_TX_MESSAGING_ERROR_HANDLER)
    fun noTxMessagingErrorHandler(
        @Qualifier(NO_TX_DEAD_LETTER_PUBLISHER_RECOVERER)
        deadLetterPublishingRecoverer: HeaderPreservingDeadLetterPublishingRecoverer
    ): DefaultErrorHandler {
        val backOff = ExponentialBackOff().apply {
            initialInterval = kafkaConsumerRetryProperties.initialInterval.toMillis()
            multiplier = kafkaConsumerRetryProperties.multiplier
            maxInterval = kafkaConsumerRetryProperties.maxInterval.toMillis()
            maxElapsedTime = kafkaConsumerRetryProperties.maxElapsedTime.toMillis()
        }

        val recoverer: ConsumerRecordRecoverer = ConsumerRecordRecoverer { record, e ->
            deadLetterPublishingRecoverer.accept(record, e)
        }

        return DefaultErrorHandler(recoverer, backOff).apply {
            isAckAfterHandle = true
            setCommitRecovered(false)
        }

//        return DefaultErrorHandler(
//            BiConsumer { consumerRecord, exception ->
//                deadLetterPublishingRecoverer.accept(consumerRecord, exception)
//            },
//            backOff
//        ).apply {
//            addNotRetryableExceptions(
//                SerializationException::class.java,
//                SupportException::class.java,
//                RecordNotExistException::class.java,
//                ValidationException::class.java,
//                IdempotentInputConflict::class.java
//            )
//        }
    }


//    @Bean(NO_TX_MESSAGING_ERROR_HANDLER)
//    fun noTxMessagingErrorHandler(
//        @Qualifier(NO_TX_DEAD_LETTER_PUBLISHER_RECOVERER)
//        deadLetterPublishingRecoverer: HeaderPreservingDeadLetterPublishingRecoverer
//    ): SeekToCurrentErrorHandler {
//        return SeekToCurrentErrorHandler(
//            deadLetterPublishingRecoverer,
//            ExponentialBackOff().apply {
//                initialInterval = kafkaConsumerRetryProperties.initialInterval.toMillis()
//                multiplier = kafkaConsumerRetryProperties.multiplier
//                maxInterval = kafkaConsumerRetryProperties.maxInterval.toMillis()
//                maxElapsedTime = kafkaConsumerRetryProperties.maxElapsedTime.toMillis()
//            }
//        ).apply {
//            setCommitRecovered(false)
//            isAckAfterHandle = true
//            addNotRetryableExceptions(SerializationException::class.java, SupportException::class.java, RecordNotExistException::class.java, ValidationException::class.java, IdempotentInputConflict::class.java)
//        }
//    }

    @Bean(NO_TX_DEAD_LETTER_PUBLISHER_RECOVERER)
    fun noTxDeadLetterPublishingRecoverer(
        producerOnlyKafkaTemplate: KafkaTemplate<Any, Any>,
        messagePayloadLogger: MessagePayloadLogger
    ) = HeaderPreservingDeadLetterPublishingRecoverer(producerOnlyKafkaTemplate, messagePayloadLogger)

    @Bean(NO_TX_BATCH_JOB_LISTENER_CONSUMER_FACTORY)
    fun consumerFactory(
        consumerConfig: Map<String, Any>,
        meterRegistry: MeterRegistry
    ): ConsumerFactory<Any, Any> {
        val batchJobConsumerConfig = HashMap(consumerConfig)
        batchJobConsumerConfig[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 2
        batchJobConsumerConfig[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = 1800000 // 30 minutes
        val consumerFactory = DefaultKafkaConsumerFactory<Any, Any>(batchJobConsumerConfig)
        // not setting micrometer listeners for tests
        if (!dragonMessagingProperties.inTestContext) {
            consumerFactory.addListener(MicrometerConsumerListener(meterRegistry, listOf()))
        }
        return consumerFactory
    }

    @Bean
    fun batchJobListenerAspect(messageProcessingLoggingAspect: MessageProcessingLoggingAspect): BatchJobListenerAspect =
        BatchJobListenerAspect(messageProcessingLoggingAspect)

    @Bean
    fun batchJobListenerBeanPostProcessor() =
        BatchJobListenerBeanPostProcessor<Any, Any>(NO_TX_BATCH_JOB_LISTENER_CONTAINER_FACTORY)
}
