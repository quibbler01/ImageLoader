package cn.quibbler.imageloader.core.imageaware

import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.media.Image
import android.view.View
import android.widget.ImageView
import cn.quibbler.imageloader.core.assist.ViewScaleType
import cn.quibbler.imageloader.core.assist.fromImageView

/**
 * Wrapper for Android {@link android.widget.ImageView ImageView}. Keeps weak reference of ImageView to prevent memory
 * leaks.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.0
 */
class ImageViewAware(imageView: ImageView, checkActualViewSize: Boolean) : ViewAware(imageView, checkActualViewSize) {

    /**
     * Constructor. <br />
     * References {@link #ImageViewAware(android.widget.ImageView, boolean) ImageViewAware(imageView, true)}.
     *
     * @param imageView {@link android.widget.ImageView ImageView} to work with
     */
    constructor(imageView: ImageView) : this(imageView, true)

    /**
     * {@inheritDoc}
     * <br />
     * 3) Get <b>maxWidth</b>.
     */
    override fun getWidth(): Int {
        var width = super.getWidth()
        if (width <= 0) {
            val imageView = viewRef.get() as ImageView?
            imageView?.let {
                width = it.maxWidth
            }
        }
        return width
    }

    /**
     * {@inheritDoc}
     * <br />
     * 3) Get <b>maxHeight</b>
     */
    override fun getHeight(): Int {
        var height = super.getHeight()
        if (height <= 0) {
            val imageView = viewRef.get() as ImageView?
            imageView?.let {
                height = it.maxHeight
            }
        }
        return height
    }

    override fun getScaleType(): ViewScaleType {
        val imageView = viewRef.get() as ImageView?
        imageView?.let {
            return fromImageView(imageView)
        }
        return super.getScaleType()
    }

    override fun getWrappedView(): ImageView? = super.getWrappedView() as ImageView?

    override fun setImageDrawableInto(drawable: Drawable?, view: View?) {
        (view as ImageView?)?.setImageDrawable(drawable)
        if (drawable is AnimationDrawable) {
            drawable.start()
        }
    }

    override fun setImageBitmapInto(bitmap: Bitmap, view: View?) {
        (view as ImageView?)?.setImageBitmap(bitmap)
    }

}