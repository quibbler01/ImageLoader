package cn.quibbler.imageloader.core.listener

import android.graphics.Bitmap
import android.view.View
import cn.quibbler.imageloader.core.assist.FailReason

class SimpleImageLoadingListener : ImageLoadingListener {
    override fun onLoadingStarted(imageUri: String, view: View) {
        // Empty implementation
    }

    override fun onLoadingFailed(imageUri: String, view: View, failReason: FailReason) {
        // Empty implementation
    }

    override fun onLoadingComplete(imageUri: String, view: View, loadedImage: Bitmap) {
        // Empty implementation
    }

    override fun onLoadingCancelled(imageUri: String, view: View) {
        // Empty implementation
    }

}