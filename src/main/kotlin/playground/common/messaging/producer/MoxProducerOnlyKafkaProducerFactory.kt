package playground.common.messaging.producer

import org.springframework.kafka.core.DefaultKafkaProducerFactory

/**
 * Producer factory with custom {@link #getCacheCountForTransactionIdPrefix()} which returns cache size
 * for producer id prefix.
 */
open class MoxProducerOnlyKafkaProducerFactory<K, V>(producerConfig: Map<String, Any>) :
    DefaultKafkaProducerFactory<K, V>(producerConfig) {

    /**
     * Returns cache size for transactionIdPrefix of factory
     *
     * @throws NullPointerException - if transactionIdPrefix is null
     */
    fun getCacheCountForTransactionIdPrefix() = getCache(transactionIdPrefix!!)!!.size
}
