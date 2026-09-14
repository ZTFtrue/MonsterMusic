#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <android/log.h>
#include "biquad_filter.h"
#include "fft.h"

#define TAG "MonsterAudioNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// =========================================================================
// EqualizerAudioProcessor Native JNI Methods
// =========================================================================

JNIEXPORT jlong JNICALL
Java_com_ztftrue_music_effects_EqualizerAudioProcessor_initNativeEqualizer(
        JNIEnv *env, jobject thiz, jint channel_count, jint band_count, jfloat sample_rate) {
    (void)env;
    (void)thiz;
    BiquadEqualizer* eq = biquad_equalizer_create(channel_count, band_count, sample_rate);
    return (jlong)(intptr_t)eq;
}

JNIEXPORT void JNICALL
Java_com_ztftrue_music_effects_EqualizerAudioProcessor_freeNativeEqualizer(
        JNIEnv *env, jobject thiz, jlong handle) {
    (void)env;
    (void)thiz;
    if (handle == 0) return;
    BiquadEqualizer* eq = (BiquadEqualizer*)(intptr_t)handle;
    biquad_equalizer_destroy(eq);
}

JNIEXPORT void JNICALL
Java_com_ztftrue_music_effects_EqualizerAudioProcessor_resetNativeEqualizer(
        JNIEnv *env, jobject thiz, jlong handle) {
    (void)env;
    (void)thiz;
    if (handle == 0) return;
    BiquadEqualizer* eq = (BiquadEqualizer*)(intptr_t)handle;
    biquad_equalizer_reset(eq);
}

JNIEXPORT void JNICALL
Java_com_ztftrue_music_effects_EqualizerAudioProcessor_resetLimiterNative(
        JNIEnv *env, jobject thiz, jlong handle) {
    (void)env;
    (void)thiz;
    if (handle == 0) return;
    BiquadEqualizer* eq = (BiquadEqualizer*)(intptr_t)handle;
    biquad_equalizer_reset_limiter(eq);
}

JNIEXPORT void JNICALL
Java_com_ztftrue_music_effects_EqualizerAudioProcessor_configureBandNative(
        JNIEnv *env, jobject thiz, jlong handle, jint channel, jint band,
        jint type, jfloat center_freq, jfloat sample_rate, jfloat q, jfloat gain_db) {
    (void)env;
    (void)thiz;
    if (handle == 0) return;
    BiquadEqualizer* eq = (BiquadEqualizer*)(intptr_t)handle;
    biquad_equalizer_configure_band(eq, channel, band, type, center_freq, sample_rate, q, gain_db);
}

JNIEXPORT void JNICALL
Java_com_ztftrue_music_effects_EqualizerAudioProcessor_setEchoParamsNative(
        JNIEnv *env, jobject thiz, jlong handle, jfloat delay_time, jfloat decay, jboolean feedback, jfloat sample_rate) {
    (void)env;
    (void)thiz;
    if (handle == 0) return;
    BiquadEqualizer* eq = (BiquadEqualizer*)(intptr_t)handle;
    biquad_equalizer_set_echo_params(eq, delay_time, decay, feedback ? 1 : 0, sample_rate);
}

JNIEXPORT jint JNICALL
Java_com_ztftrue_music_effects_EqualizerAudioProcessor_processPcmDirectNative(
        JNIEnv *env, jobject thiz, jlong handle,
        jobject input_buffer, jobject output_buffer,
        jint frames_count, jint channel_count, jint encoding,
        jboolean eq_active, jboolean echo_active,
        jfloatArray vis_array) {
    (void)thiz;
    if (handle == 0 || !input_buffer || !output_buffer || frames_count <= 0) {
        return 0;
    }

    BiquadEqualizer* eq = (BiquadEqualizer*)(intptr_t)handle;
    void* in_ptr = (*env)->GetDirectBufferAddress(env, input_buffer);
    void* out_ptr = (*env)->GetDirectBufferAddress(env, output_buffer);

    if (!in_ptr || !out_ptr) {
        return 0;
    }

    float* vis_ptr = NULL;
    int max_vis = 0;
    int actual_vis = 0;

    if (vis_array != NULL) {
        max_vis = (*env)->GetArrayLength(env, vis_array);
        vis_ptr = (*env)->GetPrimitiveArrayCritical(env, vis_array, NULL);
    }

    biquad_equalizer_process_pcm(
        eq,
        in_ptr,
        out_ptr,
        frames_count,
        channel_count,
        encoding,
        eq_active ? 1 : 0,
        echo_active ? 1 : 0,
        vis_ptr,
        max_vis,
        &actual_vis
    );

    if (vis_ptr != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, vis_array, vis_ptr, 0);
    }

    return actual_vis;
}

JNIEXPORT void JNICALL
Java_com_ztftrue_music_effects_EqualizerAudioProcessor_setEqualizerTypeNative(
        JNIEnv *env, jobject thiz, jlong handle, jint type) {
    (void)env;
    (void)thiz;
    if (handle == 0) return;
    BiquadEqualizer* eq = (BiquadEqualizer*)(intptr_t)handle;
    biquad_equalizer_set_type(eq, type);
}

// =========================================================================
// Native FFT JNI Methods
// =========================================================================

JNIEXPORT jlong JNICALL
Java_com_ztftrue_music_effects_EqualizerAudioProcessor_initNativeFft(
        JNIEnv *env, jobject thiz, jint size) {
    (void)env;
    (void)thiz;
    FftContext* ctx = fft_create(size);
    return (jlong)(intptr_t)ctx;
}

JNIEXPORT void JNICALL
Java_com_ztftrue_music_effects_EqualizerAudioProcessor_freeNativeFft(
        JNIEnv *env, jobject thiz, jlong handle) {
    (void)env;
    (void)thiz;
    if (handle == 0) return;
    FftContext* ctx = (FftContext*)(intptr_t)handle;
    fft_destroy(ctx);
}

JNIEXPORT void JNICALL
Java_com_ztftrue_music_effects_EqualizerAudioProcessor_processNativeFft(
        JNIEnv *env, jobject thiz, jlong handle,
        jfloatArray input_array, jfloatArray output_array) {
    (void)thiz;
    if (handle == 0 || !input_array || !output_array) return;

    FftContext* ctx = (FftContext*)(intptr_t)handle;
    jfloat* in_ptr = (*env)->GetPrimitiveArrayCritical(env, input_array, NULL);
    jfloat* out_ptr = (*env)->GetPrimitiveArrayCritical(env, output_array, NULL);

    if (in_ptr && out_ptr) {
        fft_process(ctx, in_ptr, out_ptr);
    }

    if (out_ptr) {
        (*env)->ReleasePrimitiveArrayCritical(env, output_array, out_ptr, 0);
    }
    if (in_ptr) {
        (*env)->ReleasePrimitiveArrayCritical(env, input_array, in_ptr, JNI_ABORT);
    }
}
