package cn.quibbler.imageloader.core

import cn.quibbler.imageloader.core.DefaultConfigurationFactory.createTaskDistributor
import cn.quibbler.imageloader.core.download.ImageDownloader
import cn.quibbler.imageloader.core.imageaware.ImageAware
import java.io.File
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/**
 * {@link ImageLoader} engine which responsible for {@linkplain LoadAndDisplayImageTask display task} execution.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.7.1
 */
class ImageLoaderEngine {

    val configuration: ImageLoaderConfiguration
    private var taskExecutor: Executor?
    private var taskExecutorForCachedImages: Executor?
    private var taskDistributor: Executor?

    private val cacheKeysForImageAwares = Collections.synchronizedMap(HashMap<Int, String>())

    private val uriLocks: WeakHashMap<String, ReentrantLock> = WeakHashMap()

    val paused = AtomicBoolean(false)
    val networkDenied = AtomicBoolean(false)
    val slowNetwork = AtomicBoolean(false)

    val pauseLock = Object()

    constructor(configuration: ImageLoaderConfiguration) {
        this.configuration = configuration

        taskExecutor = configuration.taskExecutor
        taskExecutorForCachedImages = configuration.taskExecutorForCachedImages

        taskDistributor = createTaskDistributor()
    }

    fun submit(task: LoadAndDisplayImageTask) {
        taskDistributor?.execute {
            val image: File? = configuration.diskCache?.get(task.getLoadingUri())
            val isImageCachedOnDisk = (image != null) && image.exists() || isLocalUri(task.getLoadingUri())
            initExecutorsIfNeed()
            if (isImageCachedOnDisk) {
                taskExecutorForCachedImages?.execute(task)
            } else {
                taskExecutor?.execute(task)
            }
        }
    }

    /** Submits task to execution pool */
    fun submit(task: ProcessAndDisplayImageTask) {
        initExecutorsIfNeed()
        taskExecutorForCachedImages?.execute(task)
    }

    private fun isLocalUri(uri: String): Boolean {
        val scheme = ImageDownloader.Scheme.ofUri(uri)
        return scheme == ImageDownloader.Scheme.ASSETS || scheme == ImageDownloader.Scheme.FILE || scheme == ImageDownloader.Scheme.DRAWABLE
    }

    private fun initExecutorsIfNeed() {
        if (!configuration.customExecutor && ((taskExecutor as ExecutorService?)?.isShutdown() ?: false)) {
            taskExecutor = createTaskExecutor()
        }
        if (!configuration.customExecutorForCachedImages && ((taskExecutorForCachedImages as ExecutorService)?.isShutdown ?: false)) {
            taskExecutorForCachedImages = createTaskExecutor();
        }

    }

    private fun createTaskExecutor(): Executor {
        return DefaultConfigurationFactory.createExecutor(
            configuration.threadPoolSize,
            configuration.threadPriority,
            configuration.tasksProcessingType
        )
    }

    /**
     * Returns URI of image which is loading at this moment into passed {@link com.nostra13.universalimageloader.core.imageaware.ImageAware}
     */
    fun getLoadingUriForView(imageAware: ImageAware): String? {
        return cacheKeysForImageAwares.get(imageAware.getId())
    }

    /**
     * Associates <b>memoryCacheKey</b> with <b>imageAware</b>. Then it helps to define image URI is loaded into View at
     * exact moment.
     */
    fun prepareDisplayTaskFor(imageAware: ImageAware, memoryCacheKey: String) {
        cacheKeysForImageAwares[imageAware.getId()] = memoryCacheKey
    }

    /**
     * Cancels the task of loading and displaying image for incoming <b>imageAware</b>.
     *
     * @param imageAware {@link com.nostra13.universalimageloader.core.imageaware.ImageAware} for which display task
     *                   will be cancelled
     */
    fun cancelDisplayTaskFor(imageAware: ImageAware) {
        cacheKeysForImageAwares.remove(imageAware.getId())
    }

    /**
     * Denies or allows engine to download images from the network.<br /> <br /> If downloads are denied and if image
     * isn't cached then {@link ImageLoadingListener#onLoadingFailed(String, View, FailReason)} callback will be fired
     * with {@link FailReason.FailType#NETWORK_DENIED}
     *
     * @param denyNetworkDownloads pass <b>true</b> - to deny engine to download images from the network; <b>false</b> -
     *                             to allow engine to download images from network.
     */
    fun denyNetworkDownloads(denyNetworkDownloads: Boolean) {
        networkDenied.set(denyNetworkDownloads)
    }

    /**
     * Sets option whether ImageLoader will use {@link FlushedInputStream} for network downloads to handle <a
     * href="http://code.google.com/p/android/issues/detail?id=6066">this known problem</a> or not.
     *
     * @param handleSlowNetwork pass <b>true</b> - to use {@link FlushedInputStream} for network downloads; <b>false</b>
     *                          - otherwise.
     */
    fun handleSlowNetwork(handleSlowNetwork: Boolean) {
        slowNetwork.set(handleSlowNetwork)

    }

    /**
     * Pauses engine. All new "load&display" tasks won't be executed until ImageLoader is {@link #resume() resumed}.<br
     * /> Already running tasks are not paused.
     */
    fun pause() {
        paused.set(true)
    }

    /** Resumes engine work. Paused "load&display" tasks will continue its work. */
    fun resume() {
        paused.set(false)
        synchronized(pauseLock) {
            pauseLock.notify()
        }
    }

    /**
     * Stops engine, cancels all running and scheduled display image tasks. Clears internal data.
     * <br />
     * <b>NOTE:</b> This method doesn't shutdown
     * {@linkplain com.nostra13.universalimageloader.core.ImageLoaderConfiguration.Builder#taskExecutor(java.util.concurrent.Executor)
     * custom task executors} if you set them.
     */
    fun stop() {
        if (!configuration.customExecutor) {
            (taskExecutor as ExecutorService?)?.shutdown()
        }
        if (!configuration.customExecutorForCachedImages) {
            (taskExecutorForCachedImages as ExecutorService?)?.shutdown()
        }

        cacheKeysForImageAwares.clear()
        uriLocks.clear()
    }

    fun fireCallback(r: Runnable) {
        taskDistributor?.execute(r)
    }

    fun getLockForUri(uri: String): ReentrantLock {
        var lock = uriLocks.get(uri)
        if (lock == null) {
            lock = ReentrantLock()
            uriLocks[uri] = lock
        }

        return lock
    }

}