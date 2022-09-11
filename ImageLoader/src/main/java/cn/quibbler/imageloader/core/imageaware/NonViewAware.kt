package cn.quibbler.imageloader.core.imageaware

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.View
import cn.quibbler.imageloader.core.assist.ImageSize
import cn.quibbler.imageloader.core.assist.ViewScaleType

class NonViewAware(protected val imageUri: String?, protected val imageSize: ImageSize, private val scaleType: ViewScaleType) : ImageAware {

    constructor(imageSize: ImageSize, scaleType_: ViewScaleType) : this(null, imageSize, scaleType_)

    override fun getWidth(): Int = imageSize.width

    override fun getHeight(): Int = imageSize.height

    override fun getScaleType(): ViewScaleType = scaleType

    override fun getWrappedView(): View? = null

    override fun isCollected(): Boolean = false

    override fun getId(): Int = if (TextUtils.isEmpty(imageUri)) super.hashCode() else imageUri.hashCode()

    override fun setImageDrawable(drawable: Drawable): Boolean = true

    override fun setImageBitmap(bitmap: Bitmap): Boolean = true

}