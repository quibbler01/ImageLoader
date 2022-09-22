package cn.quibbler.imageloader.core

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Handler
import cn.quibbler.imageloader.core.assist.ImageScaleType
import cn.quibbler.imageloader.core.display.BitmapDisplayer
import cn.quibbler.imageloader.core.process.BitmapProcessor

/**
 * Contains options for image display. Defines:
 * <ul>
 * <li>whether stub image will be displayed in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
 * image aware view} during image loading</li>
 * <li>whether stub image will be displayed in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
 * image aware view} if empty URI is passed</li>
 * <li>whether stub image will be displayed in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
 * image aware view} if image loading fails</li>
 * <li>whether {@link com.nostra13.universalimageloader.core.imageaware.ImageAware image aware view} should be reset
 * before image loading start</li>
 * <li>whether loaded image will be cached in memory</li>
 * <li>whether loaded image will be cached on disk</li>
 * <li>image scale type</li>
 * <li>decoding options (including bitmap decoding configuration)</li>
 * <li>delay before loading of image</li>
 * <li>whether consider EXIF parameters of image</li>
 * <li>auxiliary object which will be passed to {@link ImageDownloader#getStream(String, Object) ImageDownloader}</li>
 * <li>pre-processor for image Bitmap (before caching in memory)</li>
 * <li>post-processor for image Bitmap (after caching in memory, before displaying)</li>
 * <li>how decoded {@link Bitmap} will be displayed</li>
 * </ul>
 * <p/>
 * You can create instance:
 * <ul>
 * <li>with {@link Builder}:<br />
 * <b>i.e.</b> :
 * <code>new {@link DisplayImageOptions}.Builder().{@link Builder#cacheInMemory() cacheInMemory()}.
 * {@link Builder#showImageOnLoading(int) showImageOnLoading()}.{@link Builder#build() build()}</code><br />
 * </li>
 * <li>or by static method: {@link #createSimple()}</li> <br />
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
class DisplayImageOptions(builder: Builder) {

    companion object {

        /**
         * Creates options appropriate for single displaying:
         * <ul>
         * <li>View will <b>not</b> be reset before loading</li>
         * <li>Loaded image will <b>not</b> be cached in memory</li>
         * <li>Loaded image will <b>not</b> be cached on disk</li>
         * <li>{@link ImageScaleType#IN_SAMPLE_POWER_OF_2} decoding type will be used</li>
         * <li>{@link Bitmap.Config#ARGB_8888} bitmap config will be used for image decoding</li>
         * <li>{@link SimpleBitmapDisplayer} will be used for image displaying</li>
         * </ul>
         * <p/>
         * These option are appropriate for simple single-use image (from drawables or from Internet) displaying.
         */
        fun createSimple(): DisplayImageOptions {
            return Builder().build()
        }

    }

    private val imageResOnLoading: Int
    private val imageResForEmptyUri: Int
    private val imageResOnFail: Int
    private val imageOnLoading: Drawable?
    private val imageForEmptyUri: Drawable?
    private val imageOnFail: Drawable?
    private val resetViewBeforeLoading: Boolean
    private val cacheInMemory: Boolean
    val cacheOnDisk: Boolean
    val imageScaleType: ImageScaleType
    val decodingOptions: BitmapFactory.Options
    val delayBeforeLoading: Int
    val considerExifParams: Boolean
    val extraForDownloader: Any
    private val preProcessor: BitmapProcessor?
    val postProcessor: BitmapProcessor?
    val displayer: BitmapDisplayer
    private val handler: Handler
    val isSyncLoading: Boolean

    init {
        imageResOnLoading = builder.imageResOnLoading
        imageResForEmptyUri = builder.imageResForEmptyUri
        imageResOnFail = builder.imageResOnFail
        imageOnLoading = builder.imageOnLoading
        imageForEmptyUri = builder.imageForEmptyUri
        imageOnFail = builder.imageOnFail
        resetViewBeforeLoading = builder.resetViewBeforeLoading
        cacheInMemory = builder.cacheInMemory
        cacheOnDisk = builder.cacheOnDisk
        imageScaleType = builder.imageScaleType
        decodingOptions = builder.decodingOptions
        delayBeforeLoading = builder.delayBeforeLoading
        considerExifParams = builder.considerExifParams
        extraForDownloader = builder.extraForDownloader!!
        preProcessor = builder.preProcessor
        postProcessor = builder.postProcessor
        displayer = builder.displayer
        handler = builder.handler!!
        isSyncLoading = builder.isSyncLoading
    }

    fun shouldShowImageOnLoading(): Boolean = imageOnLoading != null && imageResOnLoading != 0

    fun shouldShowImageForEmptyUri(): Boolean = imageForEmptyUri != null && imageResForEmptyUri != 0

    fun shouldShowImageOnFail(): Boolean = imageOnFail != null && imageResOnFail != 0

    fun shouldPreProcess(): Boolean = preProcessor != null

    fun shouldPostProcess(): Boolean = postProcessor != null;

    fun shouldDelayBeforeLoading(): Boolean = delayBeforeLoading > 0

    fun getImageOnLoading(res: Resources): Drawable? {
        return if (imageResOnLoading != 0) {
            res.getDrawable(imageResOnLoading, null)
        } else {
            imageOnLoading
        }
    }

    fun getImageForEmptyUri(res: Resources): Drawable? = if (imageResForEmptyUri != 0) {
        res.getDrawable(imageResForEmptyUri, null)
    } else {
        imageForEmptyUri
    }

    fun getImageOnFail(res: Resources): Drawable? = if (imageResOnFail != 0) {
        res.getDrawable(imageResOnFail, null)
    } else {
        imageOnFail
    }

    /**
     * Builder for {@link DisplayImageOptions}
     *
     * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
     */
    class Builder {
        var imageResOnLoading = 0
        var imageResForEmptyUri = 0
        var imageResOnFail = 0
        var imageOnLoading: Drawable? = null
        var imageForEmptyUri: Drawable? = null
        var imageOnFail: Drawable? = null
        var resetViewBeforeLoading = false
        var cacheInMemory = false
        var cacheOnDisk = false
        var imageScaleType = ImageScaleType.IN_SAMPLE_POWER_OF_2
        var decodingOptions = BitmapFactory.Options()
        var delayBeforeLoading = 0
        var considerExifParams = false
        var extraForDownloader: Any? = null
        var preProcessor: BitmapProcessor? = null
        var postProcessor: BitmapProcessor? = null
        var displayer = DefaultConfigurationFactory.createBitmapDisplayer()
        var handler: Handler? = null
        var isSyncLoading = false

        /**
         * Stub image will be displayed in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
         * image aware view} during image loading
         *
         * @param imageRes Stub image resource
         */
        @Deprecated("Use {@link #showImageOnLoading(int)} instead")
        fun showStubImage(imageRes: Int): Builder {
            imageResOnLoading = imageRes
            return this
        }

        /**
         * Incoming image will be displayed in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
         * image aware view} during image loading
         *
         * @param imageRes Image resource
         */
        fun showImageOnLoading(imageRes: Int): Builder {
            imageResOnLoading = imageRes
            return this
        }

        /**
         * Incoming drawable will be displayed in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
         * image aware view} during image loading.
         * This option will be ignored if {@link DisplayImageOptions.Builder#showImageOnLoading(int)} is set.
         */
        fun showImageOnLoading(drawable: Drawable): Builder {
            imageOnLoading = drawable
            return this
        }

        /**
         * Incoming image will be displayed in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
         * image aware view} if empty URI (null or empty
         * string) will be passed to <b>ImageLoader.displayImage(...)</b> method.
         *
         * @param imageRes Image resource
         */
        fun showImageForEmptyUri(imageRes: Int): Builder {
            imageResForEmptyUri = imageRes
            return this
        }

        /**
         * Incoming drawable will be displayed in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
         * image aware view} if empty URI (null or empty
         * string) will be passed to <b>ImageLoader.displayImage(...)</b> method.
         * This option will be ignored if {@link DisplayImageOptions.Builder#showImageForEmptyUri(int)} is set.
         */
        fun showImageForEmptyUri(drawable: Drawable): Builder {
            imageForEmptyUri = drawable
            return this
        }

        /**
         * Incoming image will be displayed in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
         * image aware view} if some error occurs during
         * requested image loading/decoding.
         *
         * @param imageRes Image resource
         */
        fun showImageOnFail(imageRes: Int): Builder {
            imageResOnFail = imageResOnLoading
            return this
        }

        /**
         * Incoming drawable will be displayed in [ image aware view][com.nostra13.universalimageloader.core.imageaware.ImageAware] if some error occurs during
         * requested image loading/decoding.
         * This option will be ignored if [DisplayImageOptions.Builder.showImageOnFail] is set.
         */
        fun showImageOnFail(drawable: Drawable): Builder {
            imageOnFail = drawable
            return this
        }

        /**
         * {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
         * image aware view} will be reset (set <b>null</b>) before image loading start
         *
         * @deprecated Use {@link #resetViewBeforeLoading(boolean) resetViewBeforeLoading(true)} instead
         */
        fun resetViewBeforeLoading(): Builder {
            resetViewBeforeLoading = true
            return this
        }

        /**
         * Sets whether {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
         * image aware view} will be reset (set <b>null</b>) before image loading start
         */
        fun resetViewBeforeLoading(resetViewBeforeLoading: Boolean): Builder {
            this.resetViewBeforeLoading = resetViewBeforeLoading
            return this
        }

        /**
         * Loaded image will be cached in memory
         *
         */
        @Deprecated("Use {@link #cacheInMemory(boolean) cacheInMemory(true)} instead")
        fun cacheInMemory(): Builder {
            cacheInMemory = true
            return this
        }

        /** Sets whether loaded image will be cached in memory */
        fun cacheInMemory(cacheInMemory: Boolean): Builder {
            this.cacheInMemory = cacheInMemory
            return this
        }

        /**
         * Loaded image will be cached on disk
         *
         */
        @Deprecated("Use {@link #cacheOnDisk(boolean) cacheOnDisk(true)} instead")
        fun cacheOnDisc(): Builder {
            return cacheOnDisk(true)
        }

        /**
         * Sets whether loaded image will be cached on disk
         *
         */
        @Deprecated("Use {@link #cacheOnDisk(boolean)} instead")
        fun cacheOnDisc(cacheOnDisk: Boolean): Builder {
            return cacheOnDisk(cacheOnDisk)
        }

        fun cacheOnDisk(cacheOnDisk: Boolean): Builder {
            this.cacheOnDisk = cacheOnDisk
            return this
        }

        /**
         * Sets {@linkplain ImageScaleType scale type} for decoding image. This parameter is used while define scale
         * size for decoding image to Bitmap. Default value - {@link ImageScaleType#IN_SAMPLE_POWER_OF_2}
         */
        fun imageScaleType(imageScaleType: ImageScaleType): Builder {
            this.imageScaleType = imageScaleType
            return this
        }

        /** Sets {@link Bitmap.Config bitmap config} for image decoding. Default value - {@link Bitmap.Config#ARGB_8888} */
        fun bitmapConfig(bitmapConfig: Bitmap.Config?): Builder {
            if (bitmapConfig == null) throw IllegalArgumentException("bitmapConfig can't be null")
            decodingOptions.inPreferredConfig = bitmapConfig
            return this
        }

        /**
         * Sets options for image decoding.<br />
         * <b>NOTE:</b> {@link Options#inSampleSize} of incoming options will <b>NOT</b> be considered. Library
         * calculate the most appropriate sample size itself according yo {@link #imageScaleType(ImageScaleType)}
         * options.<br />
         * <b>NOTE:</b> This option overlaps {@link #bitmapConfig(android.graphics.Bitmap.Config) bitmapConfig()}
         * option.
         */
        fun decodingOptions(decodingOptions: BitmapFactory.Options?): Builder {
            if (decodingOptions == null) throw IllegalArgumentException("decodingOptions can't be null")
            this.decodingOptions = decodingOptions
            return this
        }

        /** Sets delay time before starting loading task. Default - no delay. */
        fun delayBeforeLoading(delayInMillis: Int): Builder {
            this.delayBeforeLoading = delayInMillis
            return this
        }

        /** Sets auxiliary object which will be passed to {@link ImageDownloader#getStream(String, Object)} */
        fun extraForDownloader(extra: Any?): Builder {
            this.extraForDownloader = extra
            return this
        }

        /** Sets whether ImageLoader will consider EXIF parameters of JPEG image (rotate, flip) */
        fun considerExifParams(considerExifParams: Boolean): Builder {
            this.considerExifParams = considerExifParams
            return this
        }

        /**
         * Sets bitmap processor which will be process bitmaps before they will be cached in memory. So memory cache
         * will contain bitmap processed by incoming preProcessor.<br />
         * Image will be pre-processed even if caching in memory is disabled.
         */
        fun preProcessor(preProcessor: BitmapProcessor): Builder {
            this.preProcessor = preProcessor
            return this
        }

        /**
         * Sets bitmap processor which will be process bitmaps before they will be displayed in
         * {@link com.nostra13.universalimageloader.core.imageaware.ImageAware image aware view} but
         * after they'll have been saved in memory cache.
         */
        fun postProcessor(postProcessor: BitmapProcessor): Builder {
            this.postProcessor = postProcessor
            return this
        }

        /**
         * Sets custom {@link BitmapDisplayer displayer} for image loading task. Default value -
         * {@link DefaultConfigurationFactory#createBitmapDisplayer()}
         */
        fun displayer(displayer: BitmapDisplayer?): Builder {
            if (displayer == null) throw IllegalArgumentException("displayer can't be null")
            this.displayer = displayer
            return this
        }

        fun syncLoading(isSyncLoading: Boolean): Builder {
            this.isSyncLoading = isSyncLoading
            return this
        }

        /**
         * Sets custom {@linkplain Handler handler} for displaying images and firing {@linkplain ImageLoadingListener
         * listener} events.
         */
        fun handler(handler: Handler): Builder {
            this.handler = handler
            return this
        }

        /** Sets all options equal to incoming options */
        fun cloneFrom(options: DisplayImageOptions): Builder {
            imageResOnLoading = options.imageResOnLoading
            imageResForEmptyUri = options.imageResForEmptyUri
            imageResOnFail = options.imageResOnFail
            imageOnLoading = options.imageOnLoading
            imageForEmptyUri = options.imageForEmptyUri
            imageOnFail = options.imageOnFail
            resetViewBeforeLoading = options.resetViewBeforeLoading
            cacheInMemory = options.cacheInMemory
            cacheOnDisk = options.cacheOnDisk
            imageScaleType = options.imageScaleType
            decodingOptions = options.decodingOptions
            delayBeforeLoading = options.delayBeforeLoading
            considerExifParams = options.considerExifParams
            extraForDownloader = options.extraForDownloader
            preProcessor = options.preProcessor
            postProcessor = options.postProcessor
            displayer = options.displayer
            handler = options.handler
            isSyncLoading = options.isSyncLoading
            return this
        }

        /** Builds configured {@link DisplayImageOptions} object */
        fun build(): DisplayImageOptions {
            return DisplayImageOptions(this)
        }

    }

}