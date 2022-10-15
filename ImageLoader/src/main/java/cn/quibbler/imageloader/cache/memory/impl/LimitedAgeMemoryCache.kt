package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.MemoryCache
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.max

/**
 * Decorator for {@link MemoryCache}. Provides special feature for cache: if some cached object age exceeds defined
 * value then this object will be removed from cache.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see MemoryCache
 * @since 1.3.1
 */
class LimitedAgeMemoryCache : MemoryCache {

    private val cache: MemoryCache

    private val maxAge: Long

    private val loadingDates = Collections.synchronizedMap(HashMap<String, Long>())

    /**
     * @param cache  Wrapped memory cache
     * @param maxAge Max object age <b>(in seconds)</b>. If object age will exceed this value then it'll be removed from
     *               cache on next treatment (and therefore be reloaded).
     */
    constructor(cache: MemoryCache, maxAge: Long) {
        this.cache = cache
        this.maxAge = maxAge * 1000 // to milliseconds
    }

    override fun put(key: String, value: Bitmap): Boolean {
        val putSuccesfully = cache.put(key, value)
        if (putSuccesfully) {
            loadingDates[key] = System.currentTimeMillis()
        }
        return putSuccesfully
    }

    override fun get(key: String): Bitmap? {
        val loadingDate = loadingDates[key]
        if (loadingDate != null && System.currentTimeMillis() - loadingDate > maxAge) {
            cache.remove(key)
            loadingDates.remove(key)
        }
        return cache.get(key)
    }

    override fun remove(key: String): Bitmap? {
        loadingDates.remove(key)
        return cache.remove(key)
    }

    override fun keys(): Collection<String> {
        return cache.keys()
    }

    override fun clear() {
        cache.clear()
        loadingDates.clear()
    }

}