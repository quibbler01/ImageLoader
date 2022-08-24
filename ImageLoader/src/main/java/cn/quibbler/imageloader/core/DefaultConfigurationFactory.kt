package cn.quibbler.imageloader.core

import cn.quibbler.imageloader.cache.disk.naming.FileNameGenerator
import cn.quibbler.imageloader.cache.disk.naming.HashCodeFileNameGenerator

object DefaultConfigurationFactory {

    fun createFileNameGenerator(): FileNameGenerator = HashCodeFileNameGenerator()


}