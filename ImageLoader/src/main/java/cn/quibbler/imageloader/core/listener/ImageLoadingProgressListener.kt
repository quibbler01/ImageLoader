package cn.quibbler.imageloader.core.listener

import android.view.View

interface ImageLoadingProgressListener {

    fun onProgressUpdate(imageUri: String, view: View, current: Int, total: Int)

}