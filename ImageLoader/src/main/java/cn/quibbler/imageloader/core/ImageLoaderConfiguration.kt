package cn.quibbler.imageloader.core

import android.content.Context
import android.content.res.Resources
import cn.quibbler.imageloader.cache.disk.DiskCache
import cn.quibbler.imageloader.cache.disk.naming.FileNameGenerator
import cn.quibbler.imageloader.cache.memory.MemoryCache
import cn.quibbler.imageloader.cache.memory.impl.FuzzyKeyMemoryCache
import cn.quibbler.imageloader.core.assist.FlushedInputStream
import cn.quibbler.imageloader.core.assist.ImageSize
import cn.quibbler.imageloader.core.assist.QueueProcessingType
import cn.quibbler.imageloader.core.decode.ImageDecoder
import cn.quibbler.imageloader.core.download.ImageDownloader
import cn.quibbler.imageloader.core.process.BitmapProcessor
import cn.quibbler.imageloader.utils.L
import cn.quibbler.imageloader.utils.createFuzzyKeyComparator
import java.io.InputStream
import java.util.concurrent.Executor

/**
 * Presents configuration for {@link ImageLoader}
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageLoader
 * @see MemoryCache
 * @see DiskCache
 * @see DisplayImageOptions
 * @see ImageDownloader
 * @see FileNameGenerator
 * @since 1.0.0
 */
class ImageLoaderConfiguration {

    companion object {

        /**
         * Creates default configuration for {@link ImageLoader} <br />
         * <b>Default values:</b>
         * <ul>
         * <li>maxImageWidthForMemoryCache = device's screen width</li>
         * <li>maxImageHeightForMemoryCache = device's screen height</li>
         * <li>maxImageWidthForDikcCache = unlimited</li>
         * <li>maxImageHeightForDiskCache = unlimited</li>
         * <li>threadPoolSize = {@link Builder#DEFAULT_THREAD_POOL_SIZE this}</li>
         * <li>threadPriority = {@link Builder#DEFAULT_THREAD_PRIORITY this}</li>
         * <li>allow to cache different sizes of image in memory</li>
         * <li>memoryCache = {@link DefaultConfigurationFactory#createMemoryCache(android.content.Context, int)}</li>
         * <li>diskCache = {@link com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache}</li>
         * <li>imageDownloader = {@link DefaultConfigurationFactory#createImageDownloader(Context)}</li>
         * <li>imageDecoder = {@link DefaultConfigurationFactory#createImageDecoder(boolean)}</li>
         * <li>diskCacheFileNameGenerator = {@link DefaultConfigurationFactory#createFileNameGenerator()}</li>
         * <li>defaultDisplayImageOptions = {@link DisplayImageOptions#createSimple() Simple options}</li>
         * <li>tasksProcessingOrder = {@link QueueProcessingType#FIFO}</li>
         * <li>detailed logging disabled</li>
         * </ul>
         */
        fun createDefault(context: Context): ImageLoaderConfiguration {
            return Builder(context).build()
        }

    }

    val resources: Resources

    val maxImageWidthForMemoryCache: Int
    val maxImageHeightForMemoryCache: Int
    val maxImageWidthForDiskCache: Int
    val maxImageHeightForDiskCache: Int
    val processorForDiskCache: BitmapProcessor?

    val taskExecutor: Executor?
    val taskExecutorForCachedImages: Executor?
    val customExecutor: Boolean
    val customExecutorForCachedImages: Boolean

    val threadPoolSize: Int
    val threadPriority: Int
    val tasksProcessingType: QueueProcessingType?

    val memoryCache: MemoryCache?
    val diskCache: DiskCache?
    val downloader: ImageDownloader?
    val decoder: ImageDecoder?
    val defaultDisplayImageOptions: DisplayImageOptions?

    val networkDeniedDownloader: ImageDownloader?
    val slowNetworkDownloader: ImageDownloader?

