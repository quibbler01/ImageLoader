package cn.quibbler.imageloader.core.download

import java.io.InputStream
import java.util.*

/**
 * Provides retrieving of {@link InputStream} of image by URI.<br />
 * Implementations have to be thread-safe.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.4.0
 */
interface ImageDownloader {

    /**
     * Retrieves {@link InputStream} of image by URI.
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to {@link DisplayImageOptions.Builder#extraForDownloader(Object)
     *                 DisplayImageOptions.extraForDownloader(Object)}; can be null
     * @return {@link InputStream} of image
     * @throws IOException                   if some I/O error occurs during getting image stream
     * @throws UnsupportedOperationException if image URI has unsupported scheme(protocol)
     */
    fun getStream(imageUri: String, extra: Any?): InputStream?


    /** Represents supported schemes(protocols) of URI. Provides convenient methods for work with schemes and URIs. */
    enum class Scheme(private val scheme: String, private val uriPrefix: String = "$scheme://") {
        HTTP("http"),
        HTTPS("https"),
        FILE("file"),
        CONTENT("content"),
        ASSETS("assets"),
        DRAWABLE("drawable"),
        UNKNOWN("");

        /**
         * Defines scheme of incoming URI
         *
         * @param uri URI for scheme detection
         * @return Scheme of incoming URI
         */
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

        /** Appends scheme to incoming path */
        fun wrap(path: String): String {
            return "$uriPrefix$path"
        }

        /** Removed scheme part ("scheme://") from incoming URI */
        fun crop(uri: String): String {
            if (!belongsTo(uri)) {
                throw IllegalArgumentException("URI $uri doesn't have expected scheme $scheme")
            }
            return uri.substring(uriPrefix.length)
        }

    }

}