package cn.quibbler.imageloader.cache.disk.naming

/**
 * Generates names for files at disk cache
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.3.1
 */
interface FileNameGenerator {

    /** Generates unique file name for image defined by URI */
    fun generate(imageUrl: String): String

}