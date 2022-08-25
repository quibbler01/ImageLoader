package cn.quibbler.imageloader.utils

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception


fun copyStream(
    input: InputStream,
    os: OutputStream,
    listener: CopyListener,
    bufferSize: Int
): Boolean {

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