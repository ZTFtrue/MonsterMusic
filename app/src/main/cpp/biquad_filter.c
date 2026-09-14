#include "biquad_filter.h"
#include <math.h>
#include <stdlib.h>
#include <string.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

// =========================================================================
// Single Biquad Filter Implementation
// =========================================================================

void biquad_reset(BiquadFilter* filter) {
    if (!filter) return;
    filter->x1 = 0.0f;
    filter->x2 = 0.0f;
    filter->y1 = 0.0f;
    filter->y2 = 0.0f;
}

void biquad_configure(BiquadFilter* filter, int type, float center_freq, float sample_rate, float Q, float gainDB) {
    if (!filter || sample_rate <= 0.0f) return;
    biquad_reset(filter);
    if (Q == 0.0f) {
        Q = 1e-9f;
    }

    float gain_abs = powf(10.0f, gainDB / 40.0f);
    double omega = 2.0 * M_PI * (double)center_freq / (double)sample_rate;
    float sn = (float)sin(omega);
    float cs = (float)cos(omega);
    float alpha = sn / (2.0f * Q);
    float beta = sqrtf(gain_abs + gain_abs);

    float a0 = 1.0f, a1 = 0.0f, a2 = 0.0f;
    float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f;

    switch (type) {
        case BIQUAD_GAIN:
            b0 = gain_abs;
            a0 = 1.0f;
            a1 = a2 = b1 = b2 = 0.0f;
            break;
        case BIQUAD_BANDPASS:
            b0 = alpha;
            b1 = 0.0f;
            b2 = -alpha;
            a0 = 1.0f + alpha;
            a1 = -2.0f * cs;
            a2 = 1.0f - alpha;
            break;
        case BIQUAD_LOWPASS:
            b0 = (1.0f - cs) / 2.0f;
            b1 = 1.0f - cs;
            b2 = (1.0f - cs) / 2.0f;
            a0 = 1.0f + alpha;
            a1 = -2.0f * cs;
            a2 = 1.0f - alpha;
            break;
        case BIQUAD_HIGHPASS:
            b0 = b2 = (1.0f + cs) / 2.0f;
            b1 = -(1.0f + cs);
            a0 = 1.0f + alpha;
            a1 = -2.0f * cs;
            a2 = 1.0f - alpha;
            break;
        case BIQUAD_NOTCH:
            b0 = 1.0f;
            b1 = -2.0f * cs;
            b2 = 1.0f;
            a0 = 1.0f + alpha;
            a1 = -2.0f * cs;
            a2 = 1.0f - alpha;
            break;
        case BIQUAD_PEAK:
            b0 = 1.0f + (alpha * gain_abs);
            b1 = -2.0f * cs;
            b2 = 1.0f - (alpha * gain_abs);
            a0 = 1.0f + (alpha / gain_abs);
            a1 = -2.0f * cs;
            a2 = 1.0f - (alpha / gain_abs);
            break;
        case BIQUAD_LOWSHELF:
            b0 = gain_abs * ((gain_abs + 1.0f) - (gain_abs - 1.0f) * cs + beta * sn);
            b1 = 2.0f * gain_abs * ((gain_abs - 1.0f) - (gain_abs + 1.0f) * cs);
            b2 = gain_abs * ((gain_abs + 1.0f) - (gain_abs - 1.0f) * cs - beta * sn);
            a0 = (gain_abs + 1.0f) + (gain_abs - 1.0f) * cs + beta * sn;
            a1 = -2.0f * ((gain_abs - 1.0f) + (gain_abs + 1.0f) * cs);
            a2 = (gain_abs + 1.0f) + (gain_abs - 1.0f) * cs - beta * sn;
            break;
        case BIQUAD_HIGHSHELF:
            b0 = gain_abs * ((gain_abs + 1.0f) + (gain_abs - 1.0f) * cs + beta * sn);
            b1 = -2.0f * gain_abs * ((gain_abs - 1.0f) + (gain_abs + 1.0f) * cs);
            b2 = gain_abs * ((gain_abs + 1.0f) + (gain_abs - 1.0f) * cs - beta * sn);
            a0 = (gain_abs + 1.0f) - (gain_abs - 1.0f) * cs + beta * sn;
            a1 = 2.0f * ((gain_abs - 1.0f) - (gain_abs + 1.0f) * cs);
            a2 = (gain_abs + 1.0f) - (gain_abs - 1.0f) * cs - beta * sn;
            break;
        default:
            b0 = 1.0f;
            b1 = b2 = a1 = a2 = 0.0f;
            a0 = 1.0f;
            break;
    }

    // prescale filter constants
    filter->b0 = b0 / a0;
    filter->b1 = b1 / a0;
    filter->b2 = b2 / a0;
    filter->a1 = a1 / a0;
    filter->a2 = a2 / a0;
}

