package playground.common.exception

fun Exception.exceptionHandlingLogMessage() =
    "Handling ${javaClass.simpleName}: $message"
