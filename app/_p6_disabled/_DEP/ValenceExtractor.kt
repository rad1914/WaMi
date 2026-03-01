// @path: app/_p6_disabled/_DEP/ValenceExtractor.kt
package com.radwrld.resonance

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.*
import org.jtransforms.fft.FloatFFT_1D
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Collections
import kotlin.math.*

object ValenceExtractor {
    private const val MODEL_ASSET = "music_valence_model_qdq.onnx"
    private const val MODEL_COPY = "music_valence_model_qdq.onnx"
    private const val SAMPLE_RATE = 16000
    private const val N_FFT = 512
    private const val HOP_LENGTH = 256
    private const val N_MELS = 96
    private const val TARGET_FRAMES = 187
    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun processDirectory(ctx: Context, treeUri: Uri, progressCb: (Int, Int, String?) -> Unit) {
        scope.launch {
            val docTree = DocumentFile.fromTreeUri(ctx, treeUri)
            val files = docTree?.listFiles()?.filter { it.isFile && it.name?.let { n -> isAudioFile(n) } == true } ?: emptyList()
            progressCb(0, files.size, null)
            try {
                copyModelAssetIfNeeded(ctx)
                initOnnxSession(ctx)
            } catch (e: Exception) {
                progressCb(0, files.size, "model init failed: ${e.message}")
                return@launch
            }
            files.forEachIndexed { idx, file ->
                try {
                    progressCb(idx, files.size, file.name)
                    val uri = file.uri
                    val pcm = decodeToMonoFloat(ctx, uri)
                    if (pcm.isEmpty()) throw Exception("empty pcm")
                    val resampled = resampleLinear(pcm, SAMPLE_RATE)
                    val mel = computeMelForModel(resampled, SAMPLE_RATE)
                    val inputTensor = prepareInputTensor(mel)
                    val output = runInference(inputTensor)
                    saveResultNextToFile(ctx, file, output)
                } catch (e: Exception) {

                }
            }
            progressCb(files.size, files.size, null)
        }
    }

