package cn.quibbler.imageloader.core.download

import java.io.InputStream
import java.util.*

interface ImageDownloader {

    fun getStream(imageUri: String, extra: Any): InputStream?


    enum class Scheme(private val scheme: String, private val uriPrefix: String = "$scheme://") {
        HTTP("http"),
        HTTPS("https"),
        FILE("file"),
        CONTENT("content"),
        ASSETS("assets"),
        DRAWABLE("drawable"),
        UNKNOWN("");

        companion object {
            fun ofUri(uri: String?): Scheme {
                if (uri != null) {
                    for (s in values()) {
                        if (s.belongsTo(uri)) {
                            return s
                        }
                    }
                }
                return UNKNOWN
            }
        }

        private fun belongsTo(uri: String): Boolean {
            return uri.lowercase(Locale.US).startsWith(uriPrefix)
        }

        fun wrap(path: String): String {
            return "$uriPrefix$path"
        }

        fun crop(uri: String): String {
            if (!belongsTo(uri)) {
                throw IllegalArgumentException("URI $uri doesn't have expected scheme $scheme")
            }
            return uri.substring(uriPrefix.length)
        }

    }

}