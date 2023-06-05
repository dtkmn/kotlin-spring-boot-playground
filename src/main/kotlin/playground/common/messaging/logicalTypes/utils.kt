package playground.common.messaging.logicalTypes

import playground.common.exception.DragonException
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import playground.common.messaging.logging.LOG_EVENT_AVRO_UNKNOWN_PROPERTIES_FOR_LOGICAL_TYPE
import org.apache.avro.LogicalType
import org.apache.avro.Schema
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Error

class MoxSchemaValidationError(override val message: String?) : Error(message)

private object SchemaValidation
private val schemaValidationLogger: Logger = LoggerFactory.getLogger(SchemaValidation::class.java)

/**
 * Validates that the schema for unknown properties and that the on the wire type is [Schema.Type.STRING].
 * This function should be called from override [LogicalType.validate] inside custom logical types.
 *
 * Unfortunately, we can't use [DragonException] or any other [RuntimeException] because kotlin generator task
 * which generates the avro classes from schemas won't fail. This is because [Schema.parse] ignores the errors thrown.
 * See [https://github.com/apache/avro/blob/37be95b0bd64b9ecc118d0ff8b30e734f94e4d3c/lang/java/avro/src/main/java/org/apache/avro/LogicalTypes.java#L111]
 *
 * @throws MoxSchemaValidationError if the logical type is not backed by [Schema.Type.STRING].
 */
fun validateSchema(
    logicalTypeName: String,
    schema: Schema,
    // we can assume "logicalType" will be present in properties
    schemaProps: List<String> = listOf("logicalType")
) {
    if (schema.objectProps.size > schemaProps.size) {
        val props = schema.objectProps.keys - schemaProps
        schemaValidationLogger.warn(
            "Logical type $logicalTypeName encountered unknown properties: $props",
            kv(MDC_KEY_LOG_EVENT, LOG_EVENT_AVRO_UNKNOWN_PROPERTIES_FOR_LOGICAL_TYPE)
        )
    }

    if (schema.type !== Schema.Type.STRING) {
        throw MoxSchemaValidationError("Logical type $logicalTypeName must be backed by string")
    }
}
