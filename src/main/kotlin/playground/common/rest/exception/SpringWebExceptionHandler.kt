package playground.common.rest.exception

import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import playground.common.exception.error.DragonErrorResponse
import playground.common.exception.error.ERR_SYS_MVC_ERROR
import playground.common.exception.error.ERR_SYS_VALIDATION_ERROR
import playground.common.exception.exceptionHandlingLogMessage
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import playground.common.rest.logging.LOG_EVENT_METHOD_ARG_NOT_VALID_EX
import playground.common.rest.logging.LOG_EVENT_MVC_EX

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class SpringWebExceptionHandler : ResponseEntityExceptionHandler() {

    val log: Logger = LoggerFactory.getLogger(SpringWebExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected fun handleMethodArgumentTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: WebRequest
    ): DragonErrorResponse {
        return mvcDragonError(ex)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val errors = HashMap<String, MutableList<String>>()
        ex.bindingResult.fieldErrors.forEach {
            val fieldName = (it as FieldError).field
            val errorMessage = it.defaultMessage ?: "Validation error"

            errors.computeIfAbsent(fieldName) { mutableListOf() }.add(errorMessage)
        }

        val throwable = Throwable("Validation failed for: ${errors.keys}")
        val debugMessage = "${throwable.message}, ${ExceptionUtils.getStackTrace(throwable)}"
        log.warn(debugMessage, kv(MDC_KEY_LOG_EVENT, LOG_EVENT_METHOD_ARG_NOT_VALID_EX))

        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                DragonErrorResponse(
                    errorCode = ERR_SYS_VALIDATION_ERROR,
                    debugMessage = debugMessage,
                    validationErrors = errors
                )
            )
    }

//    fun handleMethodArgumentNotValid(
//        ex: MethodArgumentNotValidException,
//        headers: HttpHeaders,
//        status: HttpStatus,
//        request: WebRequest
//    ): ResponseEntity<Any> {
//        val errors = HashMap<String, MutableList<String>>()
//        ex.bindingResult.fieldErrors.forEach {
//            val fieldName = (it as FieldError).field
//            val errorMessage = it.defaultMessage ?: "Validation error"
//
//            errors.computeIfAbsent(fieldName) { mutableListOf() }.add(errorMessage)
//        }
//
//        val throwable = Throwable("Validation failed for: ${errors.keys}")
//        val debugMessage = "${throwable.message}, ${ExceptionUtils.getStackTrace(throwable)}"
//        log.warn(debugMessage, kv(MDC_KEY_LOG_EVENT, LOG_EVENT_METHOD_ARG_NOT_VALID_EX))
//
//        return ResponseEntity
//            .status(HttpStatus.UNPROCESSABLE_ENTITY)
//            .body(
//                DragonErrorResponse(
//                    errorCode = ERR_SYS_VALIDATION_ERROR,
//                    debugMessage = debugMessage,
//                    validationErrors = errors
//                )
//            )
//    }

//    fun handleExceptionInternal(
//        ex: Exception,
//        @Nullable body: Any?,
//        headers: HttpHeaders,
//        status: HttpStatus,
//        request: WebRequest
//    ): ResponseEntity<Any> {
//
//        val apiError = mvcDragonError(ex)
//
//        return ResponseEntity(apiError, headers, status)
//    }

    override fun handleExceptionInternal(
        ex: java.lang.Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val apiError = mvcDragonError(ex)

        return ResponseEntity(apiError, headers, statusCode)
    }

    private fun mvcDragonError(ex: Exception): DragonErrorResponse {
        log.warn(ex.exceptionHandlingLogMessage(), ex, kv(MDC_KEY_LOG_EVENT, LOG_EVENT_MVC_EX))

        return DragonErrorResponse(
            errorCode = ERR_SYS_MVC_ERROR,
            debugMessage = "${ex.message}"
        )
    }
}
