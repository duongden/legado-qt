package io.legado.app.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.Display
import splitties.init.appCtx
import splitties.systemservices.displayManager
import splitties.systemservices.powerManager


@Suppress("unused")
object SystemUtils {

    @SuppressLint("ObsoleteSdkInt")
    fun ignoreBatteryOptimization(activity: Activity) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return

        val hasIgnored = powerManager.isIgnoringBatteryOptimizations(activity.packageName)
        // Check if current APP is in battery optimization whitelist, if not, pop up dialog to add to whitelist.
        if (!hasIgnored) {
            try {
                @SuppressLint("BatteryLife")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:" + activity.packageName)
                activity.startActivity(intent)
            } catch (ignored: Throwable) {
            }

        }
    }

    fun isScreenOn(): Boolean {
        return displayManager.displays.filterNotNull().any {
            it.state != Display.STATE_OFF
        }
    }

    /**
     * Screen pixel width
     */
    val screenWidthPx by lazy {
        appCtx.resources.displayMetrics.widthPixels
    }

    /**
     * Screen pixel height
     */
    val screenHeightPx by lazy {
        appCtx.resources.displayMetrics.heightPixels
    }
}
