package cn.quibbler.imageloader.core.decode

import android.graphics.BitmapFactory.Options
import android.os.Build
import cn.quibbler.imageloader.core.DisplayImageOptions
import cn.quibbler.imageloader.core.assist.ImageScaleType
import cn.quibbler.imageloader.core.assist.ImageSize
import cn.quibbler.imageloader.core.assist.ViewScaleType
import cn.quibbler.imageloader.core.download.ImageDownloader

/**
 * Contains needed information for decoding image to Bitmap
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.8.3
 */
class ImageDecodingInfo {

    private val imageKey: String?
    private val imageUri: String?
    private val originalImageUri: String?
    private val targetSize: ImageSize?

    private var imageScaleType: ImageScaleType?
    private val viewScaleType: ViewScaleType?

    private val downloader: ImageDownloader?
    private var extraForDownloader: Any?

    private var considerExifParams: Boolean
    private var decodingOptions: Options

    constructor(
        imageKey: String?, imageUri: String?, originalImageUri: String?, targetSize: ImageSize?, viewScaleType: ViewScaleType?,
        downloader: ImageDownloader?, displayOptions: DisplayImageOptions
    ) {
        this.imageKey = imageKey
        this.imageUri = imageUri
        this.originalImageUri = originalImageUri
        this.targetSize = targetSize
        imageScaleType = displayOptions.imageScaleType
        this.viewScaleType = viewScaleType
        this.downloader = downloader
        extraForDownloader = displayOptions.extraForDownloader
        considerExifParams = displayOptions.considerExifParams
        decodingOptions = Options()
        copyOptions(displayOptions.decodingOptions, decodingOptions)
    }

    private fun copyOptions(srcOptions: Options, destOptions: Options) {
        destOptions.inDensity = srcOptions.inDensity
        destOptions.inDither = srcOptions.inDither
        destOptions.inInputShareable = srcOptions.inInputShareable
        destOptions.inJustDecodeBounds = srcOptions.inJustDecodeBounds
        destOptions.inPreferredConfig = srcOptions.inPreferredConfig
        destOptions.inPurgeable = srcOptions.inPurgeable
        destOptions.inScaled = srcOptions.inScaled
        destOptions.inScreenDensity = srcOptions.inScreenDensity
        destOptions.inTargetDensity = srcOptions.inTargetDensity
        destOptions.inTempStorage = srcOptions.inTempStorage
        if (Build.VERSION.SDK_INT >= 10) copyOptions10(srcOptions, destOptions)
        if (Build.VERSION.SDK_INT >= 11) copyOptions11(srcOptions, destOptions)
    }

    private fun copyOptions10(srcOptions: Options, destOptions: Options) {
        destOptions.inPreferQualityOverSpeed = srcOptions.inPreferQualityOverSpeed
    }

    private fun copyOptions11(srcOptions: Options, destOptions: Options) {
        destOptions.inBitmap = srcOptions.inBitmap
        destOptions.inMutable = srcOptions.inMutable
    }

}