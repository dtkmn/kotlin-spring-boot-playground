package playground.common.observability.logging

import net.logstash.logback.argument.StructuredArguments
import net.logstash.logback.encoder.LogstashEncoder

/**
 * Logback encoder to log in JSON format
 *
 * Usage in logback-spring.xml:
 * <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
 *   <encoder class="JsonEncoder"/>
 * </appender>
 */
class JsonEncoder : LogstashEncoder()

/**
 * Wraps a key-value pair to include it in the JSON log output
 *
 * Usage with SLF4J logger:
 * if (log.isInfoEnabled) log.info("my message {}", kv("mykey", "myvalue"))
 * will log { "message": "my message mykey=myvalue", "mykey": "myvalue" }
 *
 * Note: StructuredArguments construct additional objects.
 * Therefore, it is best practice to surround the log lines with logger.isXXXEnabled(),
 * to avoid the object construction if the log level is disabled.
 */
fun kv(key: String, value: Any) = StructuredArguments.keyValue(key, value)

/**
 * Like kv(key, value), but omits the key in the formatted message
 *
 * Usage with SL4J logger:
 * if (log.isInfoEnabled) log.info("my message {}", v("mykey", "myvalue"))
 * will log { "message": "my message myvalue", "mykey": "myvalue" }
 *
 * Note: StructuredArguments construct additional objects.
 * Therefore, it is best practice to surround the log lines with logger.isXXXEnabled(),
 * to avoid the object construction if the log level is disabled.
 */
fun v(key: String, value: Any) = StructuredArguments.value(key, value)
