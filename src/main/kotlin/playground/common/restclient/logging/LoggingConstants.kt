package playground.common.restclient.logging

const val PROFILE_REST_CLIENT_DEBUG_LOGGING = "rest-client-debug-logging"
const val PROFILE_REST_BODY_DEBUG_LOGGING = "rest-body-debug-logging"

// REST LOG EVENT
const val LOG_EVENT_CREATING_REST_TEMPLATE_BUILDER = "rest_client_creating_template_builder"
const val LOG_EVENT_REST_REQUEST = "rest_client_request"

const val MDC_KEY_REST_BODY = "restRequestBody"
const val MDC_KEY_REST_REQUEST_LENGTH = "restRequestContentLength"
const val MDC_KEY_REST_REQUEST_CONTENT_TYPE = "restRequestContentType"
const val MDC_KEY_REST_REQUEST_METHOD = "restRequestMethod"
const val MDC_KEY_REST_REQUEST_URI = "restRequestURI"
