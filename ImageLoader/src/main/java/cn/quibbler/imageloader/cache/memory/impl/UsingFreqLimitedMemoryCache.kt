package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.LimitedMemoryCache
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashMap

/**
 * Limited {@link Bitmap bitmap} cache. Provides {@link Bitmap bitmaps} storing. Size of all stored bitmaps will not to
 * exceed size limit. When cache reaches limit size then the bitmap which used the least frequently is deleted from
 * cache.<br />
 * <br />
 * <b>NOTE:</b> This cache uses strong and weak references for stored Bitmaps. Strong references - for limited count of
 * Bitmaps (depends on cache size), weak references - for all other cached Bitmaps.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
class UsingFreqLimitedMemoryCache(sizeLimit: Int) : LimitedMemoryCache(sizeLimit) {

    /**
     * Contains strong references to stored objects (keys) and last object usage date (in milliseconds). If hard cache
     * size will exceed limit then object with the least frequently usage is deleted (but it continue exist at
     * {@link #softMap} and can be collected by GC at any time)
     */
    private val usingCounts = Collections.synchronizedMap(HashMap<Bitmap, Int>())

    override fun get(key: String): Bitmap? {
        val value = super.get(key)
        // Increment usage count for value if value is contained in hardCahe
        value?.let {
            val usageCount = usingCounts.get(value)
            if (usageCount != null) {
                usingCounts[value] = usageCount + 1
            }
        }
        return value
    }

    override fun put(key: String, value: Bitmap): Boolean {
        if (super.put(key, value)) {
            usingCounts.put(value, 0)
            return true
        } else {
            return false
        }
    }

    override fun remove(key: String): Bitmap? {
        val value = super.remove(key)
        value?.let {
            usingCounts.remove(value)
        }
        return super.remove(key)
    }

    override fun createReference(value: Bitmap): Reference<Bitmap> = WeakReference<Bitmap>(value)

    override fun getSize(value: Bitmap?): Int = if (value != null) {
        value.rowBytes * value.height
    } else {
        0
    }

    override fun clear() {
        usingCounts.clear()
        super.clear()
    }

    override fun removeNext(): Bitmap? {
        var minUsageCount = 0
        var leastUsedValue: Bitmap? = null
        synchronized(usingCounts) {
            for (entry in usingCounts.entries) {
                if (leastUsedValue == null) {
                    leastUsedValue = entry.key
                    minUsageCount = entry.value
                } else {
                    val lastValueUsage = entry.value
                    if (lastValueUsage < minUsageCount) {
                        minUsageCount = lastValueUsage
                        leastUsedValue = entry.key
                    }
                }
            }
        }
        usingCounts.remove(leastUsedValue)
        return leastUsedValue
    }

}