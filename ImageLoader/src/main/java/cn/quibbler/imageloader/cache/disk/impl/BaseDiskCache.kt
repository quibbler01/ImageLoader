package cn.quibbler.imageloader.cache.disk.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.disk.DiskCache
import cn.quibbler.imageloader.cache.disk.naming.FileNameGenerator
import cn.quibbler.imageloader.core.DefaultConfigurationFactory
import cn.quibbler.imageloader.utils.CopyListener
import cn.quibbler.imageloader.utils.closeSilently
import cn.quibbler.imageloader.utils.copyStream
import java.io.*
import java.util.*

abstract class BaseDiskCache : DiskCache {

    companion object {
        const val DEFAULT_BUFFER_SIZE = 32 * 1024
        const val DEFAULT_COMPRESS_QUALITY = 100
        val DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.PNG

        private const val ERROR_ARG_NULL = "argument must be not null"
        private const val TEMP_IMAGE_POSTFIX = ".tmp"
    }

    protected val cacheDir: File
    protected val reserveCacheDir: File?
    protected val fileNameGenerator: FileNameGenerator

    var bufferSize: Int = DEFAULT_BUFFER_SIZE
    var compressFormat: Bitmap.CompressFormat = DEFAULT_COMPRESS_FORMAT
    var compressQuality: Int = DEFAULT_COMPRESS_QUALITY

    constructor(cacheDir: File?) : this(cacheDir, null)

    constructor(cacheDir: File?, reserveCacheDir: File?) : this(
        cacheDir,
        reserveCacheDir,
        DefaultConfigurationFactory.createFileNameGenerator()
    )

    constructor(cacheDir: File?, reserveCacheDir: File?, fileNameGenerator: FileNameGenerator?) {
        if (cacheDir == null) {
            throw IllegalArgumentException("cacheDir $ERROR_ARG_NULL")
        }
        if (fileNameGenerator == null) {
            throw IllegalArgumentException("fileNameGenerator $ERROR_ARG_NULL")
        }
        this.cacheDir = cacheDir
        this.reserveCacheDir = reserveCacheDir
        this.fileNameGenerator = fileNameGenerator
    }

    override fun getDirectory(): File = cacheDir

    override fun get(imageUrl: String): File? = getFile(imageUrl)

    override fun save(
        imageUrl: String,
        imageStream: InputStream,
        listener: CopyListener
    ): Boolean {
        val imageFile: File = getFile(imageUrl)
        val tmpFile: File = File(imageFile.absolutePath + TEMP_IMAGE_POSTFIX)
        var loaded = false

        try {
            val os: OutputStream = BufferedOutputStream(FileOutputStream(tmpFile), bufferSize)
            try {
                loaded = copyStream(imageStream, os, listener, bufferSize)
            } finally {
                closeSilently(os)
            }
        } finally {
            if (loaded && !tmpFile.renameTo(imageFile)) {
                loaded = false
            }
            if (!loaded) {
                tmpFile.delete()
            }
        }

        return loaded
    }

    override fun save(imageUrl: String, bitmap: Bitmap): Boolean {
        val imageFile: File = getFile(imageUrl)
        val tmpFile: File = File(imageFile.absolutePath + TEMP_IMAGE_POSTFIX)
        val os = BufferedOutputStream(FileOutputStream(tmpFile), bufferSize)
        var savedSuccessfully: Boolean = false
        try {
            savedSuccessfully = bitmap.compress(compressFormat, compressQuality, os)
        } finally {
            closeSilently(os)
            if (savedSuccessfully && !tmpFile.renameTo(imageFile)) {
                savedSuccessfully = false
            }
            if (!savedSuccessfully) {
                tmpFile.delete()
            }
        }
        return savedSuccessfully
    }

    override fun remove(imageUrl: String) = getFile(imageUrl).delete()

    override fun close() {
        // Nothing to do
    }

    override fun clear() {
        val fileList = cacheDir.listFiles()
        fileList?.let {
            for (file in it) {
                file.delete()
            }
        }
    }

    /**
     * Returns file object (not null) for incoming image URI. File object can reference to non-existing file.
     * @param imageUri String
     * @return File
     */
    protected fun getFile(imageUri: String): File {
        val fileName: String = fileNameGenerator.generate(imageUri)
        var dir: File = cacheDir
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            if (reserveCacheDir != null && (reserveCacheDir.exists() || reserveCacheDir.mkdirs())) {
                dir = reserveCacheDir
            }
        }
        return File(dir, fileName)
    }

}