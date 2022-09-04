package cn.quibbler.imageloader.core.decode

import android.graphics.Bitmap
import java.io.IOException

interface ImageDecoder {

    @Throws(IOException::class)
    fun decode(imageDecodingInfo: ImageDecodingInfo): Bitmap

}