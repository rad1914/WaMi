#include <jni.h>
#include <vector>
#include <cmath>
#include <algorithm>
#include <android/log.h>
#include "kiss_fftr.h"
#include "mel_filterbank.h"
#include <cstring>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "native_mel", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "native_mel", __VA_ARGS__)

extern "C" {

JNIEXPORT jfloatArray JNICALL
Java_com_example_moodapp_NativeMel_computeMelWindows(
    JNIEnv *env, jclass clazz,
    jfloatArray pcmArray, jint pcmLen, jint sampleRate, jfloat windowSec, jfloat strideSec) {

    // Copy samples into vector
    jfloat* pcm = env->GetFloatArrayElements(pcmArray, nullptr);
    std::vector<float> signal(pcm, pcm + pcmLen);
    env->ReleaseFloatArrayElements(pcmArray, pcm, JNI_ABORT);

    // Resample if needed (simple linear)
    const int target_sr = 16000;
    std::vector<float> resampled;
    if (sampleRate != target_sr) {
        double ratio = (double) target_sr / sampleRate;
        size_t new_len = (size_t) floor(signal.size() * ratio);
        resampled.resize(new_len);
        for (size_t i = 0; i < new_len; ++i) {
            double src_pos = i / ratio;
            size_t i0 = (size_t) floor(src_pos);
            size_t i1 = std::min(i0 + 1, signal.size() - 1);
            double frac = src_pos - i0;
            resampled[i] = (float)((1.0 - frac)*signal[i0] + frac*signal[i1]);
        }
    } else {
        resampled = std::move(signal);
    }

    // STFT params
    const int n_fft = 512;
    const int hop_length = 256;
    const int n_mels = 96;
    const float top_db = 80.0f;

    int win_len = static_cast<int>(round(windowSec * target_sr));
    int stride = static_cast<int>(round(strideSec * target_sr));
    if (win_len < n_fft) return nullptr;

    int start = 0;
    int num_frames_per_window = 1 + (win_len - n_fft) / hop_length;
    std::vector<float> db_mel_flat; // will append windows sequentially

    MelFilterbank mel(n_fft, n_mels, target_sr);

    // Hanning window
    std::vector<float> win(n_fft);
    for (int i = 0; i < n_fft; ++i) win[i] = 0.5f - 0.5f * cosf(2.0f*M_PI*i/(n_fft-1));

    // prepare rfft
    kiss_fftr_cfg cfg = kiss_fftr_alloc(n_fft, 0, nullptr, nullptr);
    std::vector<kiss_fft_scalar> in(n_fft);
    std::vector<kiss_fft_cpx> out(n_fft/2 + 1);
    std::vector<float> power_spectrum(n_fft/2 + 1);

    while (start + win_len <= (int)resampled.size()) {
        // extract windowed signal of length win_len -> but STFT frames require n_fft each frame
        // We'll compute STFT across this window by sliding frames
        std::vector<float> window_signal(resampled.begin()+start, resampled.begin()+start+win_len);
        // pad last frame if necessary
        // compute frames
        std::vector<float> mel_per_frame(num_frames_per_window * n_mels);
        for (int f = 0; f < num_frames_per_window; ++f) {
            int offset = f * hop_length;
            // prepare frame (n_fft)
            for (int i = 0; i < n_fft; ++i) {
                float s = 0.0f;
                if (offset + i < (int)window_signal.size()) s = window_signal[offset + i];
                in[i] = s * win[i];
            }
            kiss_fftr(cfg, in.data(), out.data());
            for (int k = 0; k <= n_fft/2; ++k) {
                float re = out[k].r;
                float im = out[k].i;
                power_spectrum[k] = re*re + im*im + 1e-20f;
            }
            // apply mel filterbank
            std::vector<float> mel_out(n_mels);
            mel.apply(power_spectrum.data(), mel_out.data());
            for (int m = 0; m < n_mels; ++m) {
                mel_per_frame[f*n_mels + m] = mel_out[m];
            }
        }
        // convert to dB per-window using ref = max across window
        float maxv = 1e-20f;
        for (float v : mel_per_frame) if (v > maxv) maxv = v;
        for (float v : mel_per_frame) {
            float db = 10.0f * log10f(v) - 10.0f * log10f(maxv);
            if (db < -top_db) db = -top_db;
            db_mel_flat.push_back(db);
        }
        start += stride;
    }

    free(cfg);

    // pack into jfloatArray
    jfloatArray outArr = env->NewFloatArray((jsize)db_mel_flat.size());
    env->SetFloatArrayRegion(outArr, 0, (jsize)db_mel_flat.size(), db_mel_flat.data());
    return outArr;
}
}