package cn.quibbler.imageloader.core.decode

import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.os.Build
import cn.quibbler.imageloader.core.assist.ImageScaleType
import cn.quibbler.imageloader.core.assist.ImageSize
import cn.quibbler.imageloader.core.assist.ViewScaleType
import cn.quibbler.imageloader.core.download.ImageDownloader

class ImageDecodingInfo(
    val imageKey: String, val imageUrl: String, val originalImageUri: String,
    val targetSize: ImageSize, val imageScaleType: ImageScaleType, val viewScaleType: ViewScaleType,
    val downloader: ImageDownloader, val extraForDownloader: Any?, val considerExifParams: Boolean, val decodingOptions: Options
) {

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