package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.MemoryCache

class FuzzyKeyMemoryCache(
    private val cache: MemoryCache,
    private val keyComparator: Comparator<String>
) : MemoryCache {

    override fun put(key: String, value: Bitmap): Boolean {
        synchronized(cache) {
            var keyToRemove: String? = null
            for (cacheKey in cache.keys()) {
                if (keyComparator.compare(key, cacheKey) == 0) {
                    keyToRemove = cacheKey
                    break
                }
            }
            if (keyToRemove != null) {
                cache.remove(keyToRemove)
            }
        }
        return cache.put(key, value)
    }

    override fun get(key: String): Bitmap? = cache.get(key)

    override fun remove(key: String): Bitmap? = cache.remove(key)

    override fun keys(): Collection<String> = cache.keys()

    override fun clear() {
        cache.clear()
    }

}