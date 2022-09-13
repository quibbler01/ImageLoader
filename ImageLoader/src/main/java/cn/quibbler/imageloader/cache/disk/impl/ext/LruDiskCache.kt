package cn.quibbler.imageloader.cache.disk.impl.ext

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.disk.DiskCache
import cn.quibbler.imageloader.cache.disk.naming.FileNameGenerator
import cn.quibbler.imageloader.utils.CopyListener
import cn.quibbler.imageloader.utils.L
import cn.quibbler.imageloader.utils.closeSilently
import cn.quibbler.imageloader.utils.copyStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class LruDiskCache : DiskCache {

    companion object {
        const val DEFAULT_BUFFER_SIZE = 32 * 1024

        val DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.PNG

        const val DEFAULT_COMPRESS_QUALITY = 100

        private const val ERROR_ARG_NULL = " argument must be not null"

        private const val ERROR_ARG_NEGATIVE = " argument must be positive number"
    }

    protected var cache: DiskLruCache? = null
    private var reserveCacheDir: File? = null

    protected val fileNameGenerator: FileNameGenerator

    var bufferSize: Int = DEFAULT_BUFFER_SIZE

    var compressFormat: Bitmap.CompressFormat = DEFAULT_COMPRESS_FORMAT

    var compressQuality: Int = DEFAULT_COMPRESS_QUALITY

    @Throws(IOException::class)
    constructor(cacheDir: File, fileNameGenerator: FileNameGenerator, cacheMaxSize: Long) : this(cacheDir, null, fileNameGenerator, cacheMaxSize, 0)

    @Throws(IOException::class)
    constructor(cacheDir: File?, reserveCacheDir: File?, fileNameGenerator: FileNameGenerator?, cacheMaxSize: Long, cacheMaxFileCount: Int) {
        if (cacheDir == null) {
            throw IllegalArgumentException("cacheDir $ERROR_ARG_NULL")
        }
        if (cacheMaxSize < 0) {
            throw IllegalArgumentException("cacheMaxSize $ERROR_ARG_NEGATIVE")
        }
        if (cacheMaxFileCount < 0) {
            throw IllegalArgumentException("cacheMaxFileCount $ERROR_ARG_NEGATIVE")
        }
        if (fileNameGenerator == null) {
            throw IllegalArgumentException("fileNameGenerator $ERROR_ARG_NULL")
        }

        var cacheMaxSize_ = cacheMaxSize
        if (cacheMaxSize_ == 0L) {
            cacheMaxSize_ = Long.MAX_VALUE
        }
        var cacheMaxFileCount_ = cacheMaxFileCount
        if (cacheMaxFileCount_ == 0) {
            cacheMaxFileCount_ = Int.MAX_VALUE
        }

        this.reserveCacheDir = reserveCacheDir
        this.fileNameGenerator = fileNameGenerator
        initCache(cacheDir, reserveCacheDir, cacheMaxSize_, cacheMaxFileCount_)
    }

    @Throws(IOException::class)
    private fun initCache(cacheDir: File, reserveCacheDir: File?, cacheMaxSize: Long, cacheMaxFileCount: Int) {
        try {
            cache = DiskLruCache.open(cacheDir, 1, 1, cacheMaxSize, cacheMaxFileCount)
        } catch (e: IOException) {
            L.e(e)
            if (reserveCacheDir != null) {
                initCache(reserveCacheDir, null, cacheMaxSize, cacheMaxFileCount)
            }
            if (cache == null) {
                throw e
            }
        }
    }

    override fun getDirectory(): File? = cache?.directory

    override fun get(imageUrl: String): File? {
        var snapshot: DiskLruCache.Snapshot? = null
        try {
            snapshot = cache?.get(getKey(imageUrl))
            return snapshot?.getFile(0)
        } catch (e: IOException) {
            L.e(e)
            return null
        } finally {
            if (snapshot != null)
                snapshot.close()
        }
    }

    override fun save(imageUrl: String, imageStream: InputStream, listener: CopyListener): Boolean {
        val editor = cache?.edit(getKey(imageUrl)) ?: return false
        val os = BufferedOutputStream(editor.newOutputStream(0), bufferSize)
        var copied = false
        try {
            copied = copyStream(imageStream, os, listener, bufferSize)
        } finally {
            closeSilently(os)
            if (copied) {
                editor.commit()
            } else {
                editor.abort()
            }
        }
        return copied
    }

    override fun save(imageUrl: String, bitmap: Bitmap): Boolean {
        val editor = cache?.edit(getKey(imageUrl)) ?: return false

        val os = BufferedOutputStream(editor.newOutputStream(0), bufferSize)
        var savedSuccessfully = false
        try {
            savedSuccessfully = bitmap.compress(compressFormat, compressQuality, os)
        } finally {
            closeSilently(os)
        }
        if (savedSuccessfully) {
            editor.commit()
        } else {
            editor.abort()
        }
        return savedSuccessfully
    }

    override fun remove(imageUrl: String): Boolean {
        try {
            return cache?.remove(getKey(imageUrl)) ?: false
        } catch (e: IOException) {
            L.e(e)
            return false
        }
    }

    override fun close() {
        try {
            cache?.close()
        } catch (e: IOException) {
            L.e(e)
        }
        cache = null
    }

    override fun clear() {
        try {
            cache?.delete()
        } catch (e: IOException) {
            L.e(e)
        }
        try {
            cache?.let {
                initCache(it.directory, reserveCacheDir, it.maxSize, it.maxFileCount)
            }
        } catch (e: IOException) {
            L.e(e)
        }
    }

    private fun getKey(imageUrl: String): String {
        return fileNameGenerator.generate(imageUrl)
    }

}