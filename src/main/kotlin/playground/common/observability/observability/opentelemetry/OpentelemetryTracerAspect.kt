package playground.common.observability.observability.opentelemetry

import io.micrometer.common.util.StringUtils
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import playground.common.observability.observability.annotations.OtelTracer

import org.springframework.stereotype.Component

/**
 * Wraps OpentelemetryTracerAspect, gets customize attribute from arguments and set into current trace span.
 */
@Aspect
@Component
class OpentelemetryTracerAspect {
    @Before("@annotation(playground.common.observability.observability.annotations.OtelTracer)")
    fun beforeProcessing(joinPoint: JoinPoint) {
        val signature = joinPoint.signature as MethodSignature
        val methodAnnotation = signature.method.getAnnotation(OtelTracer::class.java)
        if (StringUtils.isNotBlank(methodAnnotation.attributeKey) && StringUtils.isNotBlank(methodAnnotation.attributeValue)) {
            OtelTraceHelper.getInstance().addSpanAttribute(methodAnnotation.attributeKey, methodAnnotation.attributeValue)
        }
    }
}
