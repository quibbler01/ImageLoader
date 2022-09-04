package cn.quibbler.imageloader.cache.memory

import android.graphics.Bitmap
import androidx.core.graphics.get
import cn.quibbler.imageloader.utils.L
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

abstract class LimitedMemoryCache(protected val sizeLimit: Int) : BaseMemoryCache() {

    companion object {
        const val MAX_NORMAL_CACHE_SIZE_IN_MB = 16

        const val MAX_NORMAL_CACHE_SIZE = MAX_NORMAL_CACHE_SIZE_IN_MB * 1024 * 1024
    }

    private val cacheSize: AtomicInteger = AtomicInteger()

    private val hardCache = Collections.synchronizedList(LinkedList<Bitmap>())

    init {
        if (sizeLimit > MAX_NORMAL_CACHE_SIZE) {
            L.w(
                "You set too large memory cache size (more than %1\$d Mb)",
                MAX_NORMAL_CACHE_SIZE_IN_MB
            )
        }
    }

    override fun put(key: String, value: Bitmap): Boolean {
        var putSuccessfully = false

        val valueSize = getSize(value)
        val sizeLimit = sizeLimit
        var curCacheSize = cacheSize.get()
        if (valueSize < sizeLimit) {
            while (curCacheSize + valueSize > sizeLimit) {
                val removedValue = removeNext()
                if (hardCache.remove(removedValue)) {
                    curCacheSize = cacheSize.addAndGet(-getSize(removedValue))
                }
            }
            hardCache.add(value)
            cacheSize.addAndGet(valueSize)

            putSuccessfully = true
        }

        super.put(key, value)
        return putSuccessfully
    }

    override fun remove(key: String): Bitmap? {
        val value = super.get(key)
        value?.let {
            if (hardCache.remove(value)) {
                cacheSize.addAndGet(-getSize(value))
            }
        }
        return super.remove(key)
    }

    override fun clear() {
        hardCache.clear()
        cacheSize.set(0)
        super.clear()
    }

    open fun getSize(value: Bitmap?): Int = if (value != null) {
        value.rowBytes * value.height
    } else {
        0
    }

    abstract fun removeNext(): Bitmap?

}