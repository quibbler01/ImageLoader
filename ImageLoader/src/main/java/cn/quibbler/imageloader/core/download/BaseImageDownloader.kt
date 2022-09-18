package cn.quibbler.imageloader.core.download

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import cn.quibbler.imageloader.core.assist.ContentLengthInputStream
import cn.quibbler.imageloader.utils.closeSilently
import cn.quibbler.imageloader.utils.readAndCloseStream
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * Provides retrieving of {@link InputStream} of image by URI from network or file system or app resources.<br />
 * {@link URLConnection} is used to retrieve image stream from network.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.8.0
 */
class BaseImageDownloader(protected val context: Context, protected val connectTimeout: Int, protected val readTimeout: Int) : ImageDownloader {

    companion object {
        const val DEFAULT_HTTP_CONNECT_TIMEOUT = 5 * 1000 // milliseconds

        const val DEFAULT_HTTP_READ_TIMEOUT = 20 * 1000 // milliseconds

        const val BUFFER_SIZE = 32 * 1024 // 32 Kb

        const val ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%"

        const val MAX_REDIRECT_COUNT = 5

        const val CONTENT_CONTACTS_URI_PREFIX = "content://com.android.contacts/"

        const val ERROR_UNSUPPORTED_SCHEME =
            "UIL doesn't support scheme(protocol) by default [%s]. You should implement this support yourself (BaseImageDownloader.getStreamFromOtherSource(...))"
    }

    constructor(context: Context) : this(context, DEFAULT_HTTP_CONNECT_TIMEOUT, DEFAULT_HTTP_READ_TIMEOUT)

    @Throws(IOException::class)
    override fun getStream(imageUri: String, extra: Any?): InputStream? {
        return when (ImageDownloader.Scheme.ofUri(imageUri)) {
            ImageDownloader.Scheme.HTTP,
            ImageDownloader.Scheme.HTTPS -> {
                getStreamFromNetwork(imageUri, extra)
            }
            ImageDownloader.Scheme.FILE -> {
                getStreamFromFile(imageUri, extra)
            }
            ImageDownloader.Scheme.CONTENT -> {
                getStreamFromContent(imageUri, extra)
            }
            ImageDownloader.Scheme.ASSETS -> {
                getStreamFromAssets(imageUri, extra)
            }
            ImageDownloader.Scheme.DRAWABLE -> {
                getStreamFromDrawable(imageUri, extra)
            }
            ImageDownloader.Scheme.UNKNOWN -> {
                getStreamFromOtherSource(imageUri, extra)
            }
        }
    }

    /**
     * Retrieves {@link InputStream} of image by URI (image is located in the network).
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to {@link DisplayImageOptions.Builder#extraForDownloader(Object)
     *                 DisplayImageOptions.extraForDownloader(Object)}; can be null
     * @return {@link InputStream} of image
     * @throws IOException if some I/O error occurs during network request or if no InputStream could be created for
     *                     URL.
     */
    @Throws(IOException::class)
    protected fun getStreamFromNetwork(imageUri: String, extra: Any?): InputStream {
        var conn = createConnection(imageUri, extra)
        var redirectCount = 0
        while (conn.responseCode / 100 == 3 && redirectCount < MAX_REDIRECT_COUNT) {
            conn = createConnection(conn.getHeaderField("Location"), extra)
            redirectCount++
        }

        var imageStream: InputStream? = null
        try {
            imageStream = conn.inputStream
        } catch (e: IOException) {
            // Read all data to allow reuse connection (http://bit.ly/1ad35PY)
            readAndCloseStream(conn.errorStream)
            throw e
        }
        if (!shouldBeProcessed(conn)) {
            closeSilently(imageStream)
            throw IOException("Image request failed with response code ${conn.responseCode}")
        }

        return ContentLengthInputStream(BufferedInputStream(imageStream, BUFFER_SIZE), conn.contentLength)
    }

    /**
     * @param conn Opened request connection (response code is available)
     * @return <b>true</b> - if data from connection is correct and should be read and processed;
     *         <b>false</b> - if response contains irrelevant data and shouldn't be processed
     * @throws IOException
     */
    @Throws(IOException::class)
    protected fun shouldBeProcessed(conn: HttpURLConnection): Boolean {
        return conn.responseCode == 200
    }

