#include "mel_filterbank.h"
#include <cmath>
#include <algorithm>
#include <cstring>

// Helpers for mel scale (Slaney-like)
static float hz_to_mel(float hz) {
    return 2595.0f * log10f(1.0f + hz / 700.0f);
}
static float mel_to_hz(float mel) {
    return 700.0f * (powf(10.0f, mel / 2595.0f) - 1.0f);
}

MelFilterbank::MelFilterbank(int n_fft, int n_mels, int sr, float fmin, float fmax) :
    n_fft_(n_fft), n_mels_(n_mels), sr_(sr) {
    if (fmax <= 0) fmax = sr / 2;
    int n_freq = n_fft_ / 2 + 1;
    std::vector<float> mel_points(n_mels_ + 2);
    float mel_min = hz_to_mel(fmin);
    float mel_max = hz_to_mel(fmax);
    for (int i = 0; i < n_mels_ + 2; ++i)
        mel_points[i] = mel_min + (mel_max - mel_min) * (float)i / (n_mels_ + 1);
    std::vector<float> hz_points(n_mels_ + 2);
    for (int i = 0; i < n_mels_ + 2; ++i) hz_points[i] = mel_to_hz(mel_points[i]);
    std::vector<int> bin(n_mels_ + 2);
    for (int i = 0; i < n_mels_ + 2; ++i)
        bin[i] = static_cast<int>(std::floor((n_fft_ + 1) * hz_points[i] / sr_));

    filterbank_.assign(n_mels_ * n_freq, 0.0f);
    for (int m = 1; m <= n_mels_; ++m) {
        int f_m_minus = bin[m - 1];
        int f_m = bin[m];
        int f_m_plus = bin[m + 1];
        for (int k = f_m_minus; k < f_m; ++k) {
            if (k >= 0 && k < n_freq)
                filterbank_[m * n_freq + k] = (float)(k - f_m_minus) / (f_m - f_m_minus);
        }
        for (int k = f_m; k < f_m_plus; ++k) {
            if (k >= 0 && k < n_freq)
                filterbank_[m * n_freq + k] = (float)(f_m_plus - k) / (f_m_plus - f_m);
        }
    }
    // normalize
    for (int m = 0; m < n_mels_; ++m) {
        float norm = 0.0f;
        for (int k = 0; k < n_freq; ++k) norm += filterbank_[m * n_freq + k];
        if (norm > 0.0f) {
            for (int k = 0; k < n_freq; ++k) filterbank_[m * n_freq + k] /= norm;
        }
    }
}

MelFilterbank::~MelFilterbank(){}

void MelFilterbank::apply(const float* S, float* out) const {
    int n_freq = n_fft_ / 2 + 1;
    for (int m = 0; m < n_mels_; ++m) {
        float s = 0.0f;
        const float* fb = &filterbank_[m * n_freq];
        for (int k = 0; k < n_freq; ++k) s += fb[k] * S[k];
        out[m] = s;
    }
}