float biquad_filter_sample(BiquadFilter* filter, float x) {
    float y = filter->b0 * x + filter->b1 * filter->x1 + filter->b2 * filter->x2
              - filter->a1 * filter->y1 - filter->a2 * filter->y2;
    filter->x2 = filter->x1;
    filter->x1 = x;
    filter->y2 = filter->y1;
    filter->y1 = y;
    return y;
}

// =========================================================================
// Echo Delay Implementation
// =========================================================================

static void echo_delay_init(EchoDelay* echo, float sample_rate) {
    echo->buffer = NULL;
    echo->size = 0;
    echo->position = 0;
    echo->decay = 0.0f;
    echo->feedback = 0;
    echo->sample_rate = sample_rate > 0.0f ? sample_rate : 44100.0f;
}

static void echo_delay_free(EchoDelay* echo) {
    if (echo->buffer) {
        free(echo->buffer);
        echo->buffer = NULL;
    }
    echo->size = 0;
    echo->position = 0;
}

static void echo_delay_set_params(EchoDelay* echo, float delay_time, float decay, int feedback, float sample_rate) {
    if (sample_rate > 0.0f) {
        echo->sample_rate = sample_rate;
    }
    echo->decay = decay < 0.0f ? 0.0f : (decay > 1.0f ? 1.0f : decay);
    echo->feedback = feedback;

    if (delay_time > 0.0f) {
        int new_size = (int)(echo->sample_rate * delay_time);
        if (new_size > 0 && new_size != echo->size) {
            float* new_buf = (float*) calloc(new_size, sizeof(float));
            if (new_buf) {
                if (echo->buffer && echo->size > 0) {
                    int copy_len = echo->size < new_size ? echo->size : new_size;
                    for (int i = 0; i < copy_len; i++) {
                        int src_idx = (echo->position + i) % echo->size;
                        new_buf[i] = echo->buffer[src_idx];
                    }
                    free(echo->buffer);
                }
                echo->buffer = new_buf;
                echo->size = new_size;
                echo->position = 0;
            }
        }
    }
}

static void echo_delay_reset(EchoDelay* echo) {
    if (echo->buffer && echo->size > 0) {
        memset(echo->buffer, 0, echo->size * sizeof(float));
    }
    echo->position = 0;
}

static void echo_delay_process(EchoDelay* echo, float* samples, int count) {
    if (!echo || !echo->buffer || echo->size <= 0) return;
    int buffer_len = echo->size;
    int cursor = echo->position;
    float decay = echo->decay;
    int feedback = echo->feedback;

    for (int i = 0; i < count; i++) {
        float in = samples[i];
        float delay_sample = echo->buffer[cursor];
        float out = in + (delay_sample * decay);
        samples[i] = out;
        echo->buffer[cursor] = feedback ? out : in;
        cursor++;
        if (cursor >= buffer_len) {
            cursor = 0;
        }
    }
    echo->position = cursor;
}

// =========================================================================
// Biquad Equalizer & PCM Pipeline Implementation
// =========================================================================

static void biquad_equalizer_update_fft_gains(BiquadEqualizer* eq) {
    if (!eq || !eq->fft_eq_gains) return;
    int half_size = eq->fft_size / 2;
    float sample_rate = eq->sample_rate > 0.0f ? eq->sample_rate : 44100.0f;
    float freq_step = sample_rate / (float)eq->fft_size;
    int band_count = eq->band_count;

    if (band_count <= 0) {
        for (int k = 0; k <= half_size; k++) eq->fft_eq_gains[k] = 1.0f;
        return;
    }

    for (int k = 0; k <= half_size; k++) {
        float f = (float)k * freq_step;
        float gain_db = 0.0f;

        if (f <= eq->band_center_freqs[0]) {
            gain_db = eq->band_gains_db[0];
        } else if (f >= eq->band_center_freqs[band_count - 1]) {
            gain_db = eq->band_gains_db[band_count - 1];
        } else {
            for (int b = 0; b < band_count - 1; b++) {
                float f0 = eq->band_center_freqs[b];
                float f1 = eq->band_center_freqs[b + 1];
                if (f >= f0 && f <= f1) {
                    float log_f = log2f(f);
                    float log_f0 = log2f(f0);
                    float log_f1 = log2f(f1);
                    float t = (log_f - log_f0) / (log_f1 - log_f0);
                    gain_db = (1.0f - t) * eq->band_gains_db[b] + t * eq->band_gains_db[b + 1];
                    break;
                }
            }
        }
        eq->fft_eq_gains[k] = powf(10.0f, gain_db / 20.0f);
    }
}

