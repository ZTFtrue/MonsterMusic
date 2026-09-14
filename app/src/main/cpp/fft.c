#include "fft.h"
#include <math.h>
#include <stdlib.h>
#include <string.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

FftContext* fft_create(int size) {
    if (size <= 0 || (size & (size - 1)) != 0) {
        return NULL; // Must be power of 2
    }

    FftContext* ctx = (FftContext*) malloc(sizeof(FftContext));
    if (!ctx) return NULL;

    ctx->size = size;
    ctx->half_size = size / 2;

    ctx->real = (float*) malloc(size * sizeof(float));
    ctx->imag = (float*) malloc(size * sizeof(float));
    ctx->window = (float*) malloc(size * sizeof(float));
    ctx->sqrt_hann_window = (float*) malloc(size * sizeof(float));
    ctx->sin_table = (float*) malloc(ctx->half_size * sizeof(float));
    ctx->cos_table = (float*) malloc(ctx->half_size * sizeof(float));
    ctx->reverse_table = (int*) malloc(size * sizeof(int));

    if (!ctx->real || !ctx->imag || !ctx->window || !ctx->sqrt_hann_window || !ctx->sin_table || !ctx->cos_table || !ctx->reverse_table) {
        fft_destroy(ctx);
        return NULL;
    }

    // 1. Hann Window: w[n] = 0.5 * (1 - cos(2*PI*n / (N-1)))
    for (int i = 0; i < size; i++) {
        ctx->window[i] = (float)(0.5 * (1.0 - cos(2.0 * M_PI * (double)i / (double)(size - 1))));
        ctx->sqrt_hann_window[i] = (float)sin(M_PI * ((double)i + 0.5) / (double)size);
    }

    // 2. Bit-Reversal Table
    int levels = 0;
    for (int temp = size; temp > 1; temp >>= 1) {
        levels++;
    }
    for (int i = 0; i < size; i++) {
        int rev = 0;
        int temp = i;
        for (int j = 0; j < levels; j++) {
            rev = (rev << 1) | (temp & 1);
            temp >>= 1;
        }
        ctx->reverse_table[i] = rev;
    }

    // 3. Trigonometry (Twiddle Factors)
    for (int i = 0; i < ctx->half_size; i++) {
        double angle = -2.0 * M_PI * (double)i / (double)size;
        ctx->cos_table[i] = (float)cos(angle);
        ctx->sin_table[i] = (float)sin(angle);
    }

    return ctx;
}

void fft_destroy(FftContext* ctx) {
    if (!ctx) return;
    if (ctx->real) free(ctx->real);
    if (ctx->imag) free(ctx->imag);
    if (ctx->window) free(ctx->window);
    if (ctx->sqrt_hann_window) free(ctx->sqrt_hann_window);
    if (ctx->sin_table) free(ctx->sin_table);
    if (ctx->cos_table) free(ctx->cos_table);
    if (ctx->reverse_table) free(ctx->reverse_table);
    free(ctx);
}

void fft_transform(FftContext* ctx, float* real, float* imag, int inverse) {
    if (!ctx || !real || !imag) return;
    int size = ctx->size;

    // For IFFT: take complex conjugate of input
    if (inverse) {
        for (int i = 0; i < size; i++) {
            imag[i] = -imag[i];
        }
    }

    // Bit-Reversal Permutation
    const int* reverse_table = ctx->reverse_table;
    for (int i = 0; i < size; i++) {
        int j = reverse_table[i];
        if (j > i) {
            float temp_r = real[i];
            real[i] = real[j];
            real[j] = temp_r;

            float temp_i = imag[i];
            imag[i] = imag[j];
            imag[j] = temp_i;
        }
    }

    // Cooley-Tukey Butterfly Operations
    const float* cos_table = ctx->cos_table;
    const float* sin_table = ctx->sin_table;

    for (int stage_size = 2; stage_size <= size; stage_size <<= 1) {
        int stage_half = stage_size >> 1;
        int tab_step = size / stage_size;

        for (int i = 0; i < size; i += stage_size) {
            int k = 0;
            for (int j = i; j < i + stage_half; j++) {
                int l = k * tab_step;
                float c = cos_table[l];
                float s = sin_table[l];

                int j_half = j + stage_half;
                float r_half = real[j_half];
                float i_half = imag[j_half];

                float t_real = r_half * c - i_half * s;
                float t_imag = r_half * s + i_half * c;

                real[j_half] = real[j] - t_real;
                imag[j_half] = imag[j] - t_imag;
                real[j] += t_real;
                imag[j] += t_imag;

                k++;
            }
        }
    }

    // For IFFT: take complex conjugate of output and normalize by 1/size
    if (inverse) {
        float scale = 1.0f / (float)size;
        for (int i = 0; i < size; i++) {
            real[i] *= scale;
            imag[i] = -imag[i] * scale;
        }
    }
}

void fft_process(FftContext* ctx, const float* input, float* output_magnitudes) {
    if (!ctx || !input || !output_magnitudes) return;

    int size = ctx->size;
    int half_size = ctx->half_size;
    float* real = ctx->real;
    float* imag = ctx->imag;

    // 1. Windowing & zero imaginary
    for (int i = 0; i < size; i++) {
        real[i] = input[i] * ctx->window[i];
        imag[i] = 0.0f;
    }

    // 2. Forward FFT
    fft_transform(ctx, real, imag, 0);

    // 3. Calculate Magnitudes (First half - Nyquist)
    for (int i = 0; i < half_size; i++) {
        float r = real[i];
        float im = imag[i];
        output_magnitudes[i] = sqrtf(r * r + im * im);
    }
}
