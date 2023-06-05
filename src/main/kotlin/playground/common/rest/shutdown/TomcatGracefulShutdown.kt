package playground.common.rest.shutdown

import playground.common.rest.logging.LOG_EVENT_GRACE_PERIOD_ELAPSED_WITH_ACTIVE_REQUEST
import playground.common.rest.logging.LOG_EVENT_GRACE_SHUTDOWN_COMPLETE
import playground.common.rest.logging.LOG_EVENT_GRACE_SHUTDOWN_NOT_STANDARD_CONTEXT
import org.apache.catalina.Container
import org.apache.catalina.Context
import org.apache.catalina.core.StandardContext
import org.apache.catalina.core.StandardWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer
import org.springframework.context.SmartLifecycle
import org.springframework.util.ReflectionUtils
import playground.common.observability.logging.MDC_KEY_LOG_EVENT
import playground.common.observability.logging.kv
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

// Based on Spring-boot 2.3 code
class TomcatGracefulShutdown(
    private val gracefulPeriod: Duration
) : SmartLifecycle, TomcatContextCustomizer {

    private val log: Logger = LoggerFactory.getLogger(javaClass)!!

    private val contexts = mutableListOf<Context>()
    private var isRunning = false
    override fun isRunning() = isRunning

    override fun start() {
        isRunning = true
    }

    override fun getPhase() = 1000

    // should not be called, but we have to implement it
    override fun stop() {
        isRunning = false
        val deadline = Instant.now().plus(gracefulPeriod)
        try {
            contexts.forEach {
                while (it.isActive) {
                    if (Instant.now().isAfter(deadline)) {
                        log.info(
                            "Grace period elapsed with one or more requests still active",
                            kv(MDC_KEY_LOG_EVENT, LOG_EVENT_GRACE_PERIOD_ELAPSED_WITH_ACTIVE_REQUEST)
                        )
                        return
                    }
                    Thread.sleep(1000)
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        log.info("Graceful shutdown complete", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_GRACE_SHUTDOWN_COMPLETE))
    }

    private val Context.isActive: Boolean get() {
        return isInProgress || hasAllocatedChild
    }

    private val Context.isInProgress: Boolean get() {
        if (this is StandardContext) {
            val field = ReflectionUtils.findField(StandardContext::class.java, "inProgressAsyncCount")
            field!!.isAccessible = true
            val activeCount = (field[this] as AtomicLong).get()
            log.debug("Active count: $activeCount")
            return activeCount > 0
        } else {
            log.warn("Not a StandardContext: $this", kv(MDC_KEY_LOG_EVENT, LOG_EVENT_GRACE_SHUTDOWN_NOT_STANDARD_CONTEXT))
            return false
        }
    }

    private val Container.hasAllocatedChild: Boolean get() {
        val allocatedCount = findChildren().sumBy { (it as StandardWrapper).countAllocated }
        log.debug("Checking in hasAllocatedChild: $allocatedCount")
        return allocatedCount > 0
    }

    override fun customize(context: Context) {
        contexts.add(context)
    }
}
