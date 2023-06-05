package playground.common.rest.shutdown

import playground.common.rest.logging.LOG_EVENT_GRACE_SHUTDOWN_STOPPING
import org.apache.catalina.connector.Connector
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer
import org.springframework.context.SmartLifecycle
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv

// Based on Spring-boot 2.3 code
class TomcatPreShutdown : SmartLifecycle, TomcatConnectorCustomizer {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)!!

    private val connectors = mutableListOf<Connector>()

    private var isRunning = false
    override fun isRunning() = isRunning

    override fun start() {
        isRunning = true
    }

    override fun stop() {
        isRunning = false
        logger.info("No longer listening for new connections", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_GRACE_SHUTDOWN_STOPPING))
        connectors.forEach {
            it.pause()
            it.protocolHandler.closeServerSocketGraceful()
        }
    }

    override fun customize(connector: Connector) {
        connectors.add(connector)
    }
}
