package playground.common.messaging.command

data class CommandRestResponse(
    /**
     * A list of possible command result payload types that the command will produce
     * For FE to simulate a sync. call behavior, FE will block until one of the payload types is returned
     * from BE (app-subscription service)
     *
     * Default to emptyList() for now until all services' REST endpoint added the payload event types
     */
    val commandResultPayloadTypes: List<String> = emptyList()
)
