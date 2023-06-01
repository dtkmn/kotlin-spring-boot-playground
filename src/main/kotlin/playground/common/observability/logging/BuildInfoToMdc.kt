package playground.common.observability.logging

import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component

const val MDC_KEY_BUILD_GROUP = "build_group"
const val MDC_KEY_BUILD_NAME = "build_name"
const val MDC_KEY_BUILD_VERSION = "build_version"
const val MDC_KEY_BUILD_TIME = "build_time"

@Component
class BuildInfoToMdc() {
    @Autowired
    val buildProperties: BuildProperties? = null

    fun putAll() {
        mdcPut(MDC_KEY_BUILD_GROUP, buildProperties?.group)
        mdcPut(MDC_KEY_BUILD_NAME, buildProperties?.name)
        mdcPut(MDC_KEY_BUILD_VERSION, buildProperties?.version)
        mdcPut(MDC_KEY_BUILD_TIME, buildProperties?.time?.toString())
    }

    private fun mdcPut(key: String, value: String?) {
        MDC.put(key, value ?: "UNKNOWN")
    }
}