BiquadEqualizer* biquad_equalizer_create(int channel_count, int band_count, float sample_rate) {
    if (channel_count <= 0 || band_count <= 0) return NULL;
    BiquadEqualizer* eq = (BiquadEqualizer*) malloc(sizeof(BiquadEqualizer));
    if (!eq) return NULL;

    eq->channel_count = channel_count;
    eq->band_count = band_count;
    eq->sample_rate = sample_rate > 0.0f ? sample_rate : 44100.0f;
    eq->eq_type = EQ_TYPE_IIR;

    int total_filters = channel_count * band_count;
    eq->filters = (BiquadFilter*) calloc(total_filters, sizeof(BiquadFilter));
    eq->channel_maxs = (float*) malloc(channel_count * sizeof(float));
    eq->channel_buffers = (float**) malloc(channel_count * sizeof(float*));
    eq->channel_delays = (EchoDelay*) malloc(channel_count * sizeof(EchoDelay));

    eq->buffer_capacity = 4096;
    for (int ch = 0; ch < channel_count; ch++) {
        eq->channel_maxs[ch] = 1.0f;
        eq->channel_buffers[ch] = (float*) malloc(eq->buffer_capacity * sizeof(float));
        echo_delay_init(&eq->channel_delays[ch], eq->sample_rate);
    }

    for (int i = 0; i < total_filters; i++) {
        eq->filters[i].b0 = 1.0f;
    }

    // FFT Equalizer initialization
    eq->band_gains_db = (float*) calloc(band_count, sizeof(float));
    eq->band_center_freqs = (float*) calloc(band_count, sizeof(float));
    const float default_centers[10] = {31.0f, 62.0f, 125.0f, 250.0f, 500.0f, 1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f};
    for (int b = 0; b < band_count; b++) {
        eq->band_center_freqs[b] = (b < 10) ? default_centers[b] : 1000.0f;
    }

    eq->fft_size = 1024;
    eq->hop_size = 512;
    eq->fft_ctx = fft_create(eq->fft_size);
    int half_fft = eq->fft_size / 2;
    eq->fft_eq_gains = (float*) malloc((half_fft + 1) * sizeof(float));
    for (int k = 0; k <= half_fft; k++) {
        eq->fft_eq_gains[k] = 1.0f;
    }

    eq->fft_in_fifo = (float**) malloc(channel_count * sizeof(float*));
    eq->fft_in_fifo_count = (int*) calloc(channel_count, sizeof(int));
    eq->fft_history = (float**) malloc(channel_count * sizeof(float*));
    eq->fft_overlap = (float**) malloc(channel_count * sizeof(float*));
    eq->fft_out_fifo_capacity = 8192;
    eq->fft_out_fifo = (float**) malloc(channel_count * sizeof(float*));
    eq->fft_out_fifo_head = (int*) calloc(channel_count, sizeof(int));
    eq->fft_out_fifo_count = (int*) calloc(channel_count, sizeof(int));

    for (int ch = 0; ch < channel_count; ch++) {
        eq->fft_in_fifo[ch] = (float*) calloc(eq->hop_size, sizeof(float));
        eq->fft_history[ch] = (float*) calloc(eq->hop_size, sizeof(float));
        eq->fft_overlap[ch] = (float*) calloc(eq->hop_size, sizeof(float));
        eq->fft_out_fifo[ch] = (float*) calloc(eq->fft_out_fifo_capacity, sizeof(float));
        eq->fft_out_fifo_count[ch] = eq->hop_size;
    }

    return eq;
}

