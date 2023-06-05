package playground.common.messaging.idempotency


import playground.common.idempotency.IdempotencyContext
import playground.common.idempotency.IdempotencyProvider
import playground.common.messaging.MessageMetadata
import org.springframework.kafka.support.KafkaNull
import playground.common.idempotency.IdempotencyAssertionError
import playground.common.idempotency.impl.GenericProperty
import playground.common.messaging.publisher.Topic

internal const val PROPERTY_NAMESPACE = "com.projectdrgn.common.idempotency.extensions"

internal val IdempotencyContext.publishStrategy: PublishStrategy by GenericProperty(PROPERTY_NAMESPACE) {
    settings.mapNotNull { it as? PublishStrategy }.also {
        if (it.isEmpty()) throw IdempotencyAssertionError("Publish strategy is required but not configured.")
        if (it.size > 1) throw IdempotencyAssertionError("Multiple publish strategies provided: $it.")
    }.first()
}

/**
 * Alternative API to [Publisher] or [TopicPublisher] for publishing in idempotently safe way.
 *
 * To be able to use this, you need to specify [PublishStrategy] for each idempotent process.
 * Behavior then depends on specified strategy.
 */
fun <T : Any> IdempotencyContext.publish(
    topic: Topic<T>,
    key: String,
    payload: T,
    metadata: MessageMetadata? = null
) = publishStrategy.publish(this, topic.name, key, payload, metadata)

fun <T : Any> IdempotencyContext.publish(
    topicName: String,
    key: String,
    payload: T,
    metadata: MessageMetadata? = null
) = publishStrategy.publish(this, topicName, key, payload, metadata)

fun <T : Any> IdempotencyContext.publishTombstone(
    topic: Topic<T>,
    key: String,
    metadata: MessageMetadata? = null
) = publishStrategy.publish(this, topic.name, key, KafkaNull.INSTANCE, metadata)

fun IdempotencyContext.publishTombstone(
    topicName: String,
    key: String,
    metadata: MessageMetadata? = null
) = publishStrategy.publish(this, topicName, key, KafkaNull.INSTANCE, metadata)

/**
 * Defines behavior of [IdempotencyContext.publish].
 *
 * It is [IdempotencyContext.Setting] so it can be provided to [IdempotencyProvider.runIdempotentProcess].
 *
 * All instances are available as beans in default configuration. Therefore you don't need to manage dependencies.
 * It is also needed for interop with REST idempotency annotations.
 *
 * See [PublishingInFinalPhase], [PublishingInKafkaConsumer] and [NoPublishing] for more details.
 */
interface PublishStrategy : IdempotencyContext.Setting {

    override val conflictsWithClasses: Set<Class<out IdempotencyContext.Setting>>
        get() = setOf(PublishStrategy::class.java)

    fun <T : Any> publish(
        context: IdempotencyContext,
        topicName: String,
        key: String,
        payload: T,
        metadata: MessageMetadata?
    )

    fun validate(context: IdempotencyContext) {}

    fun <R> wrapBody(data: IdempotencyContext.PhaseData, body: () -> R): () -> R = body
}
