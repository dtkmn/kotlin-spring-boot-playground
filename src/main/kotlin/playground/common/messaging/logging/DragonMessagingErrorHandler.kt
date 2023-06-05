package playground.common.messaging.logging

import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import playground.common.messaging.consumer.MessageListenerContainerStartHelper
import io.sentry.Sentry
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.errors.InvalidPidMappingException
import org.apache.kafka.common.errors.ProducerFencedException
import org.apache.kafka.common.errors.UnknownProducerIdException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.kafka.listener.ErrorHandler
import org.springframework.kafka.listener.ListenerExecutionFailedException
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.transaction.CannotCreateTransactionException
import java.util.concurrent.Executor
import org.apache.commons.lang3.exception.ExceptionUtils


/**
 * Contains two error handlers:
 *
 * handle(Exception, ConsumerRecord) - used by when listener fails on record
 * handle(Exception, List, consumer, container) - used when rollback processing fails - for some unrecoverable
 * exceptions restarts container.
 *
 */
class DragonMessagingErrorHandler(private val messageListenerContainerStartHelper: MessageListenerContainerStartHelper) : ErrorHandler {

    private val log: Logger = LoggerFactory.getLogger(javaClass)!!
    private val executor: Executor = SimpleAsyncTaskExecutor()

    // used by when listener fails on record
    override fun handle(ex: Exception, data: ConsumerRecord<*, *>?) {
        Sentry.captureException(unwrap(ex))
        throw ex
    }

    // used when rollback processing fails - for some unrecoverable exceptions restarts container
    override fun handle(
        ex: java.lang.Exception,
        records: MutableList<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer
    ) {
        if (exceptionQualifyForContainerRestart(ex)) {
            restartContainer(container)
        }
        Sentry.captureException(unwrap(ex))
        throw ex
    }

    private fun exceptionQualifyForContainerRestart(ex: Exception): Boolean {
        // any transactional exceptions are thrown by rollback processing only
        // because error handler for listener errors is called from inside transaction
        // we do not want to restart container for ProducerFencedException as it is not needed for this error
        // or
        // any transient producer exception which means that message could not be sent to DLT
        return isKafkaTransactionalNotFencedException(ex) || isKafkaProducerException(ex)
    }

    private fun isKafkaProducerException(ex: Exception): Boolean {
        return when {
            ExceptionUtils.getRootCause(ex) is UnknownProducerIdException -> true
            ExceptionUtils.getRootCause(ex) is InvalidPidMappingException -> true
            ex is KafkaException && ex.message == "The producer closed forcefully" -> true
            else -> false
        }
    }

    private fun isKafkaTransactionalNotFencedException(ex: Exception) =
        ex is CannotCreateTransactionException && (ex.cause == null || ex.cause !is ProducerFencedException)

    private fun restartContainer(container: MessageListenerContainer) {
        log.warn("Stopping and restarting container", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_RESTARTING_CONTAINER_DUE_TO_ERROR))
        this.executor.execute {
            container.stop()
            messageListenerContainerStartHelper.startContainer(container)
            log.warn("Restarted container because of not recoverable error for listener", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_CONTAINER_RESTARTED_DUE_TO_ERROR))
        }
    }

    private fun unwrap(thrownException: Exception): Throwable {
        return if (thrownException is ListenerExecutionFailedException && thrownException.cause != null) {
            thrownException.cause!!
        } else {
            thrownException
        }
    }
}
