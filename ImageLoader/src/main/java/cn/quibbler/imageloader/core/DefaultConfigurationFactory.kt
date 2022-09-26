package cn.quibbler.imageloader.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import cn.quibbler.imageloader.cache.disk.DiskCache
import cn.quibbler.imageloader.cache.disk.impl.UnlimitedDiskCache
import cn.quibbler.imageloader.cache.disk.impl.ext.LruDiskCache
import cn.quibbler.imageloader.cache.disk.naming.FileNameGenerator
import cn.quibbler.imageloader.cache.disk.naming.HashCodeFileNameGenerator
import cn.quibbler.imageloader.cache.memory.MemoryCache
import cn.quibbler.imageloader.cache.memory.impl.LruMemoryCache
import cn.quibbler.imageloader.core.assist.QueueProcessingType
import cn.quibbler.imageloader.core.assist.deque.LIFOLinkedBlockingDeque
import cn.quibbler.imageloader.core.assist.deque.LinkedBlockingDeque
import cn.quibbler.imageloader.core.decode.BaseImageDecoder
import cn.quibbler.imageloader.core.decode.ImageDecoder
import cn.quibbler.imageloader.core.display.BitmapDisplayer
import cn.quibbler.imageloader.core.display.SimpleBitmapDisplayer
import cn.quibbler.imageloader.core.download.BaseImageDownloader
import cn.quibbler.imageloader.core.download.ImageDownloader
import cn.quibbler.imageloader.utils.L
import cn.quibbler.imageloader.utils.getCacheDirectory
import cn.quibbler.imageloader.utils.getIndividualCacheDirectory
import java.io.File
import java.io.IOException
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.log

/**
 * Factory for providing of default options for {@linkplain ImageLoaderConfiguration configuration}
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.5.6
 */
object DefaultConfigurationFactory {

    /** Creates default implementation of task executor */
    fun createExecutor(threadPoolSize: Int, threadPriority: Int, tasksProcessingType: QueueProcessingType?): Executor {
        val lifo = tasksProcessingType == QueueProcessingType.LIFO
        val taskQueue = if (lifo) {
            LIFOLinkedBlockingDeque<Runnable>()
        } else {
            LinkedBlockingDeque<Runnable>()
        }
        return ThreadPoolExecutor(
            threadPoolSize,
            threadPoolSize,
            0L,
            TimeUnit.MILLISECONDS,
            taskQueue,
            createThreadFactory(threadPriority, "uil-pool-")
        )
    }

    /** Creates default implementation of task distributor */
    fun createTaskDistributor(): Executor {
        return Executors.newCachedThreadPool(createThreadFactory(Thread.NORM_PRIORITY, "uil-pool-d-"))
    }

    /** Creates {@linkplain HashCodeFileNameGenerator default implementation} of FileNameGenerator */
    fun createFileNameGenerator(): FileNameGenerator = HashCodeFileNameGenerator()

    fun createDiskCache(context: Context, diskCacheFileNameGenerator: FileNameGenerator, diskCacheSize: Int, diskCacheFileCount: Int): DiskCache {
        val reserveCacheDir = createReserveDiskCacheDir(context)
        if (diskCacheSize > 0 || diskCacheFileCount > 0) {
            val individualCacheDir = getIndividualCacheDirectory(context)
            try {
                return LruDiskCache(individualCacheDir, reserveCacheDir, diskCacheFileNameGenerator, diskCacheSize.toLong(), diskCacheFileCount)
            } catch (e: IOException) {
                L.e(e)
                // continue and create unlimited cache
            }
        }
        val cacheDir = getCacheDirectory(context)
        return UnlimitedDiskCache(cacheDir, reserveCacheDir, diskCacheFileNameGenerator)
    }

    /** Creates reserve disk cache folder which will be used if primary disk cache folder becomes unavailable */
    private fun createReserveDiskCacheDir(context: Context): File? {
        var cacheDir = getCacheDirectory(context, false)
        val individualDir = File(cacheDir, "uil-images")
        if (individualDir.exists() || individualDir.mkdir()) {
            cacheDir = individualDir
        }
        return cacheDir
    }

    /**
     * Creates default implementation of {@link MemoryCache} - {@link LruMemoryCache}<br />
     * Default cache size = 1/8 of available app memory.
     */
    fun createMemoryCache(context: Context, memoryCacheSize: Int): MemoryCache {
        var memoryCacheSize_ = memoryCacheSize
        if (memoryCacheSize_ == 0) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            var memoryClass = am.memoryClass
            if (hasHoneycomb() && isLargeHeap(context)) {
                memoryClass = getLargeMemoryClass(am)
            }
            memoryCacheSize_ = 1024 * 1024 * memoryClass / 8
        }
        return LruMemoryCache(memoryCacheSize_)
    }

    private fun hasHoneycomb(): Boolean = Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB

    private fun isLargeHeap(context: Context): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP != 0
    }

    private fun getLargeMemoryClass(am: ActivityManager): Int = am.largeMemoryClass

    /** Creates default implementation of {@link ImageDownloader} - {@link BaseImageDownloader} */
    fun createImageDownloader(context: Context): ImageDownloader = BaseImageDownloader(context)

    /** Creates default implementation of {@link ImageDecoder} - {@link BaseImageDecoder} */
    fun createImageDecoder(loggingEnabled: Boolean): ImageDecoder = BaseImageDecoder(loggingEnabled)

    /** Creates default implementation of {@link BitmapDisplayer} - {@link SimpleBitmapDisplayer} */
    fun createBitmapDisplayer(): BitmapDisplayer = SimpleBitmapDisplayer()

    /** Creates default implementation of {@linkplain ThreadFactory thread factory} for task executor */
    private fun createThreadFactory(threadPriority: Int, threadNamePrefix: String): ThreadFactory {
        return DefaultThreadFactory(threadPriority, threadNamePrefix)
    }

    private class DefaultThreadFactory(private val threadPriority: Int, private val threadNamePrefix: String) : ThreadFactory {

        private val poolNumber: AtomicInteger = AtomicInteger(1)

        private val threadNumber: AtomicInteger = AtomicInteger(1)

        private val group = Thread.currentThread().threadGroup

        private val namePrefix = "$threadNamePrefix${poolNumber.getAndIncrement()}-thread-"

        override fun newThread(r: Runnable?): Thread {
            val thread = Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0)
            if (thread.isDaemon) {
                thread.isDaemon = false
            }
            thread.priority = threadPriority
            return thread
        }

    }

}