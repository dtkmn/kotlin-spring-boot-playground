package playground.publisher

import org.springframework.stereotype.Component
import playground.common.messaging.MessageMetadata
import playground.common.messaging.publisher.Topic
import playground.customer.contract.avro.customerprofilesnapshot.v2.CustomerProfileSnapshot
import java.time.Clock
import java.util.*

@Component
class EventPublisher(
    private val customerProfileSnapshotTopic: Topic<CustomerProfileSnapshot>,
    private val publisher: Publisher<CustomerProfileSnapshot>,
) {

    fun publishCustomerProfileSnapshot(customerProfileSnapshot: CustomerProfileSnapshot) {
        publisher.publish(
            topic = customerProfileSnapshotTopic.name,
            key = customerProfileSnapshot.customerId.toString(),
//            payload = "customerProfileSnapshot-message",
            payload = customerProfileSnapshot,
            metadata = MessageMetadata(
                idempotencyKey = UUID.randomUUID().toString(),
                customerId = customerProfileSnapshot.customerId.toString(),
                timestamp = Clock.systemUTC().instant()
            )
        )
    }

//    private fun <T : Any> publishEvent(
//        key: String,
//        payload: T,
//        topic: Topic<T>,
//        metadata: MessageMetadata? = null
//    ) = {
//        publisher.publish(
//            topic = topic.name,
//            key = key,
//            payload = payload,
//            metadata = metadata
//        )
//    }
}
