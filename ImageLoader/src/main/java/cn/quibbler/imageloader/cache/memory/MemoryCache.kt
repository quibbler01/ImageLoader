package cn.quibbler.imageloader.cache.memory

import android.graphics.Bitmap

interface MemoryCache {

    fun put(key: String, value: Bitmap): Boolean

    fun get(key: String): Bitmap?

    fun remove(key: String): Bitmap?

    fun keys(): Collection<String>

    fun clear()

}