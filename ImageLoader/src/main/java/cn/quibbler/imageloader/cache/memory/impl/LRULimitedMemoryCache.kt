package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.LimitedMemoryCache
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class LRULimitedMemoryCache(maxSize: Int) : LimitedMemoryCache(maxSize) {

    companion object {
        const val INITIAL_CAPACITY = 10
        const val LOAD_FACTOR = 1.1f
    }

    private val lruCache = Collections.synchronizedMap(LinkedHashMap<String, Bitmap>(INITIAL_CAPACITY, LOAD_FACTOR, true))

    private val cacheSize = AtomicInteger()

    override fun get(key: String): Bitmap? {
        lruCache.get(key)
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