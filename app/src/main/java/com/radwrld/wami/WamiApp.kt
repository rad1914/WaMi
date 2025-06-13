// @path: app/src/main/java/com/radwrld/wami/WamiApp.kt
package com.radwrld.wami

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WamiApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // --- Start of Merged Code ---
        // Set the app's theme (Light/Dark) based on user preference from Settings.
        // This runs first to establish the base theme.
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(SettingsActivity.DARK_MODE_KEY, true) // Default to dark mode is ON
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        // --- End of Merged Code ---

        // Apply dynamic colors (Material You) to the app theme.
        // This will override the base theme's colors with wallpaper colors if available.
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Your existing crash logging setup
        setupCrashLogging()
    }

    private fun setupCrashLogging() {
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log the crash to a file
            handleUncaughtException(throwable)

            // Let the default handler do its job to show the "App has crashed" dialog
            defaultExceptionHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun handleUncaughtException(throwable: Throwable) {
        val logFile = File(filesDir, "crash.log")
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val stackTrace = Log.getStackTraceString(throwable)

        val logMessage = """
            |=====================
            |Timestamp: $timestamp
            |---------------------
            |Error:
            |$stackTrace
            |=====================
            |
            |
        """.trimMargin()

        try {
            // Using appendText to add new crashes to the same file
            logFile.appendText(logMessage)
        } catch (e: Exception) {
            Log.e("CrashLogger", "Failed to write to crash log", e)
        }
    }
}
