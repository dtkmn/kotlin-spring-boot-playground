package playground.common.messaging.logicalTypes

import org.apache.avro.Conversion
import org.apache.avro.LogicalType
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import javax.money.CurrencyUnit
import javax.money.Monetary

/**
 * The name of the logical type used in the schema.
 */
private const val MOX_CURRENCY_LOGICAL_TYPE_NAME = "mox-currency"

/**
 * The custom logical type for representing currency.
 *
 * @see LogicalTypes
 */
class MoxCurrencyLogicalType : LogicalType(MOX_CURRENCY_LOGICAL_TYPE_NAME) {

    /**
     * @see [LogicalType.validate].
     */
    override fun validate(schema: Schema) {
        super.validate(schema)

        validateSchema(MOX_CURRENCY_LOGICAL_TYPE_NAME, schema)
    }
}

/**
 * Supports conversion (serialization and deserialization) of [MoxCurrencyLogicalType].
 *
 * We only support conversion from/to char sequence, because we require the underlying format
 * to be [Schema.Type.STRING].
 *
 * @see [MoxCurrencyLogicalType.validate].
 */
class MoxCurrencyAvroConversion : Conversion<CurrencyUnit>() {
    override fun getLogicalTypeName(): String = MOX_CURRENCY_LOGICAL_TYPE_NAME

    override fun getConvertedType(): Class<CurrencyUnit> = CurrencyUnit::class.java

    override fun fromCharSequence(
        value: CharSequence?,
        schema: Schema?,
        type: LogicalType?
    ): CurrencyUnit = Monetary.getCurrency(value.toString())

    override fun toCharSequence(
        value: CurrencyUnit?,
        schema: Schema?,
        type: LogicalType?
    ): CharSequence = value!!.toString()
}