    /**
     * Create {@linkplain HttpURLConnection HTTP connection} for incoming URL
     *
     * @param url   URL to connect to
     * @param extra Auxiliary object which was passed to {@link DisplayImageOptions.Builder#extraForDownloader(Object)
     *              DisplayImageOptions.extraForDownloader(Object)}; can be null
     * @return {@linkplain HttpURLConnection Connection} for incoming URL. Connection isn't established so it still configurable.
     * @throws IOException if some I/O error occurs during network request or if no InputStream could be created for
     *                     URL.
     */
    @Throws(IOException::class)
    protected fun createConnection(url: String, extra: Any?): HttpURLConnection {
        val encodedUrl = Uri.encode(url, ALLOWED_URI_CHARS)
        val conn = URL(encodedUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = connectTimeout
        conn.readTimeout = readTimeout
        return conn
    }

    /**
     * Retrieves {@link InputStream} of image by URI (image is located on the local file system or SD card).
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to {@link DisplayImageOptions.Builder#extraForDownloader(Object)
     *                 DisplayImageOptions.extraForDownloader(Object)}; can be null
     * @return {@link InputStream} of image
     * @throws IOException if some I/O error occurs reading from file system
     */
    @Throws(IOException::class)
    protected fun getStreamFromFile(imageUri: String, extra: Any?): InputStream? {
        val filePath = ImageDownloader.Scheme.FILE.crop(imageUri)
        if (isVideoFileUri(imageUri)) {
            return getVideoThumbnailStream(filePath)
        } else {
            val imageStream = BufferedInputStream(FileInputStream(filePath), BUFFER_SIZE)
            return ContentLengthInputStream(imageStream, File(filePath).length().toInt())
        }
    }

    protected fun getVideoThumbnailStream(filePath: String): InputStream? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            val bitmap = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND)
            bitmap?.let {
                val bos = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.PNG, 0, bos)
                return ByteArrayInputStream(bos.toByteArray())
            }
        }
        return null
    }

    /**
     * Retrieves {@link InputStream} of image by URI (image is accessed using {@link ContentResolver}).
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to {@link DisplayImageOptions.Builder#extraForDownloader(Object)
     *                 DisplayImageOptions.extraForDownloader(Object)}; can be null
     * @return {@link InputStream} of image
     * @throws FileNotFoundException if the provided URI could not be opened
     */
    @Throws(FileNotFoundException::class)
    protected fun getStreamFromContent(imageUri: String, extra: Any?): InputStream? {
        val res = context.contentResolver
        val uri = Uri.parse(imageUri)
        if (isVideoContentUri(uri)) {
            val origId = uri.lastPathSegment?.toLong() ?: 0L
            val bitmap = MediaStore.Video.Thumbnails.getThumbnail(res, origId, MediaStore.Images.Thumbnails.MINI_KIND, null)
            bitmap?.let {
                val bos = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.PNG, 0, bos)
                return ByteArrayInputStream(bos.toByteArray())
            }
        } else if (imageUri.startsWith(CONTENT_CONTACTS_URI_PREFIX)) { // contacts photo
            return getContactPhotoStream(uri)
        }
        return res.openInputStream(uri)
    }

    protected fun getContactPhotoStream(uri: Uri): InputStream {
        val res = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return ContactsContract.Contacts.openContactPhotoInputStream(res, uri, true)
        } else {
            return ContactsContract.Contacts.openContactPhotoInputStream(res, uri)
        }
    }

    /**
     * Retrieves {@link InputStream} of image by URI (image is located in assets of application).
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to {@link DisplayImageOptions.Builder#extraForDownloader(Object)
     *                 DisplayImageOptions.extraForDownloader(Object)}; can be null
     * @return {@link InputStream} of image
     * @throws IOException if some I/O error occurs file reading
     */
    @Throws(IOException::class)
    protected fun getStreamFromAssets(imageUri: String, extra: Any?): InputStream {
        val filePath = ImageDownloader.Scheme.ASSETS.crop(imageUri)
        return context.assets.open(filePath)
    }

    /**
     * Retrieves {@link InputStream} of image by URI (image is located in drawable resources of application).
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to {@link DisplayImageOptions.Builder#extraForDownloader(Object)
     *                 DisplayImageOptions.extraForDownloader(Object)}; can be null
     * @return {@link InputStream} of image
     */
    protected fun getStreamFromDrawable(imageUri: String, extra: Any?): InputStream {
        val drawableIdString = ImageDownloader.Scheme.DRAWABLE.crop(imageUri)
        val drawableId = drawableIdString.toInt()
        return context.resources.openRawResource(drawableId)
    }

    /**
     * Retrieves {@link InputStream} of image by URI from other source with unsupported scheme. Should be overriden by
     * successors to implement image downloading from special sources.<br />
     * This method is called only if image URI has unsupported scheme. Throws {@link UnsupportedOperationException} by
     * default.
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to {@link DisplayImageOptions.Builder#extraForDownloader(Object)
     *                 DisplayImageOptions.extraForDownloader(Object)}; can be null
     * @return {@link InputStream} of image
     * @throws IOException                   if some I/O error occurs
     * @throws UnsupportedOperationException if image URI has unsupported scheme(protocol)
     */
    @Throws(IOException::class)
    protected fun getStreamFromOtherSource(imageUri: String, extra: Any?): InputStream {
        throw UnsupportedOperationException(String.format(ERROR_UNSUPPORTED_SCHEME, imageUri))
    }

    private fun isVideoContentUri(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType != null && mimeType.startsWith("video/")
    }

    private fun isVideoFileUri(uri: String): Boolean {
        val extension = MimeTypeMap.getFileExtensionFromUrl(Uri.encode(uri))
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return mimeType != null && mimeType.startsWith("video/")
    }

}