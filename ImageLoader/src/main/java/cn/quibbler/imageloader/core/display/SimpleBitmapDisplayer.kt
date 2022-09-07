package cn.quibbler.imageloader.core.display

import android.graphics.Bitmap
import cn.quibbler.imageloader.core.assist.LoadedFrom
import cn.quibbler.imageloader.core.imageaware.ImageAware

class SimpleBitmapDisplayer : BitmapDisplayer {

    override fun display(bitmap: Bitmap, imageAware: ImageAware, loadedFrom: LoadedFrom) {
        imageAware.setImageBitmap(bitmap)
    }

}