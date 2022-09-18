package cn.quibbler.imageloader.core.decode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import cn.quibbler.imageloader.core.assist.ImageScaleType
import cn.quibbler.imageloader.core.assist.ImageSize
import cn.quibbler.imageloader.core.download.ImageDownloader
import cn.quibbler.imageloader.utils.*
import java.io.IOException
import java.io.InputStream

/**
 * Decodes images to {@link Bitmap}, scales them to needed size
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageDecodingInfo
 * @since 1.8.3
 */
open class BaseImageDecoder(protected val loggingEnabled: Boolean) : ImageDecoder {

    companion object {
        const val LOG_SUBSAMPLE_IMAGE = "Subsample original image (%1\$s) to %2\$s (scale = %3\$d) [%4\$s]"

        const val LOG_SCALE_IMAGE = "Scale subsampled image (%1\$s) to %2\$s (scale = %3$.5f) [%4\$s]"

        const val LOG_ROTATE_IMAGE = "Rotate image on %1\$d\u00B0 [%2\$s]"

        const val LOG_FLIP_IMAGE = "Flip image horizontally [%s]"

        const val ERROR_NO_IMAGE_STREAM = "No stream for image [%s]"

        const val ERROR_CANT_DECODE_IMAGE = "Image can't be decoded [%s]"
    }

    /**
     * Decodes image from URI into {@link Bitmap}. Image is scaled close to incoming {@linkplain ImageSize target size}
     * during decoding (depend on incoming parameters).
     *
     * @param decodingInfo Needed data for decoding image
     * @return Decoded bitmap
     * @throws IOException                   if some I/O exception occurs during image reading
     * @throws UnsupportedOperationException if image URI has unsupported scheme(protocol)
     */
    override fun decode(decodingInfo: ImageDecodingInfo): Bitmap? {
        var decodedBitmap: Bitmap? = null
        var imageInfo: ImageFileInfo? = null

        var imageStream = getImageStream(decodingInfo)
        if (imageStream == null) {
            L.e(ERROR_NO_IMAGE_STREAM, decodingInfo.imageKey)
            return null
        }

        try {
            imageInfo = defineImageSizeAndRotation(imageStream, decodingInfo)
            imageStream = resetStream(imageStream, decodingInfo)
            val decodingOptions = prepareDecodingOptions(imageInfo.imageSize, decodingInfo)
            decodedBitmap = BitmapFactory.decodeStream(imageStream, null, decodingOptions)
        } finally {
            closeSilently(imageStream)
        }

        if (decodedBitmap == null) {
            L.e(ERROR_CANT_DECODE_IMAGE, decodingInfo.imageKey)
        } else {
            decodedBitmap = considerExactScaleAndOrientatiton(decodedBitmap, decodingInfo, imageInfo!!.exif.rotation, imageInfo.exif.flipHorizontal)
        }
        return decodedBitmap
    }

    @Throws(IOException::class)
    protected fun getImageStream(decodingInfo: ImageDecodingInfo): InputStream? {
        return decodingInfo.downloader.getStream(decodingInfo.imageUri, decodingInfo.extraForDownloader)
    }

    @Throws(IOException::class)
    protected fun defineImageSizeAndRotation(imageStream: InputStream, decodingInfo: ImageDecodingInfo): ImageFileInfo {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(imageStream, null, options)

        val imageUri = decodingInfo.imageUri
        val exif: ExifInfo = if (decodingInfo.considerExifParams && canDefineExifParams(imageUri, options.outMimeType)) {
            defineExifOrientation(imageUri)
        } else {
            ExifInfo()
        }

        return ImageFileInfo(ImageSize(options.outWidth, options.outHeight, exif.rotation), exif)
    }

    private fun canDefineExifParams(imageUri: String, mimeType: String): Boolean {
        return "image/jpeg".equals(mimeType, true) && (ImageDownloader.Scheme.ofUri(imageUri) == ImageDownloader.Scheme.FILE)
    }

