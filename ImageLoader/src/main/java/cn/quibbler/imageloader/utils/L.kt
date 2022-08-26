package cn.quibbler.imageloader.utils

import android.util.Log
import cn.quibbler.imageloader.core.ImageLoader

object L {

    const val LOG_FORMAT = "%1\$s\n%2\$s"

    @Volatile
    private var writeDebugLogs = false

    @Volatile
    private var writeLogs = true

    fun enableLogging() {
        writeLogs(true)
    }

    fun disableLogging() {
        writeLogs(false)
    }

    fun writeDebugLogs(writeDebugLogs: Boolean) {
        L.writeDebugLogs = writeDebugLogs
    }

    fun writeLogs(writeLogs: Boolean) {
        L.writeLogs = writeLogs
    }

    fun d(message: String, vararg args: Any) {
        if (writeDebugLogs) {
            log(Log.DEBUG, null, message, args)
        }
    }

    fun i(message: String, vararg args: Any) {
        log(Log.INFO, null, message, args)
    }

    fun w(message: String, vararg args: Any) {
        log(Log.WARN, null, message, args)
    }

    fun e(ex: Throwable) {
        log(Log.ERROR, ex, null)
    }

    fun e(message: String, vararg args: Any) {
        log(Log.ERROR, null, message, args)
    }

    fun e(ex: Throwable, message: String, vararg args: Any) {
        log(Log.ERROR, ex, message, args)
    }

    private fun log(priority: Int, ex: Throwable?, message: String?, vararg args: Any) {
        if (!writeLogs) return
        var msg: String? = message
        if (message != null && args.isNotEmpty()) {
            msg = String.format(message, *args)
        }
        var log: String? = null
        log = if (ex == null) {
            msg
        } else {
            val logMessage = msg ?: ex.message
            val logBody = Log.getStackTraceString(ex)
            String.format(LOG_FORMAT, logMessage, logBody)
        }
        if (log != null) {
            Log.println(priority, ImageLoader.TAG, log)
        }
    }

}