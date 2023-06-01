package playground.common.rest.logging

import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import playground.common.rest.logging.BufferedServletInputStream

class BufferedWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
    private val inputStream: ServletInputStream = BufferedServletInputStream(request.inputStream, request.contentLength + 1)

    override fun getInputStream(): ServletInputStream = inputStream

    fun body(): String {
        val bytes = inputStream.readBytes()
        inputStream.reset()
        return String(bytes)
    }
}
