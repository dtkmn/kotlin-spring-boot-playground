package playground.common.messaging

object MoxHeaders {
    const val PREFIX = "mox"
    const val IDEMPOTENCY_KEY = "${PREFIX}_idempotency_key"
    const val TIMESTAMP = "${PREFIX}_timestamp"
    const val CUSTOMER_ID = "${PREFIX}_customer_id"
    const val PUBLISHED_BY = "${PREFIX}_published_by"
    const val PUBLISHED_BY_HOSTNAME = "${PREFIX}_published_by_hostname"
    const val RECORD_BYTE_VALUE = "${PREFIX}_record_byte_value"
    const val DATADOG_TRACE_ID = "x-datadog-trace-id"
}
