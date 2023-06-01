package playground.common.messaging.logicalTypes

import org.apache.avro.Conversion
import org.apache.avro.LogicalType
import org.apache.avro.LogicalTypes
import org.apache.avro.specific.SpecificData

/**
 * Using custom logical types is tedious and lot can go wrong. They need to be registered at
 * several places. We abstract the registration using [MoxLogicalTypes.registerAll]
 * and we group all logical in this enum class, which mitigates the issues partially.
 *
 * To create a new logical type add new [MoxLogicalTypes] enum value to this file and provide
 * the implementation in a standalone file (in this folder).
 *
 * **WARNING: Be careful, because avro template record relies on the fully quantified name of [MoxLogicalTypes.registerAll]!**
 */
enum class MoxLogicalTypes(
    val logicalType: LogicalType,
    val avroConversion: Conversion<*>
) {
    MOX_DECIMAL(MoxDecimalLogicalType(), MoxDecimalAvroConversion()),
    MOX_MONEY(MoxMoneyLogicalType(), MoxMoneyAvroConversion()),
    MOX_CURRENCY(MoxCurrencyLogicalType(), MoxCurrencyAvroConversion())
    ;

    val avroLogicalTypeFactory = LogicalTypes.LogicalTypeFactory { logicalType }

    val logicalName: String = logicalType.name

    companion object {
        /**
         * True, if logical types have already been registered.
         */
        private var alreadyRegistered = false

        /**
         * Registers all custom logical types defined by this class.
         *
         * Registers the logical type represented by this interface, so avro converter is able to generate
         * these fields or serialize/deserialize the type correctly.
         *
         * There are several places where we need to register the custom logical types.
         *  1.  At runtime of client services which use the generated code. The conversion for custom logical types
         *      is done by parsing the inlined avro schema (which is part of the generated files as a string) and using
         *      converters obtained from the registered types. To make sure they are available, we register the types
         *      before the inlined schema is parsed.
         *  2.  In [dragon-contracts](https://github.com/ProjectDrgn/dragon-contracts) because there are checks
         *      verifying the correctness of the schemas.
         *  3.  In contract gradle plugins code generator which generates the code from the schema.
         *
         * Logical types need to be registered only once, but this function might be called multiple times,
         * because it will be called when initializing static fields in avro generated files. If this function
         * was already called in current runtime, it will immediately return.
         *
         * There was a lot of effort spent with the implementation of the registration of custom logical types.
         * The technical document highlighting alternatives considered can be found at
         * [https://projectdrgn.atlassian.net/wiki/x/MQCZHg](https://projectdrgn.atlassian.net/wiki/x/MQCZHg).
         */
        fun registerAll() {
            if (alreadyRegistered) return

            values().forEach {
                LogicalTypes.register(it.logicalName, it.avroLogicalTypeFactory)
            }
        }

        /**
         * Registering all logical types in [registerAll] is not enough for
         * [kotlin avro generator](https://github.com/ProjectDrgn/contract-gradle-plugins/blob/master/src/main/kotlin/com/projectdrgn/common/avro/codegen/KotlinGenerator.kt),
         * which needs to have the converters also in it's [SpecificData].
         *
         * @param kotlinGeneratorData data for kotlin generator, which needs to be initialized with
         * all logical type conversions.
         */
        fun addAllCustomLogicalTypeConversions(kotlinGeneratorData: SpecificData) {
            values().forEach {
                kotlinGeneratorData.addLogicalTypeConversion(it.avroConversion)
            }
        }
    }
}
