#include "effects_dsp.h"
#include <math.h>
#include <stdlib.h>
#include <string.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

// =========================================================================
// Helpers
// =========================================================================

static inline float soft_clip(float x) {
    if (isnan(x)) return 0.0f;
    if (x > 0.95f) {
        float diff = x - 0.95f;
        float res = 0.95f + (0.05f * diff) / (0.05f + diff);
        return res > 1.0f ? 1.0f : res;
    } else if (x < -0.95f) {
        float diff = -0.95f - x;
        float res = -0.95f - (0.05f * diff) / (0.05f + diff);
        return res < -1.0f ? -1.0f : res;
    }
    return x;
}

static inline float undenormalise(float x) {
    if (fabsf(x) < 1.0e-15f) return 0.0f;
    return x;
}

static inline float read_fractional(const float* buffer, int size, float read_index) {
    if (!buffer || size <= 0 || isnan(read_index) || isinf(read_index)) return 0.0f;
    while (read_index < 0.0f) read_index += (float)size;
    while (read_index >= (float)size) read_index -= (float)size;

    int idx0 = (int)read_index;
    int idx1 = idx0 + 1;
    if (idx1 >= size) idx1 = 0;

    float frac = read_index - (float)idx0;
    float val = buffer[idx0] * (1.0f - frac) + buffer[idx1] * frac;
    return undenormalise(val);
}

// =========================================================================
// 1. 3D Virtual Surround Implementation
// =========================================================================

Virtualizer3D* virtualizer_create(float sample_rate) {
    Virtualizer3D* v = (Virtualizer3D*)calloc(1, sizeof(Virtualizer3D));
    if (!v) return NULL;

    v->sample_rate = sample_rate > 0.0f ? sample_rate : 44100.0f;
    v->cross_buf_size = 512;
    v->cross_buf_left = (float*)calloc(v->cross_buf_size, sizeof(float));
    v->cross_buf_right = (float*)calloc(v->cross_buf_size, sizeof(float));

    virtualizer_set_params(v, 0, 0.0f, v->sample_rate);
    return v;
}

void virtualizer_destroy(Virtualizer3D* v) {
    if (!v) return;
    if (v->cross_buf_left) free(v->cross_buf_left);
    if (v->cross_buf_right) free(v->cross_buf_right);
    free(v);
}

void virtualizer_reset(Virtualizer3D* v) {
    if (!v) return;
    v->bass_lp_left = 0.0f;
    v->bass_lp_right = 0.0f;
    v->cross_lp_left = 0.0f;
    v->cross_lp_right = 0.0f;
    v->cross_head = 0;
    if (v->cross_buf_left) memset(v->cross_buf_left, 0, v->cross_buf_size * sizeof(float));
    if (v->cross_buf_right) memset(v->cross_buf_right, 0, v->cross_buf_size * sizeof(float));
}

void virtualizer_set_params(Virtualizer3D* v, int enabled, float strength, float sample_rate) {
    if (!v) return;
    if (sample_rate > 0.0f) v->sample_rate = sample_rate;

    v->enabled = enabled;
    if (strength < 0.0f) strength = 0.0f;
    if (strength > 1.0f) strength = 1.0f;
    v->strength = strength;

    // Headroom factor to prevent harsh clipping when expanding soundstage
    v->headroom = 1.0f / (1.0f + strength * 0.45f);

    // 120 Hz cutoff for bass anchor (mono anchor preserves low-end impact)
    float bass_fc = 120.0f;
    v->bass_alpha = expf(-2.0f * (float)M_PI * bass_fc / v->sample_rate);

    // 0.3 ms delay for interaural time difference (approx 13 samples @ 44.1kHz)
    v->cross_delay_samples = (int)(0.0003f * v->sample_rate);
    if (v->cross_delay_samples < 1) v->cross_delay_samples = 1;
    if (v->cross_delay_samples >= v->cross_buf_size) v->cross_delay_samples = v->cross_buf_size - 1;

    // 2500 Hz cutoff for head-shadowing lowpass filter
    float cross_fc = 2500.0f;
    v->cross_alpha = expf(-2.0f * (float)M_PI * cross_fc / v->sample_rate);
}

