package cn.quibbler.imageloader.core.display

import android.graphics.Bitmap
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import cn.quibbler.imageloader.core.assist.LoadedFrom
import cn.quibbler.imageloader.core.imageaware.ImageAware

/**
 * Displays image with "fade in" animation
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com), Daniel Martí
 * @since 1.6.4
 */
class FadeInBitmapDisplayer(
    private val durationMillis: Int, private val animateFromNetwork: Boolean,
    private val animateFromDisk: Boolean, private val animateFromMemory: Boolean
) : BitmapDisplayer {

    companion object {
        /**
         * Animates {@link ImageView} with "fade-in" effect
         *
         * @param imageView      {@link ImageView} which display image in
         * @param durationMillis The length of the animation in milliseconds
         */
        fun animate(imageView: View?, durationMillis: Int) {
            imageView?.let {
                val fadeImage = AlphaAnimation(0f, 1f)
                fadeImage.duration = durationMillis.toLong()
                fadeImage.interpolator = DecelerateInterpolator()
                it.startAnimation(fadeImage)
            }
        }
    }

    /**
     * @param durationMillis Duration of "fade-in" animation (in milliseconds)
     */
    constructor(durationMillis: Int) : this(durationMillis, true, true, true)

    override fun display(bitmap: Bitmap, imageAware: ImageAware, loadedFrom: LoadedFrom) {
        imageAware.setImageBitmap(bitmap)

        if ((animateFromNetwork && loadedFrom == LoadedFrom.NETWORK) ||
            (animateFromDisk && loadedFrom == LoadedFrom.DISC_CACHE) ||
            (animateFromMemory && loadedFrom == LoadedFrom.MEMORY_CACHE)
        ) {
            animate(imageAware.getWrappedView(), durationMillis)
        }
    }

}