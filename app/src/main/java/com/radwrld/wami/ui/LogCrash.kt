// @path: app/src/main/java/com/radwrld/wami/ui/LogCrash.kt
package com.radwrld.wami.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object LogCrash {
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
