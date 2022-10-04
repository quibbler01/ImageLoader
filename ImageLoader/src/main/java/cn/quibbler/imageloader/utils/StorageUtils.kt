package cn.quibbler.imageloader.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import java.io.File
import java.io.IOException

private const val EXTERNAL_STORAGE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE"

private const val INDIVIDUAL_DIR_NAME = "uil-images"

/**
 * Returns application cache directory. Cache directory will be created on SD card
 * <i>("/Android/data/[app_package_name]/cache")</i> if card is mounted and app has appropriate permission. Else -
 * Android defines cache directory on device's file system.
 *
 * @param context Application context
 * @return Cache {@link File directory}.<br />
 * <b>NOTE:</b> Can be null in some unpredictable cases (if SD card is unmounted and
 * {@link android.content.Context#getCacheDir() Context.getCacheDir()} returns null).
 */
fun getCacheDirectory(context: Context): File? = getCacheDirectory(context, true)

/**
 * Returns application cache directory. Cache directory will be created on SD card
 * <i>("/Android/data/[app_package_name]/cache")</i> (if card is mounted and app has appropriate permission) or
 * on device's file system depending incoming parameters.
 *
 * @param context        Application context
 * @param preferExternal Whether prefer external location for cache
 * @return Cache {@link File directory}.<br />
 * <b>NOTE:</b> Can be null in some unpredictable cases (if SD card is unmounted and
 * {@link android.content.Context#getCacheDir() Context.getCacheDir()} returns null).
 */
fun getCacheDirectory(context: Context, preferExternal: Boolean): File? {
    var appCacheDir: File? = null
    var externalStorageState: String? = null
    try {
        externalStorageState = Environment.getExternalStorageState()
    } catch (e: NullPointerException) { // (sh)it happens (Issue #660)
        externalStorageState = ""
    } catch (e: IncompatibleClassChangeError) { // (sh)it happens too (Issue #989)
        externalStorageState = ""
    }
    if (preferExternal && Environment.MEDIA_MOUNTED.equals(externalStorageState) && hasExternalStoragePermission(context)) {
        appCacheDir = getExternalCacheDir(context)
    }
    if (appCacheDir == null) {
        appCacheDir = context.cacheDir
    }
    if (appCacheDir == null) {
        val cacheDirPath = "/data/data/${context.packageName}/cache/"
        L.w("Can't define system cache directory! '%s' will be used.", cacheDirPath)
        appCacheDir = File(cacheDirPath)
    }

    return appCacheDir
}

/**
 * Returns individual application cache directory (for only image caching from ImageLoader). Cache directory will be
 * created on SD card <i>("/Android/data/[app_package_name]/cache/uil-images")</i> if card is mounted and app has
 * appropriate permission. Else - Android defines cache directory on device's file system.
 *
 * @param context Application context
 * @return Cache {@link File directory}
 */
fun getIndividualCacheDirectory(context: Context): File = getIndividualCacheDirectory(context, INDIVIDUAL_DIR_NAME)

/**
 * Returns individual application cache directory (for only image caching from ImageLoader). Cache directory will be
 * created on SD card <i>("/Android/data/[app_package_name]/cache/uil-images")</i> if card is mounted and app has
 * appropriate permission. Else - Android defines cache directory on device's file system.
 *
 * @param context Application context
 * @param cacheDir Cache directory path (e.g.: "AppCacheDir", "AppDir/cache/images")
 * @return Cache {@link File directory}
 */
fun getIndividualCacheDirectory(context: Context, cacheDir: String): File {
    var appCacheDir: File? = getCacheDirectory(context)
    var individualCacheDir: File = File(appCacheDir, cacheDir)
    if (!individualCacheDir.exists()) {
        if (!individualCacheDir.mkdir()) {
            appCacheDir?.let {
                individualCacheDir = it
            }
        }
    }
    return individualCacheDir
}

/**
 * Returns specified application cache directory. Cache directory will be created on SD card by defined path if card
 * is mounted and app has appropriate permission. Else - Android defines cache directory on device's file system.
 *
 * @param context  Application context
 * @param cacheDir Cache directory path (e.g.: "AppCacheDir", "AppDir/cache/images")
 * @return Cache {@link File directory}
 */
fun getOwnCacheDirectory(context: Context, cacheDir: String): File? {
    var appCacheDir: File? = null
    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && hasExternalStoragePermission(context)) {
        appCacheDir = File(Environment.getExternalStorageDirectory(), cacheDir)
    }
    if (appCacheDir == null || (!appCacheDir.exists() && !appCacheDir.mkdirs())) {
        appCacheDir = context.cacheDir
    }
    return appCacheDir
}

/**
 * Returns specified application cache directory. Cache directory will be created on SD card by defined path if card
 * is mounted and app has appropriate permission. Else - Android defines cache directory on device's file system.
 *
 * @param context  Application context
 * @param cacheDir Cache directory path (e.g.: "AppCacheDir", "AppDir/cache/images")
 * @return Cache {@link File directory}
 */
fun getOwnCacheDirectory(context: Context,cacheDir: String,preferExternal:Boolean): File? {
    var appCacheDir: File? = null
    if (preferExternal && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        appCacheDir = File(Environment.getExternalStorageDirectory(), cacheDir)
    }
    if (appCacheDir == null || (!appCacheDir.exists() && !appCacheDir.mkdirs())) {
        appCacheDir = context.cacheDir
    }
    return appCacheDir
}

fun getExternalCacheDir(context: Context): File? {
    val dataDir: File = File(File(Environment.getExternalStorageDirectory(), "Android"), "data")
    val appCacheDir: File = File(File(dataDir, context.packageName), "cache")
    if (!appCacheDir.exists()) {
        if (!appCacheDir.mkdir()) {
            L.w("Unable to create external cache directory")
            return null
        }
        try {
            File(appCacheDir, ".nomedia").createNewFile()
        } catch (e: IOException) {
            L.i("Can't create \".nomedia\" file in application external cache directory")
        }
    }
    return appCacheDir
}

fun hasExternalStoragePermission(context: Context): Boolean {
    return context.checkCallingOrSelfPermission(EXTERNAL_STORAGE_PERMISSION) == PackageManager.PERMISSION_GRANTED
}