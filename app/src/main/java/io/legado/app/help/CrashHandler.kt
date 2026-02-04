package io.legado.app.help

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Debug
import android.os.Looper
import android.webkit.WebSettings
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppLog
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.model.ReadAloud
import io.legado.app.utils.FileDoc
import io.legado.app.utils.FileUtils
import io.legado.app.utils.createFileIfNotExist
import io.legado.app.utils.createFolderReplace
import io.legado.app.utils.externalCache
import io.legado.app.utils.getFile
import io.legado.app.utils.longToastOnUiLegacy
import io.legado.app.utils.stackTraceStr
import io.legado.app.utils.writeText
import splitties.init.appCtx
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * 异常管理类
 */
class CrashHandler(val context: Context) : Thread.UncaughtExceptionHandler {

    /**
     * System default UncaughtExceptionHandler
     */
    private var mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    init {
        //Set this CrashHandler as system default
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * uncaughtException callback
     */
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (shouldAbsorb(ex)) {
            AppLog.put(appCtx.getString(io.legado.app.R.string.uncaught_exception, ex.localizedMessage), ex)
            Looper.loop()
        } else {
            ReadAloud.stop(context)
            handleException(ex)
            mDefaultHandler?.uncaughtException(thread, ex)
        }
    }

    private fun shouldAbsorb(e: Throwable): Boolean {
        return when {
            e::class.simpleName == "CannotDeliverBroadcastException" -> true
            e is SecurityException && e.message?.contains(
                "nor current process has android.permission.OBSERVE_GRANT_REVOKE_PERMISSIONS",
                true
            ) == true -> true

            else -> false
        }
    }

    /**
     * Handle this exception
     */
    private fun handleException(ex: Throwable?) {
        if (ex == null) return
        LocalConfig.appCrash = true
        //Save log file
        saveCrashInfo2File(ex)
        if ((ex is OutOfMemoryError || ex.cause is OutOfMemoryError) && AppConfig.recordHeapDump) {
            doHeapDump()
        }
        context.longToastOnUiLegacy(ex.stackTraceStr)
        Thread.sleep(3000)
    }

    companion object {
        /**
         * Store exception and parameter info
         */
        private val paramsMap by lazy {
            val map = LinkedHashMap<String, String>()
            kotlin.runCatching {
                //Get system info
                map["MANUFACTURER"] = Build.MANUFACTURER
                map["BRAND"] = Build.BRAND
                map["MODEL"] = Build.MODEL
                map["SDK_INT"] = Build.VERSION.SDK_INT.toString()
                map["RELEASE"] = Build.VERSION.RELEASE
                map["WebViewUserAgent"] = try {
                    WebSettings.getDefaultUserAgent(appCtx)
                } catch (e: Throwable) {
                    e.toString()
                }
                map["packageName"] = appCtx.packageName
                map["heapSize"] = Runtime.getRuntime().maxMemory().toString()
                //Get app version info
                AppConst.appInfo.let {
                    map["versionName"] = it.versionName
                    map["versionCode"] = it.versionCode.toString()
                }
            }
            map
        }

        /**
         * Format time
         */
        @SuppressLint("SimpleDateFormat")
        private val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")

        /**
         * Save error info to file
         */
        fun saveCrashInfo2File(ex: Throwable) {
            val sb = StringBuilder()
            for ((key, value) in paramsMap) {
                sb.append(key).append("=").append(value).append("\n")
            }

            val writer = StringWriter()
            val printWriter = PrintWriter(writer)
            ex.printStackTrace(printWriter)
            var cause: Throwable? = ex.cause
            while (cause != null) {
                cause.printStackTrace(printWriter)
                cause = cause.cause
            }
            printWriter.close()
            val result = writer.toString()
            sb.append(result)
            val crashLog = sb.toString()
            val timestamp = System.currentTimeMillis()
            val time = format.format(Date())
            val fileName = "crash-$time-$timestamp.log"
            try {
                val backupPath = AppConfig.backupPath
                    ?: throw NoStackTraceException(appCtx.getString(io.legado.app.R.string.backup_path_not_configured))
                val uri = Uri.parse(backupPath)
                val fileDoc = FileDoc.fromUri(uri, true)
                fileDoc.createFileIfNotExist(fileName, "crash")
                    .writeText(crashLog)
            } catch (_: Exception) {
            }
            kotlin.runCatching {
                appCtx.externalCacheDir?.let { rootFile ->
                    val exceedTimeMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                    rootFile.getFile("crash").listFiles()?.forEach {
                        if (it.lastModified() < exceedTimeMillis) {
                            it.delete()
                        }
                    }
                    FileUtils.createFileIfNotExist(rootFile, "crash", fileName)
                        .writeText(crashLog)
                }
            }
        }

        /**
         * Perform heap dump
         */
        fun doHeapDump(manually: Boolean = false) {
            val heapDir = appCtx
                .externalCache
                .getFile("heapDump")
            heapDir.createFolderReplace()
            val fileName = if (manually) {
                "heap-dump-manually-${System.currentTimeMillis()}.hprof"
            } else {
                "heap-dump-${System.currentTimeMillis()}.hprof"
            }
            val heapFile = heapDir.getFile(fileName)
            val heapDumpName = heapFile.absolutePath
            Debug.dumpHprofData(heapDumpName)
        }

    }

}