void biquad_equalizer_destroy(BiquadEqualizer* eq) {
    if (!eq) return;
    if (eq->filters) {
        free(eq->filters);
        eq->filters = NULL;
    }
    if (eq->channel_maxs) {
        free(eq->channel_maxs);
        eq->channel_maxs = NULL;
    }
    if (eq->channel_buffers) {
        for (int ch = 0; ch < eq->channel_count; ch++) {
            if (eq->channel_buffers[ch]) {
                free(eq->channel_buffers[ch]);
            }
        }
        free(eq->channel_buffers);
        eq->channel_buffers = NULL;
    }
    if (eq->channel_delays) {
        for (int ch = 0; ch < eq->channel_count; ch++) {
            echo_delay_free(&eq->channel_delays[ch]);
        }
        free(eq->channel_delays);
        eq->channel_delays = NULL;
    }
    if (eq->band_gains_db) free(eq->band_gains_db);
    if (eq->band_center_freqs) free(eq->band_center_freqs);
    if (eq->fft_eq_gains) free(eq->fft_eq_gains);
    if (eq->fft_ctx) fft_destroy(eq->fft_ctx);
    if (eq->fft_in_fifo) {
        for (int ch = 0; ch < eq->channel_count; ch++) free(eq->fft_in_fifo[ch]);
        free(eq->fft_in_fifo);
    }
    if (eq->fft_in_fifo_count) free(eq->fft_in_fifo_count);
    if (eq->fft_history) {
        for (int ch = 0; ch < eq->channel_count; ch++) free(eq->fft_history[ch]);
        free(eq->fft_history);
    }
    if (eq->fft_overlap) {
        for (int ch = 0; ch < eq->channel_count; ch++) free(eq->fft_overlap[ch]);
        free(eq->fft_overlap);
    }
    if (eq->fft_out_fifo) {
        for (int ch = 0; ch < eq->channel_count; ch++) free(eq->fft_out_fifo[ch]);
        free(eq->fft_out_fifo);
    }
    if (eq->fft_out_fifo_head) free(eq->fft_out_fifo_head);
    if (eq->fft_out_fifo_count) free(eq->fft_out_fifo_count);
    free(eq);
}

void biquad_equalizer_reset(BiquadEqualizer* eq) {
    if (!eq) return;
    int total_filters = eq->channel_count * eq->band_count;
    for (int i = 0; i < total_filters; i++) {
        biquad_reset(&eq->filters[i]);
    }
    for (int ch = 0; ch < eq->channel_count; ch++) {
        echo_delay_reset(&eq->channel_delays[ch]);
        eq->fft_in_fifo_count[ch] = 0;
        memset(eq->fft_history[ch], 0, eq->hop_size * sizeof(float));
        memset(eq->fft_overlap[ch], 0, eq->hop_size * sizeof(float));
        eq->fft_out_fifo_head[ch] = 0;
        eq->fft_out_fifo_count[ch] = eq->hop_size;
        memset(eq->fft_out_fifo[ch], 0, eq->fft_out_fifo_capacity * sizeof(float));
    }
}

void biquad_equalizer_reset_limiter(BiquadEqualizer* eq) {
    if (!eq || !eq->channel_maxs) return;
    for (int ch = 0; ch < eq->channel_count; ch++) {
        eq->channel_maxs[ch] = 1.0f;
    }
}

void biquad_equalizer_configure_band(BiquadEqualizer* eq, int channel, int band, int type, float center_freq, float sample_rate, float Q, float gainDB) {
    if (!eq || !eq->filters) return;
    if (channel < 0 || channel >= eq->channel_count) return;
    if (band < 0 || band >= eq->band_count) return;

    int idx = channel * eq->band_count + band;
    biquad_configure(&eq->filters[idx], type, center_freq, sample_rate, Q, gainDB);

    eq->band_gains_db[band] = gainDB;
    eq->band_center_freqs[band] = center_freq;
    biquad_equalizer_update_fft_gains(eq);
}

void biquad_equalizer_set_echo_params(BiquadEqualizer* eq, float delay_time, float decay, int feedback, float sample_rate) {
    if (!eq || !eq->channel_delays) return;
    for (int ch = 0; ch < eq->channel_count; ch++) {
        echo_delay_set_params(&eq->channel_delays[ch], delay_time, decay, feedback, sample_rate);
    }
}

void biquad_equalizer_set_type(BiquadEqualizer* eq, int type) {
    if (!eq) return;
    eq->eq_type = (type == EQ_TYPE_FFT) ? EQ_TYPE_FFT : EQ_TYPE_IIR;
}

