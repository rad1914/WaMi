// @path: app/src/main/java/com/radwrld/resonance/App.kt
package com.radwrld.resonance

import android.app.Application
import java.io.File
import com.microsoft.onnxruntime.OrtEnvironment
import com.microsoft.onnxruntime.OrtSession

class App : Application() {
    companion object {
        lateinit var ortEnv: OrtEnvironment
        lateinit var session: OrtSession
        lateinit var modelPath: String
    }

    override fun onCreate() {
        super.onCreate()
        copyModelIfNeeded()
        initializeOrt()
    }

    private fun copyModelIfNeeded() {
        val target = File(filesDir, "merged_std_final_qdq.onnx")
        modelPath = target.absolutePath
        if (!target.exists()) {
            assets.open("merged_std_final_qdq.onnx").use { inp ->
                target.outputStream().use { out ->
                    inp.copyTo(out)
                }
            }
        }
    }

    private fun initializeOrt() {
        ortEnv = OrtEnvironment.getEnvironment()
        val so = OrtSession.SessionOptions()
        so.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPTIMIZATIONS)
        val threads = Runtime.getRuntime().availableProcessors().coerceAtMost(4)
        so.setIntraOpNumThreads(threads)
        session = ortEnv.createSession(modelPath, so)
    }
}
