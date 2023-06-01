package playground.common.observability.observability.opentelemetry

import io.opentelemetry.api.trace.Span

class OtelTraceHelper private constructor() {

    companion object {
        @Volatile
        private lateinit var instance: OtelTraceHelper
        fun getInstance(): OtelTraceHelper {
            synchronized(this) {
                if (!Companion::instance.isInitialized) {
                    instance = OtelTraceHelper()
                }
                return instance
            }
        }
    }

    fun addSpanAttribute(key: String, value: String) {
        val currentSpan = Span.current()
        currentSpan.setAttribute(key, value)
    }
}
