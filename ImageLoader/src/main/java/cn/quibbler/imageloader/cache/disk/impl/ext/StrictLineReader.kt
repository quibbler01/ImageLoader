package cn.quibbler.imageloader.cache.disk.impl.ext

import java.io.*
import java.nio.charset.Charset

class StrictLineReader : Closeable {

    companion object {
        private const val CR = '\r'.code.toByte()
        private const val LF = '\n'.code.toByte()
    }

    constructor(input: InputStream, charset: Charset) : this(input = input, capacity = 8192, charset = charset)

    constructor(input: InputStream, capacity: Int, charset: Charset) {
        if (capacity < 0) {
            throw IllegalArgumentException("capacity <= 0")
        }
        if (charset != US_ASCII) {
            throw IllegalArgumentException("Unsupported encoding")
        }
        this.input = input
        this.charset = charset
        this.buf = ByteArray(capacity)
    }

    private val input: InputStream
    private val charset: Charset

    /*
   * Buffered data is stored in {@code buf}. As long as no exception occurs, 0 <= pos <= end
   * and the data in the range [pos, end) is buffered for reading. At end of input, if there is
   * an unterminated line, we set end == -1, otherwise end == pos. If the underlying
   * {@code InputStream} throws an {@code IOException}, end may remain as either pos or -1.
   */
    private var buf: ByteArray
    private var pos = 0
    private var end = 0

    override fun close() {
        synchronized(input) {
            input.close()
        }
    }

    @Throws(IOException::class)
    fun readLine(): String {
        synchronized(input) {
            // Read more data if we are at the end of the buffered data.
            // Though it's an error to read after an exception, we will let {@code fillBuf()}
            // throw again if that happens; thus we need to handle end == -1 as well as end == pos.
            if (pos >= end) {
                fillBuf()
            }
            // Try to find LF in the buffered data and return the line if successful.
            for (i in pos until end) {
                if (buf[i] == LF) {
                    val lineEnd = if (i != pos && buf[i - 1] == CR) i - 1 else i
                    val res = String(buf, pos, lineEnd - pos, charset)
                    pos = i + 1
                    return res
                }
            }

            // Let's anticipate up to 80 characters on top of those already read.
            val out = object : ByteArrayOutputStream(end - pos + 80) {
                override fun toString(): String {
                    val length = if ((count > 0 && buf[count - 1] == CR)) count - 1 else count
                    try {
                        return String(buf, 0, length, charset)
                    } catch (e: UnsupportedEncodingException) {
                        throw AssertionError(e)
                    }
                }
            }

            while (true) {
                out.write(buf, pos, end - pos)
                // Mark unterminated line in case fillBuf throws EOFException or IOException.
                end = -1
                fillBuf()
                // Try to find LF in the buffered data and return the line if successful.
                // Try to find LF in the buffered data and return the line if successful.
                for (i in pos until end) {
                    if (buf[i] == LF) {
                        if (i != pos) {
                            out.write(buf, pos, i - pos)
                        }
                        pos = i + 1
                        return out.toString()
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun fillBuf() {
        val result = input.read(buf, 0, buf.size)
        if (result == -1) {
            throw EOFException()
        }
        pos = 0
        end = result
    }

}