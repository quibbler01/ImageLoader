package cn.quibbler.imageloader.cache.memory

import android.graphics.Bitmap

/**
 * Interface for memory cache
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.2
 */
interface MemoryCache {

    /**
     * Puts value into cache by key
     *
     * @return <b>true</b> - if value was put into cache successfully, <b>false</b> - if value was <b>not</b> put into
     * cache
     */
    fun put(key: String, value: Bitmap): Boolean

    /** Returns value by key. If there is no value for key then null will be returned. */
    fun get(key: String): Bitmap?

    /** Removes item by key */
    fun remove(key: String): Bitmap?

    /** Returns all keys of cache */
    fun keys(): Collection<String>

    /** Remove all items from cache */
    fun clear()

}