package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.LimitedMemoryCache
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.*

class LargestLimitedMemoryCache : LimitedMemoryCache {

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