package io.legado.app.utils

import kotlin.math.abs
import io.legado.app.R
import splitties.init.appCtx

fun Long.toTimeAgo(): String {
    val curTime = System.currentTimeMillis()
    val time = this
    val seconds = abs(System.currentTimeMillis() - time) / 1000f
    val end = if (time < curTime) appCtx.getString(R.string.time_ago) else appCtx.getString(R.string.time_later)


    val start = when {
        seconds < 60 -> "${seconds.toInt()}${appCtx.getString(R.string.time_seconds)}"
        seconds < 3600 -> {
            val minutes = seconds / 60f
            "${minutes.toInt()}${appCtx.getString(R.string.time_minutes)}"
        }
        seconds < 86400 -> {
            val hours = seconds / 3600f
            "${hours.toInt()}${appCtx.getString(R.string.time_hours)}"
        }
        seconds < 604800 -> {
            val days = seconds / 86400f
            "${days.toInt()}${appCtx.getString(R.string.time_days)}"
        }
        seconds < 2_628_000 -> {
            val weeks = seconds / 604800f
            "${weeks.toInt()}${appCtx.getString(R.string.time_weeks)}"
        }
        seconds < 31_536_000 -> {
            val months = seconds / 2_628_000f
            "${months.toInt()}${appCtx.getString(R.string.time_months)}"
        }
        else -> {
            val years = seconds / 31_536_000f
            "${years.toInt()}${appCtx.getString(R.string.time_years)}"
        }
    }
    return start + end
}