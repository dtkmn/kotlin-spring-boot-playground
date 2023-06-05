package playground.common.rest.logging

const val PROFILE_REST_BODY_DEBUG_LOGGING = "rest-body-debug-logging"

const val LOG_EVENT_HTTP = "http"

// HTTP LOG EVENTS
const val LOG_EVENT_HTTP_REQUEST = "http_request"
const val LOG_EVENT_HTTP_RESPONSE = "http_response"

// AUTH LOG EVENTS
const val LOG_EVENT_OAUTH2_AUTHENTICATION_FAILED = "rest_oauth2_authentication_failed"

// DRAGON ERROR HANDLING LOG EVENT
const val LOG_EVENT_DRAGON_EX = "rest_dragon_ex"
const val LOG_EVENT_DRAGON_5XX_EX = "rest_dragon_5xx_ex"
const val LOG_EVENT_ACCESS_DENIED = "rest_access_denied_ex"
const val LOG_EVENT_CONSTRAINT_VIOLATION = "rest_constraint_violation_ex"
const val LOG_EVENT_TIMEOUT_EXCEPTION = "rest_timeout_ex"
const val LOG_EVENT_RETRIABLE_EXCEPTION = "rest_retriable_ex"
const val LOG_EVENT_EXECUTION_EXCEPTION = "rest_execution_ex"
const val LOG_EVENT_DATA_ACCESS_EXCEPTION = "rest_data_access_ex"
const val LOG_EVENT_HTTP_MESSAGE_CONVERSION_EXCEPTION = "rest_http_message_conversion_ex"
const val LOG_EVENT_CLIENT_ABORT_EXCEPTION = "rest_client_abort_ex"
const val LOG_EVENT_UNHANDLED_EX = "rest_unhandled_ex"
const val LOG_EVENT_STEP_UP_REQUIRED = "rest_step_up_required_ex"
const val LOG_EVENT_ACTION_BLOCKED_REQUIRED = "rest_action_blocked_ex"

// MVC LOG EVENT
const val LOG_EVENT_METHOD_ARG_NOT_VALID_EX = "rest_method_arg_not_valid_ex"
const val LOG_EVENT_MVC_EX = "rest_mvc_ex"

// GRACEFUL SHUTDOWN LOG EVENTS
const val LOG_EVENT_GRACE_PERIOD_ELAPSED_WITH_ACTIVE_REQUEST = "graceful_shutdown_grace_period_elapsed_with_active_request"
const val LOG_EVENT_GRACE_SHUTDOWN_COMPLETE = "graceful_shutdown_complete"
const val LOG_EVENT_GRACE_SHUTDOWN_STOPPING = "graceful_shutdown_stopping"
const val LOG_EVENT_GRACE_SHUTDOWN_NOT_STANDARD_CONTEXT = "graceful_shutdown_not_standard_context"

const val LOG_EVENT_MOX_REST_REQUEST = "rest_request"
const val LOG_EVENT_MOX_REST_RESPONSE = "rest_response"
