package cn.quibbler.imageloader.core.listener

import android.graphics.Bitmap
import android.view.View
import cn.quibbler.imageloader.core.assist.FailReason

interface ImageLoadingListener {

    fun onLoadingStarted(imageUri: String, view: View)

    fun onLoadingFailed(imageUri: String, view: View, failReason: FailReason)

    fun onLoadingComplete(imageUri: String, view: View, loadedImage: Bitmap)

    fun onLoadingCancelled(imageUri: String, view: View)

}