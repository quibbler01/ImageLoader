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

/**
 * Defines target size for image aware view. Size is defined by target
 * {@link com.nostra13.universalimageloader.core.imageaware.ImageAware view} parameters, configuration
 * parameters or device display dimensions.<br />
 */
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

/**
 * Computes sample size for downscaling image size (<b>srcSize</b>) to view size (<b>targetSize</b>). This sample
 * size is used during
 * {@linkplain BitmapFactory#decodeStream(java.io.InputStream, android.graphics.Rect, android.graphics.BitmapFactory.Options)
 * decoding image} to bitmap.<br />
 * <br />
 * <b>Examples:</b><br />
 * <p/>
 * <pre>
 * srcSize(100x100), targetSize(10x10), powerOf2Scale = true -> sampleSize = 8
 * srcSize(100x100), targetSize(10x10), powerOf2Scale = false -> sampleSize = 10
 *
 * srcSize(100x100), targetSize(20x40), viewScaleType = FIT_INSIDE -> sampleSize = 5
 * srcSize(100x100), targetSize(20x40), viewScaleType = CROP       -> sampleSize = 2
 * </pre>
 * <p/>
 * <br />
 * The sample size is the number of pixels in either dimension that correspond to a single pixel in the decoded
 * bitmap. For example, inSampleSize == 4 returns an image that is 1/4 the width/height of the original, and 1/16
 * the number of pixels. Any value <= 1 is treated the same as 1.
 *
 * @param srcSize       Original (image) size
 * @param targetSize    Target (view) size
 * @param viewScaleType {@linkplain ViewScaleType Scale type} for placing image in view
 * @param powerOf2Scale <i>true</i> - if sample size be a power of 2 (1, 2, 4, 8, ...)
 * @return Computed sample size
 */
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
                while ((halfWidth / scale) > targetWidth || (halfHeight / scale) > targetHeight) {// ||
                    scale *= 2
                }
            } else {
                scale = max(srcWidth / targetWidth, srcHeight / targetHeight) // max
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

/**
 * Computes minimal sample size for downscaling image so result image size won't exceed max acceptable OpenGL
 * texture size.<br />
 * We can't create Bitmap in memory with size exceed max texture size (usually this is 2048x2048) so this method
 * calculate minimal sample size which should be applied to image to fit into these limits.
 *
 * @param srcSize Original image size
 * @return Minimal sample size
 */
fun computeMinImageSampleSize(srcSize: ImageSize): Int {
    val srcWidth = srcSize.width
    val srcHeight = srcSize.height

    val targetWidth = maxBitmapSize.width
    val targetHeight = maxBitmapSize.height

    val widthScale = ceil(srcWidth.toFloat() / targetWidth).toInt()
    val heightScale = ceil(srcHeight.toFloat() / targetHeight).toInt()

    return max(widthScale, heightScale) // max
}

/**
 * Computes scale of target size (<b>targetSize</b>) to source size (<b>srcSize</b>).<br />
 * <br />
 * <b>Examples:</b><br />
 * <p/>
 * <pre>
 * srcSize(40x40), targetSize(10x10) -> scale = 0.25
 *
 * srcSize(10x10), targetSize(20x20), stretch = false -> scale = 1
 * srcSize(10x10), targetSize(20x20), stretch = true  -> scale = 2
 *
 * srcSize(100x100), targetSize(20x40), viewScaleType = FIT_INSIDE -> scale = 0.2
 * srcSize(100x100), targetSize(20x40), viewScaleType = CROP       -> scale = 0.4
 * </pre>
 *
 * @param srcSize       Source (image) size
 * @param targetSize    Target (view) size
 * @param viewScaleType {@linkplain ViewScaleType Scale type} for placing image in view
 * @param stretch       Whether source size should be stretched if target size is larger than source size. If <b>false</b>
 *                      then result scale value can't be greater than 1.
 * @return Computed scale
 */
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





