package playground.common.messaging.consumer

import java.io.StringReader
import java.lang.reflect.Method
import java.util.Collections
import java.util.Properties
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.BeanInitializationException
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.core.MethodIntrospector
import org.springframework.core.MethodIntrospector.MetadataLookup
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.kafka.annotation.KafkaListenerConfigurer
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar
import org.springframework.kafka.config.MethodKafkaListenerEndpoint
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory

/**
 * Post processor similar to KafkaListenerAnnotationBeanPostProcessor but working on BatchJobListener annotation and simplified.
 * When post processing gets all beans methods annotated with BatchJobListener annotation and creates kafka endpoints (MethodKafkaListenerEndpoint).
 * Each endpoint is then created as this is also KafkaListenerConfigurer.
 */
open class BatchJobListenerBeanPostProcessor<K, V>(private val listenerContainerFactoryName: String) :
    InitializingBean, BeanPostProcessor, BeanFactoryAware, KafkaListenerConfigurer {

    private lateinit var bf: BeanFactory

    private val messageHandlerMethodFactory = DefaultMessageHandlerMethodFactory()

    private val endpoints: MutableList<MethodKafkaListenerEndpoint<K, V>> =
        Collections.synchronizedList(mutableListOf())

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        // get all beans with annotated methods with @BatchJobListener and prepare endpoints
        val targetClass = AopUtils.getTargetClass(bean)
        val annotatedMethods = MethodIntrospector.selectMethods(
            targetClass,
            MetadataLookup<BatchJobListener?> { method: Method ->
                findListenerAnnotations(method)
            }
        )
        annotatedMethods.filter { entry -> entry.value != null }
            .forEach { (method, batchJobListener) -> processBatchJobListener(batchJobListener!!, method, bean) }
        return bean
    }

    override fun configureKafkaListeners(registrar: KafkaListenerEndpointRegistrar) {
        // for all prepared endpoints - register kafka listeners
        try {
            val listenerContainerFactory: KafkaListenerContainerFactory<*> = this.bf.getBean(
                listenerContainerFactoryName,
                KafkaListenerContainerFactory::class.java
            )
            endpoints.forEach { registrar.registerEndpoint(it, listenerContainerFactory) }
        } catch (ex: NoSuchBeanDefinitionException) {
            throw BeanInitializationException(
                "Could not start batch job listener. No  ${KafkaListenerContainerFactory::class.java.simpleName} " +
                    "with id '$listenerContainerFactoryName' was found in the application context",
                ex
            )
        }
    }

    override fun setBeanFactory(beanFactory: BeanFactory) {
        this.bf = beanFactory
    }

    override fun afterPropertiesSet() {
        messageHandlerMethodFactory.setBeanFactory(this.bf)
        messageHandlerMethodFactory.afterPropertiesSet()
    }

    private fun processBatchJobListener(
        kafkaListener: BatchJobListener,
        method: Method,
        bean: Any
    ) {
        val endpoint: MethodKafkaListenerEndpoint<K, V> = MethodKafkaListenerEndpoint()
        endpoint.method = method
        endpoint.bean = bean
        endpoint.setMessageHandlerMethodFactory(this.messageHandlerMethodFactory)
        resolveExpressionAsString(kafkaListener.id, "id")?.let { endpoint.setId(it) }
        endpoint.id?.let { endpoint.setGroupId(it) }
        endpoint.setTopics(*resolveTopics(kafkaListener.topics))
        resolveExpressionAsString(kafkaListener.clientIdPrefix, "clientIdPrefix")?.let { endpoint.setClientIdPrefix(it) }
        endpoint.setConcurrency(1)
        if (kafkaListener.autoStartup.isNotBlank()) {
            resolveExpressionAsBoolean(kafkaListener.autoStartup, "autoStartup")?.let { endpoint.setAutoStartup(it) }
        }
        val properties = Properties()
        kafkaListener.properties.forEach { properties.load(StringReader(it)) }
        endpoint.setConsumerProperties(properties)
        endpoint.setBeanFactory(this.bf)
        endpoints.add(endpoint)
    }

    private fun resolveTopics(topics: Array<String>): Array<String> {
        val result = mutableListOf<String>()
        if (topics.isNotEmpty()) {
            for (topic in topics) {
                val resolvedTopic = resolveExpression(topic)!!
                resolveAsString(resolvedTopic, result)
            }
        }
        return result.toTypedArray()
    }

    private fun resolveAsString(resolvedValue: Any, result: MutableList<String>) {
        when (resolvedValue) {
            is Array<*> -> {
                for (obj in resolvedValue) {
                    resolveAsString(obj!!, result)
                }
            }
            is String -> result.add(resolvedValue)
            is Iterable<*> -> {
                for (`object` in resolvedValue as Iterable<Any>) {
                    resolveAsString(`object`, result)
                }
            }
            else -> throw IllegalArgumentException("@KafKaListener can't resolve '$resolvedValue' as a String")
        }
    }

    private fun findListenerAnnotations(method: Method): BatchJobListener? {
        return AnnotatedElementUtils.findMergedAnnotation(method, BatchJobListener::class.java)
    }

    private fun resolveExpression(value: String): Any? {
        return (this.bf as ConfigurableBeanFactory).resolveEmbeddedValue(value)
    }

    private fun resolveExpressionAsString(value: String, attribute: String): String? {
        val resolved = resolveExpression(value)
        if (resolved is String) {
            return resolved
        } else if (resolved != null) {
            throw IllegalStateException("The [$attribute] must resolve to a String. Resolved to [${resolved.javaClass.typeName}] for [$value]")
        }
        return null
    }

    private fun resolveExpressionAsBoolean(value: String, attribute: String): Boolean? {
        val resolved = resolveExpression(value)
        var result: Boolean? = null
        when (resolved) {
            is Boolean -> result = resolved
            is String -> result = resolved.toBoolean()
            else -> if (resolved != null) {
                throw IllegalStateException(
                    "The [$attribute] must resolve to a Boolean or a String that can be parsed as a Boolean. " +
                        "Resolved to [${resolved.javaClass.typeName}] for [$value]"
                )
            }
        }
        return result
    }
}
