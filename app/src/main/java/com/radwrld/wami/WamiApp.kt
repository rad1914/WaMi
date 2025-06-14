// @path: app/src/main/java/com/radwrld/wami/WamiApp.kt
// @-d.app.Application
package com.radwrld.wami

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class WamiApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.w("WamiApp", "Application started")

        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        val darkModeEnabled = prefs.getBoolean(SettingsActivity.DARK_MODE_KEY, true)
        Log.w("WamiApp", "Dark mode preference: $darkModeEnabled")

        AppCompatDelegate.setDefaultNightMode(
            if (darkModeEnabled)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )

        try {
            DynamicColors.applyToActivitiesIfAvailable(this)
            Log.w("WamiApp", "Dynamic colors applied")
        } catch (e: Exception) {
            Log.w("WamiApp", "Failed to apply dynamic colors", e)
        }

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Log.w("WamiApp", "Uncaught exception in thread: ${t.name}", e)
            logCrash(e)
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(t, e)
        }
    }

    private fun logCrash(e: Throwable) {
        val logFile = File(filesDir, "crash.log")
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val stackTrace = Log.getStackTraceString(e)

        try {
            logFile.appendText(
                "\n==== $timestamp ====\n$stackTrace\n====================\n"
            )
            Log.w("CrashLogger", "Crash logged to file")
        } catch (ex: Exception) {
            Log.e("CrashLogger", "Can't write crash log", ex)
        }
    }
}