    private constructor(builder: Builder) {

        resources = builder.context.resources
        maxImageWidthForMemoryCache = builder.maxImageWidthForMemoryCache
        maxImageHeightForMemoryCache = builder.maxImageHeightForMemoryCache
        maxImageWidthForDiskCache = builder.maxImageWidthForDiskCache
        maxImageHeightForDiskCache = builder.maxImageHeightForDiskCache
        processorForDiskCache = builder.processorForDiskCache
        taskExecutor = builder.taskExecutor
        taskExecutorForCachedImages = builder.taskExecutorForCachedImages
        threadPoolSize = builder.threadPoolSize
        threadPriority = builder.threadPriority
        tasksProcessingType = builder.tasksProcessingType
        diskCache = builder.diskCache
        memoryCache = builder.memoryCache
        defaultDisplayImageOptions = builder.defaultDisplayImageOptions
        downloader = builder.downloader
        decoder = builder.decoder

        customExecutor = builder.customExecutor
        customExecutorForCachedImages = builder.customExecutorForCachedImages

        networkDeniedDownloader = NetworkDeniedImageDownloader(downloader!!)
        slowNetworkDownloader = SlowNetworkImageDownloader(downloader)

        L.writeDebugLogs(builder.writeLogs)
    }

    fun getMaxImageSize(): ImageSize {
        val displayMetrics = resources.displayMetrics

        var width = maxImageWidthForMemoryCache
        if (width <= 0) {
            width = displayMetrics.widthPixels
        }

        var height = maxImageHeightForMemoryCache
        if (height <= 0) {
            height = displayMetrics.heightPixels
        }

        return ImageSize(width, height)
    }

    /**
     * Builder for {@link ImageLoaderConfiguration}
     *
     * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
     */
    class Builder {
        companion object {
            private const val WARNING_OVERLAP_DISK_CACHE_PARAMS = "diskCache(), diskCacheSize() and diskCacheFileCount calls overlap each other"
            private const val WARNING_OVERLAP_DISK_CACHE_NAME_GENERATOR = "diskCache() and diskCacheFileNameGenerator() calls overlap each other"
            private const val WARNING_OVERLAP_MEMORY_CACHE = "memoryCache() and memoryCacheSize() calls overlap each other"
            private const val WARNING_OVERLAP_EXECUTOR =
                "threadPoolSize(), threadPriority() and tasksProcessingOrder() calls can overlap taskExecutor() and taskExecutorForCachedImages() calls."

            const val DEFAULT_THREAD_POOL_SIZE = 3

            const val DEFAULT_THREAD_PRIORITY = Thread.NORM_PRIORITY - 2

            val DEFAULT_TASK_PROCESSING_TYPE: QueueProcessingType = QueueProcessingType.FIFO
        }

        val context: Context

        var maxImageWidthForMemoryCache = 0
        var maxImageHeightForMemoryCache = 0
        var maxImageWidthForDiskCache = 0
        var maxImageHeightForDiskCache = 0
        var processorForDiskCache: BitmapProcessor? = null

        var taskExecutor: Executor? = null
        var taskExecutorForCachedImages: Executor? = null
        var customExecutor = false
        var customExecutorForCachedImages = false

        var threadPoolSize: Int = DEFAULT_THREAD_POOL_SIZE
        var threadPriority: Int = DEFAULT_THREAD_PRIORITY
        var denyCacheImageMultipleSizesInMemory = false
        var tasksProcessingType: QueueProcessingType = DEFAULT_TASK_PROCESSING_TYPE

        var memoryCacheSize = 0
        var diskCacheSize: Long = 0
        var diskCacheFileCount = 0

        var memoryCache: MemoryCache? = null
        var diskCache: DiskCache? = null
        var diskCacheFileNameGenerator: FileNameGenerator? = null
        var downloader: ImageDownloader? = null
        var decoder: ImageDecoder? = null
        var defaultDisplayImageOptions: DisplayImageOptions? = null

        var writeLogs = false

        constructor(context: Context) {
            this.context = context.applicationContext
        }

        /**
         * Sets options for memory cache
         *
         * @param maxImageWidthForMemoryCache  Maximum image width which will be used for memory saving during decoding
         *                                     an image to {@link android.graphics.Bitmap Bitmap}. <b>Default value - device's screen width</b>
         * @param maxImageHeightForMemoryCache Maximum image height which will be used for memory saving during decoding
         *                                     an image to {@link android.graphics.Bitmap Bitmap}. <b>Default value</b> - device's screen height
         */
        fun memoryCacheExtraOptions(maxImageWidthForMemoryCache: Int, maxImageHeightForMemoryCache: Int): Builder {
            this.maxImageWidthForMemoryCache = maxImageWidthForMemoryCache
            this.maxImageHeightForMemoryCache = maxImageHeightForMemoryCache
            return this
        }

