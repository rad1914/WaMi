// @path: app/src/main/java/com/radwrld/resonance/NativeMel.kt
package com.radwrld.resonance

object NativeMel {
    init { System.loadLibrary("native_mel") }

    external fun computeMelWindows(pcm: FloatArray, pcmLen: Int, sampleRate: Int, windowSec: Float, strideSec: Float): FloatArray?
}
