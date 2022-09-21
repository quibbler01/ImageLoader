package cn.quibbler.imageloader.core

import android.graphics.Bitmap
import android.os.Handler
import cn.quibbler.imageloader.core.assist.*
import cn.quibbler.imageloader.core.decode.ImageDecoder
import cn.quibbler.imageloader.core.decode.ImageDecodingInfo
import cn.quibbler.imageloader.core.download.ImageDownloader
import cn.quibbler.imageloader.core.imageaware.ImageAware
import cn.quibbler.imageloader.core.listener.ImageLoadingListener
import cn.quibbler.imageloader.core.listener.ImageLoadingProgressListener
import cn.quibbler.imageloader.utils.CopyListener
import cn.quibbler.imageloader.utils.L
import cn.quibbler.imageloader.utils.closeSilently
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Presents load'n'display image task. Used to load image from Internet or file system, decode it to {@link Bitmap}, and
 * display it in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware} using {@link DisplayBitmapTask}.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageLoaderConfiguration
 * @see ImageLoadingInfo
 * @since 1.3.1
 */
class LoadAndDisplayImageTask : Runnable, CopyListener {

    companion object {
        private const val LOG_WAITING_FOR_RESUME = "ImageLoader is paused. Waiting...  [%s]"
        private const val LOG_RESUME_AFTER_PAUSE = ".. Resume loading [%s]"
        private const val LOG_DELAY_BEFORE_LOADING = "Delay %d ms before loading...  [%s]"
        private const val LOG_START_DISPLAY_IMAGE_TASK = "Start display image task [%s]"
        private const val LOG_WAITING_FOR_IMAGE_LOADED = "Image already is loading. Waiting... [%s]"
        private const val LOG_GET_IMAGE_FROM_MEMORY_CACHE_AFTER_WAITING = "...Get cached bitmap from memory after waiting. [%s]"
        private const val LOG_LOAD_IMAGE_FROM_NETWORK = "Load image from network [%s]"
        private const val LOG_LOAD_IMAGE_FROM_DISK_CACHE = "Load image from disk cache [%s]"
        private const val LOG_RESIZE_CACHED_IMAGE_FILE = "Resize image in disk cache [%s]"
        private const val LOG_PREPROCESS_IMAGE = "PreProcess image before caching in memory [%s]"
        private const val LOG_POSTPROCESS_IMAGE = "PostProcess image before displaying [%s]"
        private const val LOG_CACHE_IMAGE_IN_MEMORY = "Cache image in memory [%s]"
        private const val LOG_CACHE_IMAGE_ON_DISK = "Cache image on disk [%s]"
        private const val LOG_PROCESS_IMAGE_BEFORE_CACHE_ON_DISK = "Process image before cache on disk [%s]"
        private const val LOG_TASK_CANCELLED_IMAGEAWARE_REUSED = "ImageAware is reused for another image. Task is cancelled. [%s]"
        private const val LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED = "ImageAware was collected by GC. Task is cancelled. [%s]"
        private const val LOG_TASK_INTERRUPTED = "Task was interrupted [%s]"

        private const val ERROR_NO_IMAGE_STREAM = "No stream for image [%s]"
        private const val ERROR_PRE_PROCESSOR_NULL = "Pre-processor returned null [%s]"
        private const val ERROR_POST_PROCESSOR_NULL = "Post-processor returned null [%s]"
        private const val ERROR_PROCESSOR_FOR_DISK_CACHE_NULL = "Bitmap processor for disk cache returned null [%s]"

        fun runTask(r: Runnable, sync: Boolean, handler: Handler?, engine: ImageLoaderEngine) {
            if (sync) {
                r.run()
            } else if (handler == null) {
                engine.fireCallback(r)
            } else {
                handler.post(r)
            }
        }

    }

    private val engine: ImageLoaderEngine
    private val imageLoadingInfo: ImageLoadingInfo
    private val handler: Handler?

