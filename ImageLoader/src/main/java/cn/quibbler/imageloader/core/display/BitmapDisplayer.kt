package cn.quibbler.imageloader.core.display

import android.graphics.Bitmap
import cn.quibbler.imageloader.core.assist.LoadedFrom
import cn.quibbler.imageloader.core.imageaware.ImageAware

/**
 * Displays {@link Bitmap} in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware}. Implementations can
 * apply some changes to Bitmap or any animation for displaying Bitmap.<br />
 * Implementations have to be thread-safe.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see com.nostra13.universalimageloader.core.imageaware.ImageAware
 * @see com.nostra13.universalimageloader.core.assist.LoadedFrom
 * @since 1.5.6
 */
interface BitmapDisplayer {

    /**
     * Displays bitmap in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware}.
     * <b>NOTE:</b> This method is called on UI thread so it's strongly recommended not to do any heavy work in it.
     *
     * @param bitmap     Source bitmap
     * @param imageAware {@linkplain com.nostra13.universalimageloader.core.imageaware.ImageAware Image aware view} to
     *                   display Bitmap
     * @param loadedFrom Source of loaded image
     */
    fun display(bitmap: Bitmap, imageAware: ImageAware, loadedFrom: LoadedFrom)

}