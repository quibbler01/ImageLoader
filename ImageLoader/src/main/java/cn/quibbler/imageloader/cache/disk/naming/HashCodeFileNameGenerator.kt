package cn.quibbler.imageloader.cache.disk.naming

class HashCodeFileNameGenerator : FileNameGenerator {

    override fun generate(imageUrl: String): String = imageUrl.hashCode().toString()

}