package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.LimitedMemoryCache
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Limited {@link Bitmap bitmap} cache. Provides {@link Bitmap bitmaps} storing. Size of all stored bitmaps will not to
 * exceed size limit. When cache reaches limit size then the least recently used bitmap is deleted from cache.<br />
 * <br />
 * <b>NOTE:</b> This cache uses strong and weak references for stored Bitmaps. Strong references - for limited count of
 * Bitmaps (depends on cache size), weak references - for all other cached Bitmaps.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.3.0
 */
class LRULimitedMemoryCache(maxSize: Int) : LimitedMemoryCache(maxSize) {

    companion object {
        const val INITIAL_CAPACITY = 10
        const val LOAD_FACTOR = 1.1f
    }

    /** Cache providing Least-Recently-Used logic */
    private val lruCache = Collections.synchronizedMap(LinkedHashMap<String, Bitmap>(INITIAL_CAPACITY, LOAD_FACTOR, true))

    private val cacheSize = AtomicInteger()

    override fun get(key: String): Bitmap? {
        lruCache.get(key) // call "get" for LRU logic
        return super.get(key)
    }

    override fun put(key: String, value: Bitmap): Boolean {
        if (super.put(key, value)) {
            lruCache.put(key, value)
            return true
        } else {
            return false
        }
    }

    override fun remove(key: String): Bitmap? {
        lruCache.remove(key)
        return super.remove(key)
    }

    override fun clear() {
        lruCache.clear()
        super.clear()
    }

    override fun createReference(value: Bitmap): Reference<Bitmap> = WeakReference<Bitmap>(value)

    override fun getSize(value: Bitmap?): Int = if (value != null) {
        value.rowBytes * value.height
    } else {
        0
    }

    override fun removeNext(): Bitmap? {
        var mostLongUsedValue: Bitmap? = null
        synchronized(lruCache) {
            val it = lruCache.entries.iterator()
            if (it.hasNext()) {
                val entry = it.next()
                mostLongUsedValue = entry.value
                it.remove()
            }
        }
        return mostLongUsedValue
    }

}