void virtualizer_process(Virtualizer3D* v, float* left, float* right, int count) {
    if (!v || !v->enabled || !left || !right || count <= 0) return;

    float strength = v->strength;
    if (strength <= 0.001f) return;

    float bass_a = v->bass_alpha;
    float bass_b = 1.0f - bass_a;
    float cross_a = v->cross_alpha;
    float cross_b = 1.0f - cross_a;
    float headroom = v->headroom;
    float side_boost = 1.0f + strength * 1.5f;
    float cross_gain = strength * 0.32f;

    int delay = v->cross_delay_samples;
    int buf_size = v->cross_buf_size;
    int head = v->cross_head;

    float b_lp_l = v->bass_lp_left;
    float b_lp_r = v->bass_lp_right;
    float c_lp_l = v->cross_lp_left;
    float c_lp_r = v->cross_lp_right;

    for (int i = 0; i < count; i++) {
        float in_l = left[i];
        float in_r = right[i];
        if (isnan(in_l) || isinf(in_l)) in_l = 0.0f;
        if (isnan(in_r) || isinf(in_r)) in_r = 0.0f;

        // 1. Bass Anchor (Low frequencies stay solid & centered in mono)
        b_lp_l = undenormalise(bass_a * b_lp_l + bass_b * in_l);
        b_lp_r = undenormalise(bass_a * b_lp_r + bass_b * in_r);
        float bass_mono = 0.5f * (b_lp_l + b_lp_r);

        float high_l = in_l - b_lp_l;
        float high_r = in_r - b_lp_r;

        // 2. Mid/Side Spatializer on mid & high frequencies
        float mid = 0.5f * (high_l + high_r);
        float side = 0.5f * (high_l - high_r);
        float expanded_side = side * side_boost;

        // 3. Binaural Cross-Feed (opposite ear delay + head shadow)
        v->cross_buf_left[head] = in_l;
        v->cross_buf_right[head] = in_r;

        int read_idx = head - delay;
        if (read_idx < 0) read_idx += buf_size;

        float opp_r = v->cross_buf_right[read_idx];
        float opp_l = v->cross_buf_left[read_idx];

        c_lp_l = undenormalise(cross_a * c_lp_l + cross_b * opp_r);
        c_lp_r = undenormalise(cross_a * c_lp_r + cross_b * opp_l);

        head++;
        if (head >= buf_size) head = 0;

        // 4. Synthesize 3D Binaural Soundfield
        float out_l = (bass_mono + mid + expanded_side + c_lp_l * cross_gain) * headroom;
        float out_r = (bass_mono + mid - expanded_side + c_lp_r * cross_gain) * headroom;

        left[i] = soft_clip(out_l);
        right[i] = soft_clip(out_r);
    }

    v->bass_lp_left = undenormalise(b_lp_l);
    v->bass_lp_right = undenormalise(b_lp_r);
    v->cross_lp_left = undenormalise(c_lp_l);
    v->cross_lp_right = undenormalise(c_lp_r);
    v->cross_head = head;
}

// =========================================================================
// 2. Freeverb Algorithmic Reverb Implementation
// =========================================================================

static const int comb_lengths_44k[FREEVERB_NUM_COMBS] = {
    1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617
};

static const int allpass_lengths_44k[FREEVERB_NUM_ALLPASSES] = {
    556, 441, 341, 225
};

static const int stereo_spread = 23;

ReverbEffect* reverb_create(float sample_rate) {
    ReverbEffect* rev = (ReverbEffect*)calloc(1, sizeof(ReverbEffect));
    if (!rev) return NULL;

    rev->sample_rate = sample_rate > 0.0f ? sample_rate : 44100.0f;
    float sr_scale = rev->sample_rate / 44100.0f;

    for (int i = 0; i < FREEVERB_NUM_COMBS; i++) {
        int len_l = (int)(comb_lengths_44k[i] * sr_scale);
        int len_r = (int)((comb_lengths_44k[i] + stereo_spread) * sr_scale);
        if (len_l < 10) len_l = 10;
        if (len_r < 10) len_r = 10;

        rev->combs_l[i].buffer = (float*)calloc(len_l, sizeof(float));
        rev->combs_l[i].size = len_l;
        rev->combs_r[i].buffer = (float*)calloc(len_r, sizeof(float));
        rev->combs_r[i].size = len_r;
    }

    for (int i = 0; i < FREEVERB_NUM_ALLPASSES; i++) {
        int len_l = (int)(allpass_lengths_44k[i] * sr_scale);
        int len_r = (int)((allpass_lengths_44k[i] + stereo_spread) * sr_scale);
        if (len_l < 5) len_l = 5;
        if (len_r < 5) len_r = 5;

        rev->allpasses_l[i].buffer = (float*)calloc(len_l, sizeof(float));
        rev->allpasses_l[i].size = len_l;
        rev->allpasses_l[i].feedback = 0.5f;

        rev->allpasses_r[i].buffer = (float*)calloc(len_r, sizeof(float));
        rev->allpasses_r[i].size = len_r;
        rev->allpasses_r[i].feedback = 0.5f;
    }

    reverb_set_params(rev, 0, 0.5f, 0.5f, 0.3f, rev->sample_rate);
    rev->dc_in = 0.0f;
    rev->dc_out = 0.0f;
    rev->dc_r = 1.0f - (2.0f * (float)M_PI * 20.0f / rev->sample_rate);
    return rev;
}

