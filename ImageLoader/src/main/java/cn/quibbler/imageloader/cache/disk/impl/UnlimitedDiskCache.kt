package cn.quibbler.imageloader.cache.disk.impl

import android.graphics.Bitmap
import cn.quibbler.imageloader.cache.disk.naming.FileNameGenerator
import cn.quibbler.imageloader.utils.CopyListener
import java.io.File
import java.io.InputStream

class UnlimitedDiskCache : BaseDiskCache {

    constructor(cacheDir: File) : super(cacheDir)

    constructor(cacheDir: File?, reserveCacheDir: File?) : super(cacheDir, reserveCacheDir)

    constructor(
        cacheDir: File?,
        reserveCacheDir: File?,
        fileNameGenerator: FileNameGenerator?
    ) : super(cacheDir, reserveCacheDir, fileNameGenerator)

}