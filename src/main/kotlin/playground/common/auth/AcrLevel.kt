package playground.common.auth

import org.slf4j.LoggerFactory
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv

const val MDC_LOG_EVENT_UNKNOWN_ACR = "unknown_acr"

enum class AcrLevel(val value: String) {
    ACRD("acr-d"), PRE_ACR0("pre-acr-0"), ACR0("acr-0"), ACR1("acr-1"), ACR2("acr-2");

    fun isAtLeastAcrLevel(acrLevel: AcrLevel): Boolean {
        return when (acrLevel) {
            ACR0 -> this == ACR0 || this == ACR1 || this == ACR2
            ACR1 -> this == ACR1 || this == ACR2
            ACR2 -> this == ACR2
            ACRD -> this == ACRD
            PRE_ACR0 -> this == PRE_ACR0
        }
    }

    companion object {

        private val log = LoggerFactory.getLogger(AcrLevel::class.java)

        fun fromStringValue(value: String?): AcrLevel? {
            val acrLevel = values().find { it.value == value }
            if (acrLevel == null && !value.isNullOrBlank()) {
                log.warn("Unknown acr level value: $value", kv(MDC_KEY_LOG_EVENT, MDC_LOG_EVENT_UNKNOWN_ACR))
            }
            return acrLevel
        }
    }
}
