package playground.common.messaging.config

import org.springframework.boot.context.properties.ConfigurationProperties
//import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.util.backoff.ExponentialBackOff
import java.time.Duration

//@ConstructorBinding
@ConfigurationProperties("dragon.messaging.consumer.retry")
data class KafkaConsumerRetryProperties(

    /** duration between first try and second attempt */
    val initialInterval: Duration = Duration.ofMillis(200),

    /** interval(n + 1) = interval(n) * duration */
    val multiplier: Double = 2.0,

    /** the longest interval before giving up */
    val maxInterval: Duration = Duration.ofMillis(ExponentialBackOff.DEFAULT_MAX_INTERVAL),

    /** the total allowed elapsed time from first to last retry */
    val maxElapsedTime: Duration = Duration.ofSeconds(5)
)
