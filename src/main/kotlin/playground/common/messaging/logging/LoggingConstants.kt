package playground.common.messaging.logging

// KAFKA KEYS
const val MDC_KEY_PROCESSING_FROM_TOPIC = "processingFromTopic"
const val MDC_KEY_PROCESSING_FROM_PARTITION = "processingFromPartition"
const val MDC_KEY_PROCESSING_FROM_OFFSET = "processingFromOffset"
const val MDC_KEY_PROCESSING_FROM_MESSAGE_KEY = "processingFromMessageKey"
const val MDC_KEY_PROCESSING_FROM_KAFKA_TIMESTAMP = "processingFromKafkaTimestamp"

// MOX HEADERS
const val MDC_KEY_MESSAGE_HEADER_IDEMPOTENCY_KEY = "messageHeaderIdempotencyKey"
const val MDC_KEY_MESSAGE_HEADER_TIMESTAMP = "messageHeaderTimestamp"
const val MDC_KEY_MESSAGE_HEADER_CUSTOMER_ID = "messageHeaderCustomerId"

// PAYLOAD LOGGING KEYS
const val MDC_KEY_ANY_PAYLOAD_TYPE = "payloadType"
const val MDC_KEY_ANY_PAYLOAD = "payload"

const val LOG_EVENT_KAFKA = "kafka"

// HEALTH CHECK LOG EVENT
const val LOG_EVENT_KAFKA_BROKER_UNHEALTHY_NOT_ENOUGH_NODES = "kafka_broker_unhealthy_not_enough_nodes"
const val LOG_EVENT_KAFKA_BROKER_UNHEALTHY = "kafka_broker_unhealthy"

// SUSPEND / KAFKA LISTENER MANAGER LOG EVENTS
const val LOG_EVENT_KAFKA_LISTENER_MANAGER_INVALID_HEALTH_CHECK_NAME = "kafka_suspend_invalid_health_check_name"
const val LOG_EVENT_KAFKA_LISTENER_STOPPED_DUE_TO_UNHEALTHY_CONTRIBUTOR = "kafka_listener_stopped_due_to_unhealthy_contributor"
const val LOG_EVENT_KAFKA_LISTENER_STARTED_DUE_TO_RECOVERED_CONTRIBUTOR = "kafka_listener_started_due_to_recovered_contributor"

// KAFKA LAYER LOG EVENTS
const val LOG_EVENT_CONSUMER_POLL = "consumer_poll"
const val LOG_EVENT_MESSAGE_PUBLISHED = "message_published"
const val LOG_EVENT_MESSAGE_PUBLISHED_DLT = "message_published_dlt"
const val LOG_EVENT_COMMITTING_OFFSETS = "committing_offsets"

// PROCESSING LOG EVENTS
const val LOG_EVENT_PROCESSING_END = "processing_message_end"
const val LOG_EVENT_PROCESSING_START = "processing_message_start"
const val LOG_EVENT_PROCESSING_ERROR = "processing_message_error"

// AVRO LOG EVENTS
const val LOG_EVENT_AVRO_UNKNOWN_PROPERTIES_FOR_LOGICAL_TYPE = "avro_unknown_properties_for_logical_type"
const val LOG_EVENT_AVRO_IGNORING_UNKNOWN_SCHEMA = "avro_ignoring_unknown_schema"

// KAFKA TEMPLATE LOG EVENTS
const val LOG_EVENT_RETRYING_TRANSACTION = "kafka_retrying_tx_due_to_transient_producer_ex"
const val LOG_EVENT_NOT_MOX_KAFKA_PRODUCER_FACTORY = "kafka_not_mox_kafka_producer_factory"

// PRODUCER LOG EVENTS
const val LOG_EVENT_PRODUCER_ERROR = "kafka_producer_error"
const val LOG_EVENT_PRODUCER_SENT_NOT_IN_IO_THREAD = "kafka_producer_sent_not_in_io_thread"
const val LOG_EVENT_DEAD_LETTER_PUBLICATION_SUCCEEDED = "kafka_event_dead_letter_publication_succeeded"
const val LOG_EVENT_DEAD_LETTER_PUBLICATION_FAILED = "kafka_event_dead_letter_publication_failed"

// ERROR HANDLER LOG EVENTS
const val LOG_EVENT_RESTARTING_CONTAINER_DUE_TO_ERROR = "kafka_restarting_container_due_to_error"
const val LOG_EVENT_CONTAINER_RESTARTED_DUE_TO_ERROR = "kafka_container_restarted_due_to_error"
const val LOG_EVENT_CONTAINER_START = "kafka_container_start"
const val LOG_EVENT_CONTAINER_START_FAILED = "kafka_container_start_failed"

// DEAD LETTER LOG EVENTS
const val LOG_EVENT_RECORD_HAS_MULTIPLE_HEADERS = "kafka_record_has_multiple_headers"
