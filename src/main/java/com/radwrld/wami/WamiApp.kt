// app/src/main/java/com/radwrld/wami/WamiApp.kt
package com.radwrld.wami

import android.app.Application
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WamiApp : Application() {

    override fun onCreate() {
        super.onCreate()
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
