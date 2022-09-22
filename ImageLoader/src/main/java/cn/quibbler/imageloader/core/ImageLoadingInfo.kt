package cn.quibbler.imageloader.core

import cn.quibbler.imageloader.core.assist.ImageSize
import cn.quibbler.imageloader.core.imageaware.ImageAware
import cn.quibbler.imageloader.core.listener.ImageLoadingListener
import cn.quibbler.imageloader.core.listener.ImageLoadingProgressListener
import java.util.concurrent.locks.ReentrantLock

/**
 * Information for load'n'display image task
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see com.nostra13.universalimageloader.utils.MemoryCacheUtils
 * @see DisplayImageOptions
 * @see ImageLoadingListener
 * @see com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener
 * @since 1.3.1
 */
data class ImageLoadingInfo(
    val uri: String,
    val memoryCacheKey: String,
    val imageAware: ImageAware,
    val targetSize: ImageSize,
    val options: DisplayImageOptions,
    val listener: ImageLoadingListener,
    val progressListener: ImageLoadingProgressListener,
    val loadFromUriLock: ReentrantLock
)