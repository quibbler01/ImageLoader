package cn.quibbler.imageloader.utils

import android.opengl.GLES10
import cn.quibbler.imageloader.core.assist.ImageSize
import cn.quibbler.imageloader.core.assist.ViewScaleType
import cn.quibbler.imageloader.core.imageaware.ImageAware
import javax.microedition.khronos.opengles.GL10
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private const val DEFAULT_MAX_BITMAP_DIMENSION = 2048

private val maxBitmapSize: ImageSize
    get() {
        val maxTextureSize = IntArray(1)
        GLES10.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0)
        val maxBitmapDimension = max(maxTextureSize[0], DEFAULT_MAX_BITMAP_DIMENSION)
        return ImageSize(maxBitmapDimension, maxBitmapDimension)
    }

fun defineTargetSizeForView(imageAware: ImageAware, maxImageSize: ImageSize): ImageSize {
    var width = imageAware.getWidth()
    if (width <= 0) {
        width = maxImageSize.width
    } else {
        width = min(width, maxImageSize.width)
    }

    var height = imageAware.getHeight()
    if (height <= 0) {
        height = maxImageSize.height
    } else {
        height = min(height, maxImageSize.height)
    }

    return ImageSize(width, height)
}

fun computeImageSampleSize(srcSize: ImageSize, targetSize: ImageSize, viewScaleType: ViewScaleType, powerOf2Scale: Boolean): Int {
    val srcWidth = srcSize.width
    val srcHeight = srcSize.height
    val targetWidth = targetSize.width
    val targetHeight = targetSize.height

    var scale = 1

    when (viewScaleType) {
        ViewScaleType.FIT_INSIDE -> {
            if (powerOf2Scale) {
                val halfWidth = srcWidth / 2
                val halfHeight = srcHeight / 2
                while ((halfWidth / scale) > targetWidth || (halfHeight / scale) > targetHeight) {
                    scale *= 2
                }
            } else {
                scale = max(srcWidth / targetWidth, srcHeight / targetHeight)
            }
        }
        ViewScaleType.CROP -> {
            if (powerOf2Scale) {
                val halfWidth = srcWidth / 2
                val halfHeight = srcHeight / 2
                while ((halfWidth / scale) > targetWidth && (halfHeight / scale) > targetHeight) {
                    scale *= 2
                }
            } else {
                scale = min(srcWidth / targetWidth, srcHeight / targetHeight)
            }
        }
    }

    if (scale < 1) {
        scale = 1
    }

    scale = considerMaxTextureSize(srcWidth, srcHeight, scale, powerOf2Scale)
    return scale
}

fun considerMaxTextureSize(srcWidth: Int, srcHeight: Int, scale: Int, powerOf2: Boolean): Int {
    var scale_ = scale
    val maxWidth = maxBitmapSize.width
    val maxHeight = maxBitmapSize.height
    while ((srcWidth / scale) > maxWidth || (srcHeight / scale) > maxHeight) {
        if (powerOf2) {
            scale_ *= 2
        } else {
            scale_++
        }
    }
    return scale_
}

fun computeMinImageSampleSize(srcSize: ImageSize): Int {
    val srcWidth = srcSize.width
    val srcHeight = srcSize.height

    val targetWidth = maxBitmapSize.width
    val targetHeight = maxBitmapSize.height

    val widthScale = ceil(srcWidth.toFloat() / targetWidth).toInt()
    val heightScale = ceil(srcHeight.toFloat() / targetHeight).toInt()

    return max(widthScale, heightScale)
}

fun computeImageScale(srcSize: ImageSize, targetSize: ImageSize, viewScaleType: ViewScaleType, stretch: Boolean): Float {
    val srcWidth = srcSize.width
    val srcHeight = srcSize.height
    val targetWidth = targetSize.width
    val targetHeight = targetSize.height

    val widthScale = srcWidth.toFloat() / targetWidth
    val heightScale = srcHeight.toFloat() / targetHeight

    var destWidth = 0
    var destHeight = 0

    if ((viewScaleType == ViewScaleType.FIT_INSIDE && widthScale >= heightScale) || (viewScaleType == ViewScaleType.CROP && widthScale < heightScale)) {
        destWidth = targetWidth
        destHeight = (srcHeight / widthScale).toInt()
    } else {
        destWidth = (srcWidth / heightScale).toInt()
        destHeight = targetHeight
    }

    var scale = 1f
    if ((!stretch && destWidth < srcWidth && destHeight < srcHeight) || (stretch && destWidth != srcWidth && destHeight != srcHeight)) {
        scale = (destWidth / srcWidth).toFloat()
    }

    return scale
}