    // Helper references
    private val configuration: ImageLoaderConfiguration
    private val downloader: ImageDownloader?
    private var networkDeniedDownloader: ImageDownloader? = null
    private var slowNetworkDownloader: ImageDownloader? = null
    private var decoder: ImageDecoder
    var uri: String
    private var memoryCacheKey: String
    var imageAware: ImageAware
    private var targetSize: ImageSize
    var options: DisplayImageOptions
    var listener: ImageLoadingListener
    var progressListener: ImageLoadingProgressListener? = null
    private var syncLoading: Boolean

    // State vars
    private var loadedFrom: LoadedFrom = LoadedFrom.NETWORK

    constructor(engine: ImageLoaderEngine, imageLoadingInfo: ImageLoadingInfo, handler: Handler?) {
        this.engine = engine
        this.imageLoadingInfo = imageLoadingInfo
        this.handler = handler

        configuration = engine.configuration
        downloader = configuration.downloader
        networkDeniedDownloader = configuration.networkDeniedDownloader
        slowNetworkDownloader = configuration.slowNetworkDownloader
        decoder = configuration.decoder!!
        uri = imageLoadingInfo.uri
        memoryCacheKey = imageLoadingInfo.memoryCacheKey
        imageAware = imageLoadingInfo.imageAware
        targetSize = imageLoadingInfo.targetSize
        options = imageLoadingInfo.options
        listener = imageLoadingInfo.listener
        progressListener = imageLoadingInfo.progressListener
        syncLoading = options.isSyncLoading
    }

    override fun run() {

    }

    /** @return <b>true</b> - if task should be interrupted; <b>false</b> - otherwise */
    private fun waitIfPaused(): Boolean {
        val pause = engine.paused
        if (pause.get()) {
            synchronized(engine.pauseLock) {
                if (pause.get()) {
                    L.d(LOG_WAITING_FOR_RESUME, memoryCacheKey)
                    try {
                        engine.pauseLock.wait()
                    } catch (e: InterruptedException) {
                        L.e(LOG_TASK_INTERRUPTED, memoryCacheKey)
                        return true
                    }
                    L.d(LOG_RESUME_AFTER_PAUSE, memoryCacheKey);
                }
            }
        }
        return isTaskNotActual()
    }

    /** @return <b>true</b> - if task should be interrupted; <b>false</b> - otherwise */
    private fun delayIfNeed(): Boolean {
        if (options.shouldDelayBeforeLoading()) {
            L.d(LOG_DELAY_BEFORE_LOADING, options.delayBeforeLoading, memoryCacheKey)
            try {
                Thread.sleep(options.delayBeforeLoading.toLong())
            } catch (e: InterruptedException) {
                L.e(LOG_TASK_INTERRUPTED, memoryCacheKey)
                return true
            }
            return isTaskNotActual()
        }
        return false
    }

    @Throws(TaskCancelledException::class)
    private fun tryLoadBitmap(): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            var imageFile: File? = configuration.diskCache?.get(uri)
            if (imageFile != null && imageFile.exists() && imageFile.length() > 0) {
                L.d(LOG_LOAD_IMAGE_FROM_DISK_CACHE, memoryCacheKey)
                loadedFrom = LoadedFrom.DISC_CACHE

                checkTaskNotActual()
                bitmap = decodeImage(ImageDownloader.Scheme.FILE.wrap(imageFile.absolutePath))
            }
            if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
                L.d(LOG_LOAD_IMAGE_FROM_NETWORK, memoryCacheKey)
                loadedFrom = LoadedFrom.NETWORK

                var imageUriForDecoding = uri
                if (options.cacheOnDisk && tryCacheImageOnDisk()) {
                    imageFile = configuration.diskCache?.get(uri)
                    if (imageFile != null) {
                        imageUriForDecoding = ImageDownloader.Scheme.FILE.wrap(imageFile.absolutePath)
                    }
                }

                checkTaskNotActual()
                bitmap = decodeImage(imageUriForDecoding)