        @Deprecated("Use {@link #diskCacheExtraOptions(int, int, BitmapProcessor)} instead")
        fun discCacheExtraOptions(maxImageWidthForDiskCache: Int, maxImageHeightForDiskCache: Int, processorForDiskCache: BitmapProcessor): Builder {
            return diskCacheExtraOptions(maxImageWidthForDiskCache, maxImageHeightForDiskCache, processorForDiskCache)
        }

        /**
         * Sets options for resizing/compressing of downloaded images before saving to disk cache.<br />
         * <b>NOTE: Use this option only when you have appropriate needs. It can make ImageLoader slower.</b>
         *
         * @param maxImageWidthForDiskCache  Maximum width of downloaded images for saving at disk cache
         * @param maxImageHeightForDiskCache Maximum height of downloaded images for saving at disk cache
         * @param processorForDiskCache      null-ok; {@linkplain BitmapProcessor Bitmap processor} which process images before saving them in disc cache
         */
        fun diskCacheExtraOptions(maxImageWidthForDiskCache: Int, maxImageHeightForDiskCache: Int, processorForDiskCache: BitmapProcessor): Builder {
            this.maxImageWidthForDiskCache = maxImageWidthForDiskCache
            this.maxImageHeightForDiskCache = maxImageHeightForDiskCache
            this.processorForDiskCache = processorForDiskCache
            return this
        }

        /**
         * Sets custom {@linkplain Executor executor} for tasks of loading and displaying images.<br />
         * <br />
         * <b>NOTE:</b> If you set custom executor then following configuration options will not be considered for this
         * executor:
         * <ul>
         * <li>{@link #threadPoolSize(int)}</li>
         * <li>{@link #threadPriority(int)}</li>
         * <li>{@link #tasksProcessingOrder(QueueProcessingType)}</li>
         * </ul>
         *
         * @see #taskExecutorForCachedImages(Executor)
         */
        fun taskExecutor(executor: Executor): Builder {
            if (threadPoolSize != DEFAULT_THREAD_POOL_SIZE || threadPriority != DEFAULT_THREAD_PRIORITY || tasksProcessingType != DEFAULT_TASK_PROCESSING_TYPE) {
                L.w(WARNING_OVERLAP_EXECUTOR)
            }
            this.taskExecutor = executor
            return this
        }

        /**
         * Sets custom {@linkplain Executor executor} for tasks of displaying <b>cached on disk</b> images (these tasks
         * are executed quickly so UIL prefer to use separate executor for them).<br />
         * <br />
         * If you set the same executor for {@linkplain #taskExecutor(Executor) general tasks} and
         * tasks about cached images (this method) then these tasks will be in the
         * same thread pool. So short-lived tasks can wait a long time for their turn.<br />
         * <br />
         * <b>NOTE:</b> If you set custom executor then following configuration options will not be considered for this
         * executor:
         * <ul>
         * <li>{@link #threadPoolSize(int)}</li>
         * <li>{@link #threadPriority(int)}</li>
         * <li>{@link #tasksProcessingOrder(QueueProcessingType)}</li>
         * </ul>
         *
         * @see #taskExecutor(Executor)
         */
        fun taskExecutorForCachedImages(executorForCachedImages: Executor): Builder {
            if (threadPoolSize != DEFAULT_THREAD_POOL_SIZE || threadPriority != DEFAULT_THREAD_PRIORITY || tasksProcessingType != DEFAULT_TASK_PROCESSING_TYPE) {
                L.w(WARNING_OVERLAP_EXECUTOR)
            }
            this.taskExecutorForCachedImages = executorForCachedImages
            return this
        }

        /**
         * Sets thread pool size for image display tasks.<br />
         * Default value - {@link #DEFAULT_THREAD_POOL_SIZE this}
         */
        fun threadPoolSize(threadPoolSize: Int): Builder {
            if (taskExecutor != null || taskExecutorForCachedImages != null) {
                L.w(WARNING_OVERLAP_EXECUTOR)
            }
            this.threadPoolSize = threadPoolSize
            return this
        }

