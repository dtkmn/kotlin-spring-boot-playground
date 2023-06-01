package playground.common.batch.configuration

import playground.common.batch.annotation.EnableOnBatchJob
import playground.common.batch.controller.BatchController
import playground.common.batch.jobstatus.BatchJobStatusUpdatedEventPublisher
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import playground.common.messaging.config.MessagingConfiguration
import playground.common.rest.config.RestConfiguration

@Configuration
@Import(
    BatchController::class,
    MessagingConfiguration::class,
    RestConfiguration::class
)
@EnableConfigurationProperties(BatchProperties::class)
@EnableOnBatchJob
class BatchConfiguration {

    @Bean
    fun batchJobStatusUpdatedEventPublisher(
        kafkaTemplate: KafkaTemplate<Any, Any>,
        producerOnlyKafkaTemplate: KafkaTemplate<Any, Any>
    ) = BatchJobStatusUpdatedEventPublisher(kafkaTemplate, producerOnlyKafkaTemplate)
}

@ConfigurationProperties(prefix = "dragon.batch")
data class BatchProperties(
    val jobCommandTopics: Map<String, String>
)
