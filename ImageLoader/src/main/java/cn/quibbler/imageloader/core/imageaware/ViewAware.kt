package cn.quibbler.imageloader.core.imageaware

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import cn.quibbler.imageloader.core.assist.ViewScaleType
import cn.quibbler.imageloader.utils.L
import java.lang.ref.Reference
import java.lang.ref.WeakReference

/**
 * Wrapper for Android {@link android.view.View View}. Keeps weak reference of View to prevent memory leaks.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.2
 */
abstract class ViewAware(view: View?, protected val checkActualViewSize: Boolean) : ImageAware {

    companion object {
        const val WARN_CANT_SET_DRAWABLE = "Can't set a drawable into view. You should call ImageLoader on UI thread for it."
        const val WARN_CANT_SET_BITMAP = "Can't set a bitmap into view. You should call ImageLoader on UI thread for it."
    }

    protected val viewRef: Reference<View>

    init {
        if (view == null) throw IllegalArgumentException("view must not be null")
        viewRef = WeakReference<View>(view)
    }

    /**
     * Constructor. <br />
     * References {@link #ViewAware(android.view.View, boolean) ImageViewAware(imageView, true)}.
     *
     * @param view {@link android.view.View View} to work with
     */
    constructor(view: View?) : this(view, true)

    /**
     * {@inheritDoc}
     * <p/>
     * Width is defined by target {@link android.view.View view} parameters, configuration
     * parameters or device display dimensions.<br />
     * Size computing algorithm (go by steps until get non-zero value):<br />
     * 1) Get the actual drawn <b>getWidth()</b> of the View<br />
     * 2) Get <b>layout_width</b>
     */
    override fun getWidth(): Int {
        val view = viewRef.get()
        view?.let {
            val params = it.layoutParams
            var width = 0
            if (checkActualViewSize && params != null && params.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                width = it.width
            }
            if (width <= 0 && params != null) width = params.width
            return width
        }
        return 0
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Height is defined by target {@link android.view.View view} parameters, configuration
     * parameters or device display dimensions.<br />
     * Size computing algorithm (go by steps until get non-zero value):<br />
     * 1) Get the actual drawn <b>getHeight()</b> of the View<br />
     * 2) Get <b>layout_height</b>
     */
    override fun getHeight(): Int {
        val view = viewRef.get()
        view?.let {
            val params = it.layoutParams
            var height = 0
            if (checkActualViewSize && params != null && params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                height = it.height
            }
            if (height <= 0 && params != null) height = params.height
            return height
        }
        return 0
    }

    override fun getScaleType(): ViewScaleType = ViewScaleType.CROP

    override fun getWrappedView(): View? = viewRef.get()

    override fun isCollected(): Boolean = (viewRef.get() == null)

    override fun getId(): Int {
        val view = viewRef.get()
        return view?.hashCode() ?: super.hashCode()
    }

    override fun setImageDrawable(drawable: Drawable?): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            val view = viewRef.get()
            view?.let {
                setImageDrawableInto(drawable, view)
                return true
            }
        } else {
            L.w(WARN_CANT_SET_DRAWABLE)
        }
        return false
    }

    override fun setImageBitmap(bitmap: Bitmap): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            var view = viewRef.get()
            view?.let {
                setImageBitmapInto(bitmap, view)
                return true
            }
        } else {
            L.w(WARN_CANT_SET_BITMAP)
        }
        return false
    }

    /**
     * Should set drawable into incoming view. Incoming view is guaranteed not null.<br />
     * This method is called on UI thread.
     */
    abstract fun setImageDrawableInto(drawable: Drawable?, view: View?)

    /**
     * Should set Bitmap into incoming view. Incoming view is guaranteed not null.< br />
     * This method is called on UI thread.
     */
    abstract fun setImageBitmapInto(bitmap: Bitmap, view: View?)

}