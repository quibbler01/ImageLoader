package cn.quibbler.imageloader.utils

import cn.quibbler.imageloader.cache.disk.DiskCache
import java.io.File

/**
 * Returns {@link File} of cached image or <b>null</b> if image was not cached in disk cache
 *
 * @param imageUri String
 * @param diskCache DiskCache
 * @return File?
 */
fun findInCache(imageUri: String, diskCache: DiskCache): File? {
    val image = diskCache.get(imageUri)
    return if (image != null && image.exists()) image else null
}

/**
 * Removed cached image file from disk cache (if image was cached in disk cache before)
 *
 * @return <b>true</b> - if cached image file existed and was deleted; <b>false</b> - otherwise.
 */
fun removeFromCache(imageUri: String, diskCache: DiskCache): Boolean {
    val image = diskCache.get(imageUri)
    return image != null && image.exists() && image.delete()
}