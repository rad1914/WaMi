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

    // Companion object for consistent logging tag
    companion object {
        private const val TAG = "WamiApp"
    }

    /**
     * Called when the application is starting, before any other application objects have been created.
     * More verbose logging has been added to trace the initialization process in detail.
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Lifecycle: onCreate() called.")

        Log.i(TAG, "========================================")
        Log.i(TAG, "WamiApp Initializing...")
        Log.i(TAG, "========================================")

        // --- Preference Handling for Dark Mode ---
        Log.d(TAG, "Attempting to retrieve SharedPreferences for settings.")
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        Log.d(TAG, "SharedPreferences object retrieved successfully.")

        // Reading the dark mode preference with a default value of 'true'
        val darkModeEnabled = prefs.getBoolean(SettingsActivity.DARK_MODE_KEY, true)
        Log.i(TAG, "Dark Mode Preference Check: key='${SettingsActivity.DARK_MODE_KEY}', value=$darkModeEnabled")

        // --- Setting the Application's Night Mode ---
        Log.d(TAG, "Preparing to set the default night mode based on preference.")
        if (darkModeEnabled) {
            Log.d(TAG, "Setting theme to AppCompatDelegate.MODE_NIGHT_YES.")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            Log.d(TAG, "Setting theme to AppCompatDelegate.MODE_NIGHT_NO.")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        Log.i(TAG, "Default night mode has been set.")

        // --- Dynamic Colors (Material You) ---
        Log.d(TAG, "Checking availability and applying Dynamic Colors (Material You).")
        try {
            DynamicColors.applyToActivitiesIfAvailable(this)
            Log.i(TAG, "Dynamic Colors have been successfully applied where available.")
        } catch (e: Exception) {
            // This catch block will handle any unexpected errors during the dynamic color application.
            Log.e(TAG, "An exception occurred while trying to apply dynamic colors.", e)
        }

        // --- Global Uncaught Exception Handler ---
        Log.d(TAG, "Setting up the default uncaught exception handler for the application.")
        val originalExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "!!! UNCAUGHT EXCEPTION DETECTED !!! in thread: ${thread.name}", throwable)

            // Log the crash to a file for persistence
            logCrash(throwable)

            // It's crucial to call the original handler to ensure the system can still
            // perform its default crash behavior (like showing the "App has stopped" dialog).
            Log.d(TAG, "Chaining to the original uncaught exception handler.")
            originalExceptionHandler?.uncaughtException(thread, throwable)
        }
        Log.i(TAG, "Custom uncaught exception handler has been set successfully.")
        Log.i(TAG, "Application initialization complete.")
    }

    /**
     * Logs the details of a throwable (crash) to a persistent file.
     * @param e The throwable exception to log.
     */
    private fun logCrash(e: Throwable) {
        val logFile = File(filesDir, "crash.log")
        Log.d(TAG, "logCrash: Attempting to write crash data to: ${logFile.absolutePath}")

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.getDefault()).format(Date())
        val stackTrace = Log.getStackTraceString(e)

        val logContent = "\n==== Crash Timestamp: $timestamp ====\n$stackTrace\n====================\n"
        Log.v(TAG, "logCrash: Content to be written:\n$logContent") // Verbose log to see the exact content

        try {
            logFile.appendText(logContent)
            Log.i(TAG, "logCrash: Successfully wrote crash details to ${logFile.name}.")
        } catch (ex: Exception) {
            // This is a critical failure, as we can't even log our crashes.
            Log.e(TAG, "logCrash: CRITICAL FAILURE - Could not write to crash log file.", ex)
        }
    }
}
