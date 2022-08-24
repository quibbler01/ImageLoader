package cn.quibbler.imageloader.cache.disk.naming

interface FileNameGenerator {

    fun generate(imageUrl: String): String

}