package cn.quibbler.imageloader.core.display

import android.graphics.Bitmap
import cn.quibbler.imageloader.core.assist.LoadedFrom
import cn.quibbler.imageloader.core.imageaware.ImageAware

/**
 * Just displays {@link Bitmap} in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware}
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.5.6
 */
class SimpleBitmapDisplayer : BitmapDisplayer {

    override fun display(bitmap: Bitmap, imageAware: ImageAware, loadedFrom: LoadedFrom) {
        imageAware.setImageBitmap(bitmap)
    }

}