    private fun isAudioFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a") || lower.endsWith(".flac") || lower.endsWith(".ogg") || lower.endsWith(".aac")
    }

    private fun copyModelAssetIfNeeded(ctx: Context) {
        val outFile = File(ctx.filesDir, MODEL_COPY)
        if (outFile.exists()) return
        ctx.assets.open(MODEL_ASSET).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun initOnnxSession(ctx: Context) {
        if (session != null && ortEnv != null) return
        ortEnv = OrtEnvironment.getEnvironment()
        val modelFile = File(ctx.filesDir, MODEL_COPY).absolutePath
        val opts = OrtSession.SessionOptions()
        opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        opts.setIntraOpNumThreads(1)
        session = ortEnv!!.createSession(modelFile, opts)
    }

    private fun decodeToMonoFloat(ctx: Context, uri: Uri): FloatArray {
        val extractor = MediaExtractor()
        val pfd = ctx.contentResolver.openFileDescriptor(uri, "r") ?: return FloatArray(0)
        extractor.setDataSource(pfd.fileDescriptor)
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = f
                break
            }
        }
        if (trackIndex == -1 || format == null) {
            extractor.release()
            return FloatArray(0)
        }
        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return FloatArray(0)
        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(format, null, null, 0)
        decoder.start()
        val inputBuffers = decoder.inputBuffers
        val outputBuffers = decoder.outputBuffers
        val info = MediaCodec.BufferInfo()
        var sawOutputEOS = false
        var sawInputEOS = false
        val outList = ArrayList<Float>()
        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                val inIndex = decoder.dequeueInputBuffer(1000)
                if (inIndex >= 0) {
                    val buffer = inputBuffers[inIndex]
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        val presentationTimeUs = extractor.sampleTime.toLong()
                        decoder.queueInputBuffer(inIndex, 0, sampleSize, presentationTimeUs, 0)
                        extractor.advance()
                    }
                }
            }
            val outIndex = decoder.dequeueOutputBuffer(info, 1000)
            when {
                outIndex >= 0 -> {
                    val outBuffer = outputBuffers[outIndex]
                    val chunk = ByteArray(info.size)
                    outBuffer.get(chunk)
                    outBuffer.clear()
                    if (info.size > 0) {
                        val shorts = bytesToShortsLE(chunk)
                        for (s in shorts) outList.add(s / 32768.0f)
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) sawOutputEOS = true
                }
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
                outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {}
            }
        }
        decoder.stop()
        decoder.release()
        extractor.release()
        pfd.close()
        val floatArray = FloatArray(outList.size)
        for (i in outList.indices) floatArray[i] = outList[i]
        return toMono(floatArray, format)
    }

    private fun toMono(samples: FloatArray, format: MediaFormat?): FloatArray {
        val channelCount = format?.getInteger(MediaFormat.KEY_CHANNEL_COUNT) ?: 1
        if (channelCount <= 1) return samples
        val mono = FloatArray((samples.size / channelCount))
        var j = 0
        var i = 0
        while (i < samples.size) {
            var sum = 0f
            for (c in 0 until channelCount) {
                sum += samples[i + c]
            }
            mono[j++] = sum / channelCount
            i += channelCount
        }
        return mono
    }

    private fun bytesToShortsLE(bytes: ByteArray): ShortArray {
        val out = ShortArray(bytes.size / 2)
        var idx = 0
        var i = 0
        while (i + 1 < bytes.size) {
            val lo = bytes[i].toInt() and 0xff
            val hi = bytes[i + 1].toInt()
            out[idx++] = ((hi shl 8) or lo).toShort()
            i += 2
        }
        return out
    }

    private fun resampleLinear(samples: FloatArray, srcRate: Int): FloatArray {
        if (srcRate == SAMPLE_RATE) return samples
        val srcLen = samples.size
        val dstLen = ((srcLen.toLong() * SAMPLE_RATE.toLong()) / srcRate.toLong()).toInt()
        if (dstLen < 1) return FloatArray(0)
        val out = FloatArray(dstLen)
        val factor = (srcLen - 1).toDouble() / (dstLen - 1)
        for (i in 0 until dstLen) {
            val pos = i * factor
            val idx = floor(pos).toInt()
            val frac = pos - idx
            val a = samples.getOrElse(idx) { 0f }
            val b = samples.getOrElse(idx + 1) { 0f }
            out[i] = (a * (1 - frac) + b * frac).toFloat()
        }
        return out
    }

    private fun computeMelForModel(samples: FloatArray, sr: Int): FloatArray {
        val frames = frameSignal(samples, N_FFT, HOP_LENGTH)
        val window = hanning(N_FFT)
        val fft = FloatFFT_1D(N_FFT)
        val specList = ArrayList<FloatArray>()
        for (frame in frames) {
            val x = FloatArray(N_FFT)
            for (i in frame.indices) x[i] = frame[i] * window[i]
            val complex = FloatArray(N_FFT * 2)
            for (i in 0 until N_FFT) complex[i] = x[i]
            fft.realForwardFull(complex)
            val mag = FloatArray(N_FFT / 2 + 1)
            for (k in 0..(N_FFT / 2)) {
                val re = complex[2 * k]
                val im = complex[2 * k + 1]
                mag[k] = re * re + im * im
            }
            specList.add(mag)
        }
        val specCount = specList.size
        val specMatrix: Array<FloatArray> = Array(specCount) { FloatArray(N_FFT / 2 + 1) }
        for (i in 0 until specCount) {
            specMatrix[i] = specList[i]
        }
        val melFilter: Array<FloatArray> = melFilterBank(N_MELS, N_FFT, sr)
        val melSpec: Array<FloatArray> = Array(specCount) { FloatArray(N_MELS) }
        for (t in 0 until specCount) {
            for (m in 0 until N_MELS) {
                var sum = 0.0f
                val filter = melFilter[m]
                for (k in filter.indices) sum += specMatrix[t][k] * filter[k]
                melSpec[t][m] = ln(1e-10f + sum)
            }
        }
        val targetSpec = if (melSpec.size >= TARGET_FRAMES) {
            if (melSpec.size >= TARGET_FRAMES && melSpec.size < TARGET_FRAMES * 2) {
                val out = Array(TARGET_FRAMES) { FloatArray(N_MELS) }
                val copy = min(melSpec.size, TARGET_FRAMES)
                for (i in 0 until copy) out[i] = melSpec[i]
                if (copy < TARGET_FRAMES) for (i in copy until TARGET_FRAMES) out[i] = FloatArray(N_MELS)
                out
            } else {
                val out = Array(TARGET_FRAMES) { FloatArray(N_MELS) }
                val step = melSpec.size.toDouble() / TARGET_FRAMES
                for (i in 0 until TARGET_FRAMES) {
                    val start = floor(i * step).toInt()
                    val end = floor((i + 1) * step).toInt().coerceAtMost(melSpec.size)
                    if (end <= start) {
                        out[i] = melSpec[start]
                    } else {
                        val acc = FloatArray(N_MELS)
                        val count = (end - start).coerceAtLeast(1)
                        for (j in start until end) for (m in 0 until N_MELS) acc[m] = acc[m] + melSpec[j][m]
                        for (m in 0 until N_MELS) acc[m] /= count
                        out[i] = acc
                    }
                }
                out
            }
        } else {
            val out = Array(TARGET_FRAMES) { FloatArray(N_MELS) }
            for (i in 0 until melSpec.size) out[i] = melSpec[i]
            for (i in melSpec.size until TARGET_FRAMES) out[i] = FloatArray(N_MELS)
            out
        }
        val flat = FloatArray(TARGET_FRAMES * N_MELS)
        var idx = 0
        for (t in 0 until TARGET_FRAMES) for (m in 0 until N_MELS) flat[idx++] = targetSpec[t][m]
        return flat
    }

    private fun frameSignal(samples: FloatArray, winLength: Int, hop: Int): Array<FloatArray> {
        val numFrames = ((samples.size - winLength + hop) / hop).coerceAtLeast(1)
        val frames = ArrayList<FloatArray>()
        var i = 0
        while (i + winLength <= samples.size) {
            val frame = FloatArray(winLength)
            System.arraycopy(samples, i, frame, 0, winLength)
            frames.add(frame)
            i += hop
        }
        if (frames.isEmpty()) frames.add(FloatArray(winLength))
        return frames.toTypedArray()
    }

    private fun hanning(n: Int): FloatArray {
        val w = FloatArray(n)
        for (i in 0 until n) w[i] = (0.5f - 0.5f * cos(2.0 * Math.PI * i / n)).toFloat()
        return w
    }

    private fun melFilterBank(nMels: Int, nFft: Int, sr: Int): Array<FloatArray> {
        val fMin = 0.0
        val fMax = sr / 2.0
        fun hzToMel(hz: Double) = 2595.0 * log10(1.0 + hz / 700.0)
        fun melToHz(mel: Double) = 700.0 * (10.0.pow(mel / 2595.0) - 1.0)
        val melMin = hzToMel(fMin)
        val melMax = hzToMel(fMax)
        val mels = DoubleArray(nMels + 2)
        for (i in mels.indices) mels[i] = melMin + i * (melMax - melMin) / (nMels + 1)
        val binsHz = DoubleArray(mels.size) { melToHz(mels[it]) }
        val bins = IntArray(binsHz.size) { floor((nFft + 1) * binsHz[it] / sr).toInt().coerceIn(0, nFft / 2) }
        val filters: Array<FloatArray> = Array(nMels) { FloatArray(nFft / 2 + 1) }
        for (m in 1..nMels) {
            val f_m_minus = bins[m - 1]
            val f_m = bins[m]
            val f_m_plus = bins[m + 1]
            if (f_m_minus == f_m || f_m == f_m_plus) continue
            for (k in f_m_minus until f_m.coerceAtMost(nFft / 2 + 1)) {
                filters[m - 1][k] = ((k - f_m_minus).toFloat() / (f_m - f_m_minus))
            }
            for (k in f_m until f_m_plus.coerceAtMost(nFft / 2 + 1)) {
                filters[m - 1][k] = ((f_m_plus - k).toFloat() / (f_m_plus - f_m))
            }
        }
        return filters
    }

    private fun prepareInputTensor(melFlat: FloatArray): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(melFlat.size * 4).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(melFlat)
        fb.rewind()
        return fb
    }

    private fun runInference(input: FloatBuffer): FloatArray {
        val env = ortEnv ?: throw IllegalStateException("ORT not initialized")
        val sess = session ?: throw IllegalStateException("session null")
        val shape = longArrayOf(1, TARGET_FRAMES.toLong(), N_MELS.toLong())
        val tensor = OnnxTensor.createTensor(env, input, shape)
        val results = sess.run(Collections.singletonMap(sess.inputNames.iterator().next(), tensor))
        val first = results[0].value
        results.close()
        tensor.close()
        return when (first) {
            is FloatArray -> first
            is Array<*> -> {
                val outList = ArrayList<Float>()
                fun flatten(o: Any?) {
                    when (o) {
                        is FloatArray -> for (v in o) outList.add(v)
                        is Array<*> -> for (sub in o) flatten(sub)
                    }
                }
                flatten(first)
                val out = FloatArray(outList.size)
                for (i in outList.indices) out[i] = outList[i]
                out
            }
            else -> FloatArray(0)
        }
    }

    private fun saveResultNextToFile(ctx: Context, docFile: DocumentFile, output: FloatArray) {
        val fname = (docFile.name ?: "result") + ".valence.json"
        val json = "{\"result\":${output.joinToString(prefix = "[", postfix = "]")}}"
        val parent = docFile.parentFile ?: return
        val existing = parent.findFile(fname)
        existing?.delete()
        val outFile = parent.createFile("application/json", fname) ?: return
        ctx.contentResolver.openOutputStream(outFile.uri)?.use { it.write(json.toByteArray()) }
    }
}
