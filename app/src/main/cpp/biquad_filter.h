#ifndef BIQUAD_FILTER_H
#define BIQUAD_FILTER_H

#include <stdint.h>
#include "fft.h"
#include "effects_dsp.h"

#ifdef __cplusplus
extern "C" {
#endif

// Filter types
enum BiquadFilterType {
    BIQUAD_LOWPASS = 0,
    BIQUAD_HIGHPASS = 1,
    BIQUAD_BANDPASS = 2,
    BIQUAD_PEAK = 3,
    BIQUAD_NOTCH = 4,
    BIQUAD_LOWSHELF = 5,
    BIQUAD_HIGHSHELF = 6,
    BIQUAD_GAIN = 7
};

enum EqualizerType {
    EQ_TYPE_IIR = 0,
    EQ_TYPE_FFT = 1
};

typedef struct {
    float b0, b1, b2;
    float a1, a2;
    float x1, x2;
    float y1, y2;
} BiquadFilter;

typedef struct {
    float* buffer;
    int size;
    int position;
    float decay;
    int feedback;
    float sample_rate;
} EchoDelay;

typedef struct {
    int channel_count;
    int band_count;
    float sample_rate;
    int eq_type;              // 0 = IIR, 1 = FFT
    BiquadFilter* filters;    // channel_count * band_count
    float* channel_maxs;      // [channel_count]
    float limiter_envelope;   // Stereo-linked peak limiter envelope with release decay
    float** channel_buffers;  // [channel_count][buffer_capacity]
    int buffer_capacity;
    EchoDelay* channel_delays;// [channel_count]

    // FFT Equalizer
    float* band_gains_db;
    float* band_center_freqs;
    int fft_size;
    int hop_size;
    float* fft_eq_gains;
    FftContext* fft_ctx;
    float** fft_in_fifo;
    int* fft_in_fifo_count;
    float** fft_history;
    float** fft_overlap;
    float** fft_out_fifo;
    int* fft_out_fifo_head;
    int* fft_out_fifo_count;
    int fft_out_fifo_capacity;

    // Advanced Audio Effects
    Virtualizer3D* virtualizer;
    ReverbEffect* reverb;
    ChorusEffect* chorus;
    FlangerEffect* flanger;
    PolyphonyEffect* polyphony;
} BiquadEqualizer;

void biquad_reset(BiquadFilter* filter);
void biquad_configure(BiquadFilter* filter, int type, float center_freq, float sample_rate, float Q, float gainDB);
float biquad_filter_sample(BiquadFilter* filter, float x);

BiquadEqualizer* biquad_equalizer_create(int channel_count, int band_count, float sample_rate);
void biquad_equalizer_destroy(BiquadEqualizer* eq);
void biquad_equalizer_reset(BiquadEqualizer* eq);
void biquad_equalizer_reset_limiter(BiquadEqualizer* eq);
void biquad_equalizer_configure_band(BiquadEqualizer* eq, int channel, int band, int type, float center_freq, float sample_rate, float Q, float gainDB);
void biquad_equalizer_set_echo_params(BiquadEqualizer* eq, float delay_time, float decay, int feedback, float sample_rate);
void biquad_equalizer_set_type(BiquadEqualizer* eq, int type);

void biquad_equalizer_set_virtualizer_params(BiquadEqualizer* eq, int enabled, float strength);
void biquad_equalizer_set_reverb_params(BiquadEqualizer* eq, int enabled, float room_size, float damping, float mix);
void biquad_equalizer_set_chorus_params(BiquadEqualizer* eq, int enabled, float rate, float depth, float mix);
void biquad_equalizer_set_flanger_params(BiquadEqualizer* eq, int enabled, float rate, float depth, float feedback, float mix);
void biquad_equalizer_set_polyphony_params(BiquadEqualizer* eq, int enabled, int semitones, float detune_cents, float mix);

// Processes interleaved PCM directly:
// input: input PCM byte buffer
// output: output PCM byte buffer
// frames_count: number of frames
// channel_count: channels
// encoding: 0 for 16-bit PCM, 1 for 32-bit Float PCM
// eq_active: 1 if equalizer cascade should be applied, 0 if bypass
// echo_active: 1 if echo delay should be applied, 0 if bypass
// vis_out: optional float buffer for mixed output samples for FFT visualization
// max_vis_count: max samples to write to vis_out
// actual_vis_count: receives number of samples written to vis_out
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
);

#ifdef __cplusplus
}
#endif

#endif // BIQUAD_FILTER_H
