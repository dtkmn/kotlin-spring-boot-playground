package playground.common.observability.observability.annotations

/**
 * Annotation that marks a method to add customize attribute to span.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OtelTracer(

    /**
     * The attribute key  for trace span.
     */
    val attributeKey: String,

    /**
     * The attribute value  for trace span.
     */
    val attributeValue: String
)
