package cn.quibbler.imageloader.core.display

import android.graphics.*
import android.graphics.drawable.Drawable
import cn.quibbler.imageloader.core.assist.LoadedFrom
import cn.quibbler.imageloader.core.imageaware.ImageAware
import cn.quibbler.imageloader.core.imageaware.ImageViewAware
import kotlin.math.min

/**
 * Can display bitmap cropped by a circle. This implementation works only with ImageViews wrapped
 * in ImageViewAware.
 * <br />
 * If this implementation doesn't meet your needs then consider
 * <a href="https://github.com/vinc3m1/RoundedImageView">RoundedImageView</a> or
 * <a href="https://github.com/Pkmmte/CircularImageView">CircularImageView</a> projects for usage.
 *
 * @author Qualtagh, Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.5
 */
class CircleBitmapDisplayer(protected val strokeColor: Int?, protected val strokeWidth: Float) : BitmapDisplayer {

    constructor() : this(null)

    constructor(strokeColor: Int?) : this(strokeColor, 0f)

    override fun display(bitmap: Bitmap, imageAware: ImageAware, loadedFrom: LoadedFrom) {
        if (!(imageAware is ImageViewAware)) {
            throw IllegalArgumentException("ImageAware should wrap ImageView. ImageViewAware is expected.")
        }

        imageAware.setImageDrawable(CircleDrawable(bitmap, strokeColor, strokeWidth))
    }

    class CircleDrawable : Drawable {

        protected var radius: Float

        protected val mRect = RectF()
        protected val mBitmapRect: RectF
        protected val bitmapShader: BitmapShader
        protected val paint: Paint
        protected val strokePaint: Paint?
        protected val strokeWidth: Float
        protected var strokeRadius: Float

        constructor(bitmap: Bitmap, strokeColor: Int?, strokeWidth: Float) {
            val diameter = min(bitmap.width, bitmap.height)
            radius = diameter / 2f

            bitmapShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val left = (bitmap.width - diameter) / 2f
            val top = (bitmap.height - diameter) / 2f
            mBitmapRect = RectF(left, top, diameter.toFloat(), diameter.toFloat())

            paint = Paint()
            paint.isAntiAlias = true
            paint.shader = bitmapShader
            paint.isFilterBitmap = true
            paint.isDither = true

            if (strokeColor == null) {
                strokePaint = null
            } else {
                strokePaint = Paint()
                strokePaint.style = Paint.Style.STROKE
                strokePaint.strokeWidth = strokeWidth
                strokePaint.isAntiAlias = true
            }
            this.strokeWidth = strokeWidth
            strokeRadius = radius - strokeWidth / 2
        }

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)
            mRect.set(0f, 0f, bounds.width().toFloat(), bounds.height().toFloat())
            radius = min(bounds.width(), bounds.height()).toFloat()
            strokeRadius = radius - strokeWidth / 2

            // Resize the original bitmap to fit the new bound
            val shaderMatrix = Matrix()
            shaderMatrix.setRectToRect(mBitmapRect, mRect, Matrix.ScaleToFit.FILL)
            bitmapShader.setLocalMatrix(shaderMatrix)
        }

        override fun draw(canvas: Canvas) {
            canvas.drawCircle(radius, radius, radius, paint)
            if (strokePaint != null) {
                canvas.drawCircle(radius, radius, radius, strokePaint)
            }
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