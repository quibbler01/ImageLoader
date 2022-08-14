package cn.quibbler.imageloader.cache.disk.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.disk.DiskCache
import cn.quibbler.imageloader.utils.CopyListener
import java.io.File
import java.io.InputStream

class LimitedAgeDiskCache : DiskCache {

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