    protected fun defineExifOrientation(imageUri: String): ExifInfo {
        var rotation = 0
        var flip = false
        try {
            val exif = ExifInterface(ImageDownloader.Scheme.FILE.crop(imageUri))
            val exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            when (exifOrientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                    flip = true
                }
                ExifInterface.ORIENTATION_NORMAL -> {
                    rotation = 0
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    flip = true
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    rotation = 90
                }
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    flip = true
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    rotation = 180
                }
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    flip = true
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    rotation = 270
                }
            }
        } catch (e: IOException) {
            L.w("Can't read EXIF tags from file [%s]", imageUri)
        }
        return ExifInfo(rotation, flip)
    }

    protected fun prepareDecodingOptions(imageSize: ImageSize, decodingInfo: ImageDecodingInfo): BitmapFactory.Options {
        val scaleType = decodingInfo.imageScaleType
        var scale = 0
        if (scaleType == ImageScaleType.NONE) {
            scale = 1
        } else if (scaleType == ImageScaleType.NONE_SAFE) {
            scale = computeMinImageSampleSize(imageSize)
        } else {
            val targetSize = decodingInfo.targetSize
            val powerOf2 = scaleType == ImageScaleType.IN_SAMPLE_POWER_OF_2
            scale = computeImageSampleSize(imageSize, targetSize, decodingInfo.viewScaleType, powerOf2)
        }
        if (scale > 1 && loggingEnabled) {
            L.d(LOG_SUBSAMPLE_IMAGE, imageSize, imageSize.scaleDown(scale), scaleType, decodingInfo.imageKey)
        }

        val decodingOptions = decodingInfo.decodingOptions
        decodingOptions.inSampleSize = scale
        return decodingOptions
    }

    @Throws(IOException::class)
    protected fun resetStream(imageStrem: InputStream, decodingInfo: ImageDecodingInfo): InputStream? {
        if (imageStrem.markSupported()) {
            try {
                imageStrem.reset()
                return imageStrem
            } catch (ignored: IOException) {
            }
        }
        closeSilently(imageStrem)
        return getImageStream(decodingInfo)
    }

    private fun considerExactScaleAndOrientatiton(
        subsampledBitmap: Bitmap,
        decodingInfo: ImageDecodingInfo,
        rotation: Int,
        flipHorizontal: Boolean
    ): Bitmap {
        val m = Matrix()
        // Scale to exact size if need
        val scaleType = decodingInfo.imageScaleType
        if (scaleType == ImageScaleType.EXACTLY || scaleType == ImageScaleType.EXACTLY_STRETCHED) {
            val srcSize: ImageSize = ImageSize(subsampledBitmap.width, subsampledBitmap.height, rotation)
            val scale = computeImageScale(srcSize, decodingInfo.targetSize, decodingInfo.viewScaleType, scaleType == ImageScaleType.EXACTLY_STRETCHED)
            if (compareValues(scale, 1f) != 0) {
                m.setScale(scale, scale)

                if (loggingEnabled) {
                    L.d(LOG_SCALE_IMAGE, srcSize, srcSize.scale(scale), scaleType, decodingInfo.imageKey)
                }
            }
        }

        // Flip bitmap if need
        if (flipHorizontal) {
            m.postScale(-1f, -1f)

            if (loggingEnabled) L.d(LOG_FLIP_IMAGE, decodingInfo.imageKey)
        }
        // Rotate bitmap if need
        if (rotation != 0) {
            m.postRotate(rotation.toFloat())

            if (loggingEnabled) L.d(LOG_ROTATE_IMAGE, rotation, decodingInfo.imageKey)
        }

        val finalBitmap = Bitmap.createBitmap(subsampledBitmap, 0, 0, subsampledBitmap.width, subsampledBitmap.height, m, true)
        if (finalBitmap != subsampledBitmap) {
            subsampledBitmap.recycle()
        }
        return finalBitmap
    }

    protected class ExifInfo(val rotation: Int = 0, val flipHorizontal: Boolean = false)

    protected class ImageFileInfo(val imageSize: ImageSize, val exif: ExifInfo)

}