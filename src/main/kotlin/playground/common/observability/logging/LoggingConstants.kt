package playground.common.observability.logging

// MDC - thread-level keys
const val MDC_KEY_REQUEST_METHOD = "requestMethod"
const val MDC_KEY_REQUEST_URI = "requestURI"
const val MDC_KEY_REQUEST_BODY = "requestBody"
const val MDC_KEY_REQUEST_HEADER_CONTENT_TYPE = "requestContentType"
const val MDC_KEY_REQUEST_HEADER_CONTENT_LENGTH = "requestContentLength"
const val MDC_KEY_RESPONSE_BODY = "responseBody"
const val MDC_KEY_RESPONSE_CODE = "responseCode"
const val MDC_KEY_ELAPSED_TIME = "elapsedTime"

const val MDC_KEY_EVENT_DESCRIPTION = "eventDescription"
const val MDC_KEY_EVENT_CHANNEL = "eventChannel"
const val MDC_KEY_CUSTOMER_ID = "customerId"
const val MDC_KEY_REQUEST_ID = "requestId"
const val MDC_KEY_RESOURCE_ID = "resourceId"
const val MDC_KEY_PUBLISHED_BY = "publishedBy"
const val MDC_KEY_PUBLISHED_BY_HOSTNAME = "publishedByHostname"
const val MDC_KEY_USER = "user"
const val MDC_KEY_OAUTH2_CUSTOMER_ID = "oauth2CustomerId"
const val MDC_KEY_OAUTH2_ACR_LEVEL = "oauth2AcrLevel"
const val MDC_KEY_OAUTH2_AMR = "oauth2Amr"
const val MDC_KEY_OAUTH2_TRANSACTION_ID = "oauth2TransactionId"
const val MDC_KEY_STAFF_NAME = "staffName"
const val MDC_KEY_AUTHENTICATION_TYPE = "authenticationType"
const val MDC_KEY_PRINCIPAL_TYPE = "principalType"

const val MDC_KEY_IDEMPOTENCY_PROCESS_ID = "idempotencyProcessId"
const val MDC_KEY_IDEMPOTENCY_KEY = "idempotencyKey"
const val MDC_KEY_IDEMPOTENCY_PHASE_ID = "idempotencyPhaseId"

// log-entry level keys
/** searchable key identifying the type of log entry */
const val MDC_KEY_LOG_EVENT = "logEvent"
