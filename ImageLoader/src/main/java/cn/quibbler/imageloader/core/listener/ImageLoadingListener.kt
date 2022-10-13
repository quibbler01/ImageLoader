package cn.quibbler.imageloader.core.listener

import android.graphics.Bitmap
import android.view.View
import cn.quibbler.imageloader.core.assist.FailReason

/**
 * Listener for image loading process.<br />
 * You can use {@link SimpleImageLoadingListener} for implementing only needed methods.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see SimpleImageLoadingListener
 * @see com.nostra13.universalimageloader.core.assist.FailReason
 * @since 1.0.0
 */
interface ImageLoadingListener {

    /**
     * Is called when image loading task was started
     *
     * @param imageUri Loading image URI
     * @param view     View for image
     */
    fun onLoadingStarted(imageUri: String, view: View)

    /**
     * Is called when an error was occurred during image loading
     *
     * @param imageUri   Loading image URI
     * @param view       View for image. Can be <b>null</b>.
     * @param failReason {@linkplain com.nostra13.universalimageloader.core.assist.FailReason The reason} why image
     *                   loading was failed
     */
    fun onLoadingFailed(imageUri: String, view: View?, failReason: FailReason)

    /**
     * Is called when image is loaded successfully (and displayed in View if one was specified)
     *
     * @param imageUri    Loaded image URI
     * @param view        View for image. Can be <b>null</b>.
     * @param loadedImage Bitmap of loaded and decoded image
     */
    fun onLoadingComplete(imageUri: String, view: View?, loadedImage: Bitmap?)

    /**
     * Is called when image loading task was cancelled because View for image was reused in newer task
     *
     * @param imageUri Loading image URI
     * @param view     View for image. Can be <b>null</b>.
     */
    fun onLoadingCancelled(imageUri: String, view: View?)

}