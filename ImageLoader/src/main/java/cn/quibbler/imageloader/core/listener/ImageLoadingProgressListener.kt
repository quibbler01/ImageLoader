package cn.quibbler.imageloader.core.listener

import android.view.View

/**
 * Listener for image loading progress.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.1
 */
interface ImageLoadingProgressListener {

    /**
     * Is called when image loading progress changed.
     *
     * @param imageUri Image URI
     * @param view     View for image. Can be <b>null</b>.
     * @param current  Downloaded size in bytes
     * @param total    Total size in bytes
     */
    fun onProgressUpdate(imageUri: String, view: View, current: Int, total: Int)

}