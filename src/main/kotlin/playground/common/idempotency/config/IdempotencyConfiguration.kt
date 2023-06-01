package playground.common.idempotency.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import playground.common.idempotency.IdempotencyInterceptor
import playground.common.idempotency.convenience.IdempotencyContextHolder
import playground.common.idempotency.impl.IdempotencyLockAdapter
import playground.common.idempotency.impl.IdempotencyProviderImpl
import playground.common.idempotency.impl.IdempotencyStoreAdapter
import playground.common.idempotency.lifecycle.IdempotencyMonitoringInterceptor
import playground.common.restclient.RestClientConfiguration

@Configuration
@Import(RestClientConfiguration::class)
abstract class IdempotencyConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun idempotencyContextHolderInterceptor() = IdempotencyContextHolder.Interceptor

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun idempotencyMonitoringInterceptor() = IdempotencyMonitoringInterceptor

    @Bean
    fun idempotencyProvider(
        idempotencyLockAdapter: IdempotencyLockAdapter,
        idempotencyStoreAdapter: IdempotencyStoreAdapter,
        interceptors: List<IdempotencyInterceptor>,
        // Allows squads to configure their own
        @Qualifier("idempotencyObjectMapper") objectMapper: ObjectMapper?,
        @Qualifier("defaultObjectMapper") defaultObjectMapper: ObjectMapper
    ) = IdempotencyProviderImpl(
        objectMapper = objectMapper ?: defaultObjectMapper,
        lockAdapter = idempotencyLockAdapter,
        storeAdapter = idempotencyStoreAdapter,
        interceptors = interceptors
    )
}