void reverb_destroy(ReverbEffect* rev) {
    if (!rev) return;
    for (int i = 0; i < FREEVERB_NUM_COMBS; i++) {
        if (rev->combs_l[i].buffer) free(rev->combs_l[i].buffer);
        if (rev->combs_r[i].buffer) free(rev->combs_r[i].buffer);
    }
    for (int i = 0; i < FREEVERB_NUM_ALLPASSES; i++) {
        if (rev->allpasses_l[i].buffer) free(rev->allpasses_l[i].buffer);
        if (rev->allpasses_r[i].buffer) free(rev->allpasses_r[i].buffer);
    }
    free(rev);
}

void reverb_reset(ReverbEffect* rev) {
    if (!rev) return;
    rev->dc_in = 0.0f;
    rev->dc_out = 0.0f;
    for (int i = 0; i < FREEVERB_NUM_COMBS; i++) {
        if (rev->combs_l[i].buffer) memset(rev->combs_l[i].buffer, 0, rev->combs_l[i].size * sizeof(float));
        rev->combs_l[i].index = 0;
        rev->combs_l[i].filter_store = 0.0f;

        if (rev->combs_r[i].buffer) memset(rev->combs_r[i].buffer, 0, rev->combs_r[i].size * sizeof(float));
        rev->combs_r[i].index = 0;
        rev->combs_r[i].filter_store = 0.0f;
    }
    for (int i = 0; i < FREEVERB_NUM_ALLPASSES; i++) {
        if (rev->allpasses_l[i].buffer) memset(rev->allpasses_l[i].buffer, 0, rev->allpasses_l[i].size * sizeof(float));
        rev->allpasses_l[i].index = 0;

        if (rev->allpasses_r[i].buffer) memset(rev->allpasses_r[i].buffer, 0, rev->allpasses_r[i].size * sizeof(float));
        rev->allpasses_r[i].index = 0;
    }
}

void reverb_set_params(ReverbEffect* rev, int enabled, float room_size, float damping, float mix, float sample_rate) {
    if (!rev) return;
    if (sample_rate > 0.0f) {
        rev->sample_rate = sample_rate;
        rev->dc_r = 1.0f - (2.0f * (float)M_PI * 20.0f / rev->sample_rate);
    }

    rev->enabled = enabled;
    rev->room_size = room_size < 0.0f ? 0.0f : (room_size > 1.0f ? 1.0f : room_size);
    rev->damping = damping < 0.0f ? 0.0f : (damping > 1.0f ? 1.0f : damping) * 0.4f;
    rev->mix = mix < 0.0f ? 0.0f : (mix > 1.0f ? 1.0f : mix);

    // Freeverb feedback formula capped at 0.96 to ensure stability
    rev->feedback = rev->room_size * 0.26f + 0.7f;
    if (rev->feedback > 0.96f) rev->feedback = 0.96f;
}

