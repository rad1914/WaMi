// @path: app/src/main/java/com/radwrld/wami/MainActivity.kt
package com.radwrld.wami

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.radwrld.wami.ui.AppNav
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrash(throwable)
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(1)
        }

        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AppNav()
            }
        }
    }

    private fun logCrash(e: Throwable) {
        try {
            val logFile = File(filesDir, "crash.log")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.getDefault()).format(Date())
            val trace = android.util.Log.getStackTraceString(e)
            logFile.appendText("\n==== Crash @ $timestamp ====\n$trace\n====================\n") 
        } catch (_: Exception) {

        }
    }
}
