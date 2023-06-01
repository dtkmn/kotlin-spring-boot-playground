package playground.common.idempotencyjpa.jpa

import playground.common.idempotency.impl.IdempotencyStoreAdapter
import playground.common.idempotency.impl.QualifiedIdempotencyKey
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

/**
 * [IdempotencyStoreAdapter] that depends on Spring JPA with any database.
 */
class JpaIdempotencyStoreAdapter(
    val repository: IdempotencyRecordRepository,
    transactionManager: PlatformTransactionManager
) : IdempotencyStoreAdapter {

    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionTemplate.PROPAGATION_REQUIRES_NEW
    }

    override fun getOrNull(key: QualifiedIdempotencyKey) =
        repository.findById(IdempotencyPK(key)).map { it.data ?: it.data2 }.orElse(null)

    override fun <R> executeInTransaction(body: (ops: IdempotencyStoreAdapter.InTransactionOperations) -> R): R {
        @Suppress("UNCHECKED_CAST")
        return transactionTemplate.execute { body(inTransactionOperations) } as R
    }

    override fun mayBeDuplicateRecordException(e: Exception): Boolean =
        e is DataIntegrityViolationException

    private val inTransactionOperations = object : IdempotencyStoreAdapter.InTransactionOperations {
        override fun insert(key: QualifiedIdempotencyKey, value: String, final: Boolean) {
            repository.save(IdempotencyRecord(IdempotencyPK(key), Instant.now(), null, value, final))
        }
    }
}