void reverb_process(ReverbEffect* rev, float* left, float* right, int count) {
    if (!rev || !rev->enabled || !left || !right || count <= 0) return;

    float mix = rev->mix;
    if (mix <= 0.001f) return;

    float dry = 1.0f - mix;
    float wet = mix * 0.025f; // Scale factor for sum of 8 comb filters
    float damp = rev->damping;
    float feedback = rev->feedback;
    float dc_r = rev->dc_r;
    float dc_in = rev->dc_in;
    float dc_out = rev->dc_out;

    for (int i = 0; i < count; i++) {
        float in_l = left[i];
        float in_r = right[i];
        float raw_mono = (in_l + in_r) * 0.5f;

        // 1. DC Blocker high-pass filter (~20 Hz) to eliminate DC accumulation in comb loops
        float next_dc_out = raw_mono - dc_in + dc_r * dc_out;
        dc_in = raw_mono;
        dc_out = undenormalise(next_dc_out);
        float in_mono = dc_out;

        float out_l = 0.0f;
        float out_r = 0.0f;

        // 2. 8 parallel Low-pass Feedback Comb Filters
        for (int c = 0; c < FREEVERB_NUM_COMBS; c++) {
            // Left Comb
            CombFilter* comb_l = &rev->combs_l[c];
            float y_l = undenormalise(comb_l->buffer[comb_l->index]);
            comb_l->filter_store = undenormalise(y_l * (1.0f - damp) + comb_l->filter_store * damp);
            float next_comb_l = in_mono + comb_l->filter_store * feedback;
            if (next_comb_l > 3.0f) next_comb_l = 3.0f;
            else if (next_comb_l < -3.0f) next_comb_l = -3.0f;
            comb_l->buffer[comb_l->index] = next_comb_l;
            if (++comb_l->index >= comb_l->size) comb_l->index = 0;
            out_l += y_l;

            // Right Comb
            CombFilter* comb_r = &rev->combs_r[c];
            float y_r = undenormalise(comb_r->buffer[comb_r->index]);
            comb_r->filter_store = undenormalise(y_r * (1.0f - damp) + comb_r->filter_store * damp);
            float next_comb_r = in_mono + comb_r->filter_store * feedback;
            if (next_comb_r > 3.0f) next_comb_r = 3.0f;
            else if (next_comb_r < -3.0f) next_comb_r = -3.0f;
            comb_r->buffer[comb_r->index] = next_comb_r;
            if (++comb_r->index >= comb_r->size) comb_r->index = 0;
            out_r += y_r;
        }

        // 3. 4 series Allpass Filters for dense diffusion
        for (int a = 0; a < FREEVERB_NUM_ALLPASSES; a++) {
            // Left Allpass
            AllpassFilter* ap_l = &rev->allpasses_l[a];
            float buf_l = undenormalise(ap_l->buffer[ap_l->index]);
            float ap_out_l = -out_l + buf_l;
            float next_ap_l = out_l + (buf_l * ap_l->feedback);
            if (next_ap_l > 3.0f) next_ap_l = 3.0f;
            else if (next_ap_l < -3.0f) next_ap_l = -3.0f;
            ap_l->buffer[ap_l->index] = undenormalise(next_ap_l);
            if (++ap_l->index >= ap_l->size) ap_l->index = 0;
            out_l = ap_out_l;

            // Right Allpass
            AllpassFilter* ap_r = &rev->allpasses_r[a];
            float buf_r = undenormalise(ap_r->buffer[ap_r->index]);
            float ap_out_r = -out_r + buf_r;
            float next_ap_r = out_r + (buf_r * ap_r->feedback);
            if (next_ap_r > 3.0f) next_ap_r = 3.0f;
            else if (next_ap_r < -3.0f) next_ap_r = -3.0f;
            ap_r->buffer[ap_r->index] = undenormalise(next_ap_r);
            if (++ap_r->index >= ap_r->size) ap_r->index = 0;
            out_r = ap_out_r;
        }

        left[i] = soft_clip(in_l * dry + out_l * wet);
        right[i] = soft_clip(in_r * dry + out_r * wet);
    }

    rev->dc_in = dc_in;
    rev->dc_out = dc_out;
}

// =========================================================================
// 3. Chorus Implementation
// =========================================================================

ChorusEffect* chorus_create(float sample_rate) {
    ChorusEffect* c = (ChorusEffect*)calloc(1, sizeof(ChorusEffect));
    if (!c) return NULL;

    c->sample_rate = sample_rate > 0.0f ? sample_rate : 44100.0f;
    // Buffer for up to 60 ms delay with headroom up to 192 kHz
    float max_sr = c->sample_rate > 192000.0f ? c->sample_rate : 192000.0f;
    c->buffer_size = (int)(0.060f * max_sr);
    if (c->buffer_size < 2048) c->buffer_size = 2048;

    c->buffer_l = (float*)calloc(c->buffer_size, sizeof(float));
    c->buffer_r = (float*)calloc(c->buffer_size, sizeof(float));

    chorus_set_params(c, 0, 1.5f, 0.5f, 0.5f, c->sample_rate);
    return c;
}

