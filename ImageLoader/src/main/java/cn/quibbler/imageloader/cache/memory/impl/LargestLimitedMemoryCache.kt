package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.LimitedMemoryCache
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.*

/**
 * Limited {@link Bitmap bitmap} cache. Provides {@link Bitmap bitmaps} storing. Size of all stored bitmaps will not to
 * exceed size limit. When cache reaches limit size then the bitmap which has the largest size is deleted from
 * cache.<br />
 * <br />
 * <b>NOTE:</b> This cache uses strong and weak references for stored Bitmaps. Strong references - for limited count of
 * Bitmaps (depends on cache size), weak references - for all other cached Bitmaps.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
class LargestLimitedMemoryCache : LimitedMemoryCache {

    /**
     * Contains strong references to stored objects (keys) and sizes of the objects. If hard cache
     * size will exceed limit then object with the largest size is deleted (but it continue exist at
     * {@link #softMap} and can be collected by GC at any time)
     */
    private val valueSizes = Collections.synchronizedMap(HashMap<Bitmap, Int>())

    constructor(sizeLimit: Int) : super(sizeLimit)

    override fun put(key: String, value: Bitmap): Boolean {
        if (super.put(key, value)) {
            valueSizes.put(value, getSize(value))
            return true
        } else {
            return false
        }
    }

    override fun remove(key: String): Bitmap? {
        val value = super.get(key)
        value?.let {
            valueSizes.remove(it)
        }
        return super.remove(key)
    }

    override fun clear() {
        valueSizes.clear()
        super.clear()
    }

    override fun createReference(value: Bitmap): Reference<Bitmap> = WeakReference<Bitmap>(value)

    override fun getSize(value: Bitmap?): Int = if (value == null) {
        0
    } else {
        value.rowBytes * value.height
    }

    override fun removeNext(): Bitmap? {
        var maxSize = 0
        var largestValue: Bitmap? = null

        val entries = valueSizes.entries
        synchronized(valueSizes) {
            for (entry in valueSizes.entries) {
                if (largestValue == null) {
                    largestValue = entry.key
                    maxSize = entry.value
                } else {
                    val size = entry.value
                    if (size > maxSize) {
                        maxSize = size
                        largestValue = entry.key
                    }
                }
            }
        }
        valueSizes.remove(largestValue)
        return largestValue
    }

}