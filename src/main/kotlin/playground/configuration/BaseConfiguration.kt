package playground.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import playground.common.messaging.publisher.Topic
import playground.customer.contract.avro.customerprofilesnapshot.v2.CustomerProfileSnapshot

@Configuration
@EnableJpaRepositories("playground.repository")
@EntityScan("playground.entity")
class BaseConfiguration {
    @Bean
    fun customerProfileSnapshotTopic(
        @Value("\${playground.messaging.customer-profile.topic}") customerProfileSnapshotTopic: String
    ) = Topic(customerProfileSnapshotTopic, CustomerProfileSnapshot::class.java)

    @Bean
    @Primary
    fun transactionManager(): PlatformTransactionManager = JpaTransactionManager()
}