void chorus_destroy(ChorusEffect* c) {
    if (!c) return;
    if (c->buffer_l) free(c->buffer_l);
    if (c->buffer_r) free(c->buffer_r);
    free(c);
}

void chorus_reset(ChorusEffect* c) {
    if (!c) return;
    c->write_pos = 0;
    c->lfo_phase = 0.0f;
    if (c->buffer_l) memset(c->buffer_l, 0, c->buffer_size * sizeof(float));
    if (c->buffer_r) memset(c->buffer_r, 0, c->buffer_size * sizeof(float));
}

void chorus_set_params(ChorusEffect* c, int enabled, float rate, float depth, float mix, float sample_rate) {
    if (!c) return;
    if (sample_rate > 0.0f) c->sample_rate = sample_rate;

    c->enabled = enabled;
    if (rate < 0.1f) rate = 0.1f;
    if (rate > 5.0f) rate = 5.0f;
    c->rate = rate;

    if (depth < 0.0f) depth = 0.0f;
    if (depth > 1.0f) depth = 1.0f;
    c->depth = depth;

    if (mix < 0.0f) mix = 0.0f;
    if (mix > 1.0f) mix = 1.0f;
    c->mix = mix;
}

void chorus_process(ChorusEffect* c, float* left, float* right, int count) {
    if (!c || !c->enabled || !left || !right || count <= 0) return;

    float mix = c->mix;
    if (mix <= 0.001f) return;

    float dry = 1.0f - mix;
    float wet = mix;

    // Base delay: 20 ms. Mod depth: up to 8 ms.
    float base_delay_samples = 0.020f * c->sample_rate;
    float depth_samples = c->depth * (0.008f * c->sample_rate);

    float phase_step = (float)(2.0 * M_PI * (double)c->rate / (double)c->sample_rate);
    float lfo = c->lfo_phase;
    int w_pos = c->write_pos;
    int b_size = c->buffer_size;

    for (int i = 0; i < count; i++) {
        float in_l = left[i];
        float in_r = right[i];
        if (isnan(in_l) || isinf(in_l)) in_l = 0.0f;
        if (isnan(in_r) || isinf(in_r)) in_r = 0.0f;

        c->buffer_l[w_pos] = in_l;
        c->buffer_r[w_pos] = in_r;

        // Quadrature LFO: Left is sine, Right is cosine (90 deg phase offset for rich stereo!)
        float delay_l = base_delay_samples + depth_samples * sinf(lfo);
        float delay_r = base_delay_samples + depth_samples * cosf(lfo);

        float read_l = (float)w_pos - delay_l;
        float read_r = (float)w_pos - delay_r;

        float wet_l = read_fractional(c->buffer_l, b_size, read_l);
        float wet_r = read_fractional(c->buffer_r, b_size, read_r);

        left[i] = soft_clip(in_l * dry + wet_l * wet);
        right[i] = soft_clip(in_r * dry + wet_r * wet);

        lfo += phase_step;
        if (lfo >= 2.0f * (float)M_PI) lfo -= 2.0f * (float)M_PI;

        w_pos++;
        if (w_pos >= b_size) w_pos = 0;
    }

    c->lfo_phase = lfo;
    c->write_pos = w_pos;
}

// =========================================================================
// 4. Flanger Implementation
// =========================================================================

FlangerEffect* flanger_create(float sample_rate) {
    FlangerEffect* f = (FlangerEffect*)calloc(1, sizeof(FlangerEffect));
    if (!f) return NULL;

    f->sample_rate = sample_rate > 0.0f ? sample_rate : 44100.0f;
    // Buffer for up to 20 ms delay with headroom up to 192 kHz
    float max_sr = f->sample_rate > 192000.0f ? f->sample_rate : 192000.0f;
    f->buffer_size = (int)(0.020f * max_sr);
    if (f->buffer_size < 1024) f->buffer_size = 1024;

    f->buffer_l = (float*)calloc(f->buffer_size, sizeof(float));
    f->buffer_r = (float*)calloc(f->buffer_size, sizeof(float));

    flanger_set_params(f, 0, 0.5f, 0.7f, 0.5f, 0.5f, f->sample_rate);
    return f;
}

void flanger_destroy(FlangerEffect* f) {
    if (!f) return;
    if (f->buffer_l) free(f->buffer_l);
    if (f->buffer_r) free(f->buffer_r);
    free(f);
}

