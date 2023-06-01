package playground.configuration

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.support.DefaultKafkaHeaderMapper
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import playground.common.messaging.publisher.Topic
import playground.customer.contract.avro.customerprofilesnapshot.v2.CustomerProfileSnapshot
import java.io.IOException

@Configuration
@EnableJpaRepositories("playground.repository")
@EntityScan("playground.entity")
class BaseConfiguration {
    @Bean
    fun customerProfileSnapshotTopic(
        @Value("\${playground.messaging.customer-profile.topic}") customerProfileSnapshotTopic: String
    ) = Topic(customerProfileSnapshotTopic, CustomerProfileSnapshot::class.java)

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

    @Bean("kafkaObjectMapper")
    fun kafkaObjectMapper(): ObjectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @Bean
    fun kafkaHeaderMapper(
        @Qualifier("kafkaObjectMapper") kafkaObjectMapper: ObjectMapper
    ): DefaultKafkaHeaderMapper = DefaultKafkaHeaderMapper(kafkaObjectMapper).apply {
        addTrustedPackages("playground.customer.contract.avro")
        addTrustedPackages("java.time")
//        setRawMappedHeaders(
//            mapOf(
//                MoxHeaders.PUBLISHED_BY to true,
//                MoxHeaders.PUBLISHED_BY_HOSTNAME to true
//            )
//        )
    }
}