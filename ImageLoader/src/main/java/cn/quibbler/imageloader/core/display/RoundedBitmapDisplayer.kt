package cn.quibbler.imageloader.core.display

import android.graphics.*
import android.graphics.drawable.Drawable
import cn.quibbler.imageloader.core.assist.LoadedFrom
import cn.quibbler.imageloader.core.imageaware.ImageAware
import cn.quibbler.imageloader.core.imageaware.ImageViewAware

/**
 * Can display bitmap with rounded corners. This implementation works only with ImageViews wrapped
 * in ImageViewAware.
 * <br />
 * This implementation is inspired by
 * <a href="http://www.curious-creature.org/2012/12/11/android-recipe-1-image-with-rounded-corners/">
 * Romain Guy's article</a>. It rounds images using custom drawable drawing. Original bitmap isn't changed.
 * <br />
 * <br />
 * If this implementation doesn't meet your needs then consider
 * <a href="https://github.com/vinc3m1/RoundedImageView">RoundedImageView</a> or
 * <a href="https://github.com/Pkmmte/CircularImageView">CircularImageView</a> projects for usage.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.5.6
 */
open class RoundedBitmapDisplayer(protected val cornerRadius: Int, protected val margin: Int) : BitmapDisplayer {

    constructor(cornerRadiusPixels: Int) : this(cornerRadiusPixels, 0)

    override fun display(bitmap: Bitmap, imageAware: ImageAware, loadedFrom: LoadedFrom) {
        if (!(imageAware is ImageViewAware)) {
            throw IllegalArgumentException("ImageAware should wrap ImageView. ImageViewAware is expected.")
        }

        imageAware.setImageDrawable(RoundedDrawable(bitmap, cornerRadius, margin))
    }

   open class RoundedDrawable : Drawable {

        protected val cornerRadius: Float
        protected val margin: Int

        protected val mRect = RectF()
        protected val mBitmapRect: RectF
        protected val bitmapShader: BitmapShader
        protected val paint: Paint

        constructor(bitmap: Bitmap, cornerRadius: Int, margin: Int) {
            this.cornerRadius = cornerRadius.toFloat()
            this.margin = margin

            bitmapShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            mBitmapRect = RectF(margin.toFloat(), margin.toFloat(), (bitmap.width - margin).toFloat(), (bitmap.height - margin).toFloat())

            paint = Paint()
            paint.isAntiAlias = true
            paint.shader = bitmapShader
            paint.isFilterBitmap = true
            paint.isDither = true
        }

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)
            mRect.set(margin.toFloat(), margin.toFloat(), (bounds.width() - margin).toFloat(), (bounds.height() - margin).toFloat())

            // Resize the original bitmap to fit the new bound
            val shaderMatrix = Matrix()
            shaderMatrix.setRectToRect(mBitmapRect, mRect, Matrix.ScaleToFit.FILL)
            bitmapShader.setLocalMatrix(shaderMatrix)
        }

        override fun draw(canvas: Canvas) {
            canvas.drawRoundRect(mRect, cornerRadius, cornerRadius, paint)
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    }

}