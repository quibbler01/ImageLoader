package cn.quibbler.imageloader.cache.disk.naming

/**
 * Names image file as image URI {@linkplain String#hashCode() hashcode}
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.3.1
 */
class HashCodeFileNameGenerator : FileNameGenerator {

    override fun generate(imageUrl: String): String = imageUrl.hashCode().toString()

}