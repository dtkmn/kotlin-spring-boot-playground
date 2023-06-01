package playground.common.messaging.logicalTypes

import org.apache.avro.Conversion
import org.apache.avro.LogicalType
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import org.javamoney.moneta.Money
import java.math.BigDecimal

/**
 * The name of the logical type used in the schema.
 */
private const val MOX_MONEY_LOGICAL_TYPE_NAME = "mox-money"

/**
 * The custom logical type for representing monetary amounts.
 *
 * @see LogicalTypes
 */
class MoxMoneyLogicalType : LogicalType(MOX_MONEY_LOGICAL_TYPE_NAME) {

    /**
     * @see [LogicalType.validate].
     */
    override fun validate(schema: Schema) {
        super.validate(schema)

        validateSchema(MOX_MONEY_LOGICAL_TYPE_NAME, schema)
    }
}

/**
 * Supports conversion (serialization and deserialization) of [MoxMoneyLogicalType].
 *
 * We only support conversion from/to char sequence, because we require the underlying format
 * to be [Schema.Type.STRING].
 *
 * @see [MoxMoneyLogicalType.validate].
 */
class MoxMoneyAvroConversion : Conversion<Money>() {
    override fun getLogicalTypeName(): String = MOX_MONEY_LOGICAL_TYPE_NAME

    override fun getConvertedType(): Class<Money> = Money::class.java

    // TODO: check that the amount has correct number of floating point digits
    override fun fromCharSequence(
        value: CharSequence?,
        schema: Schema?,
        type: LogicalType?
    ): Money = Money.parse(value.toString())

    override fun toCharSequence(
        value: Money?,
        schema: Schema?,
        type: LogicalType?
    ): CharSequence = value!!.toString()
}

// TODO: move the extension functions to a seprate file/module - https://projectdrgn.atlassian.net/browse/APS-1022
operator fun Money.unaryPlus(): Money = this
operator fun Money.unaryMinus(): Money = this.multiply(-1L)
operator fun Money.plus(m: Money): Money = this.add(m)
operator fun Money.minus(m: Money): Money = this.subtract(m)
operator fun Money.times(number: BigDecimal): Money = this.multiply(number)
operator fun Money.div(number: BigDecimal): Money = this.divide(number)

val Money.amount: BigDecimal
    get() = this.number.numberValueExact(BigDecimal::class.java)
val Money.currencyCode: String
    get() = this.currency.currencyCode
fun Money.isSameCurrencyAs(m: Money) = this.currency == m.currency
