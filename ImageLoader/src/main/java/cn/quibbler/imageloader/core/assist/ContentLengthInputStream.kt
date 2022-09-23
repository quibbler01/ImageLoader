package cn.quibbler.imageloader.core.assist

import java.io.InputStream

class ContentLengthInputStream(private val stream: InputStream, private val length: Int) : InputStream() {

    override fun available(): Int {
        return length
    }

    override fun close() {
        stream.close()
    }

    override fun mark(readlimit: Int) {
        stream.mark(read())
    }

    override fun read(): Int {
        return stream.read()
    }

    override fun read(b: ByteArray?): Int {
        return stream.read(b)
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        return stream.read(b, off, len)
    }

    override fun skip(byteCount: Long): Long {
        return stream.skip(byteCount)
    }

    override fun reset() {
        stream.reset()
    }

    override fun markSupported(): Boolean {
        return stream.markSupported()
    }

}