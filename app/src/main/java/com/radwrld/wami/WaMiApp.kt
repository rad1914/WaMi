// @path: app/src/main/java/com/radwrld/wami/WaMiApp.kt
package com.radwrld.wami

import android.app.Application
import com.radwrld.wami.util.LogCrash
import dagger.hilt.android.HiltAndroidApp
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
}
