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
    float* magnitudes;
} FftContext;

FftContext* fft_create(int size);
void fft_destroy(FftContext* ctx);
void fft_process(FftContext* ctx, const float* input, float* output_magnitudes);
void fft_transform(FftContext* ctx, float* real, float* imag, int inverse);

void downsample_magnitudes(
    const float* magnitudes,
    int total_size,
    float* downsampled,
    int target_size,
    float min_db,
    int need_normalize,
    int need_positive,
    float ref_value,
    float tilt_factor,
    int sample_rate
);

void fft_process_and_downsample(
    FftContext* ctx,
    const float* input,
    float* output_downsampled,
    int target_size,
    float min_db,
    int need_normalize,
    int need_positive,
    float ref_value,
    float tilt_factor,
    int sample_rate
);

#ifdef __cplusplus
}
#endif

#endif // FFT_H
