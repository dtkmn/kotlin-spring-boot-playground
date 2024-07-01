package playground.configuration

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.transaction.KafkaTransactionManager

@Configuration
class KafkaProducerConfig {

    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        val props = hashMapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka:9092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            ProducerConfig.TRANSACTIONAL_ID_CONFIG to "transactional-id"
        )
        val producerFactory = DefaultKafkaProducerFactory<String, String>(props)
        producerFactory.setTransactionIdPrefix("tx-")
        return producerFactory
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory())
    }

    @Bean
    fun kafkaTransactionManager(producerFactory: ProducerFactory<String, String>): KafkaTransactionManager<String, String> {
        return KafkaTransactionManager(producerFactory)
    }

}