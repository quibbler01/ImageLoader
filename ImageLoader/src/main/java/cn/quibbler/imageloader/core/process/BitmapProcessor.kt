package cn.quibbler.imageloader.core.process

import android.graphics.Bitmap

/**
 * Makes some processing on [Bitmap]. Implementations can apply any changes to original {@link Bitmap}.<br />
 * Implementations have to be thread-safe.
 */
interface BitmapProcessor {

    /**
     * Makes some processing of incoming bitmap.<br />
     * This method is executing on additional thread (not on UI thread).<br />
     * <b>Note:</b> If this processor is used as {@linkplain DisplayImageOptions.Builder#preProcessor(BitmapProcessor)
     * pre-processor} then don't forget {@linkplain Bitmap#recycle() to recycle} incoming bitmap if you return a new
     * created one.
     *
     * @param bitmap Original [Bitmap]
     * @return Processed [Bitmap]
     */
    fun process(bitmap: Bitmap): Bitmap?

}