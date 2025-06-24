// @path: app/src/main/java/com/radwrld/wami/WamiApp.kt
package com.radwrld.wami

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.radwrld.wami.network.SyncManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class WamiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        SyncManager.initialize(this)
        setupTheme()
        applyDynamicColors()
        setupCrashHandler()
    }

    private fun setupTheme() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        when (prefs.getString(SettingsActivity.THEME_KEY, "system")) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun applyDynamicColors() {
        try {
            DynamicColors.applyToActivitiesIfAvailable(this)
        } catch (_: Exception) { }
    }

    private fun setupCrashHandler() {
        val original = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            logCrash(e)
            original?.uncaughtException(t, e)
        }
    }

    private fun logCrash(e: Throwable) {
        try {
            val logFile = File(filesDir, "crash.log")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.getDefault()).format(Date())
            val trace = android.util.Log.getStackTraceString(e)
            logFile.appendText("\n==== Crash @ $timestamp ====\n$trace\n====================\n")
        } catch (_: Exception) { }
    }
}
