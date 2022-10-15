package cn.quibbler.imageloader.cache.memory

import android.graphics.Bitmap
import androidx.core.graphics.get
import cn.quibbler.imageloader.utils.L
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Limited cache. Provides object storing. Size of all stored bitmaps will not to exceed size limit (
 * {@link #getSizeLimit()}).<br />
 * <br />
 * <b>NOTE:</b> This cache uses strong and weak references for stored Bitmaps. Strong references - for limited count of
 * Bitmaps (depends on cache size), weak references - for all other cached Bitmaps.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see BaseMemoryCache
 * @since 1.0.0
 */
abstract class LimitedMemoryCache(protected val sizeLimit: Int) : BaseMemoryCache() {

    companion object {
        const val MAX_NORMAL_CACHE_SIZE_IN_MB = 16

        const val MAX_NORMAL_CACHE_SIZE = MAX_NORMAL_CACHE_SIZE_IN_MB * 1024 * 1024
    }

    private val cacheSize: AtomicInteger = AtomicInteger()

    /**
     * Contains strong references to stored objects. Each next object is added last. If hard cache size will exceed
     * limit then first object is deleted (but it continue exist at {@link #softMap} and can be collected by GC at any
     * time)
     */
    private val hardCache = Collections.synchronizedList(LinkedList<Bitmap>())

    /** sizeLimit Maximum size for cache (in bytes) */
    init {
        if (sizeLimit > MAX_NORMAL_CACHE_SIZE) {
            L.w(
                "You set too large memory cache size (more than %1\$d Mb)",
                MAX_NORMAL_CACHE_SIZE_IN_MB
            )
        }
    }

    override fun put(key: String, value: Bitmap): Boolean {
        var putSuccessfully = false

        // Try to add value to hard cache
        val valueSize = getSize(value)
        val sizeLimit = sizeLimit
        var curCacheSize = cacheSize.get()
        if (valueSize < sizeLimit) {
            while (curCacheSize + valueSize > sizeLimit) {
                val removedValue = removeNext()
                if (hardCache.remove(removedValue)) {
                    curCacheSize = cacheSize.addAndGet(-getSize(removedValue))
                }
            }
            hardCache.add(value)
            cacheSize.addAndGet(valueSize)

            putSuccessfully = true
        }

        // Add value to soft cache
        super.put(key, value)
        return putSuccessfully
    }

    override fun remove(key: String): Bitmap? {
        val value = super.get(key)
        value?.let {
            if (hardCache.remove(value)) {
                cacheSize.addAndGet(-getSize(value))
            }
        }
        return super.remove(key)
    }

    override fun clear() {
        hardCache.clear()
        cacheSize.set(0)
        super.clear()
    }

    open fun getSize(value: Bitmap?): Int = if (value != null) {
        value.rowBytes * value.height
    } else {
        0
    }

    abstract fun removeNext(): Bitmap?

}