package cn.quibbler.imageloader.utils

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.MemoryCache
import cn.quibbler.imageloader.core.assist.ImageSize

private const val URI_AND_SIZE_SEPARATOR = "_"

private const val WIDTH_AND_HEIGHT_SEPARATOR = "x"

/**
 * Generates key for memory cache for incoming image (URI + size).<br />
 * Pattern for cache key - <b>[imageUri]_[width]x[height]</b>.
 */
fun generateKey(imageUri: String, targetSize: ImageSize): String {
    return StringBuilder(imageUri)
        .append(URI_AND_SIZE_SEPARATOR)
        .append(targetSize.width)
        .append(WIDTH_AND_HEIGHT_SEPARATOR)
        .append(targetSize.height)
        .toString()
}

fun createFuzzyKeyComparator(): Comparator<String> {
    return Comparator { o1, o2 ->
        val imageUri1 = o1.substring(0, o1.lastIndexOf(URI_AND_SIZE_SEPARATOR))
        val imageUri2 = o1.substring(0, o2.lastIndexOf(URI_AND_SIZE_SEPARATOR))
        imageUri1.compareTo(imageUri2)
    }
}

/**
 * Searches all bitmaps in memory cache which are corresponded to incoming URI.<br />
 * <b>Note:</b> Memory cache can contain multiple sizes of the same image if only you didn't set
 * {@link ImageLoaderConfiguration.Builder#denyCacheImageMultipleSizesInMemory()
 * denyCacheImageMultipleSizesInMemory()} option in {@linkplain ImageLoaderConfiguration configuration}
 */
fun findCachedBitmapsForImageUri(imageUri: String, memoryCache: MemoryCache): List<Bitmap?> {
    val list = ArrayList<Bitmap>()
    for (key in memoryCache.keys()) {
        if (key.startsWith(imageUri)) {
            memoryCache.get(key)?.let { list.add(it) }
        }
    }
    return list
}

/**
 * Searches all keys in memory cache which are corresponded to incoming URI.<br />
 * <b>Note:</b> Memory cache can contain multiple sizes of the same image if only you didn't set
 * {@link ImageLoaderConfiguration.Builder#denyCacheImageMultipleSizesInMemory()
 * denyCacheImageMultipleSizesInMemory()} option in {@linkplain ImageLoaderConfiguration configuration}
 */
fun findCacheKeysForImageUri(imageUri: String, memoryCache: MemoryCache): List<String> {
    val list = ArrayList<String>()
    for (key in memoryCache.keys()) {
        if (key.startsWith(imageUri)) {
            list.add(key)
        }
    }
    return list
}

/**
 * Removes from memory cache all images for incoming URI.<br />
 * <b>Note:</b> Memory cache can contain multiple sizes of the same image if only you didn't set
 * {@link ImageLoaderConfiguration.Builder#denyCacheImageMultipleSizesInMemory()
 * denyCacheImageMultipleSizesInMemory()} option in {@linkplain ImageLoaderConfiguration configuration}
 */
fun removeFromCache(imageUri: String, memoryCache: MemoryCache) {
    val keysToRemove = ArrayList<String>()
    for (key in memoryCache.keys()) {
        if (key.startsWith(imageUri)) {
            keysToRemove.add(key)
        }
    }
    for (keyToRemove in keysToRemove) {
        memoryCache.remove(keyToRemove)
    }
}
