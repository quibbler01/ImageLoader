package cn.quibbler.imageloader.core

import android.graphics.Bitmap
import cn.quibbler.imageloader.core.assist.LoadedFrom
import cn.quibbler.imageloader.core.display.BitmapDisplayer
import cn.quibbler.imageloader.core.imageaware.ImageAware
import cn.quibbler.imageloader.core.listener.ImageLoadingListener
import cn.quibbler.imageloader.utils.L

/**
 * Displays bitmap in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware}. Must be called on UI thread.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageLoadingListener
 * @see BitmapDisplayer
 * @since 1.3.1
 */
class DisplayBitmapTask : Runnable {

    companion object {
        private const val LOG_DISPLAY_IMAGE_IN_IMAGEAWARE = "Display image in ImageAware (loaded from %1\$s) [%2\$s]"
        private const val LOG_TASK_CANCELLED_IMAGEAWARE_REUSED = "ImageAware is reused for another image. Task is cancelled. [%s]"
        private const val LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED = "ImageAware was collected by GC. Task is cancelled. [%s]"
    }

    private val bitmap: Bitmap?
    private val imageUri: String
    private val imageAware: ImageAware
    private val memoryCacheKey: String
    private val displayer: BitmapDisplayer
    private val listener: ImageLoadingListener
    private val engine: ImageLoaderEngine
    private val loadedFrom: LoadedFrom

    constructor(
        bitmap: Bitmap?, imageLoadingInfo: ImageLoadingInfo, engine: ImageLoaderEngine,
        loadedFrom: LoadedFrom
    ) {
        this.bitmap = bitmap
        imageUri = imageLoadingInfo.uri
        imageAware = imageLoadingInfo.imageAware
        memoryCacheKey = imageLoadingInfo.memoryCacheKey
        displayer = imageLoadingInfo.options.displayer
        listener = imageLoadingInfo.listener
        this.engine = engine
        this.loadedFrom = loadedFrom
    }

    override fun run() {
        if (imageAware.isCollected()) {
            L.d(LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED, memoryCacheKey)
            listener.onLoadingCancelled(imageUri, imageAware.getWrappedView())
        } else if (isViewWasReused()) {
            L.d(LOG_TASK_CANCELLED_IMAGEAWARE_REUSED, memoryCacheKey)
            listener.onLoadingCancelled(imageUri, imageAware.getWrappedView())
        } else {
            L.d(LOG_DISPLAY_IMAGE_IN_IMAGEAWARE, loadedFrom, memoryCacheKey)
            if (bitmap != null) {
                displayer.display(bitmap, imageAware, loadedFrom)
            }
            engine.cancelDisplayTaskFor(imageAware)
            listener.onLoadingComplete(imageUri, imageAware.getWrappedView(), bitmap)
        }
    }

    /** Checks whether memory cache key (image URI) for current ImageAware is actual */
    private fun isViewWasReused(): Boolean {
        val currentCacheKey = engine.getLoadingUriForView(imageAware)
        return memoryCacheKey != currentCacheKey
    }

}