        /**
         * Sets the priority for image loading threads. Should be <b>NOT</b> greater than {@link Thread#MAX_PRIORITY} or
         * less than {@link Thread#MIN_PRIORITY}<br />
         * Default value - {@link #DEFAULT_THREAD_PRIORITY this}
         */
        fun threadPriority(threadPriority: Int): Builder {
            if (taskExecutor != null || taskExecutorForCachedImages != null) {
                L.w(WARNING_OVERLAP_EXECUTOR)
            }

            if (threadPriority < Thread.MIN_PRIORITY) {
                this.threadPriority = Thread.MIN_PRIORITY
            } else {
                if (threadPriority > Thread.MAX_PRIORITY) {
                    this.threadPriority = Thread.MAX_PRIORITY
                } else {
                    this.threadPriority = threadPriority
                }
            }
            return this
        }

        /**
         * When you display an image in a small {@link android.widget.ImageView ImageView} and later you try to display
         * this image (from identical URI) in a larger {@link android.widget.ImageView ImageView} so decoded image of
         * bigger size will be cached in memory as a previous decoded image of smaller size.<br />
         * So <b>the default behavior is to allow to cache multiple sizes of one image in memory</b>. You can
         * <b>deny</b> it by calling <b>this</b> method: so when some image will be cached in memory then previous
         * cached size of this image (if it exists) will be removed from memory cache before.
         */
        fun denyCacheImageMultipleSizesInMemory(): Builder {
            denyCacheImageMultipleSizesInMemory = true
            return this
        }

        /**
         * Sets type of queue processing for tasks for loading and displaying images.<br />
         * Default value - {@link QueueProcessingType#FIFO}
         */
        fun tasksProcessingOrder(tasksProcessingType: QueueProcessingType): Builder {
            if (taskExecutor != null || taskExecutorForCachedImages != null) {
                L.w(WARNING_OVERLAP_EXECUTOR)
            }

            this.tasksProcessingType = tasksProcessingType
            return this
        }

        /**
         * Sets maximum memory cache size for {@link android.graphics.Bitmap bitmaps} (in bytes).<br />
         * Default value - 1/8 of available app memory.<br />
         * <b>NOTE:</b> If you use this method then
         * {@link com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache LruMemoryCache} will be used as
         * memory cache. You can use {@link #memoryCache(MemoryCache)} method to set your own implementation of
         * {@link MemoryCache}.
         */
        fun memoryCacheSize(memoryCacheSize: Int): Builder {
            if (memoryCacheSize <= 0) throw IllegalArgumentException("memoryCacheSize must be a positive number")


            if (memoryCache != null) {
                L.w(WARNING_OVERLAP_MEMORY_CACHE)
            }

            this.memoryCacheSize = memoryCacheSize
            return this
        }

        /**
         * Sets maximum memory cache size (in percent of available app memory) for {@link android.graphics.Bitmap
         * bitmaps}.<br />
         * Default value - 1/8 of available app memory.<br />
         * <b>NOTE:</b> If you use this method then
         * {@link com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache LruMemoryCache} will be used as
         * memory cache. You can use {@link #memoryCache(MemoryCache)} method to set your own implementation of
         * {@link MemoryCache}.
         */
        fun memoryCacheSizePercentage(availableMemoryPercent: Int): Builder {
            if (availableMemoryPercent <= 0 || availableMemoryPercent >= 100) {
                throw IllegalArgumentException("availableMemoryPercent must be in range (0 < % < 100)")
            }

            if (memoryCache != null) {
                L.w(WARNING_OVERLAP_MEMORY_CACHE)
            }

            val availableMemory = Runtime.getRuntime().maxMemory()
            memoryCacheSize = (availableMemory * (availableMemoryPercent / 100f)).toInt()
            return this
        }

        /**
         * Sets memory cache for {@link android.graphics.Bitmap bitmaps}.<br />
         * Default value - {@link com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache LruMemoryCache}
         * with limited memory cache size (size = 1/8 of available app memory)<br />
         * <br />
         * <b>NOTE:</b> If you set custom memory cache then following configuration option will not be considered:
         * <ul>
         * <li>{@link #memoryCacheSize(int)}</li>
         * </ul>
         */
        fun memoryCache(memoryCache: MemoryCache): Builder {
            if (memoryCacheSize != 0) {
                L.w(WARNING_OVERLAP_MEMORY_CACHE)
            }

            this.memoryCache = memoryCache
            return this
        }

        @Deprecated("Use {@link #diskCacheSize(int)} instead")
        fun discCacheSize(maxCacheSize: Int): Builder {
            return diskCacheSize(maxCacheSize)
        }

