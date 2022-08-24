package cn.quibbler.imageloader.cache.disk.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.disk.DiskCache
import cn.quibbler.imageloader.cache.disk.naming.FileNameGenerator
import cn.quibbler.imageloader.core.DefaultConfigurationFactory
import cn.quibbler.imageloader.utils.CopyListener
import java.io.File
import java.io.InputStream

class BaseDiskCache : DiskCache {

    constructor(cacheDir: File?) : this(cacheDir, null)

    constructor(cacheDir: File?, reserveCacheDir: File?) : this(
        cacheDir,
        reserveCacheDir,
        DefaultConfigurationFactory.createFileNameGenerator()
    )

    constructor(cacheDir: File?, reserveCacheDir: File?, generator: FileNameGenerator?) {

    }

    override fun getDirectory(): File? {
        TODO("Not yet implemented")
    }

    override fun get(imageUrl: String?): File? {
        TODO("Not yet implemented")
    }

    override fun save(
        imageUrl: String?,
        imageStream: InputStream?,
        listener: CopyListener?
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun save(imageUrl: String?, bitmap: Bitmap?): Boolean {
        TODO("Not yet implemented")
    }

    override fun remove(imageUrl: String?) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

}