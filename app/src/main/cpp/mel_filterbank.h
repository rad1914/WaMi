#ifndef MEL_FILTERBANK_H
#define MEL_FILTERBANK_H
#include <vector>

class MelFilterbank {
public:
    MelFilterbank(int n_fft, int n_mels, int sr, float fmin = 0.0f, float fmax = -1.0f);
    ~MelFilterbank();
    // S input is power spectrum length n_fft/2+1
    // out is length n_mels
    void apply(const float* S, float* out) const;
private:
    int n_fft_;
    int n_mels_;
    int sr_;
    std::vector<float> filterbank_; // flattened n_mels x (n_fft/2 +1)
};
#endif