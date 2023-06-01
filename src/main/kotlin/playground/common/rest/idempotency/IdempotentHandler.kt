package playground.common.rest.idempotency


import playground.common.idempotency.lifecycle.validateProcessInputs
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.context.ApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import playground.common.exception.SupportException
import playground.common.exception.error.ERR_SYS_IDEMPOTENCY_KEY_MISSING
import playground.common.idempotency.IdempotencyAssertionError
import playground.common.idempotency.IdempotencyContext
import playground.common.idempotency.IdempotencyProvider
import playground.common.idempotency.runIdempotentProcess
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.time.Duration
import java.util.Optional
import kotlin.reflect.KClass

const val IDEMPOTENCY_KEY_HEADER = "requestId"

/**
 * Wraps [IdempotencyProvider.runIdempotentProcess] around annotated handler method
 * using [processId] and [lockDurationMs] parameters.
 *
 * Idempotency key is taken from header of name specified by [IDEMPOTENCY_KEY_HEADER].
 *
 * Calls [validateProcessInputs] on all handler parameters except those annotated with [IgnoreInput].
 * Parameter names are used as inputIds. This can be overridden by annotating parameter with [InputId].
 *
 * Both [IdempotentInputConflict] and [IdempotencyProvider.ProcessLockedException]
 * are propagated to controller advice exception handlers.
 *
 * Implemented by [IdempotencyProvidingHandlerAspect].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class IdempotentHandler(val processId: String, val lockDurationMs: Long)

/**
 * Can be used together with [IdempotentHandler] to provide additional [IdempotencyContext.Setting]s.
 * Settings must be available as beans of type [value] in the application context.
 *
 * For use cases where there are multiple beans of same type, it is optionally possible to specify bean [name].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
@MustBeDocumented
annotation class AutowireSetting(val value: KClass<out IdempotencyContext.Setting>, val name: String = "")

/** See [IdempotentHandler] */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class InputId(val value: String)

/** See [IdempotentHandler] */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class IgnoreInput

/**
 * Aspect that implements functionality of [IdempotentHandler]
 */
@Aspect
class IdempotencyProvidingHandlerAspect(
    val idempotencyProvider: Optional<IdempotencyProvider>,
    val applicationContext: ApplicationContext,
    // Note that this module should not know there is concept like publishing strategy,
    // but we prefer squad convenience over strict separation
    val defaultPublishingForRest: Optional<IdempotencyContext.Setting>
) {

    @Around("@annotation(playground.common.rest.idempotency.IdempotentHandler)")
    fun aroundRestHandler(joinPoint: ProceedingJoinPoint): Any? {
        val provider = idempotencyProvider.orElse(null) ?: throw IdempotencyAssertionError(
            "IdempotentHandler annotation used but IdempotencyProvider bean not in context"
        )

        val signature = joinPoint.signature as MethodSignature
        val methodAnnotation = signature.method.getAnnotation(IdempotentHandler::class.java)

        val requestAttributes = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes

        val idempotencyKey = requestAttributes.request.getHeader(IDEMPOTENCY_KEY_HEADER)
            ?: throw SupportException("Idempotency key not specified", ERR_SYS_IDEMPOTENCY_KEY_MISSING)

        return provider.runIdempotentProcess(
            idempotencyKey,
            methodAnnotation.processId,
            Duration.ofMillis(methodAnnotation.lockDurationMs),
            *signature.method.getSettings().addDefaultSetting().toTypedArray()
        ) { ctx ->
            ctx.validateProcessInputs(signature.method.getInputs(joinPoint.args))
            joinPoint.proceed()
        }
    }

    private val Parameter.isIgnored
        get() = getAnnotation(IgnoreInput::class.java) != null

    private val Parameter.inputKey
        get() = getAnnotation(InputId::class.java)?.value ?: name

    private fun Method.getInputs(args: Array<Any?>) =
        (parameters zip args)
            .filterNot { (param, _) -> param.isIgnored }
            .map { (param, arg) -> param.inputKey to arg }
            .toMap()

    private fun Method.getSettings(): List<IdempotencyContext.Setting> =
        getAnnotationsByType(AutowireSetting::class.java).map { annotation ->
            if (annotation.name.isEmpty()) applicationContext.getBean(annotation.value.java)
            else applicationContext.getBean(annotation.name, annotation.value.java)
        }

    private fun List<IdempotencyContext.Setting>.addDefaultSetting(): List<IdempotencyContext.Setting> {
        val additions = mutableListOf<IdempotencyContext.Setting>()

        defaultPublishingForRest.map { dpr ->
            if (this.all { setting -> dpr.conflictsWithClasses.all { !it.isInstance(setting) } }) {
                additions.add(dpr)
            }
        }

        return this + additions
    }
}
