package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.MemoryCache
import kotlin.math.max

class LruMemoryCache(private val maxSize: Int) : MemoryCache {

    init {
        if (maxSize <= 0) {
            throw IllegalArgumentException("maxSize <= 0")
        }
    }

    private val map = LinkedHashMap<String, Bitmap>(0, 0.75f, true)

    private var size = 0

    override fun put(key: String, value: Bitmap): Boolean {
        synchronized(this) {
            size += sizeOf(value)
            val previous = map.put(key, value)
            if (previous != null) {
                size -= sizeOf(previous)
            }
        }
        trimToSize(maxSize)
        return true
    }

    override fun get(key: String): Bitmap? {
        synchronized(this) {
            return map[key]
        }
    }

    override fun remove(key: String): Bitmap? {
        synchronized(this) {
            val previous = map.remove(key)
            if (previous != null) {
                size -= sizeOf(previous)
            }
            return previous
        }
    }

    override fun keys(): Collection<String> {
        synchronized(this) {
            return HashSet<String>(map.keys)
        }
    }

    override fun clear() {
        trimToSize(-1)
    }

    private fun trimToSize(maxSize: Int) {
        while (true) {
            var key: String? = null
            var value: Bitmap? = null

            synchronized(this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw IllegalStateException("${javaClass.name}.sizeOf() is reporting inconsistent results!")
                }
                if (size <= maxSize || map.isEmpty()) {
                    return
                }
                val toEvict = map.entries.iterator().next()
                if (toEvict == null) {
                    return
                }
                key = toEvict.key
                value = toEvict.value
                map.remove(key)
                size -= sizeOf(value)
            }
        }
    }

    private fun sizeOf(value: Bitmap?): Int = if (value != null) {
        value.rowBytes * value.height
    } else {
        0
    }

    override fun toString(): String {
        return "LruCache[maxSize=$maxSize]"
    }

}