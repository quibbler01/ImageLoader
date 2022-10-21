package cn.quibbler.imageloader.core.listener

import android.graphics.Bitmap
import android.view.View
import cn.quibbler.imageloader.core.assist.FailReason

/**
 * A convenient class to extend when you only want to listen for a subset of all the image loading events. This
 * implements all methods in the {@link com.nostra13.universalimageloader.core.listener.ImageLoadingListener} but does
 * nothing.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.4.0
 */
class SimpleImageLoadingListener : ImageLoadingListener {
    override fun onLoadingStarted(imageUri: String, view: View) {
        // Empty implementation
    }

    override fun onLoadingFailed(imageUri: String, view: View?, failReason: FailReason) {
        // Empty implementation
    }

    override fun onLoadingComplete(imageUri: String, view: View?, loadedImage: Bitmap?) {
        // Empty implementation
    }

    override fun onLoadingCancelled(imageUri: String, view: View?) {
        // Empty implementation
    }

}