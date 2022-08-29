package cn.quibbler.imageloader.cache.disk.impl.ext

import cn.quibbler.imageloader.utils.closeSilently
import java.io.*
import java.lang.RuntimeException
import java.nio.charset.Charset
import kotlin.jvm.Throws

val US_ASCII = Charset.forName("US-ASCII")

val UTF_8 = Charset.forName("UTF-8")

@Throws(IOException::class)
fun readFully(reader: Reader): String {
    try {
        val writer = StringWriter()
        val buffer = CharArray(1024)
        var count = 0
        while ((reader.read(buffer).also { count = it }) != -1) {
            writer.write(buffer, 0, count)
        }
        return writer.toString()
    } finally {
        closeSilently(reader)
    }
}

@Throws(IOException::class)
fun deleteContents(dir: File) {
    val files = dir.listFiles() ?: throw IOException("not a readable directory:$dir")
    for (file in files) {
        if (file.isDirectory) {
            deleteContents(file)
        }
        if (!file.delete()) {
            throw IOException("failed to delete file:$file")
        }
    }
}

@Throws(RuntimeException::class)
fun closeQuietly(closeable: Closeable?) {
    closeable?.let {
        try {
            it.close()
        } catch (rethrown: RuntimeException) {
            throw rethrown
        } catch (ignored: Exception) {

        }
    }
}
