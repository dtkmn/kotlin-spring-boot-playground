package playground.common.rest.exception

import com.fasterxml.jackson.databind.JsonMappingException
import playground.common.auth.exception.ActionBlockedException
import playground.common.auth.exception.StepupRequiredException
import playground.common.auth.error.StepupDragonErrorResponse
import playground.common.rest.logging.LOG_EVENT_ACCESS_DENIED
import playground.common.rest.logging.LOG_EVENT_ACTION_BLOCKED_REQUIRED
import playground.common.rest.logging.LOG_EVENT_CLIENT_ABORT_EXCEPTION
import playground.common.rest.logging.LOG_EVENT_CONSTRAINT_VIOLATION
import playground.common.rest.logging.LOG_EVENT_DATA_ACCESS_EXCEPTION
import playground.common.rest.logging.LOG_EVENT_DRAGON_5XX_EX
import playground.common.rest.logging.LOG_EVENT_DRAGON_EX
import playground.common.rest.logging.LOG_EVENT_UNHANDLED_EX
import playground.common.rest.logging.LOG_EVENT_EXECUTION_EXCEPTION
import playground.common.rest.logging.LOG_EVENT_HTTP_MESSAGE_CONVERSION_EXCEPTION
import playground.common.rest.logging.LOG_EVENT_RETRIABLE_EXCEPTION
import playground.common.rest.logging.LOG_EVENT_STEP_UP_REQUIRED
import playground.common.rest.logging.LOG_EVENT_TIMEOUT_EXCEPTION
import io.sentry.Sentry
import jakarta.validation.ConstraintViolationException
import org.apache.catalina.connector.ClientAbortException
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.kafka.common.errors.RetriableException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.dao.DataAccessException
import org.springframework.dao.RecoverableDataAccessException
import org.springframework.dao.TransientDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.client.HttpClientErrorException
import playground.common.exception.*
import playground.common.exception.error.*
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import java.util.concurrent.ExecutionException

@Order(Ordered.HIGHEST_PRECEDENCE + 1000)
@ControllerAdvice
class DragonRestExceptionHandler {
    val log: Logger = LoggerFactory.getLogger(DragonRestExceptionHandler::class.java)

    @ExceptionHandler(DragonException::class)
    fun handleDragonException(ex: DragonException): ResponseEntity<DragonErrorResponse> {

        if (ex.statusCode.is5xxServerError) {
            log.error(ex.exceptionHandlingLogMessage(), kv(MDC_KEY_LOG_EVENT, LOG_EVENT_DRAGON_5XX_EX), ex)
        } else {
            log.warn(ex.exceptionHandlingLogMessage(), kv(MDC_KEY_LOG_EVENT, LOG_EVENT_DRAGON_EX), ex)
        }

        Sentry.captureException(ex)

        return ResponseEntity
            .status(ex.statusCode)
            .body(
                DragonErrorResponse(
                    errorCode = ex.errorCode,
                    debugMessage = ex.message,
                    validationErrors = if (ex is ValidationException) ex.validationErrors else null
                )
            )
    }

    @ExceptionHandler(RecordNotExistException::class)
    fun handleRecordNotExistException(ex: RecordNotExistException): ResponseEntity<DragonErrorResponse> {

        log.warn(ex.exceptionHandlingLogMessage(), ex, kv(MDC_KEY_LOG_EVENT, LOG_EVENT_DRAGON_EX))

        return ResponseEntity
            .status(ex.statusCode)
            .body(
                DragonErrorResponse(
                    errorCode = ex.errorCode,
                    debugMessage = ex.message
                )
            )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<DragonErrorResponse> {

        log.warn(ex.exceptionHandlingLogMessage(), ex, kv(MDC_KEY_LOG_EVENT, LOG_EVENT_CONSTRAINT_VIOLATION))

        val errorsByFieldName = ex.constraintViolations.groupBy({ action -> action.propertyPath.last().name }) { it.message }

        Sentry.captureException(ex)

        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                DragonErrorResponse(
                    errorCode = ERR_SYS_VALIDATION_ERROR,
                    debugMessage = ex.message,
                    validationErrors = errorsByFieldName
                )
            )
    }

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAccessDeniedException(ex: AccessDeniedException): DragonErrorResponse {
        log.warn(ex.exceptionHandlingLogMessage(), ex, kv(MDC_KEY_LOG_EVENT, LOG_EVENT_ACCESS_DENIED))

        Sentry.captureException(ex)
        return DragonErrorResponse(
            errorCode = ERR_SYS_ACCESS_DENIED,
            debugMessage = ex.message
        )
    }

    @ExceptionHandler(TimeoutException::class)
    fun handleTimeoutException(ex: TimeoutException): ResponseEntity<DragonErrorResponse> {
        log.warn(ex.message, ex, kv(MDC_KEY_LOG_EVENT, LOG_EVENT_TIMEOUT_EXCEPTION))
        Sentry.captureException(ex)
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(
                DragonErrorResponse(
                    errorCode = ERR_SYS_DEPENDENCIES_TIMEOUT_ERROR,
                    debugMessage = ex.message
                )
            )
    }

    @ExceptionHandler(TransientDataAccessException::class, RecoverableDataAccessException::class)
    fun handleRetriableSpringDataAccessException(ex: DataAccessException): ResponseEntity<DragonErrorResponse> {
        log.error(ex.exceptionHandlingLogMessage(), kv(MDC_KEY_LOG_EVENT, LOG_EVENT_DATA_ACCESS_EXCEPTION), ex)
        Sentry.captureException(ex)
        return handleTimeoutException(
            TimeoutException(
                message = "Retriable database/Redis data access error",
                cause = ex
            )
        )
    }

