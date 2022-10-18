package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.MemoryCache
import kotlin.math.max

/**
 * A cache that holds strong references to a limited number of Bitmaps. Each time a Bitmap is accessed, it is moved to
 * the head of a queue. When a Bitmap is added to a full cache, the Bitmap at the end of that queue is evicted and may
 * become eligible for garbage collection.<br />
 * <br />
 * <b>NOTE:</b> This cache uses only strong references for stored Bitmaps.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.8.1
 */
class LruMemoryCache(
    /** Size of this cache in bytes */
    private val maxSize: Int
) : MemoryCache {

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

    /**
     * Returns the Bitmap for {@code key} if it exists in the cache. If a Bitmap was returned, it is moved to the head
     * of the queue. This returns null if a Bitmap is not cached.
     */
    override fun get(key: String): Bitmap? {
        synchronized(this) {
            return map[key]
        }
    }

    /** Removes the entry for {@code key} if it exists. */
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
        trimToSize(-1) // -1 will evict 0-sized elements
    }

    /**
     * Remove the eldest entries until the total of remaining entries is at or below the requested size.
     *
     * @param maxSize the maximum size of the cache before returning. May be -1 to evict even 0-sized elements.
     */
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

    /**
     * Returns the size {@code Bitmap} in bytes.
     * <p/>
     * An entry's size must not change while it is in the cache.
     */
    private fun sizeOf(value: Bitmap?): Int = if (value != null) {
        value.rowBytes * value.height
    } else {
        0
    }

    override fun toString(): String {
        return "LruCache[maxSize=$maxSize]"
    }

}