static void ensure_channel_capacity(BiquadEqualizer* eq, int frames_count) {
    if (frames_count > eq->buffer_capacity) {
        int new_cap = frames_count + 1024;
        for (int ch = 0; ch < eq->channel_count; ch++) {
            float* new_buf = (float*) realloc(eq->channel_buffers[ch], new_cap * sizeof(float));
            if (new_buf) {
                eq->channel_buffers[ch] = new_buf;
            }
        }
        eq->buffer_capacity = new_cap;
    }
}

static void fft_equalizer_process_channel(BiquadEqualizer* eq, int ch, float* samples, int frames_count) {
    if (!eq || !eq->fft_ctx || ch < 0 || ch >= eq->channel_count || !samples || frames_count <= 0) return;

    int H = eq->hop_size;
    int N = eq->fft_size;
    FftContext* ctx = eq->fft_ctx;
    float* in_fifo = eq->fft_in_fifo[ch];
    int in_count = eq->fft_in_fifo_count[ch];
    float* history = eq->fft_history[ch];
    float* overlap = eq->fft_overlap[ch];
    float* out_fifo = eq->fft_out_fifo[ch];
    int cap = eq->fft_out_fifo_capacity;
    int head = eq->fft_out_fifo_head[ch];
    int out_count = eq->fft_out_fifo_count[ch];

    int needed = out_count + frames_count + H + 512;
    if (needed > cap) {
        int new_cap = needed + 2048;
        float* new_buf = (float*) malloc(new_cap * sizeof(float));
        for (int i = 0; i < out_count; i++) {
            new_buf[i] = out_fifo[(head + i) % cap];
        }
        free(out_fifo);
        eq->fft_out_fifo[ch] = new_buf;
        out_fifo = new_buf;
        head = 0;
        cap = new_cap;
        eq->fft_out_fifo_capacity = new_cap;
    }

    for (int i = 0; i < frames_count; i++) {
        in_fifo[in_count++] = samples[i];
        if (in_count == H) {
            for (int j = 0; j < H; j++) {
                ctx->real[j] = history[j] * ctx->sqrt_hann_window[j];
                ctx->imag[j] = 0.0f;
            }
            for (int j = 0; j < H; j++) {
                ctx->real[H + j] = in_fifo[j] * ctx->sqrt_hann_window[H + j];
                ctx->imag[H + j] = 0.0f;
            }
            memcpy(history, in_fifo, H * sizeof(float));
            in_count = 0;

            fft_transform(ctx, ctx->real, ctx->imag, 0);

            for (int k = 0; k <= H; k++) {
                float g = eq->fft_eq_gains[k];
                ctx->real[k] *= g;
                ctx->imag[k] *= g;
            }
            for (int k = 1; k < H; k++) {
                float g = eq->fft_eq_gains[k];
                ctx->real[N - k] *= g;
                ctx->imag[N - k] *= g;
            }

            fft_transform(ctx, ctx->real, ctx->imag, 1);

            for (int j = 0; j < N; j++) {
                ctx->real[j] *= ctx->sqrt_hann_window[j];
            }

            for (int j = 0; j < H; j++) {
                float out_val = overlap[j] + ctx->real[j];
                out_fifo[(head + out_count) % cap] = out_val;
                out_count++;
            }
            for (int j = 0; j < H; j++) {
                overlap[j] = ctx->real[H + j];
            }
        }
    }

    eq->fft_in_fifo_count[ch] = in_count;

    int to_pop = frames_count < out_count ? frames_count : out_count;
    for (int i = 0; i < to_pop; i++) {
        samples[i] = out_fifo[head];
        head = (head + 1) % cap;
        out_count--;
    }
    for (int i = to_pop; i < frames_count; i++) {
        samples[i] = 0.0f;
    }

    eq->fft_out_fifo_head[ch] = head;
    eq->fft_out_fifo_count[ch] = out_count;
}

