package cn.quibbler.imageloader.cache.disk.impl.ext

import java.io.*
import java.nio.charset.Charset

/**
 * Buffers input from an {@link InputStream} for reading lines.
 *
 * <p>This class is used for buffered reading of lines. For purposes of this class, a line ends
 * with "\n" or "\r\n". End of input is reported by throwing {@code EOFException}. Unterminated
 * line at end of input is invalid and will be ignored, the caller may use {@code
 * hasUnterminatedLine()} to detect it after catching the {@code EOFException}.
 *
 * <p>This class is intended for reading input that strictly consists of lines, such as line-based
 * cache entries or cache journal. Unlike the {@link java.io.BufferedReader} which in conjunction
 * with {@link java.io.InputStreamReader} provides similar functionality, this class uses different
 * end-of-input reporting and a more restrictive definition of a line.
 *
 * <p>This class supports only charsets that encode '\r' and '\n' as a single byte with value 13
 * and 10, respectively, and the representation of no other character contains these values.
 * We currently check in constructor that the charset is one of US-ASCII, UTF-8 and ISO-8859-1.
 * The default charset is US_ASCII.
 */
class StrictLineReader : Closeable {

    companion object {
        private const val CR = '\r'.code.toByte()
        private const val LF = '\n'.code.toByte()
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

    /**
     * Constructs a new {@code LineReader} with the specified charset and the default capacity.
     *
     * @param in the {@code InputStream} to read data from.
     * @param charset the charset used to decode data. Only US-ASCII, UTF-8 and ISO-8859-1 are
     * supported.
     * @throws NullPointerException if {@code in} or {@code charset} is null.
     * @throws IllegalArgumentException if the specified charset is not supported.
     */
    constructor(input: InputStream, charset: Charset) : this(input = input, capacity = 8192, charset = charset)

    /**
     * Constructs a new {@code LineReader} with the specified capacity and charset.
     *
     * @param in the {@code InputStream} to read data from.
     * @param capacity the capacity of the buffer.
     * @param charset the charset used to decode data. Only US-ASCII, UTF-8 and ISO-8859-1 are
     * supported.
     * @throws NullPointerException if {@code in} or {@code charset} is null.
     * @throws IllegalArgumentException if {@code capacity} is negative or zero
     * or the specified charset is not supported.
     */
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

    /**
     * Closes the reader by closing the underlying {@code InputStream} and
     * marking this reader as closed.
     *
     * @throws IOException for errors when closing the underlying {@code InputStream}.
     */
    override fun close() {
        synchronized(input) {
            input.close()
        }
    }

    /**
     * Reads the next line. A line ends with {@code "\n"} or {@code "\r\n"},
     * this end of line marker is not included in the result.
     *
     * @return the next line from the input.
     * @throws IOException for underlying {@code InputStream} errors.
     * @throws EOFException for the end of source stream.
     */
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

    /**
     * Reads new input data into the buffer. Call only with pos == end or end == -1,
     * depending on the desired outcome if the function throws.
     */
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