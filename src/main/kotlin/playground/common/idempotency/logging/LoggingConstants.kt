package playground.common.idempotency.logging

// CONTEXT
const val LOG_EVENT_IDEMPOTENCY_OLD_CONTEXT_NOT_REMOVED = "idempotency_old_context_not_removed"

// PROVIDER
const val LOG_EVENT_RELEASE_IDEMPOTENCY_LOCK_FAILED = "idempotency_lock_release_failed"
const val LOG_EVENT_IDEMPOTENCY_SKIP_PHASE_DUE_TO_RESULT_STORED = "idempotency_result_stored_so_skip_phase"
const val LOG_EVENT_IDEMPOTENCY_MULTIPLE_EXECUTION_IN_PARALLEL = "idempotency_multiple_executions_in_parallel"

// MONITORING
const val LOG_EVENT_CONTEXT_CLOSED = "idempotency_context_closed"
const val LOG_EVENT_CONTEXT_CREATED = "idempotency_context_created"
const val LOG_EVENT_ENTERING_NEW_PHASE = "idempotency_entering_new_phase"
const val LOG_EVENT_PHASE_FINISHED = "idempotency_phase_finished_no_retry"
const val LOG_EVENT_IDEMPOTENCY_PHASE_EXECUTOR_ERROR = "idempotency_phase_executor_error"
const val LOG_EVENT_IDEMPOTENCY_PHASE_BODY_FINISHED_EXCEPTIONALLY = "idempotency_phase_finished_exceptionally"

// INPUT VALIDATION
const val LOG_EVENT_IDEMPOTENCY_INPUT_DIFFER_FROM_CAHCED = "idpempotency_inputs_differ_from_cached"