void flanger_reset(FlangerEffect* f) {
    if (!f) return;
    f->write_pos = 0;
    f->lfo_phase = 0.0f;
    if (f->buffer_l) memset(f->buffer_l, 0, f->buffer_size * sizeof(float));
    if (f->buffer_r) memset(f->buffer_r, 0, f->buffer_size * sizeof(float));
}

void flanger_set_params(FlangerEffect* f, int enabled, float rate, float depth, float feedback, float mix, float sample_rate) {
    if (!f) return;
    if (sample_rate > 0.0f) f->sample_rate = sample_rate;

    f->enabled = enabled;
    if (rate < 0.05f) rate = 0.05f;
    if (rate > 3.0f) rate = 3.0f;
    f->rate = rate;

    if (depth < 0.0f) depth = 0.0f;
    if (depth > 1.0f) depth = 1.0f;
    f->depth = depth;

    if (feedback < -0.85f) feedback = -0.85f;
    if (feedback > 0.85f) feedback = 0.85f;
    f->feedback = feedback;

    if (mix < 0.0f) mix = 0.0f;
    if (mix > 1.0f) mix = 1.0f;
    f->mix = mix;
}

void flanger_process(FlangerEffect* f, float* left, float* right, int count) {
    if (!f || !f->enabled || !left || !right || count <= 0) return;

    float mix = f->mix;
    if (mix <= 0.001f) return;

    float dry = 1.0f - mix;
    float wet = mix;
    float feedback = f->feedback;

    // Base delay: 1.0 ms. Mod sweep: up to 3.5 ms (total 1.0 ms .. 4.5 ms)
    float base_delay_samples = 0.0010f * f->sample_rate;
    float depth_samples = f->depth * (0.0035f * f->sample_rate);

    float phase_step = (float)(2.0 * M_PI * (double)f->rate / (double)f->sample_rate);
    float lfo = f->lfo_phase;
    int w_pos = f->write_pos;
    int b_size = f->buffer_size;

    for (int i = 0; i < count; i++) {
        float in_l = left[i];
        float in_r = right[i];
        if (isnan(in_l) || isinf(in_l)) in_l = 0.0f;
        if (isnan(in_r) || isinf(in_r)) in_r = 0.0f;

        // Modulation sweeps between 0 and 1
        float mod_l = 0.5f * (1.0f + sinf(lfo));
        float mod_r = 0.5f * (1.0f + cosf(lfo)); // Stereo swirl

        float delay_l = base_delay_samples + depth_samples * mod_l;
        float delay_r = base_delay_samples + depth_samples * mod_r;

        float read_l = (float)w_pos - delay_l;
        float read_r = (float)w_pos - delay_r;

        float delayed_l = read_fractional(f->buffer_l, b_size, read_l);
        float delayed_r = read_fractional(f->buffer_r, b_size, read_r);

        // Feedback comb filtering with soft saturation and anti-denormalization
        f->buffer_l[w_pos] = undenormalise(soft_clip(in_l + delayed_l * feedback));
        f->buffer_r[w_pos] = undenormalise(soft_clip(in_r + delayed_r * feedback));

        left[i] = soft_clip(in_l * dry + delayed_l * wet);
        right[i] = soft_clip(in_r * dry + delayed_r * wet);

        lfo += phase_step;
        if (lfo >= 2.0f * (float)M_PI) lfo -= 2.0f * (float)M_PI;

        w_pos++;
        if (w_pos >= b_size) w_pos = 0;
    }

    f->lfo_phase = lfo;
    f->write_pos = w_pos;
}

// =========================================================================
// 5. Polyphony (Pitch Shifter / Harmonizer) Implementation
// =========================================================================

PolyphonyEffect* polyphony_create(float sample_rate) {
    PolyphonyEffect* p = (PolyphonyEffect*)calloc(1, sizeof(PolyphonyEffect));
    if (!p) return NULL;

    p->sample_rate = sample_rate > 0.0f ? sample_rate : 44100.0f;
    // Window length: 50 ms
    p->window_size = 0.050f * p->sample_rate;
    if (p->window_size < 512.0f) p->window_size = 512.0f;

    p->buffer_size = (int)(p->window_size * 4.0f);
    p->buffer_l = (float*)calloc(p->buffer_size, sizeof(float));
    p->buffer_r = (float*)calloc(p->buffer_size, sizeof(float));

    p->grain_pos1 = 0.0f;
    p->grain_pos2 = p->window_size * 0.5f;

    polyphony_set_params(p, 0, 0, 0.0f, 0.5f, p->sample_rate);
    return p;
}

