// @path: app/u/resonance/ValenceExtractor.kt
package com.radwrld.resonance.valence

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.mfcc.MFCC
import be.tarsos.dsp.util.fft.FFT
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

data class ValenceResult(
    val trackUri: String,
    val displayName: String,
    val durationMs: Long,
    val sampleRate: Int,
    val predictedValence: Double,
    val featuresSummary: Map<String, Any>
)

object ValenceExtractor {

    private const val bufferSize = 2048
    private const val bufferOverlap = 1024
    private const val sampleRate = 44100
    private val sampleRateF = sampleRate.toFloat()

    suspend fun extractPerTrackValence(context: Context, trackUri: Uri): ValenceResult =
        withContext(Dispatchers.IO) {

            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, trackUri)
            val durationMs =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val displayName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: getDisplayName(context, trackUri)

            val pcmFloats = try {
                decodeToFloatArray(context, trackUri)
            } catch (ex: Exception) {

                ValenceResult(
                    trackUri = trackUri.toString(),
                    displayName = displayName,
                    durationMs = durationMs,
                    sampleRate = sampleRate,
                    predictedValence = 0.0,
                    featuresSummary = mapOf("error" to "decode failed: ${ex.message}")
                )
            }

            if (pcmFloats is ValenceResult) return@withContext pcmFloats

            var frameCount = 0L
            var sumRms = 0.0
            var sumCentroid = 0.0
            val mfccSum = DoubleArray(13) { 0.0 }
            val chromaSum = DoubleArray(12) { 0.0 }

            val dispatcher: AudioDispatcher =
                AudioDispatcherFactory.fromFloatArray(pcmFloats as FloatArray, sampleRate, bufferSize, bufferOverlap)

            val fft = FFT(bufferSize)
            val mfccProcessor = MFCC(bufferSize, sampleRateF, 13, 20, 50f, (sampleRate / 2).toFloat())

            val processor = object : AudioProcessor {
                override fun processingFinished() {}
                override fun process(audioEvent: AudioEvent?): Boolean {
                    if (audioEvent == null) return true
                    frameCount++

                    val buffer = audioEvent.floatBuffer
                    var sumSquares = 0.0
                    for (i in buffer.indices) sumSquares += (buffer[i] * buffer[i]).toDouble()
                    val rms = sqrt(sumSquares / buffer.size)
                    sumRms += rms

                    val bufferCopy = buffer.copyOf()

                    fft.forwardTransform(bufferCopy)
                    val magnitudes = FloatArray(bufferCopy.size / 2)
                    fft.modulus(bufferCopy, magnitudes)

                    var num = 0.0
                    var den = 1e-9
                    for (k in magnitudes.indices) {
                        val mag = magnitudes[k].toDouble()
                        val freq = k * (sampleRate.toDouble() / bufferSize)
                        num += freq * mag
                        den += mag
                    }
                    val centroid = num / den
                    sumCentroid += centroid

                    mfccProcessor.process(audioEvent)
                    val mfccs = mfccProcessor.mfcc
                    if (mfccs != null) {
                        for (i in mfccSum.indices) {
                            mfccSum[i] += mfccs.getOrNull(i)?.toDouble() ?: 0.0
                        }
                    }

                    val f0 = 27.5
                    for (k in magnitudes.indices) {
                        val mag = magnitudes[k].toDouble()
                        val freq = k * (sampleRate.toDouble() / bufferSize)
                        if (freq < 50.0 || freq > 5000.0) continue
                        val centsFromA0 = 1200.0 * ln(freq / f0) / ln(2.0)
                        val midi = (centsFromA0 / 100.0) + 21.0
                        val pc = ((midi.toInt()) % 12 + 12) % 12
                        chromaSum[pc] += mag
                    }

                    return true
                }
            }

            dispatcher.addAudioProcessor(processor)
            dispatcher.run()

            val frames = max(1, frameCount.toInt())
            val meanRms = sumRms / frames
            val meanCentroid = sumCentroid / frames
            val meanMfcc = mfccSum.map { it / frames }
            val meanChroma = chromaSum.map { it / frames }
            val chromaMax = meanChroma.maxOrNull() ?: 1.0
            val chromaNormalized = meanChroma.map { it / (chromaMax.coerceAtLeast(1e-9)) }.toDoubleArray()

            val majorScore = scoreMajorness(chromaNormalized)
            val energyScore = normalize01(meanRms, 1e-6, 0.3)
            val centroidScore = 1.0 - normalize01(meanCentroid, 500.0, 6000.0)

            val rawValence = 0.55 * majorScore + 0.30 * centroidScore + 0.15 * energyScore
            val predictedValence = (rawValence * 2.0) - 1.0

            val featuresSummary = mapOf(
                "meanRms" to meanRms,
                "meanSpectralCentroid" to meanCentroid,
                "majorScore" to majorScore,
                "energyScore" to energyScore,
                "centroidScore" to centroidScore,
                "meanMfcc" to meanMfcc
            )

            ValenceResult(
                trackUri = trackUri.toString(),
                displayName = displayName,
                durationMs = durationMs,
                sampleRate = sampleRate,
                predictedValence = predictedValence,
                featuresSummary = featuresSummary
            )
        }

    private fun getDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) return cursor.getString(idx)
        }
        return uri.lastPathSegment ?: "unknown"
    }

    private fun normalize01(value: Double, minVal: Double, maxVal: Double): Double {
        val v = value.coerceIn(minVal, maxVal)
        return (v - minVal) / (maxVal - minVal)
    }

    private fun scoreMajorness(chroma: DoubleArray): Double {
        if (chroma.isEmpty()) return 0.5
        val tonic = chroma.indices.maxByOrNull { chroma[it] } ?: 0
        val majorThird = chroma[(tonic + 4) % 12]
        val perfectFifth = chroma[(tonic + 7) % 12]
        val minorThird = chroma[(tonic + 3) % 12]
        val raw = (majorThird + perfectFifth - minorThird).coerceAtLeast(0.0)
        val denom = chroma.sum().coerceAtLeast(1e-9)
        return (raw / denom).coerceIn(0.0, 1.0)
    }

    private fun decodeToFloatArray(context: Context, uri: Uri): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        var trackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                trackIndex = i
                break
            }
        }
        if (trackIndex < 0) {
            extractor.release()
            return FloatArray(0)
        }

        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val bufferInfo = BufferInfo()
        val outputBytes = ArrayList<Byte>()

        var sawInputEOS = false
        var sawOutputEOS = false

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        val presentationTimeUs = extractor.sampleTime
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0)
                        extractor.advance()
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                outputIndex >= 0 -> {
                    val outBuffer = codec.getOutputBuffer(outputIndex)
                    val chunk = ByteArray(bufferInfo.size)
                    outBuffer?.get(chunk)
                    outBuffer?.clear()
                    for (b in chunk) outputBytes.add(b)
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {

                }
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {

                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        val pcmBytes = ByteArray(outputBytes.size)
        for (i in pcmBytes.indices) pcmBytes[i] = outputBytes[i]
        val bb = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)

        val floats = FloatArray(pcmBytes.size / 2)
        var idx = 0
        while (bb.remaining() >= 2) {
            val s = bb.short.toInt()
            floats[idx++] = s / 32768f
        }
        return if (idx == floats.size) floats else floats.copyOf(idx)
    }
}
