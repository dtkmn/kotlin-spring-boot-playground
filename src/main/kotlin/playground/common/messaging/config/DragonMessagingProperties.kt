package playground.common.messaging.config

import org.springframework.boot.context.properties.ConfigurationProperties
//import org.springframework.boot.context.properties.ConstructorBinding

//@ConstructorBinding
@ConfigurationProperties("playground.messaging")
data class DragonMessagingProperties(
    val auth: Boolean = false,
    val ssl: Boolean = false,
    val username: String? = null,
    val password: String? = null,
    val transactionIdPrefix: String,
    val inTestContext: Boolean = false
)
