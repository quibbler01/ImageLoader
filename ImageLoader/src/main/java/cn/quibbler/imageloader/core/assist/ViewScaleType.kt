package cn.quibbler.imageloader.core.assist

import android.view.View
import android.widget.ImageView

enum class ViewScaleType {

    FIT_INSIDE,

    CROP;

    fun fromImageView(imageView: ImageView): ViewScaleType {
        return when (imageView.scaleType) {
            ImageView.ScaleType.FIT_CENTER,
            ImageView.ScaleType.FIT_XY,
            ImageView.ScaleType.FIT_START,
            ImageView.ScaleType.FIT_END,
            ImageView.ScaleType.CENTER_INSIDE,
            -> {
                FIT_INSIDE
            }
            ImageView.ScaleType.MATRIX,
            ImageView.ScaleType.CENTER,
            ImageView.ScaleType.CENTER_CROP,
            -> {
                CROP
            }
            else -> {
                CROP
            }
        }
    }

}