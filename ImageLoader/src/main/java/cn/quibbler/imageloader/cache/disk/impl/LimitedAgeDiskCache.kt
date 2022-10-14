package cn.quibbler.imageloader.cache.disk.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.disk.naming.FileNameGenerator
import cn.quibbler.imageloader.core.DefaultConfigurationFactory
import cn.quibbler.imageloader.utils.CopyListener
import java.io.File
import java.io.InputStream
import java.util.*

/**
 * Cache which deletes files which were loaded more than defined time. Cache size is unlimited.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.3.1
 */
class LimitedAgeDiskCache : BaseDiskCache {

    private var maxFileAge: Long

    private val loadingDates = Collections.synchronizedMap(HashMap<File, Long>())

    /**
     * @param cacheDir Directory for file caching
     * @param maxAge   Max file age (in seconds). If file age will exceed this value then it'll be removed on next
     *                 treatment (and therefore be reloaded).
     */
    constructor(cacheDir: File, maxAge: Long) : this(cacheDir, null, maxAge)

    /**
     * @param cacheDir Directory for file caching
     * @param maxAge   Max file age (in seconds). If file age will exceed this value then it'll be removed on next
     *                 treatment (and therefore be reloaded).
     */
    constructor(cacheDir: File, reserveCacheDir: File?, maxAge: Long) : this(
        cacheDir,
        reserveCacheDir,
        DefaultConfigurationFactory.createFileNameGenerator(),
        maxAge
    )

    /**
     * @param cacheDir          Directory for file caching
     * @param reserveCacheDir   null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
     * @param fileNameGenerator Name generator for cached files
     * @param maxAge            Max file age (in seconds). If file age will exceed this value then it'll be removed on next
     *                          treatment (and therefore be reloaded).
     */
    constructor(
        cacheDir: File,
        reserveCacheDir: File?,
        fileNameGenerator: FileNameGenerator?,
        maxAge: Long
    ) : super(cacheDir, reserveCacheDir, fileNameGenerator) {
        this.maxFileAge = maxAge * 1000 // to milliseconds
    }

    override fun get(imageUrl: String): File? {
        val file: File? = super.get(imageUrl)
        if (file != null && file.exists()) {
            var cached: Boolean = false
            var loadingDate = loadingDates.get(file)
            if (loadingDate == null) {
                cached = false
                loadingDate = file.lastModified()
            } else {
                cached = true
            }

            if (System.currentTimeMillis() - loadingDate > maxFileAge) {
                file.delete()
                loadingDates.remove(file)
            } else if (!cached) {
                loadingDates.put(file, loadingDate)
            }
        }
        return file
    }

    override fun save(imageUrl: String, imageStream: InputStream, listener: CopyListener): Boolean {
        val saved = super.save(imageUrl, imageStream, listener)
        rememberUsage(imageUrl)
        return saved
    }

    override fun save(imageUrl: String, bitmap: Bitmap): Boolean {
        val saved = super.save(imageUrl, bitmap)
        rememberUsage(imageUrl)
        return saved
    }

    override fun remove(imageUrl: String): Boolean {
        loadingDates.remove(getFile(imageUrl))
        return super.remove(imageUrl)
    }

    override fun clear() {
        super.clear()
        loadingDates.clear()
    }

    private fun rememberUsage(imageUrl: String) {
        val file: File = getFile(imageUrl)
        val currentTime = System.currentTimeMillis()
        file.setLastModified(currentTime)
        loadingDates.put(file, currentTime)
    }

}