void polyphony_destroy(PolyphonyEffect* p) {
    if (!p) return;
    if (p->buffer_l) free(p->buffer_l);
    if (p->buffer_r) free(p->buffer_r);
    free(p);
}

void polyphony_reset(PolyphonyEffect* p) {
    if (!p) return;
    p->write_pos = 0;
    p->grain_pos1 = 0.0f;
    p->grain_pos2 = p->window_size * 0.5f;
    if (p->buffer_l) memset(p->buffer_l, 0, p->buffer_size * sizeof(float));
    if (p->buffer_r) memset(p->buffer_r, 0, p->buffer_size * sizeof(float));
}

void polyphony_set_params(PolyphonyEffect* p, int enabled, int semitones, float detune_cents, float mix, float sample_rate) {
    if (!p) return;
    if (sample_rate > 0.0f) p->sample_rate = sample_rate;

    p->enabled = enabled;
    if (semitones < -12) semitones = -12;
    if (semitones > 12) semitones = 12;
    p->semitones = semitones;

    if (detune_cents < -50.0f) detune_cents = -50.0f;
    if (detune_cents > 50.0f) detune_cents = 50.0f;
    p->detune_cents = detune_cents;

    if (mix < 0.0f) mix = 0.0f;
    if (mix > 1.0f) mix = 1.0f;
    p->mix = mix;

    float total_semitones = (float)semitones + (detune_cents / 100.0f);
    p->pitch_ratio = powf(2.0f, total_semitones / 12.0f);
}

void polyphony_process(PolyphonyEffect* p, float* left, float* right, int count) {
    if (!p || !p->enabled || !left || !right || count <= 0) return;

    // If semitones == 0 and detune == 0, pitch ratio is 1.0 (bypass pitch shift)
    if (p->semitones == 0 && fabsf(p->detune_cents) < 0.01f) return;

    float mix = p->mix;
    if (mix <= 0.001f) return;

    float dry = 1.0f - mix;
    float wet = mix;
    float rate = 1.0f - p->pitch_ratio; // relative playback rate inside delay window
    float win_size = p->window_size;
    float half_win = win_size * 0.5f;
    int b_size = p->buffer_size;

    float g1 = p->grain_pos1;
    float g2 = p->grain_pos2;
    int w_pos = p->write_pos;

    for (int i = 0; i < count; i++) {
        float in_l = left[i];
        float in_r = right[i];
        if (isnan(in_l) || isinf(in_l)) in_l = 0.0f;
        if (isnan(in_r) || isinf(in_r)) in_r = 0.0f;

        p->buffer_l[w_pos] = in_l;
        p->buffer_r[w_pos] = in_r;

        // Symmetric linear crossfade envelope normalized to constant total amplitude
        float w1 = 1.0f - fabsf((g1 - half_win) / half_win);
        float w2 = 1.0f - fabsf((g2 - half_win) / half_win);
        if (w1 < 0.0f) w1 = 0.0f;
        if (w2 < 0.0f) w2 = 0.0f;
        float total_w = w1 + w2;
        if (total_w > 1e-6f) {
            float inv_w = 1.0f / total_w;
            w1 *= inv_w;
            w2 *= inv_w;
        }

        float read1 = (float)w_pos - g1;
        float read2 = (float)w_pos - g2;

        float s1_l = read_fractional(p->buffer_l, b_size, read1);
        float s2_l = read_fractional(p->buffer_l, b_size, read2);
        float pitched_l = s1_l * w1 + s2_l * w2;

        float s1_r = read_fractional(p->buffer_r, b_size, read1);
        float s2_r = read_fractional(p->buffer_r, b_size, read2);
        float pitched_r = s1_r * w1 + s2_r * w2;

        left[i] = soft_clip(in_l * dry + pitched_l * wet);
        right[i] = soft_clip(in_r * dry + pitched_r * wet);

        // Advance grain delay read heads
        g1 += rate;
        if (g1 >= win_size) g1 -= win_size;
        else if (g1 < 0.0f) g1 += win_size;

        g2 += rate;
        if (g2 >= win_size) g2 -= win_size;
        else if (g2 < 0.0f) g2 += win_size;

        w_pos++;
        if (w_pos >= b_size) w_pos = 0;
    }

    p->grain_pos1 = g1;
    p->grain_pos2 = g2;
    p->write_pos = w_pos;
}
