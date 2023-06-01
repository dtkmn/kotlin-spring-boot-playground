package playground.common.messaging

object DltHeaders {
    // using kebab case in dlt prefix to be consistent with naming in KafkaHeaders
    const val DLT_ORIGINAL_PREFIX = "kafka_dlt-original"
    const val DLT_ORIGINAL_PUBLISHED_BY = "$DLT_ORIGINAL_PREFIX-${MoxHeaders.PUBLISHED_BY}"
    const val DLT_ORIGINAL_PUBLISHED_BY_HOSTNAME = "$DLT_ORIGINAL_PREFIX-${MoxHeaders.PUBLISHED_BY_HOSTNAME}"
    const val DLT_ORIGINAL_DATADOG_TRACE_ID = "$DLT_ORIGINAL_PREFIX-${MoxHeaders.DATADOG_TRACE_ID}"
    const val DLT_DEAD_LETTER_ID = "dead_letter_id"
}
