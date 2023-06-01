package playground.common.exception.error

/********************************************************************************
 * SYSTEM
 *******************************************************************************/

// Unspecified Server Errors
const val ERR_SYS_SERVER_ERROR: String = "ERR_SYS_001"

// Messaging Errors
const val ERR_SYS_MESSAGING_UNAVAILABLE: String = "ERR_SYS_002"

// MVC Errors
const val ERR_SYS_MVC_ERROR: String = "ERR_SYS_003"

// Message Processing Error
const val ERR_SYS_MESSAGE_PROCESSING_FAILED: String = "ERR_SYS_004"

// Dependencies timeout error
const val ERR_SYS_DEPENDENCIES_TIMEOUT_ERROR: String = "ERR_SYS_010"

// CLIENT ERROR
const val ERR_SYS_CLIENT_ERROR: String = "ERR_SYS_17"

/********************************************************************************
 * SECURITY
 *******************************************************************************/

// Access Denied Errors
const val ERR_SYS_ACCESS_DENIED: String = "ERR_SYS_005"

/********************************************************************************
 * BUSINESS
 *******************************************************************************/

// Business - Support Error
const val ERR_SYS_SUPPORT_ERROR: String = "ERR_SYS_006"

// Business - Validation Error
const val ERR_SYS_VALIDATION_ERROR: String = "ERR_SYS_007"

// Business - Undefined Processor Error
const val ERR_SYS_PROCESSOR_ERROR: String = "ERR_SYS_PROCESSOR_ERROR"

/********************************************************************************
 * DATA
 *******************************************************************************/

// Resource Not Found Errors
const val ERR_SYS_RESOURCE_NOT_FOUND: String = "ERR_SYS_008"

const val ERR_SYS_NUMERIC_TIMESTAMP: String = "ERR_SYS_009"

/********************************************************************************
 * IDEMPOTENCY
 *******************************************************************************/

// There was an attempt to start an idempotent process retry while the lock of the previous execution is active
const val ERR_SYS_IDEMPOTENT_PROCESS_LOCKED = "ERR_SYS_011"

// The inputs of idempotent process retry do not match with previously stored ones
const val ERR_SYS_IDEMPOTENT_INPUT_DOES_NOT_MATCH = "ERR_SYS_012"

// Indicates serious misuse of Idempotency library. Should not happen in production.
const val ERR_SYS_IDEMPOTENCY_ASSERTION_ERROR = "ERR_SYS_013"

// An @IdempotentHandler was called without idempotency key header
const val ERR_SYS_IDEMPOTENCY_KEY_MISSING = "ERR_SYS_014"

// HTTP message conversion error
const val ERR_SYS_HTTP_CONVERSION_ERROR = "ERR_SYS_015"

// Client abort exception
const val ERR_SYS_CLIENT_ABORT_ERROR = "ERR_SYS_016"

/********************************************************************************
 * MESSAGING
 *******************************************************************************/
const val ERR_SYS_CANNOT_SEND_TOMBSTONE_TO_NOT_SNAPSHOT_TOPIC = "ERR_SYS_016"
