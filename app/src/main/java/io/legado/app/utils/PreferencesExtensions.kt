@file:Suppress("unused")

package io.legado.app.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import splitties.init.appCtx
import java.io.File

/**
 * Get SharedPreferences of custom path, generate SharedPreferences using reflection
 * @param dir Directory path
 * @param fileName File name, no need for '.xml' suffix
 * @return SharedPreferences
 */
@SuppressLint("DiscouragedPrivateApi")
fun Context.getSharedPreferences(
    dir: String,
    fileName: String
): SharedPreferences? {
    try {
        // Get mBase variable in ContextWrapper object. This variable saves ContextImpl object
        val fieldMBase = ContextWrapper::class.java.getDeclaredField("mBase")
        fieldMBase.isAccessible = true
        // Get mBase variable
        val objMBase = fieldMBase.get(this)
        // Get ContextImpl.mPreferencesDir variable, which saves contents path of data file
        val fieldMPreferencesDir = objMBase.javaClass.getDeclaredField("mPreferencesDir")
        fieldMPreferencesDir.isAccessible = true
        // Create custom path
        val file = File(dir)
        // Modify value of mPreferencesDir variable
        fieldMPreferencesDir.set(objMBase, file)
        // Return SharedPreferences after path modification :%FILE_PATH%/%fileName%.xml
        return getSharedPreferences(fileName, Activity.MODE_PRIVATE)
    } catch (e: NoSuchFieldException) {
        e.printOnDebug()
    } catch (e: IllegalArgumentException) {
        e.printOnDebug()
    } catch (e: IllegalAccessException) {
        e.printOnDebug()
    }
    return null
}

fun SharedPreferences.getString(key: String): String? {
    return getString(key, null)
}

fun SharedPreferences.putString(key: String, value: String) {
    edit {
        putString(key, value)
    }
}

fun SharedPreferences.getBoolean(key: String): Boolean {
    return getBoolean(key, false)
}

fun SharedPreferences.putBoolean(key: String, value: Boolean) {
    edit {
        putBoolean(key, value)
    }
}

fun SharedPreferences.getInt(key: String): Int {
    return getInt(key, 0)
}

fun SharedPreferences.putInt(key: String, value: Int) {
    edit {
        putInt(key, value)
    }
}

fun SharedPreferences.getLong(key: String): Long {
    return getLong(key, 0)
}

fun SharedPreferences.putLong(key: String, value: Long) {
    edit {
        putLong(key, value)
    }
}

fun SharedPreferences.getFloat(key: String): Float {
    return getFloat(key, 0f)
}

fun SharedPreferences.putFloat(key: String, value: Float) {
    edit {
        putFloat(key, value)
    }
}

fun SharedPreferences.remove(key: String) {
    edit {
        remove(key)
    }
}

fun LifecycleOwner.observeSharedPreferences(
    prefs: SharedPreferences = appCtx.defaultSharedPreferences,
    l: SharedPreferences.OnSharedPreferenceChangeListener
) {
    val observer = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            prefs.registerOnSharedPreferenceChangeListener(l)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            prefs.unregisterOnSharedPreferenceChangeListener(l)
            lifecycle.removeObserver(this)
        }

        override fun onPause(owner: LifecycleOwner) {
            prefs.unregisterOnSharedPreferenceChangeListener(l)
        }

        override fun onResume(owner: LifecycleOwner) {
            prefs.registerOnSharedPreferenceChangeListener(l)
        }
    }
    lifecycle.addObserver(observer)
}
