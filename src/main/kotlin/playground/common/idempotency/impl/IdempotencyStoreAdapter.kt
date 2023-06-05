package playground.common.idempotency.impl

import playground.common.idempotency.IdempotencyContext

/**
 * TODO(APS-680): Document
 */
interface IdempotencyStoreAdapter {

    /**
     * Returns a record associated with given [key] or `null` when the record does not exist.
     *
     * This method can be called both inside and outside transaction.
     */
    fun getOrNull(key: QualifiedIdempotencyKey): String?

    /**
     * Run the provided [body] in a transaction over the represented store.
     * If the [body] throws exceptions, all store operations should be aborted.
     *
     * Inside, there are few more operations that may be executed, represented by [InTransactionOperations]
     */
    fun <R> executeInTransaction(body: (ops: InTransactionOperations) -> R): R

    /**
     * Should return `true` if the the exception thrown from [executeInTransaction] might have been caused
     * by duplicate attempt to insert idempotency record using [InTransactionOperations.insert].
     */
    fun mayBeDuplicateRecordException(e: Exception): Boolean

    /** See [executeInTransaction] */
    interface InTransactionOperations {

        /**
         * Create a record associating given [key] with provided [value].
         * If the record for given [key] already exists, this function may succeed, but the transaction must fail.
         *
         * @param final Metadata to store with the record. See [IdempotencyContext.wasFinalPhase] for more info.
         */
        fun insert(key: QualifiedIdempotencyKey, value: String, final: Boolean)
    }
}
