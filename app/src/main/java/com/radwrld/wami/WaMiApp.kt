// @path: app/src/main/java/com/radwrld/wami/WaMiApp.kt
package com.radwrld.wami

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.radwrld.wami.ui.AppNav
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

@HiltAndroidApp
class WamiApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            LogCrash.report(e, this)

            Thread.sleep(250)

     
       android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }

    private object LogCrash {
        fun report(e: Throwable, context: Context) {
            try {
                val logFile = File(context.filesDir, "crash.log")
                
val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.getDefault()).format(Date())
                val trace = Log.getStackTraceString(e)

                logFile.appendText("\n==== Crash @ $timestamp ====\n$trace\n====================\n")
                Log.d("LogCrash", "Crash logged at ${logFile.absolutePath}")
            } catch (ex: Exception) {
                ex.printStackTrace()
    
        }
        }
    }

    object Constants {
        // IMPORTANT: Replace with your server's public IP or domain and port
        const val BASE_URL = "http://127.0.0.1:3000"
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                
AppNav()
            }
        }
    }
}