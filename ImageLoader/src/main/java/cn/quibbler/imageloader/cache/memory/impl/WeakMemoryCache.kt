package cn.quibbler.imageloader.cache.memory.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.memory.BaseMemoryCache
import java.lang.ref.Reference
import java.lang.ref.WeakReference

/**
 * Memory cache with {@linkplain WeakReference weak references} to {@linkplain android.graphics.Bitmap bitmaps}<br />
 * <br />
 * <b>NOTE:</b> This cache uses only weak references for stored Bitmaps.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.5.3
 */
class WeakMemoryCache : BaseMemoryCache() {

    override fun createReference(value: Bitmap): Reference<Bitmap> {
        return WeakReference<Bitmap>(value)
    }

}