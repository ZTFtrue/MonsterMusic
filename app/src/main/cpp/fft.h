#ifndef FFT_H
#define FFT_H

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    int size;
    int half_size;
    float* real;
    float* imag;
    float* window;
    float* sqrt_hann_window;
    float* sin_table;
    float* cos_table;
    int* reverse_table;
} FftContext;

FftContext* fft_create(int size);
void fft_destroy(FftContext* ctx);
void fft_process(FftContext* ctx, const float* input, float* output_magnitudes);
void fft_transform(FftContext* ctx, float* real, float* imag, int inverse);

#ifdef __cplusplus
}
#endif

#endif // FFT_H
