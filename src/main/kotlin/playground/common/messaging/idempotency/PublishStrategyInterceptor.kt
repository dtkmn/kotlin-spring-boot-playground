package playground.common.messaging.idempotency

import com.fasterxml.jackson.core.type.TypeReference
import playground.common.idempotency.IdempotencyContext
import playground.common.idempotency.IdempotencyInterceptor
import playground.common.messaging.config.KafkaProducerConfig
import playground.common.messaging.idempotency.publishStrategy

/**
 * Needs to be configured for some implementations of [PublishStrategy] to work.
 *
 * It is included in [KafkaProducerConfig].
 **/
class PublishStrategyInterceptor() : IdempotencyInterceptor {

    override fun onContextCreated(context: IdempotencyContext) {
        context.publishStrategy.validate(context)
    }

    override fun wrapPhaseExecutor(next: IdempotencyInterceptor.PhaseExecutor): IdempotencyInterceptor.PhaseExecutor =
        object : IdempotencyInterceptor.PhaseExecutor {
            override fun <R> execute(data: IdempotencyContext.PhaseData, type: TypeReference<R>, body: () -> R): R =
                next.execute(
                    data,
                    type,
                    data.context.publishStrategy.wrapBody(data, body)
                )
        }
}