        /**
         * Sets maximum disk cache size for images (in bytes).<br />
         * By default: disk cache is unlimited.<br />
         * <b>NOTE:</b> If you use this method then
         * {@link com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiskCache LruDiskCache}
         * will be used as disk cache. You can use {@link #diskCache(DiskCache)} method for introduction your own
         * implementation of {@link DiskCache}
         */
        fun diskCacheSize(maxCacheSize: Int): Builder {
            if (maxCacheSize <= 0) throw IllegalArgumentException("maxCacheSize must be a positive number")

            if (diskCache != null) {
                L.w(WARNING_OVERLAP_DISK_CACHE_PARAMS)
            }

            this.diskCacheSize = maxCacheSize.toLong()
            return this
        }

        @Deprecated("Use {@link #diskCacheFileCount(int)} instead")
        fun discCacheFileCount(maxFileCount: Int): Builder {
            return diskCacheFileCount(maxFileCount)
        }

        /**
         * Sets maximum file count in disk cache directory.<br />
         * By default: disk cache is unlimited.<br />
         * <b>NOTE:</b> If you use this method then
         * {@link com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiskCache LruDiskCache}
         * will be used as disk cache. You can use {@link #diskCache(DiskCache)} method for introduction your own
         * implementation of {@link DiskCache}
         */
        fun diskCacheFileCount(maxFileCount: Int): Builder {
            if (maxFileCount <= 0) throw IllegalArgumentException("maxFileCount must be a positive number")

            if (diskCache != null) {
                L.w(WARNING_OVERLAP_DISK_CACHE_PARAMS)
            }

            this.diskCacheFileCount = maxFileCount
            return this
        }

        @Deprecated("Use {@link #diskCacheFileNameGenerator(FileNameGenerator)")
        fun discCacheFileNameGenerator(fileNameGenerator: FileNameGenerator): Builder {
            return diskCacheFileNameGenerator(fileNameGenerator)
        }

        /**
         * Sets name generator for files cached in disk cache.<br />
         * Default value -
         * {@link com.nostra13.universalimageloader.core.DefaultConfigurationFactory#createFileNameGenerator()
         * DefaultConfigurationFactory.createFileNameGenerator()}
         */
        fun diskCacheFileNameGenerator(fileNameGenerator: FileNameGenerator): Builder {
            if (diskCache != null) {
                L.w(WARNING_OVERLAP_DISK_CACHE_NAME_GENERATOR)
            }

            this.diskCacheFileNameGenerator = fileNameGenerator
            return this
        }

        @Deprecated("Use {@link #diskCache(DiskCache)}")
        fun discCache(diskCache: DiskCache): Builder {
            return diskCache(diskCache)
        }

        /**
         * Sets disk cache for images.<br />
         * Default value - {@link com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache
         * UnlimitedDiskCache}. Cache directory is defined by
         * {@link com.nostra13.universalimageloader.utils.StorageUtils#getCacheDirectory(Context)
         * StorageUtils.getCacheDirectory(Context)}.<br />
         * <br />
         * <b>NOTE:</b> If you set custom disk cache then following configuration option will not be considered:
         * <ul>
         * <li>{@link #diskCacheSize(int)}</li>
         * <li>{@link #diskCacheFileCount(int)}</li>
         * <li>{@link #diskCacheFileNameGenerator(FileNameGenerator)}</li>
         * </ul>
         */
        fun diskCache(diskCache: DiskCache): Builder {
            if (diskCacheSize > 0 || diskCacheFileCount > 0) {
                L.w(WARNING_OVERLAP_DISK_CACHE_PARAMS)
            }
            if (diskCacheFileNameGenerator != null) {
                L.w(WARNING_OVERLAP_DISK_CACHE_NAME_GENERATOR)
            }

            this.diskCache = diskCache
            return this
        }

        /**
         * Sets utility which will be responsible for downloading of image.<br />
         * Default value -
         * {@link com.nostra13.universalimageloader.core.DefaultConfigurationFactory#createImageDownloader(Context)
         * DefaultConfigurationFactory.createImageDownloader()}
         */
        fun imageDownloader(imageDownloader: ImageDownloader): Builder {
            this.downloader = imageDownloader
            return this
        }

        /**
         * Sets utility which will be responsible for decoding of image stream.<br />
         * Default value -
         * {@link com.nostra13.universalimageloader.core.DefaultConfigurationFactory#createImageDecoder(boolean)
         * DefaultConfigurationFactory.createImageDecoder()}
         */
        fun imageDecoder(imageDecoder: ImageDecoder): Builder {
            this.decoder = imageDecoder
            return this
        }

