// @path: app/src/main/java/com/radwrld/resonance/InferenceManager.kt
package com.radwrld.resonance

import com.microsoft.onnxruntime.OnnxTensor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

object InferenceManager {
    private val session by lazy { App.session }
    private val env by lazy { App.ortEnv }

    suspend fun predictMelWindow(melDb: FloatArray, frames: Int = 187, nMels: Int = 96): FloatArray {
        return withContext(Dispatchers.Default) {
            val shape = longArrayOf(1, frames.toLong(), nMels.toLong())
            val buffer = FloatBuffer.wrap(melDb)
            val tensor = OnnxTensor.createTensor(env.ortEnv, buffer, shape)
            val input = mapOf("mel_input" to tensor)
            val results = session.run(input)
            val res0 = results[0].value

            val outArr = when (res0) {
                is Array<*> -> {
                    val outer = res0 as Array<*>
                    if (outer.isNotEmpty() && outer[0] is FloatArray) {
                        val f0 = outer[0] as FloatArray
                        floatArrayOf(f0[0], f0[1])
                    } else { floatArrayOf(0f,0f) }
                }
                is FloatArray -> res0 as FloatArray
                else -> floatArrayOf(0f, 0f)
            }
            tensor.close()
            results.forEach { it.close() }
            outArr
        }
    }
}
