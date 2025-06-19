// @path: app/src/main/java/com/radwrld/wami/WamiApp.kt
package com.radwrld.wami

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class WamiApp : Application() {

    companion object {
        private const val TAG = "WamiApp"
    }

    override fun onCreate() {
        super.onCreate()
        // MODIFIED: Using AppConfig.DEBUG instead of BuildConfig.DEBUG
        if (AppConfig.DEBUG) {
            Log.d(TAG, "Lifecycle: onCreate() called.")
            Log.i(TAG, "========================================")
            Log.i(TAG, "WamiApp Initializing (DEBUG MODE)...")
            Log.i(TAG, "========================================")
        }

        setupTheme()

        // MODIFIED: Using AppConfig.DEBUG
        if (AppConfig.DEBUG) Log.d(TAG, "Checking availability and applying Dynamic Colors (Material You).")
        try {
            DynamicColors.applyToActivitiesIfAvailable(this)
            // MODIFIED: Using AppConfig.DEBUG
            if (AppConfig.DEBUG) Log.i(TAG, "Dynamic Colors have been applied where available.")
        } catch (e: Exception) {
            Log.e(TAG, "An exception occurred while trying to apply dynamic colors.", e)
        }

        // MODIFIED: Using AppConfig.DEBUG
        if (AppConfig.DEBUG) Log.d(TAG, "Setting up the default uncaught exception handler.")
        val originalExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "!!! UNCAUGHT EXCEPTION DETECTED !!! in thread: ${thread.name}", throwable)
            logCrash(throwable)
            originalExceptionHandler?.uncaughtException(thread, throwable)
        }
        // MODIFIED: Using AppConfig.DEBUG
        if (AppConfig.DEBUG) {
            Log.i(TAG, "Custom uncaught exception handler has been set successfully.")
            Log.i(TAG, "Application initialization complete.")
        }
    }

    private fun setupTheme() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        val themePreference = prefs.getString(SettingsActivity.THEME_KEY, "system") ?: "system"

        // MODIFIED: Using AppConfig.DEBUG
        if (AppConfig.DEBUG) Log.i(TAG, "Theme Preference Check: key='${SettingsActivity.THEME_KEY}', value=$themePreference")

        val nightMode = when (themePreference) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(nightMode)
        // MODIFIED: Using AppConfig.DEBUG
        if (AppConfig.DEBUG) Log.i(TAG, "Default night mode has been set to: $themePreference")
    }



    private fun logCrash(e: Throwable) {
        val logFile = File(filesDir, "crash.log")
        // MODIFIED: Using AppConfig.DEBUG
        if (AppConfig.DEBUG) Log.d(TAG, "logCrash: Attempting to write crash data to: ${logFile.absolutePath}")
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.getDefault()).format(Date())
            val stackTrace = Log.getStackTraceString(e)
            val logContent = "\n==== Crash Timestamp: $timestamp ====\n$stackTrace\n====================\n"
            logFile.appendText(logContent)
            // MODIFIED: Using AppConfig.DEBUG
            if (AppConfig.DEBUG) Log.i(TAG, "logCrash: Successfully wrote crash details to ${logFile.name}.")
        } catch (ex: Exception) {
            Log.e(TAG, "logCrash: CRITICAL FAILURE - Could not write to crash log file.", ex)
        }
    }
}
