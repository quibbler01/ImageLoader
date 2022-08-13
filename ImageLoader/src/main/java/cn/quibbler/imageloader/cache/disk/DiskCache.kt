package cn.quibbler.imageloader.cache.disk

import android.graphics.Bitmap
import cn.quibbler.imageloader.utils.CopyListener
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 *
 */
interface DiskCache {

    fun getDirectory(): File?

    fun get(imageUrl: String?): File?

    @Throws(IOException::class)
    fun save(imageUrl: String?, imageStream: InputStream?, listener: CopyListener?): Boolean

    fun save(imageUrl: String?, bitmap: Bitmap?): Boolean

    fun remove(imageUrl: String?)

    fun close()

    fun clear()

}