    /**
     * Kafka publishing exceptions are wrapped in java ExecutionException
     */
    @ExceptionHandler(ExecutionException::class)
    fun handleExecutionException(ex: ExecutionException): ResponseEntity<DragonErrorResponse> {
        log.error(ex.exceptionHandlingLogMessage(), kv(MDC_KEY_LOG_EVENT, LOG_EVENT_EXECUTION_EXCEPTION), ex)

        Sentry.captureException(ex)
        val (status, error) = if (ex.cause is RetriableException) {
            val retriableException = ex.cause as RetriableException
            HttpStatus.SERVICE_UNAVAILABLE to messagingUnavailableDragonError(retriableException)
        } else {
            HttpStatus.INTERNAL_SERVER_ERROR to unexpectedExceptionDragonError(ex.message)
        }

        return ResponseEntity
            .status(status)
            .body(error)
    }

    @ExceptionHandler(RetriableException::class)
    @ResponseBody
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handleException(ex: RetriableException): DragonErrorResponse {
        log.error(ex.exceptionHandlingLogMessage(), kv(MDC_KEY_LOG_EVENT, LOG_EVENT_RETRIABLE_EXCEPTION), ex)
        Sentry.captureException(ex)

        return messagingUnavailableDragonError(ex)
    }

    @ExceptionHandler(HttpMessageConversionException::class)
    @ResponseBody
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleHttpMessageConversionException(ex: HttpMessageConversionException): DragonErrorResponse {
        log.warn(ex.exceptionHandlingLogMessage(), ex, kv(MDC_KEY_LOG_EVENT, LOG_EVENT_HTTP_MESSAGE_CONVERSION_EXCEPTION))
        val exFields = when (val cause = ex.cause) {
            is JsonMappingException -> mapOf(cause.pathReference to (ex.message ?: ""))
            else -> null
        }

        return DragonErrorResponse(
            errorCode = ERR_SYS_HTTP_CONVERSION_ERROR,
            debugMessage = ex.message,
            validationErrors = exFields
        )
    }

    @ExceptionHandler(ClientAbortException::class)
    fun handleClientAbortException(ex: ClientAbortException): ResponseEntity<DragonErrorResponse>? {
        log.info(ex.exceptionHandlingLogMessage(), ex, kv(MDC_KEY_LOG_EVENT, LOG_EVENT_CLIENT_ABORT_EXCEPTION))
        // When pipe broken, the connection closed, can't return anything
        if (ExceptionUtils.getRootCause(ex)?.message.equals("Broken pipe", true))
            return null
        return ResponseEntity.status(499).body(
            DragonErrorResponse(
                errorCode = ERR_SYS_CLIENT_ABORT_ERROR,
                debugMessage = ex.message
            )
        )
    }

    @ExceptionHandler(Exception::class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(ex: Exception): DragonErrorResponse {
        log.error(ex.exceptionHandlingLogMessage(), kv(MDC_KEY_LOG_EVENT, LOG_EVENT_UNHANDLED_EX), ex)
        Sentry.captureException(ex)
        return unexpectedExceptionDragonError(ex.message)
    }

    @ExceptionHandler(StepupRequiredException::class)
    fun handleStepupRequiredException(ex: StepupRequiredException): ResponseEntity<DragonErrorResponse> {
        log.info(ex.message, ex, kv(MDC_KEY_LOG_EVENT, LOG_EVENT_STEP_UP_REQUIRED))
        Sentry.captureException(ex)
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                StepupDragonErrorResponse(
                    errorCode = ex.errorCode,
                    debugMessage = ex.message,
                    userActionCode = ex.userActionCode,
                    userActionReferenceId = ex.userActionReferenceId,
                    allowedAuthFactors = ex.allowedAuthFactors
                )
            )
    }

    @ExceptionHandler(ActionBlockedException::class)
    fun handleActionBlockedException(ex: ActionBlockedException): ResponseEntity<DragonErrorResponse> {
        log.info(ex.message, ex, kv(MDC_KEY_LOG_EVENT, LOG_EVENT_ACTION_BLOCKED_REQUIRED))
        Sentry.captureException(ex)
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                DragonErrorResponse(
                    errorCode = ex.errorCode,
                    debugMessage = ex.message
                )
            )
    }

    @ExceptionHandler(HttpClientErrorException::class)
    fun handleHttpClientErrorException(ex: HttpClientErrorException): ResponseEntity<DragonErrorResponse> {
        log.warn(ex.exceptionHandlingLogMessage(), kv(MDC_KEY_LOG_EVENT, LOG_EVENT_DRAGON_EX), ex)

        Sentry.captureException(ex)

        return ResponseEntity.status(ex.statusCode).body(
            DragonErrorResponse(
                errorCode = ERR_SYS_CLIENT_ERROR,
                debugMessage = ex.message
            )
        )
    }

    private fun unexpectedExceptionDragonError(message: String?): DragonErrorResponse {
        return DragonErrorResponse(
            errorCode = ERR_SYS_SERVER_ERROR,
            debugMessage = message
        )
    }

    private fun messagingUnavailableDragonError(ex: RetriableException): DragonErrorResponse {
        return DragonErrorResponse(
            errorCode = ERR_SYS_MESSAGING_UNAVAILABLE,
            debugMessage = ex.message
        )
    }
}
