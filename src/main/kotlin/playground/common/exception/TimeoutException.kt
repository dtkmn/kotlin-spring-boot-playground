package playground.common.exception

/**
 * A TimeoutException should be thrown when the connection to non-dragon system (i.e. external API, infrastructure)
 * appears to be closed because of timeout.
 */
class TimeoutException(
    override val message: String,
    override val cause: Throwable?
) : RuntimeException(message, cause)
