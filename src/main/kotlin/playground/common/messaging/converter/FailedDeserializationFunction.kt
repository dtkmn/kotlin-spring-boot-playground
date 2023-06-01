package playground.common.messaging.converter

import org.springframework.kafka.support.serializer.FailedDeserializationInfo
import java.util.function.Function

class FailedDeserializationFunction : Function<FailedDeserializationInfo, Any> {
    override fun apply(failedDeserializationInfo: FailedDeserializationInfo) = failedDeserializationInfo
}
