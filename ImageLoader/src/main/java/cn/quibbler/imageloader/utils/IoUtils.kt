package cn.quibbler.imageloader.utils

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

const val DEFAULT_BUFFER_SIZE = 32 * 1024 // 32 KB

const val DEFAULT_IMAGE_TOTAL_SIZE = 500 * 1024 // 500 Kb

const val CONTINUE_LOADING_PERCENTAGE = 75

/**
 * Copies stream, fires progress events by listener, can be interrupted by listener. Uses buffer size =
 * {@value #DEFAULT_BUFFER_SIZE} bytes.
 *
 * @param is       Input stream
 * @param os       Output stream
 * @param listener null-ok; Listener of copying progress and controller of copying interrupting
 * @return <b>true</b> - if stream copied successfully; <b>false</b> - if copying was interrupted by listener
 * @throws IOException
 */
@Throws(IOException::class)
fun copyStream(
    input: InputStream,
    os: OutputStream,
    listener: CopyListener,
): Boolean {
    return copyStream(input, os, listener, DEFAULT_BUFFER_SIZE)
}

/**
 * Copies stream, fires progress events by listener, can be interrupted by listener.
 *
 * @param is         Input stream
 * @param os         Output stream
 * @param listener   null-ok; Listener of copying progress and controller of copying interrupting
 * @param bufferSize Buffer size for copying, also represents a step for firing progress listener callback, i.e.
 *                   progress event will be fired after every copied <b>bufferSize</b> bytes
 * @return <b>true</b> - if stream copied successfully; <b>false</b> - if copying was interrupted by listener
 * @throws IOException
 */
@Throws(IOException::class)
fun copyStream(
    input: InputStream,
    os: OutputStream,
    listener: CopyListener,
    bufferSize: Int
): Boolean {
    var current = 0
    var total = input.available()
    if (total <= 0) {
        total = DEFAULT_IMAGE_TOTAL_SIZE
    }

    val bytes = ByteArray(bufferSize)
    var count = 0
    if (shouldStopLoading(listener, current, total)) return false
    while (input.read(bytes, 0, bufferSize).also { count = it } != -1) {
        os.write(bytes, 0, count)
        current += count
        if (shouldStopLoading(listener, current, total)) return false
    }
    os.flush()
    return true
}

fun shouldStopLoading(listener: CopyListener?, current: Int, total: Int): Boolean {
    listener?.let {
        val shouldContinue = it.onBytesCopied(current, total)
        if (shouldContinue) {
            if (100 * current / total < CONTINUE_LOADING_PERCENTAGE) {
                return true // if loaded more than 75% then continue loading anyway
            }
        }
    }
    return false
}

/**
 * Reads all data from stream and close it silently
 *
 * @param is Input stream
 */
fun readAndCloseStream(`is`: InputStream) {
    val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
    try {
        while (`is`.read(bytes, 0, DEFAULT_BUFFER_SIZE) != -1);
    } catch (ignored: IOException) {
    } finally {
        closeSilently(`is`)
    }
}

fun closeSilently(closeable: Closeable?) {
    closeable?.let {
        try {
            it.close()
        } catch (e: Exception) {
        }
    }
}

/** Listener and controller for copy process */
interface CopyListener {

    /**
     * @param current Loaded bytes
     * @param total   Total bytes for loading
     * @return <b>true</b> - if copying should be continued; <b>false</b> - if copying should be interrupted
     */
    fun onBytesCopied(current: Int, total: Int): Boolean

}