package playground.common.messaging.consumer

import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import playground.common.messaging.logging.LOG_EVENT_CONTAINER_START
import playground.common.messaging.logging.LOG_EVENT_CONTAINER_START_FAILED
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.AlwaysRetryPolicy
import org.springframework.retry.support.RetryTemplate

/**
 * Helper class for message listener container start - as this can fail and should be retried infinitely.
 */
class MessageListenerContainerStartHelper(val backOffPeriodMs: Long = 200) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)!!

    private var template = RetryTemplate()
        .also { retryTemplate -> retryTemplate.setRetryPolicy(AlwaysRetryPolicy()) }
        .also { retryTemplate -> retryTemplate.setBackOffPolicy(FixedBackOffPolicy().also { it.backOffPeriod = backOffPeriodMs }) }

    fun startContainer(container: MessageListenerContainer) {

        template.execute<Unit, Exception> {
            try {
                log.info(
                    "Starting container with id ${container.listenerId}",
                    kv(MDC_KEY_LOG_EVENT, LOG_EVENT_CONTAINER_START)
                )
                container.start()
            } catch (e: Exception) {
                log.error(
                    "Could not start container with id ${container.listenerId} because of error. Will perform retry",
                    kv(MDC_KEY_LOG_EVENT, LOG_EVENT_CONTAINER_START_FAILED), e
                )
                // need to stop to be able to start again
                container.stop()
                throw e
            }
        }
    }
}
