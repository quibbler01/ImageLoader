package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.LimitedMemoryCache
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.*

/**
 * Limited {@link Bitmap bitmap} cache. Provides {@link Bitmap bitmaps} storing. Size of all stored bitmaps will not to
 * exceed size limit. When cache reaches limit size then cache clearing is processed by FIFO principle.<br />
 * <br />
 * <b>NOTE:</b> This cache uses strong and weak references for stored Bitmaps. Strong references - for limited count of
 * Bitmaps (depends on cache size), weak references - for all other cached Bitmaps.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
class FIFOLimitedMemoryCache : LimitedMemoryCache {

    private val queue = Collections.synchronizedList(LinkedList<Bitmap>())

    constructor(sizeLimit: Int) : super(sizeLimit)

    override fun put(key: String, value: Bitmap): Boolean {
        if (super.put(key, value)) {
            queue.add(value)
            return true
        } else {
            return false
        }
    }

    override fun remove(key: String): Bitmap? {
        val value = super.get(key)
        value?.let {
            queue.remove(value)
        }
        return super.remove(key)
    }

    override fun clear() {
        queue.clear()
        super.clear()
    }

    override fun getSize(value: Bitmap?): Int = if (value != null) {
        value.rowBytes * value.height
    } else {
        0
    }

    override fun removeNext(): Bitmap? {
        return queue.removeAt(0)
    }

    override fun createReference(value: Bitmap): Reference<Bitmap> {
        return WeakReference<Bitmap>(value)
    }

}