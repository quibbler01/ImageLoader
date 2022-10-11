package cn.quibbler.imageloader.core.decode

import android.graphics.Bitmap
import java.io.IOException

/**
 * Provide decoding image to result {@link Bitmap}.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageDecodingInfo
 * @since 1.8.3
 */
interface ImageDecoder {

    /**
     * Decodes image to {@link Bitmap} according target size and other parameters.
     *
     * @param imageDecodingInfo
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun decode(imageDecodingInfo: ImageDecodingInfo): Bitmap?

}