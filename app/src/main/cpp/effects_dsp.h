#ifndef EFFECTS_DSP_H
#define EFFECTS_DSP_H

#include <stdint.h>
#include <stdbool.h>
#include <math.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#ifdef __cplusplus
extern "C" {
#endif

// =========================================================================
// Shared DSP Helpers
// =========================================================================

static inline float undenormalise(float x) {
    if (fabsf(x) < 1.0e-15f) return 0.0f;
    return x;
}

// Optimized soft clipping: asymptotic saturation towards 1.0f without hard-clipping discontinuity
static inline float soft_clip_optimized(float x) {
    if (isnan(x)) return 0.0f;
    if (x > 0.95f) {
        float diff = x - 0.95f;
        float res = 0.95f + (0.05f * diff) / (0.05f + diff);
        return res;
    } else if (x < -0.95f) {
        float diff = -0.95f - x;
        float res = -0.95f - (0.05f * diff) / (0.05f + diff);
        return res;
    }
    return x;
}

// Original soft clipping formula (res reaches up to ~1.95f, intended for use with peak tracking & scaling)
static inline float soft_clip_original(float x) {
    if (isnan(x)) return 0.0f;
    if (x > 0.95f) {
        float diff = x - 0.95f;
        float res = 0.95f + diff / (1.0f + diff);
        return res;
    } else if (x < -0.95f) {
        float diff = -0.95f - x;
        float res = -0.95f - diff / (1.0f + diff);
        return res;
    }
    return x;
}

static inline float soft_clip(float x) {
    return soft_clip_optimized(x);
}


// =========================================================================
// 1. 3D Virtual Surround (Optimized Spatializer)
// =========================================================================
typedef struct {
    float sample_rate;
    int enabled;
    float strength;           // 0.0f .. 1.0f (mapped from 0 .. 1000)
    float headroom;

    // Bass anchor 1-pole filter state
    float bass_lp_left;
    float bass_lp_right;
    float bass_alpha;

    // Binaural cross-feed delay & head-shadow lowpass
    float* cross_buf_left;
    float* cross_buf_right;
    int cross_buf_size;
    int cross_head;
    int cross_delay_samples;
    float cross_lp_left;
    float cross_lp_right;
    float cross_alpha;
} Virtualizer3D;

Virtualizer3D* virtualizer_create(float sample_rate);
void virtualizer_destroy(Virtualizer3D* v);
void virtualizer_reset(Virtualizer3D* v);
void virtualizer_set_params(Virtualizer3D* v, int enabled, float strength, float sample_rate);
void virtualizer_process(Virtualizer3D* v, float* left, float* right, int count);

// =========================================================================
// 2. Reverb (Freeverb Algorithmic Reverb)
// =========================================================================
#define FREEVERB_NUM_COMBS 8
#define FREEVERB_NUM_ALLPASSES 4

typedef struct {
    float* buffer;
    int size;
    int index;
    float filter_store;
} CombFilter;

typedef struct {
    float* buffer;
    int size;
    int index;
    float feedback;
} AllpassFilter;

typedef struct {
    float sample_rate;
    int enabled;
    float room_size;          // 0.0f .. 1.0f
    float damping;            // 0.0f .. 1.0f
    float mix;                // 0.0f .. 1.0f
    float feedback;

    // DC Blocker filter state
    float dc_in;
    float dc_out;
    float dc_r;

    CombFilter combs_l[FREEVERB_NUM_COMBS];
    CombFilter combs_r[FREEVERB_NUM_COMBS];
    AllpassFilter allpasses_l[FREEVERB_NUM_ALLPASSES];
    AllpassFilter allpasses_r[FREEVERB_NUM_ALLPASSES];
} ReverbEffect;

ReverbEffect* reverb_create(float sample_rate);
void reverb_destroy(ReverbEffect* rev);
void reverb_reset(ReverbEffect* rev);
void reverb_set_params(ReverbEffect* rev, int enabled, float room_size, float damping, float mix, float sample_rate);
void reverb_process(ReverbEffect* rev, float* left, float* right, int count);

// =========================================================================
// 3. Chorus (Stereo Modulated Circular Delay)
// =========================================================================
typedef struct {
    float sample_rate;
    int enabled;
    float rate;               // 0.1f .. 5.0f Hz
    float depth;              // 0.0f .. 1.0f
    float mix;                // 0.0f .. 1.0f

    float* buffer_l;
    float* buffer_r;
    int buffer_size;
    int write_pos;
    float lfo_phase;
} ChorusEffect;

ChorusEffect* chorus_create(float sample_rate);
void chorus_destroy(ChorusEffect* c);
void chorus_reset(ChorusEffect* c);
void chorus_set_params(ChorusEffect* c, int enabled, float rate, float depth, float mix, float sample_rate);
void chorus_process(ChorusEffect* c, float* left, float* right, int count);

// =========================================================================
// 4. Flanger (Short Modulated Delay with Feedback)
// =========================================================================
typedef struct {
    float sample_rate;
    int enabled;
    float rate;               // 0.05f .. 3.0f Hz
    float depth;              // 0.0f .. 1.0f
    float feedback;           // -0.85f .. 0.85f
    float mix;                // 0.0f .. 1.0f

    float* buffer_l;
    float* buffer_r;
    int buffer_size;
    int write_pos;
    float lfo_phase;
} FlangerEffect;

FlangerEffect* flanger_create(float sample_rate);
void flanger_destroy(FlangerEffect* f);
void flanger_reset(FlangerEffect* f);
void flanger_set_params(FlangerEffect* f, int enabled, float rate, float depth, float feedback, float mix, float sample_rate);
void flanger_process(FlangerEffect* f, float* left, float* right, int count);

// =========================================================================
// 5. Polyphony (Dual Crossfading Grain-Delay Pitch Shifter / Harmonizer)
// =========================================================================
typedef struct {
    float sample_rate;
    int enabled;
    int semitones;            // -12 .. +12
    float detune_cents;       // -50.0f .. +50.0f
    float mix;                // 0.0f .. 1.0f
    float pitch_ratio;

    float* buffer_l;
    float* buffer_r;
    int buffer_size;
    int write_pos;

    float window_size;
    float grain_pos1;
    float grain_pos2;
} PolyphonyEffect;

PolyphonyEffect* polyphony_create(float sample_rate);
void polyphony_destroy(PolyphonyEffect* p);
void polyphony_reset(PolyphonyEffect* p);
void polyphony_set_params(PolyphonyEffect* p, int enabled, int semitones, float detune_cents, float mix, float sample_rate);
void polyphony_process(PolyphonyEffect* p, float* left, float* right, int count);

#ifdef __cplusplus
}
#endif

#endif // EFFECTS_DSP_H
