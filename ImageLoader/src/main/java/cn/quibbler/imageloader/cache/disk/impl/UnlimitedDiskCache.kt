package cn.quibbler.imageloader.cache.disk.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.disk.naming.FileNameGenerator
import cn.quibbler.imageloader.utils.CopyListener
import java.io.File
import java.io.InputStream

/**
 * Default implementation of {@linkplain com.nostra13.universalimageloader.cache.disc.DiskCache disk cache}.
 * Cache size is unlimited.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
class UnlimitedDiskCache : BaseDiskCache {

    /** @param cacheDir Directory for file caching */
    constructor(cacheDir: File) : super(cacheDir)

    /**
     * @param cacheDir        Directory for file caching
     * @param reserveCacheDir null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
     */
    constructor(cacheDir: File?, reserveCacheDir: File?) : super(cacheDir, reserveCacheDir)

    /**
     * @param cacheDir          Directory for file caching
     * @param reserveCacheDir   null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
     * @param fileNameGenerator {@linkplain com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator
     *                          Name generator} for cached files
     */
    constructor(
        cacheDir: File?,
        reserveCacheDir: File?,
        fileNameGenerator: FileNameGenerator?
    ) : super(cacheDir, reserveCacheDir, fileNameGenerator)

}