package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.LimitedMemoryCache
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.*

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