        /**
         * Sets default {@linkplain DisplayImageOptions display image options} for image displaying. These options will
         * be used for every {@linkplain ImageLoader#displayImage(String, android.widget.ImageView) image display call}
         * without passing custom {@linkplain DisplayImageOptions options}<br />
         * Default value - {@link DisplayImageOptions#createSimple() Simple options}
         */
        fun defaultDisplayImageOptions(defaultDisplayImageOptions: DisplayImageOptions): Builder {
            this.defaultDisplayImageOptions = defaultDisplayImageOptions
            return this
        }

        /**
         * Enables detail logging of {@link ImageLoader} work. To prevent detail logs don't call this method.
         * Consider {@link com.nostra13.universalimageloader.utils.L#disableLogging()} to disable
         * ImageLoader logging completely (even error logs)
         */
        fun writeDebugLogs(): Builder {
            this.writeLogs = true
            return this
        }

        /** Builds configured {@link ImageLoaderConfiguration} object */
        fun build(): ImageLoaderConfiguration {
            initEmptyFieldsWithDefaultValues()
            return ImageLoaderConfiguration(this)
        }

        private fun initEmptyFieldsWithDefaultValues() {
            if (taskExecutor == null) {
                taskExecutor = DefaultConfigurationFactory
                    .createExecutor(threadPoolSize, threadPriority, tasksProcessingType)
            } else {
                customExecutor = true
            }
            if (taskExecutorForCachedImages == null) {
                taskExecutorForCachedImages = DefaultConfigurationFactory
                    .createExecutor(threadPoolSize, threadPriority, tasksProcessingType)
            } else {
                customExecutorForCachedImages = true
            }
            if (diskCache == null) {
                if (diskCacheFileNameGenerator == null) {
                    diskCacheFileNameGenerator = DefaultConfigurationFactory.createFileNameGenerator()
                }
                diskCache = DefaultConfigurationFactory
                    .createDiskCache(context, diskCacheFileNameGenerator!!, diskCacheSize.toInt(), diskCacheFileCount)
            }
            if (memoryCache == null) {
                memoryCache = DefaultConfigurationFactory.createMemoryCache(context, memoryCacheSize)
            }
            if (denyCacheImageMultipleSizesInMemory) {
                memoryCache = FuzzyKeyMemoryCache(memoryCache!!, createFuzzyKeyComparator())
            }
            if (downloader == null) {
                downloader = DefaultConfigurationFactory.createImageDownloader(context)
            }
            if (decoder == null) {
                decoder = DefaultConfigurationFactory.createImageDecoder(writeLogs)
            }
            if (defaultDisplayImageOptions == null) {
                defaultDisplayImageOptions = DisplayImageOptions.createSimple()
            }
        }

    }

    /**
     * Decorator. Prevents downloads from network (throws {@link IllegalStateException exception}).<br />
     * In most cases this downloader shouldn't be used directly.
     *
     * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
     * @since 1.8.0
     */
    private class NetworkDeniedImageDownloader(private val wrappedDownloader: ImageDownloader) : ImageDownloader {

        override fun getStream(imageUri: String, extra: Any?): InputStream? {
            when (ImageDownloader.Scheme.ofUri(imageUri)) {
                ImageDownloader.Scheme.HTTP,
                ImageDownloader.Scheme.HTTPS -> {
                    throw IllegalStateException()
                }
                else -> {
                    return wrappedDownloader.getStream(imageUri, extra)
                }
            }
        }

    }

    /**
     * Decorator. Handles <a href="http://code.google.com/p/android/issues/detail?id=6066">this problem</a> on slow networks
     * using {@link com.nostra13.universalimageloader.core.assist.FlushedInputStream}.
     *
     * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
     * @since 1.8.1
     */
    private class SlowNetworkImageDownloader(private val wrappedDownloader: ImageDownloader) : ImageDownloader {

        override fun getStream(imageUri: String, extra: Any?): InputStream? {
            val imageStream = wrappedDownloader.getStream(imageUri, extra)
            when (ImageDownloader.Scheme.ofUri(imageUri)) {
                ImageDownloader.Scheme.HTTP,
                ImageDownloader.Scheme.HTTPS -> {
                    return FlushedInputStream(imageStream)
                }
                else -> {
                    return imageStream
                }
            }
        }

    }


}