package playground.common.messaging.publisher

open class Topic<T : Any>(val name: String, val messageType: Class<T>)
