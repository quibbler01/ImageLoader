package cn.quibbler.imageloader.utils

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

const val DEFAULT_BUFFER_SIZE = 32 * 1024
const val DEFAULT_IMAGE_TOTAL_SIZE = 500 * 1024
const val CONTINUE_LOADING_PERCENTAGE = 75


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
                return true
            }
        }
    }
    return false
}

fun closeSilently(closeable: Closeable?) {
    closeable?.let {
        try {
            it.close()
        } catch (e: Exception) {
        }
    }
}

interface CopyListener {
    fun onBytesCopied(current: Int, total: Int): Boolean
}