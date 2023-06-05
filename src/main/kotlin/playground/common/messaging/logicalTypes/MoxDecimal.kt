package playground.common.messaging.logicalTypes

import org.apache.avro.Conversion
import org.apache.avro.LogicalType
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import java.math.BigDecimal

/**
 * The name of the logical type used in the schema.
 */
private const val MOX_DECIMAL_LOGICAL_TYPE_NAME = "mox-decimal"

/**
 * The custom logical type for representing [BigDecimal].
 *
 * @see LogicalTypes
 */
class MoxDecimalLogicalType : LogicalType(MOX_DECIMAL_LOGICAL_TYPE_NAME) {

    /**
     * @see [LogicalType.validate].
     */
    override fun validate(schema: Schema) {
        super.validate(schema)

        validateSchema(MOX_DECIMAL_LOGICAL_TYPE_NAME, schema)
    }
}

/**
 * Supports conversion (serialization and deserialization) of [MoxDecimalLogicalType].
 *
 * We only support conversion from/to char sequence, because we require the underlying format
 * to be [Schema.Type.STRING].
 *
 * @see [MoxDecimalLogicalType.validate].
 */
class MoxDecimalAvroConversion : Conversion<BigDecimal>() {
    override fun getLogicalTypeName(): String = MOX_DECIMAL_LOGICAL_TYPE_NAME

    override fun getConvertedType(): Class<BigDecimal> = BigDecimal::class.java

    override fun fromCharSequence(
        value: CharSequence?,
        schema: Schema?,
        type: LogicalType?
    ): BigDecimal = BigDecimal(value.toString())

    override fun toCharSequence(
        value: BigDecimal?,
        schema: Schema?,
        type: LogicalType?
    ): CharSequence = value!!.toString()
}
