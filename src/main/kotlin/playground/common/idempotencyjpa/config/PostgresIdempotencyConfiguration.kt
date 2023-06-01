package playground.common.idempotencyjpa.config

import playground.common.idempotencyjpa.jpa.IdempotencyRecordRepository
import playground.common.idempotencyjpa.jpa.JpaIdempotencyStoreAdapter
import playground.common.idempotencyjpa.jpa.PostgresIdempotencyLockAdapter
import playground.common.idempotencyjpa.jpa.PostgresIdempotencyLockRepository
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.PlatformTransactionManager
import playground.common.idempotency.config.IdempotencyConfiguration

@Configuration
@Import(IdempotencyConfiguration::class, PostgresIdempotencyCleanupConfiguration::class)
@ComponentScan("playground.common.idempotencyjpa.jpa")
@EnableJpaRepositories("playground.common.idempotencyjpa.jpa")
@EntityScan("playground.common.idempotencyjpa.jpa")
class PostgresIdempotencyConfiguration {

    @Bean
    fun jpaIdempotencyStoreAdapter(
        repository: IdempotencyRecordRepository,
        transactionManager: PlatformTransactionManager
    ) = JpaIdempotencyStoreAdapter(repository, transactionManager)

    @Bean
    fun postgresIdempotencyLockAdapter(
        repository: PostgresIdempotencyLockRepository
    ) = PostgresIdempotencyLockAdapter(repository)
}
