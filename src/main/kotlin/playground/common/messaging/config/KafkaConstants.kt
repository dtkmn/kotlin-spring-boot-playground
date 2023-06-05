package playground.common.messaging.config

const val KAFKA_TRANSACTION_MANAGER = "kafkaTransactionManager"
const val PRODUCER_ONLY_KAFKA_TRANSACTION_MANAGER = "producerOnly-kafkaTransactionManager"

const val MESSAGE_PAYLOAD_LOGGER_BEAN = "messagePayloadLogger"
const val MESSAGING_OBJECT_MAPPER = "kafkaObjectMapper"
const val MESSAGING_JSON_TOPICS = "jsonTopics"
const val MESSAGING_SERIALIZER_RETRY_TEMPLATE = "serializerRetryTemplate"

const val NO_TX_MESSAGING_ERROR_HANDLER = "noTxMessagingErrorHandler"
const val NO_TX_DEAD_LETTER_PUBLISHER_RECOVERER = "noTxDeadLetterPublishingRecoverer"
const val NO_TX_BATCH_JOB_LISTENER_CONTAINER_FACTORY = "noTxBatchJobListenerContainerFactory"
const val NO_TX_BATCH_JOB_LISTENER_CONSUMER_FACTORY = "noTxBatchJobListenerConsumerFactory"
