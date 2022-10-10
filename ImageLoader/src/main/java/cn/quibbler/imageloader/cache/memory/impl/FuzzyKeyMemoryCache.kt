package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.MemoryCache

/**
 * Decorator for {@link MemoryCache}. Provides special feature for cache: some different keys are considered as
 * equals (using {@link Comparator comparator}). And when you try to put some value into cache by key so entries with
 * "equals" keys will be removed from cache before.<br />
 * <b>NOTE:</b> Used for internal needs. Normally you don't need to use this class.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
class FuzzyKeyMemoryCache(
    private val cache: MemoryCache,
    private val keyComparator: Comparator<String>
) : MemoryCache {

    override fun put(key: String, value: Bitmap): Boolean {
        // Search equal key and remove this entry
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