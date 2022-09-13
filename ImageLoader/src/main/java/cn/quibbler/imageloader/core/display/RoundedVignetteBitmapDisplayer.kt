package cn.quibbler.imageloader.core.display

import android.graphics.*
import cn.quibbler.imageloader.core.assist.LoadedFrom
import cn.quibbler.imageloader.core.imageaware.ImageAware
import cn.quibbler.imageloader.core.imageaware.ImageViewAware

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