void biquad_equalizer_process_pcm(
    BiquadEqualizer* eq,
    const void* input,
    void* output,
    int frames_count,
    int channel_count,
    int encoding,
    int eq_active,
    int echo_active,
    float* vis_out,
    int max_vis_count,
    int* actual_vis_count
) {
    if (!eq || !input || !output || frames_count <= 0 || channel_count <= 0) {
        if (actual_vis_count) *actual_vis_count = 0;
        return;
    }

    ensure_channel_capacity(eq, frames_count);
    int active_channels = channel_count < eq->channel_count ? channel_count : eq->channel_count;

    // 1. De-interleave & Convert to Float [-1.0, 1.0]
    if (encoding == 1) {
        // Float PCM
        const float* in_float = (const float*) input;
        for (int i = 0; i < frames_count; i++) {
            for (int ch = 0; ch < active_channels; ch++) {
                eq->channel_buffers[ch][i] = in_float[i * channel_count + ch];
            }
        }
    } else {
        // 16-bit PCM
        const int16_t* in_16 = (const int16_t*) input;
        const float inv_scale = 1.0f / 32768.0f;
        for (int i = 0; i < frames_count; i++) {
            for (int ch = 0; ch < active_channels; ch++) {
                eq->channel_buffers[ch][i] = ((float) in_16[i * channel_count + ch]) * inv_scale;
            }
        }
    }

    // 2. Apply Echo Delay (if active)
    if (echo_active) {
        for (int ch = 0; ch < active_channels; ch++) {
            echo_delay_process(&eq->channel_delays[ch], eq->channel_buffers[ch], frames_count);
        }
    }

    // 3. Apply Equalizer (if active)
    if (eq_active) {
        if (eq->eq_type == EQ_TYPE_FFT) {
            for (int ch = 0; ch < active_channels; ch++) {
                fft_equalizer_process_channel(eq, ch, eq->channel_buffers[ch], frames_count);
            }
        } else {
            int band_count = eq->band_count;
            for (int ch = 0; ch < active_channels; ch++) {
                float* samples = eq->channel_buffers[ch];
                BiquadFilter* filters = &eq->filters[ch * band_count];
                for (int i = 0; i < frames_count; i++) {
                    float s = samples[i];
                    for (int b = 0; b < band_count; b++) {
                        BiquadFilter* f = &filters[b];
                        float y = f->b0 * s + f->b1 * f->x1 + f->b2 * f->x2 - f->a1 * f->y1 - f->a2 * f->y2;
                        f->x2 = f->x1;
                        f->x1 = s;
                        f->y2 = f->y1;
                        f->y1 = y;
                        s = y;
                    }
                    samples[i] = s;
                }
            }
        }
    }

    // 4. Limiter Logic (if EQ or Echo active)
    if (eq_active || echo_active) {
        for (int ch = 0; ch < active_channels; ch++) {
            float* samples = eq->channel_buffers[ch];
            float max_val = 0.0f;
            for (int i = 0; i < frames_count; i++) {
                float abs_val = fabsf(samples[i]);
                if (abs_val > max_val) max_val = abs_val;
            }
            if (max_val > eq->channel_maxs[ch]) {
                eq->channel_maxs[ch] = max_val;
            }
            if (eq->channel_maxs[ch] > 1.0f) {
                float inv_max = 1.0f / eq->channel_maxs[ch];
                for (int i = 0; i < frames_count; i++) {
                    samples[i] *= inv_max;
                }
            }
        }
    }

    // 5. Downmix for Visualization (if requested)
    if (vis_out != NULL && max_vis_count > 0) {
        int vis_samples = frames_count < max_vis_count ? frames_count : max_vis_count;
        if (active_channels >= 2) {
            for (int i = 0; i < vis_samples; i++) {
                vis_out[i] = (eq->channel_buffers[0][i] + eq->channel_buffers[1][i]) * 0.5f;
            }
        } else {
            for (int i = 0; i < vis_samples; i++) {
                vis_out[i] = eq->channel_buffers[0][i];
            }
        }
        if (actual_vis_count) *actual_vis_count = vis_samples;
    } else {
        if (actual_vis_count) *actual_vis_count = 0;
    }

    // 6. Interleave & Write Output
    if (encoding == 1) {
        // Float PCM
        float* out_float = (float*) output;
        for (int i = 0; i < frames_count; i++) {
            for (int ch = 0; ch < active_channels; ch++) {
                out_float[i * channel_count + ch] = eq->channel_buffers[ch][i];
            }
        }
    } else {
        // 16-bit PCM with saturation clipping
        int16_t* out_16 = (int16_t*) output;
        for (int i = 0; i < frames_count; i++) {
            for (int ch = 0; ch < active_channels; ch++) {
                float v = eq->channel_buffers[ch][i] * 32767.0f;
                if (v > 32767.0f) v = 32767.0f;
                else if (v < -32768.0f) v = -32768.0f;
                out_16[i * channel_count + ch] = (int16_t) v;
            }
        }
    }
}
