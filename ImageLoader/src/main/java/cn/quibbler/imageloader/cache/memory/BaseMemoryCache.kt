package cn.quibbler.imageloader.cache.memory

import android.graphics.Bitmap
import java.lang.ref.Reference
import java.util.*
import kotlin.collections.HashSet

/**
 * Base memory cache. Implements common functionality for memory cache. Provides object references (
 * {@linkplain Reference not strong}) storing.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
abstract class BaseMemoryCache : MemoryCache {

    /** Stores not strong references to objects */
    private val softMap = Collections.synchronizedMap(HashMap<String, Reference<Bitmap>>())

    override fun put(key: String, value: Bitmap): Boolean {
        softMap.put(key, createReference(value))
        return true
    }

    override fun get(key: String): Bitmap? {
        var result: Bitmap? = null
        val reference = softMap.get(key)
        reference?.let {
            result = it.get()
        }
        return result
    }

    override fun remove(key: String): Bitmap? {
        val bmpRef = softMap.remove(key)
        return bmpRef?.get()
    }

    override fun keys(): Collection<String> {
        synchronized(softMap) {
            return HashSet<String>(softMap.keys)
        }
    }

    override fun clear() {
        softMap.clear()
    }

    /** Creates {@linkplain Reference not strong} reference of value */
    abstract fun createReference(value: Bitmap): Reference<Bitmap>

}