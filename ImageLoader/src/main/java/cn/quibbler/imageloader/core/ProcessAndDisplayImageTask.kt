package cn.quibbler.imageloader.core

import android.graphics.Bitmap
import android.os.Handler
import cn.quibbler.imageloader.core.assist.LoadedFrom
import cn.quibbler.imageloader.core.process.BitmapProcessor
import cn.quibbler.imageloader.utils.L

/**
 * Presents process'n'display image task. Processes image {@linkplain Bitmap} and display it in {@link ImageView} using
 * {@link DisplayBitmapTask}.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.8.0
 */
class ProcessAndDisplayImageTask(
    private val engine: ImageLoaderEngine,
    private val bitmap: Bitmap,
    private val imageLoadingInfo: ImageLoadingInfo,
    private val handler: Handler
) : Runnable {

    companion object {
        private const val LOG_POSTPROCESS_IMAGE = "PostProcess image before displaying [%s]"
    }

    override fun run() {
        L.d(LOG_POSTPROCESS_IMAGE, imageLoadingInfo.memoryCacheKey)

        val processor: BitmapProcessor? = imageLoadingInfo.options.postProcessor
        val processedBitmap: Bitmap? = processor?.process(bitmap)
        val displayBitmapTask = DisplayBitmapTask(processedBitmap, imageLoadingInfo, engine, LoadedFrom.MEMORY_CACHE)
        LoadAndDisplayImageTask.runTask(displayBitmapTask, imageLoadingInfo.options.isSyncLoading, handler, engine)
    }

}