                if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
                    fireFailEvent(FailReason.FailType.DECODING_ERROR, null)
                }
            }
        } catch (e: IllegalStateException) {
            fireFailEvent(FailReason.FailType.NETWORK_DENIED, null)
        } catch (e: TaskCancelledException) {
            throw e
        } catch (e: IOException) {
            L.e(e)
            fireFailEvent(FailReason.FailType.IO_ERROR, e)
        } catch (e: OutOfMemoryError) {
            L.e(e)
            fireFailEvent(FailReason.FailType.OUT_OF_MEMORY, e)
        } catch (e: Throwable) {
            L.e(e)
            fireFailEvent(FailReason.FailType.UNKNOWN, e)
        }
        return bitmap
    }

    private fun decodeImage(imageUri: String): Bitmap? {
        val viewScaleType = imageAware.getScaleType()
        val decodingInfo = ImageDecodingInfo(memoryCacheKey, imageUri, uri, targetSize, viewScaleType, getDownloader(), options)
        return decoder.decode(decodingInfo)
    }

    @Throws(TaskCancelledException::class)
    private fun tryCacheImageOnDisk(): Boolean {
        L.d(LOG_CACHE_IMAGE_ON_DISK, memoryCacheKey)

        var loaded = false
        try {
            loaded = downloadImage()
            if (loaded) {
                val width = configuration.maxImageWidthForDiskCache
                val height = configuration.maxImageHeightForDiskCache
                if (width > 0 || height > 0) {
                    L.d(LOG_RESIZE_CACHED_IMAGE_FILE, memoryCacheKey)
                    resizeAndSaveImage(width, height) // TODO : process boolean result
                }
            }
        } catch (e: IOException) {
            L.e(e)
            loaded = false
        }
        return loaded
    }

    @Throws(IOException::class)
    private fun downloadImage(): Boolean {
        val input: InputStream? = getDownloader()?.getStream(uri, options.extraForDownloader)
        if (input == null) {
            L.e(ERROR_NO_IMAGE_STREAM, memoryCacheKey)
            return false
        } else {
            try {
                return configuration.diskCache?.save(uri, input, this) ?: false
            } finally {
                closeSilently(input)
            }
        }
    }

    /** Decodes image file into Bitmap, resize it and save it back */
    @Throws(IOException::class)
    private fun resizeAndSaveImage(maxWidth: Int, maxHeight: Int): Boolean {
        // Decode image file, compress and re-save it
        var saved = false

        val targetFile: File? = configuration.diskCache?.get(uri)
        if (targetFile != null && targetFile.exists()) {
            val targetImageSize = ImageSize(maxWidth, maxHeight)
            val specialOptions = DisplayImageOptions.Builder().cloneFrom(options).imageScaleType(ImageScaleType.IN_SAMPLE_INT).build()
            val decodingInfo = ImageDecodingInfo(
                memoryCacheKey,
                ImageDownloader.Scheme.FILE.wrap(targetFile.absolutePath),
                uri,
                targetImageSize,
                ViewScaleType.FIT_INSIDE,
                getDownloader(),
                specialOptions
            )
            var bmp: Bitmap? = decoder.decode(decodingInfo)
            if (bmp != null && configuration.processorForDiskCache != null) {
                L.d(LOG_PROCESS_IMAGE_BEFORE_CACHE_ON_DISK, memoryCacheKey)
                bmp = configuration.processorForDiskCache.process(bmp)
                if (bmp == null) {
                    L.e(ERROR_PROCESSOR_FOR_DISK_CACHE_NULL, memoryCacheKey)
                }
            }
            if (bmp != null) {
                saved = configuration.diskCache?.save(uri, bmp) ?: false
                bmp.recycle()
            }
        }

        return saved
    }

    override fun onBytesCopied(current: Int, total: Int): Boolean {
        return syncLoading || fireProgressEvent(current, total)
    }

    private fun fireProgressEvent(current: Int, total: Int): Boolean {
        if (isTaskInterrupted() || isTaskNotActual()) return false;
        if (progressListener != null) {
            val r = Runnable {
                progressListener!!.onProgressUpdate(uri, imageAware.getWrappedView()!!, current, total)
            }
            runTask(r, false, handler, engine)
        }
        return true
    }

    private fun fireFailEvent(failType: FailReason.FailType, failCause: Throwable?) {
        if (syncLoading || isTaskInterrupted() || isTaskNotActual()) return
        val r = Runnable {
            if (options.shouldShowImageOnFail()) {
                imageAware.setImageDrawable(options.getImageOnFail(configuration.resources))
            }
            listener.onLoadingFailed(uri, imageAware.getWrappedView(), FailReason(failType, failCause))
        }
        runTask(r, false, handler, engine)
    }

    private fun fireCancelEvent() {
        if (syncLoading || isTaskInterrupted()) return
        val r = Runnable {
            listener.onLoadingCancelled(uri, imageAware.getWrappedView())
        }
        runTask(r, false, handler, engine)
    }


    private fun getDownloader(): ImageDownloader? {
        return if (engine.networkDenied.get()) {
            networkDeniedDownloader
        } else if (engine.slowNetwork.get()) {
            slowNetworkDownloader
        } else {
            downloader
        }
    }

    /**
     * @throws TaskCancelledException if task is not actual (target ImageAware is collected by GC or the image URI of
     *                                this task doesn't match to image URI which is actual for current ImageAware at
     *                                this moment)
     */
    @Throws(TaskCancelledException::class)
    private fun checkTaskNotActual() {
        checkViewCollected()
        checkViewReused()
    }

    /**
     * @return <b>true</b> - if task is not actual (target ImageAware is collected by GC or the image URI of this task
     * doesn't match to image URI which is actual for current ImageAware at this moment)); <b>false</b> - otherwise
     */
    private fun isTaskNotActual(): Boolean {
        return isViewCollected() || isViewReused()
    }

    /** @throws TaskCancelledException if target ImageAware is collected */
    @Throws(TaskCancelledException::class)
    private fun checkViewCollected() {
        if (isViewCollected()) {
            throw TaskCancelledException()
        }
    }

    /** @return <b>true</b> - if target ImageAware is collected by GC; <b>false</b> - otherwise */
    private fun isViewCollected(): Boolean {
        if (imageAware.isCollected()) {
            L.d(LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED, memoryCacheKey)
            return true
        }
        return false
    }

    /** @throws TaskCancelledException if target ImageAware is collected by GC */
    @Throws(TaskCancelledException::class)
    private fun checkViewReused() {
        if (isViewReused()) {
            throw TaskCancelledException()
        }
    }

    /** @return <b>true</b> - if current ImageAware is reused for displaying another image; <b>false</b> - otherwise */
    private fun isViewReused(): Boolean {
        val currentCacheKey = engine.getLoadingUriForView(imageAware)
        // Check whether memory cache key (image URI) for current ImageAware is actual.
        // If ImageAware is reused for another task then current task should be cancelled.
        val imageAwareWasReused = !memoryCacheKey.equals(currentCacheKey)
        if (imageAwareWasReused) {
            L.d(LOG_TASK_CANCELLED_IMAGEAWARE_REUSED, memoryCacheKey)
            return true
        }
        return false
    }

    /** @throws TaskCancelledException if current task was interrupted */
    @Throws(TaskCancelledException::class)
    private fun checkTaskInterrupted() {
        if (isTaskInterrupted()) {
            throw TaskCancelledException()
        }
    }

    /** @return <b>true</b> - if current task was interrupted; <b>false</b> - otherwise */
    private fun isTaskInterrupted(): Boolean {
        if (Thread.interrupted()) {
            L.d(LOG_TASK_INTERRUPTED, memoryCacheKey)
            return true
        }
        return false
    }

    fun getLoadingUri(): String = uri

    /**
     * Exceptions for case when task is cancelled (thread is interrupted, image view is reused for another task, view is
     * collected by GC).
     *
     * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
     * @since 1.9.1
     */
    class TaskCancelledException : Exception()

}