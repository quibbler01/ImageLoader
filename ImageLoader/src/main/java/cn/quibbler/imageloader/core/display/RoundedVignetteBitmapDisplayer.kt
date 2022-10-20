package cn.quibbler.imageloader.core.display

import android.graphics.*
import cn.quibbler.imageloader.core.assist.LoadedFrom
import cn.quibbler.imageloader.core.imageaware.ImageAware
import cn.quibbler.imageloader.core.imageaware.ImageViewAware

/**
 * Can display bitmap with rounded corners and vignette effect. This implementation works only with ImageViews wrapped
 * in ImageViewAware.
 * <br />
 * This implementation is inspired by
 * <a href="http://www.curious-creature.org/2012/12/11/android-recipe-1-image-with-rounded-corners/">
 * Romain Guy's article</a>. It rounds images using custom drawable drawing. Original bitmap isn't changed.
 * <br />
 * <br />
 * If this implementation doesn't meet your needs then consider
 * <a href="https://github.com/vinc3m1/RoundedImageView">this project</a> for usage.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.1
 */
class RoundedVignetteBitmapDisplayer(cornerRadiusPixels: Int, marginPixels: Int) : RoundedBitmapDisplayer(cornerRadiusPixels, marginPixels) {

    override fun display(bitmap: Bitmap, imageAware: ImageAware, loadedFrom: LoadedFrom) {
        if (!(imageAware is ImageViewAware)) {
            throw IllegalArgumentException("ImageAware should wrap ImageView. ImageViewAware is expected.")
        }

        imageAware.setImageDrawable(RoundedVignetteDrawable(bitmap, cornerRadius, margin))
    }

    protected class RoundedVignetteDrawable(bitmap: Bitmap, cornerRadius: Int, margin: Int) : RoundedDrawable(bitmap, cornerRadius, margin) {

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)
            val vignette = RadialGradient(
                mRect.centerX(), mRect.centerY() * 1.0f / 0.7f, mRect.centerX() * 1.3f,
                intArrayOf(0, 0, 0x7f000000),
                floatArrayOf(0.0f, 0.7f, 1.0f),
                Shader.TileMode.CLAMP
            )

            val oval = Matrix()
            oval.setScale(1.0f, 0.7f)
            vignette.setLocalMatrix(oval)

            paint.shader = ComposeShader(bitmapShader, vignette, PorterDuff.Mode.SRC_OVER)
        }

    }

}