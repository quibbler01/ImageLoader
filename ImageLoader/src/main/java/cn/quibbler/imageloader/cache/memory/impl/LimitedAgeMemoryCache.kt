package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.MemoryCache
import java.util.*
import kotlin.collections.HashMap

class LimitedAgeMemoryCache(private val cache: MemoryCache, private val maxAge: Long) : MemoryCache {

    private val loadingDates = Collections.synchronizedMap(HashMap<String, Long>())

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