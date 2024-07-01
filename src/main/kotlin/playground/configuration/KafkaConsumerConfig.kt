package playground.configuration

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.transaction.KafkaTransactionManager

@Configuration
@EnableKafka
class KafkaConsumerConfig {

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        val props = hashMapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka:9092",
            ConsumerConfig.GROUP_ID_CONFIG to "group_id",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
        )
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        @Qualifier("kafkaTransactionManager") kafkaTransactionManager: KafkaTransactionManager<String, String>
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory()
        factory.containerProperties.transactionManager = kafkaTransactionManager
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        return factory
    }

}