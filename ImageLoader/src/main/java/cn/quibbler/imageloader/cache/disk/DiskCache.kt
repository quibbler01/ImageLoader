package cn.quibbler.imageloader.cache.disk

import android.graphics.Bitmap
import cn.quibbler.imageloader.utils.CopyListener
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 *
 */
interface DiskCache {

    /**
     * Returns root directory of disk cache
     *
     * @return Root directory of disk cache
     */
    fun getDirectory(): File?

    /**
     * Returns file of cached image
     *
     * @param imageUri Original image URI
     * @return File of cached image or <b>null</b> if image wasn't cached
     */
    fun get(imageUrl: String): File?

    /**
     * Saves image stream in disk cache.
     * Incoming image stream shouldn't be closed in this method.
     *
     * @param imageUri    Original image URI
     * @param imageStream Input stream of image (shouldn't be closed in this method)
     * @param listener    Listener for saving progress, can be ignored if you don't use
     *                    {@linkplain com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener
     *                    progress listener} in ImageLoader calls
     * @return <b>true</b> - if image was saved successfully; <b>false</b> - if image wasn't saved in disk cache.
     * @throws java.io.IOException
     */
    @Throws(IOException::class)
    fun save(imageUrl: String, imageStream: InputStream?, listener: CopyListener?): Boolean

    /**
     * Saves image bitmap in disk cache.
     *
     * @param imageUri Original image URI
     * @param bitmap   Image bitmap
     * @return <b>true</b> - if bitmap was saved successfully; <b>false</b> - if bitmap wasn't saved in disk cache.
     * @throws IOException
     */
    fun save(imageUrl: String, bitmap: Bitmap?): Boolean

    /**
     * Removes image file associated with incoming URI
     *
     * @param imageUri Image URI
     * @return <b>true</b> - if image file is deleted successfully; <b>false</b> - if image file doesn't exist for
     * incoming URI or image file can't be deleted.
     */
    fun remove(imageUrl: String): Boolean

    /**
     * Closes disk cache, releases resources.
     */
    fun close()

    /**
     *  Clears disk cache.
     */
    fun clear()

}