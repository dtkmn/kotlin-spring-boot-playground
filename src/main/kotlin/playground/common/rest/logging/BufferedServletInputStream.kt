package playground.common.rest.logging

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream


class BufferedServletInputStream(
    private val original: ServletInputStream,
    private val numberOfReuse: Int = 10
) : ServletInputStream() {
    private val buf = original.buffered()
    private var used = 0

    init {
        buf.mark(numberOfReuse)
    }

    override fun isReady(): Boolean = original.isReady

    override fun isFinished(): Boolean = false

    override fun read(): Int = buf.read()

    override fun read(b: ByteArray): Int = buf.read(b)

    override fun read(b: ByteArray, off: Int, len: Int): Int = buf.read(b, off, len)

    override fun setReadListener(readListener: ReadListener?) {
        original.setReadListener(readListener)
    }

    override fun mark(readlimit: Int) = buf.mark(readlimit)
    override fun markSupported(): Boolean = true
    override fun reset() {
        ++used
        buf.reset()
    }

    override fun close() {
        if (++used >= numberOfReuse) {
            buf.close()
            super.close()
        } else {
            buf.reset()
        }
    }
}
