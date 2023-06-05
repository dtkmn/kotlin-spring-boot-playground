package playground.common.messaging.consumer

/**
 * Annotation that marks a method to be kafka listener.
 * Works in similar way as @KafkaListener annotation but has fewer properties and can be set only on method.
 * It is designed only for batch job listeners as its logic is to commit consumer offset at the beginning of processing,
 * this is done by {@link playground.common.messaging.consumer.BatchJobListenerAspect}.
 *
 * Method annotated by this annotation has to have Acknowledgement parameter which will be used by aspect
 * to commit the message:
 * <pre>
 * {@code
 *
 * @BatchJobListener(
 * id = "\${dragon.messaging.sync-customer-identity-command.consumer-group-id}",
 * clientIdPrefix = "\${dragon.messaging.sync-customer-identity-command.consumer-group-id}",
 * topics = ["\${dragon.messaging.sync-customer-identity-command.topic}"]
 * )
 * @Throws(Exception::class)
 * fun process(
 * @Payload payload: StartBatchJobCommand,
 * acknowledgment: Acknowledgment
 * )
 *
 * }
 * </pre>
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BatchJobListener(

    /**
     * The unique identifier of the container managing for this endpoint.
     * Also, used as groupId.
     */
    val id: String,

    /**
     * The topics for this listener.
     */
    val topics: Array<String>,

    /**
     * Overrides the client id property in the consumer factory
     * configuration. A suffix ('-n') is added for each container instance to ensure
     * uniqueness when concurrency is used.
     */
    val clientIdPrefix: String,

    /**
     * Set to true/false to override the default setting in the container factory,
     * so that listener will be started when application context is created.
     */
    val autoStartup: String = "",

    /**
     * Kafka consumer properties; they will supersede any properties with the same name
     * defined in the consumer factory.
     */
    val properties: Array<String> = []
)
