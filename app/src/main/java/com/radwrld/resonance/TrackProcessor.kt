// @path: app/src/main/java/com/radwrld/resonance/TrackProcessor.kt
package com.radwrld.resonance

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.radwrld.resonance.data.db.AppDatabase
import com.radwrld.resonance.data.db.TrackResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class TrackProcessor(private val context: Context, private val uri: Uri) {
    private val db = AppDatabase.getInstance(context)
    private val gson = Gson()

    suspend fun processTrack(): TrackResult = withContext(Dispatchers.IO) {

        val extractor = MediaExtractor()
        val afd = context.contentResolver.openAssetFileDescriptor(uri, "r")!!
        extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        var trackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) { trackIndex = i; break }
        }
        require(trackIndex >= 0) { "No audio track found" }
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else -1L
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        val audioFloats = ArrayList<Float>()
        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                val inIndex = codec.dequeueInputBuffer(10_000L)
                if (inIndex >= 0) {
                    val inputBuf = codec.getInputBuffer(inIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuf, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
            if (outIndex >= 0) {
                val outBuf = codec.getOutputBuffer(outIndex)!!
                val outBytes = ByteArray(bufferInfo.size)
                outBuf.get(outBytes)
                outBuf.clear()

                val shorts = ShortArray(outBytes.size / 2)
                ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                for (s in shorts) audioFloats.add(s.toFloat() / Short.MAX_VALUE)
                codec.releaseOutputBuffer(outIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) sawOutputEOS = true
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

            }
        }

        codec.stop(); codec.release(); extractor.release(); afd.close()

        val windowSec = 3.0f
        val strideSec = 1.5f
        val sr = sampleRate
        val pcm = FloatArray(audioFloats.size)
        for (i in audioFloats.indices) pcm[i] = audioFloats[i]
        val pcmLen = pcm.size

        val melFlat = NativeMel.computeMelWindows(pcm, pcmLen, sr, windowSec, strideSec)
            ?: FloatArray(0)

        val nMels = 96
        val framesPerWindow = 187
        val perWindowFrames = (melFlat.size / nMels) / ((pcmLen.toDouble() / (windowSec*sr)).toInt().coerceAtLeast(1))

        val winLen = (windowSec*sr).toInt()
        val stride = (strideSec*sr).toInt()
        val windowsCount = if (pcmLen >= winLen) 1 + (pcmLen - winLen) / stride else 0

        val perWindowResults = mutableListOf<WindowResult>()
        var idx = 0
        for (w in 0 until windowsCount) {

            val frames = if (melFlat.isNotEmpty()) (melFlat.size / nMels) / windowsCount else 0

            val raw = FloatArray(frames * nMels)
            val copyLen = raw.size.coerceAtMost(melFlat.size - idx)
            System.arraycopy(melFlat, idx, raw, 0, copyLen)
            idx += copyLen
            val modelInput = preprocessMelForModel(raw, frames, nMels, framesPerWindow)

            val out = InferenceManager.predictMelWindow(modelInput, framesPerWindow, nMels)
            val valence = out[0]; val arousal = out[1]
            val startS = w * stride.toDouble() / sr
            val endS = (w*stride + winLen).toDouble() / sr
            val energy = computeRms(pcm, w*stride, winLen)
            perWindowResults.add(WindowResult(startS, endS, valence, arousal, energy))
        }

        val aggregated = aggregateWindows(perWindowResults)

        val trackId = uri.toString()
        val jsonWindows = gson.toJson(perWindowResults)
        val trackResult = TrackResult(
            trackId = trackId,
            modelVersion = "merged_std_final_qdq_v1",
            durationSeconds = if (durationUs > 0) durationUs / 1e6 else null,
            windowsProcessed = perWindowResults.size,
            valence = aggregated.first,
            arousal = aggregated.second,
            confidence = 0.9f,
            perWindowJson = jsonWindows,
            lastUpdatedMs = System.currentTimeMillis()
        )

        db.trackResultDao().insert(trackResult)
        return@withContext trackResult
    }

    private fun computeRms(pcm: FloatArray, offset: Int, len: Int): Float {
        if (offset < 0 || offset >= pcm.size) return 0f
        var s = 0.0f
        var count = 0
        val end = (offset + len).coerceAtMost(pcm.size)
        for (i in offset until end) { s += pcm[i] * pcm[i]; count++ }
        return if (count > 0) kotlin.math.sqrt(s / count) else 0f
    }

    private fun preprocessMelForModel(raw: FloatArray, frames: Int, nMels: Int, targetFrames: Int = 187): FloatArray {
        val out = FloatArray(targetFrames * nMels)
        if (frames == targetFrames) {
            System.arraycopy(raw, 0, out, 0, raw.size)
            return out
        } else if (frames > targetFrames && frames > 0) {

            val block = frames / targetFrames
            for (t in 0 until targetFrames) {
                val start = t * block; val end = if (t == targetFrames - 1) frames else (start + block)
                for (m in 0 until nMels) {
                    var sum = 0f
                    for (f in start until end) sum += raw[f * nMels + m]
                    out[t*nMels + m] = sum / (end - start)
                }
            }
        } else {
            val minv = if (raw.isEmpty()) -80f else raw.minOrNull()!!
            if (raw.isNotEmpty()) System.arraycopy(raw, 0, out, 0, raw.size)
            for (i in raw.size until out.size) out[i] = minv
        }
        return out
    }

    private fun aggregateWindows(windows: List<WindowResult>): Pair<Float, Float> {
        if (windows.isEmpty()) return Pair(0f,0f)
        val valsV = windows.map { it.valence }.sorted()
        val valsA = windows.map { it.arousal }.sorted()
        val p10 = (valsV.size * 0.1).toInt()
        val p90 = valsV.size - p10
        val subV = if (p90 > p10) valsV.subList(p10, p90) else valsV
        val subA = if (p90 > p10) valsA.subList(p10, p90) else valsA
        fun median(list: List<Float>): Float {
            if (list.isEmpty()) return 0f
            val mid = list.size / 2
            return if (list.size % 2 == 1) list[mid] else (list[mid-1] + list[mid]) / 2f
        }
        return Pair(median(subV), median(subA))
    }

    data class WindowResult(val start: Double, val end: Double, val valence: Float, val arousal: Float, val energy: Float)
}
