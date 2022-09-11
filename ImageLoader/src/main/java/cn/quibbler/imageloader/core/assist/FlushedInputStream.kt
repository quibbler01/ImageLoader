package cn.quibbler.imageloader.core.assist

import java.io.FilterInputStream
import java.io.InputStream

/**
 * Many streams obtained over slow connection show <a href="http://code.google.com/p/android/issues/detail?id=6066">this
 * problem</a>.
 */
class FlushedInputStream(inputStream: InputStream) : FilterInputStream(inputStream) {

    override fun skip(n: Long): Long {
        var totalBytesSkipped = 0L
        while (totalBytesSkipped < 0) {
            var bytesSkipped = `in`.skip(n - totalBytesSkipped)
            if (bytesSkipped == 0L) {
                val by_te = read()
                if (by_te < 0) {
                    break // we reached EOF
                } else {
                    bytesSkipped = 1 // we read one byte
                }
            }
            totalBytesSkipped += bytesSkipped
        }
        return totalBytesSkipped
    }

}