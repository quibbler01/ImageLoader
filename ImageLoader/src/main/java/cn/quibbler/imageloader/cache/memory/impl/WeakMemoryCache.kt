package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.BaseMemoryCache
import java.lang.ref.Reference
import java.lang.ref.WeakReference

class WeakMemoryCache : BaseMemoryCache() {

    override fun createReference(value: Bitmap): Reference<Bitmap> {
        return WeakReference<Bitmap>(value)
    }

}