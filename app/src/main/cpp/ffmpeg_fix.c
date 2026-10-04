#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#define LOG_TAG "ffmpeg_fix"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#define AUDIO_DECODER_ERROR_INVALID_DATA (-1)
#define AUDIO_DECODER_ERROR_OTHER (-2)
#define FFMPEG_AVERROR_INVALIDDATA (-1094995529)
#define FFMPEG_AVERROR_EAGAIN (-11)

#if defined(__aarch64__) || defined(__x86_64__)
#define OFF_CTX_OPAQUE 0x30
#define OFF_CTX_SAMPLE_RATE 0x160
#define OFF_CTX_SAMPLE_FMT 0x168
#define OFF_CTX_REQUEST_SAMPLE_FMT 0x194
#define OFF_CTX_CH_LAYOUT 0x390
#define OFF_CTX_NB_CHANNELS 0x394
#define OFF_FRAME_EXTENDED_DATA 0x60
#define OFF_FRAME_NB_SAMPLES 0x70
#define OFF_FRAME_FORMAT 0x74
#define OFF_PKT_DATA 0x18
#define OFF_PKT_SIZE 0x20
#else
#define OFF_CTX_OPAQUE 0x20
#define OFF_CTX_SAMPLE_RATE 0x130
#define OFF_CTX_SAMPLE_FMT 0x138
#define OFF_CTX_REQUEST_SAMPLE_FMT 0x164
#define OFF_CTX_CH_LAYOUT 0x308
#define OFF_CTX_NB_CHANNELS 0x30c
#define OFF_FRAME_EXTENDED_DATA 0x40
#define OFF_FRAME_NB_SAMPLES 0x4c
#define OFF_FRAME_FORMAT 0x50
#define OFF_PKT_DATA 0x18
#define OFF_PKT_SIZE 0x1c
#endif

typedef struct AVPacket AVPacket;
typedef struct AVFrame AVFrame;
typedef struct AVCodecContext AVCodecContext;
typedef struct SwrContext SwrContext;
typedef struct AVChannelLayout AVChannelLayout;

typedef AVPacket* (*fn_av_packet_alloc_t)(void);
typedef void (*fn_av_packet_free_t)(AVPacket** pkt);
typedef int (*fn_avcodec_send_packet_t)(AVCodecContext* avctx, const AVPacket* avpkt);
typedef int (*fn_avcodec_receive_frame_t)(AVCodecContext* avctx, AVFrame* frame);
typedef AVFrame* (*fn_av_frame_alloc_t)(void);
typedef void (*fn_av_frame_free_t)(AVFrame** frame);
typedef int (*fn_swr_alloc_set_opts2_t)(SwrContext** ps,
                                        const AVChannelLayout* out_ch_layout, int out_sample_fmt, int out_sample_rate,
                                        const AVChannelLayout* in_ch_layout, int in_sample_fmt, int in_sample_rate,
                                        int log_offset, void* log_ctx);
typedef int (*fn_swr_init_t)(SwrContext* s);
typedef int (*fn_swr_convert_t)(SwrContext* s, uint8_t** out, int out_count,
                                const uint8_t** in, int in_count);
typedef int (*fn_swr_get_out_samples_t)(SwrContext* s, int in_samples);
typedef void (*fn_swr_free_t)(SwrContext** s);
typedef int (*fn_av_get_bytes_per_sample_t)(int sample_fmt);
typedef void (*fn_av_channel_layout_default_t)(void* ch_layout, int nb_channels);

static fn_av_packet_alloc_t fn_av_packet_alloc = NULL;
static fn_av_packet_free_t fn_av_packet_free = NULL;
static fn_avcodec_send_packet_t fn_avcodec_send_packet = NULL;
static fn_avcodec_receive_frame_t fn_avcodec_receive_frame = NULL;
static fn_av_frame_alloc_t fn_av_frame_alloc = NULL;
static fn_av_frame_free_t fn_av_frame_free = NULL;
static fn_swr_alloc_set_opts2_t fn_swr_alloc_set_opts2 = NULL;
static fn_swr_init_t fn_swr_init = NULL;
static fn_swr_convert_t fn_swr_convert = NULL;
static fn_swr_get_out_samples_t fn_swr_get_out_samples = NULL;
static fn_swr_free_t fn_swr_free = NULL;
static fn_av_get_bytes_per_sample_t fn_av_get_bytes_per_sample = NULL;
static fn_av_channel_layout_default_t fn_av_channel_layout_default = NULL;

static jmethodID growOutputBufferMethod = NULL;

static char g_ffmpeg_path[512] = {0};

static const char* find_ffmpeg_library_path(void) {
    FILE* fp = fopen("/proc/self/maps", "r");
    if (!fp) return NULL;
    char line[1024];
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, "libffmpegJNI.so")) {
            char* slash = strchr(line, '/');
            if (slash) {
                char* nl = strchr(slash, '\n');
                if (nl) *nl = '\0';
                size_t len = strlen(slash);
                while (len > 0 && (slash[len - 1] == ' ' || slash[len - 1] == '\r')) {
                    slash[--len] = '\0';
                }
                strncpy(g_ffmpeg_path, slash, sizeof(g_ffmpeg_path) - 1);
                g_ffmpeg_path[sizeof(g_ffmpeg_path) - 1] = '\0';
                fclose(fp);
                return g_ffmpeg_path;
            }
        }
    }
    fclose(fp);
    return NULL;
}

static int resolve_ffmpeg_symbols(const char* lib_path) {
    void* handle = NULL;
    if (lib_path != NULL && lib_path[0] != '\0') {
        handle = dlopen(lib_path, RTLD_NOW | RTLD_GLOBAL);
        if (handle) {
            LOGI("Loaded libffmpegJNI from specified path: %s", lib_path);
        } else {
            LOGW("Could not dlopen %s: %s", lib_path, dlerror());
        }
    }
    if (!handle) {
        const char* proc_path = find_ffmpeg_library_path();
        if (proc_path) {
            handle = dlopen(proc_path, RTLD_NOW | RTLD_GLOBAL);
            if (handle) {
                LOGI("Loaded libffmpegJNI from /proc/self/maps: %s", proc_path);
            } else {
                LOGW("Could not dlopen proc path %s: %s", proc_path, dlerror());
            }
        }
    }
    if (!handle) {
        handle = dlopen("libffmpegJNI.so", RTLD_NOW | RTLD_GLOBAL);
    }
    if (!handle) {
        handle = RTLD_DEFAULT;
    }
    fn_av_packet_alloc = (fn_av_packet_alloc_t) dlsym(handle, "av_packet_alloc");
    fn_av_packet_free = (fn_av_packet_free_t) dlsym(handle, "av_packet_free");
    fn_avcodec_send_packet = (fn_avcodec_send_packet_t) dlsym(handle, "avcodec_send_packet");
    fn_avcodec_receive_frame = (fn_avcodec_receive_frame_t) dlsym(handle, "avcodec_receive_frame");
    fn_av_frame_alloc = (fn_av_frame_alloc_t) dlsym(handle, "av_frame_alloc");
    fn_av_frame_free = (fn_av_frame_free_t) dlsym(handle, "av_frame_free");
    fn_swr_alloc_set_opts2 = (fn_swr_alloc_set_opts2_t) dlsym(handle, "swr_alloc_set_opts2");
    fn_swr_init = (fn_swr_init_t) dlsym(handle, "swr_init");
    fn_swr_convert = (fn_swr_convert_t) dlsym(handle, "swr_convert");
    fn_swr_get_out_samples = (fn_swr_get_out_samples_t) dlsym(handle, "swr_get_out_samples");
    fn_swr_free = (fn_swr_free_t) dlsym(handle, "swr_free");
    fn_av_get_bytes_per_sample = (fn_av_get_bytes_per_sample_t) dlsym(handle, "av_get_bytes_per_sample");
    fn_av_channel_layout_default = (fn_av_channel_layout_default_t) dlsym(handle, "av_channel_layout_default");

    return (fn_av_packet_alloc && fn_av_packet_free && fn_avcodec_send_packet &&
            fn_avcodec_receive_frame && fn_av_frame_alloc && fn_av_frame_free &&
            fn_swr_alloc_set_opts2 && fn_swr_init && fn_swr_convert &&
            fn_swr_get_out_samples && fn_swr_free && fn_av_get_bytes_per_sample);
}

static jint JNICALL hook_ffmpegDecode(
    JNIEnv* env, jobject thiz, jlong context_handle,
    jobject inputData, jint inputSize,
    jobject decoderOutputBuffer, jobject outputData, jint outputSize) {

    if (!context_handle || !inputData || !decoderOutputBuffer || !outputData ||
        inputSize < 0 || outputSize < 0) {
        LOGE("Invalid arguments in hook_ffmpegDecode");
        return AUDIO_DECODER_ERROR_INVALID_DATA;
    }

    uint8_t* ctx = (uint8_t*)(intptr_t)context_handle;
    uint8_t* inputBuffer = (uint8_t*)(*env)->GetDirectBufferAddress(env, inputData);
    uint8_t* outputBuffer = (uint8_t*)(*env)->GetDirectBufferAddress(env, outputData);
    if (!inputBuffer || !outputBuffer) {
        LOGE("Direct buffer addresses are NULL");
        return AUDIO_DECODER_ERROR_INVALID_DATA;
    }

    AVPacket* packet = fn_av_packet_alloc();
    if (!packet) {
        LOGE("Failed to allocate AVPacket");
        return AUDIO_DECODER_ERROR_INVALID_DATA;
    }
    *(uint8_t**)((uint8_t*)packet + OFF_PKT_DATA) = inputBuffer;
    *(int32_t*)((uint8_t*)packet + OFF_PKT_SIZE) = inputSize;

    int ret = fn_avcodec_send_packet((AVCodecContext*)ctx, packet);
    fn_av_packet_free(&packet);
    if (ret != 0) {
        return (ret == FFMPEG_AVERROR_INVALIDDATA) ? AUDIO_DECODER_ERROR_INVALID_DATA
                                                  : AUDIO_DECODER_ERROR_OTHER;
    }

    int outSize = 0;
    while (1) {
        AVFrame* frame = fn_av_frame_alloc();
        if (!frame) {
            LOGE("Failed to allocate AVFrame");
            return AUDIO_DECODER_ERROR_OTHER;
        }

        ret = fn_avcodec_receive_frame((AVCodecContext*)ctx, frame);
        if (ret != 0) {
            fn_av_frame_free(&frame);
            if (ret == FFMPEG_AVERROR_EAGAIN) {
                break;
            }
            return (ret == FFMPEG_AVERROR_INVALIDDATA) ? AUDIO_DECODER_ERROR_INVALID_DATA
                                                      : AUDIO_DECODER_ERROR_OTHER;
        }

        int sampleCount = *(int32_t*)((uint8_t*)frame + OFF_FRAME_NB_SAMPLES);
        if (sampleCount <= 0) {
            fn_av_frame_free(&frame);
            continue;
        }

        int sampleRate = *(int32_t*)(ctx + OFF_CTX_SAMPLE_RATE);
        int sampleFormat = *(int32_t*)(ctx + OFF_CTX_SAMPLE_FMT);
        int requestSampleFmt = *(int32_t*)(ctx + OFF_CTX_REQUEST_SAMPLE_FMT);
        int channelCount = *(int32_t*)(ctx + OFF_CTX_NB_CHANNELS);
        void* chLayout = (void*)(ctx + OFF_CTX_CH_LAYOUT);

        int outChannels = channelCount;
        uint8_t stereoLayoutBuf[32];
        memset(stereoLayoutBuf, 0, sizeof(stereoLayoutBuf));
        const void* outChLayout = chLayout;

        if (channelCount > 2) {
            if (fn_av_channel_layout_default) {
                fn_av_channel_layout_default(stereoLayoutBuf, 2);
                outChLayout = (const void*)stereoLayoutBuf;
                outChannels = 2;
            }
        }

        SwrContext* resampleContext = *(SwrContext**)(ctx + OFF_CTX_OPAQUE);
        if (!resampleContext) {
            ret = fn_swr_alloc_set_opts2(
                &resampleContext,
                (const AVChannelLayout*)outChLayout,
                requestSampleFmt,
                sampleRate,
                (const AVChannelLayout*)chLayout,
                sampleFormat,
                sampleRate,
                0,
                NULL
            );
            if (ret < 0 || !resampleContext) {
                fn_av_frame_free(&frame);
                return AUDIO_DECODER_ERROR_OTHER;
            }
            ret = fn_swr_init(resampleContext);
            if (ret < 0) {
                fn_av_frame_free(&frame);
                return AUDIO_DECODER_ERROR_OTHER;
            }
            *(SwrContext**)(ctx + OFF_CTX_OPAQUE) = resampleContext;
        }

        int outSampleSize = fn_av_get_bytes_per_sample(requestSampleFmt);
        int outSamples = fn_swr_get_out_samples(resampleContext, sampleCount);
        int bufferOutSize = outSampleSize * outChannels * outSamples;

        // Reallocate buffer dynamically if needed
        if (outSize + bufferOutSize > outputSize) {
            outputSize = outSize + bufferOutSize;
            jobject newOutputData = (*env)->CallObjectMethod(
                env, thiz, growOutputBufferMethod, decoderOutputBuffer, outputSize);
            if ((*env)->ExceptionCheck(env) || !newOutputData) {
                LOGE("growOutputBuffer failed");
                fn_av_frame_free(&frame);
                return AUDIO_DECODER_ERROR_OTHER;
            }
            uint8_t* newBase = (uint8_t*)(*env)->GetDirectBufferAddress(env, newOutputData);
            if (!newBase) {
                LOGE("Failed to get address of grown output buffer");
                fn_av_frame_free(&frame);
                return AUDIO_DECODER_ERROR_OTHER;
            }
            // CRITICAL FIX: Offset by outSize to maintain contiguous stream position!
            outputBuffer = newBase + outSize;
        }

        uint8_t** in_data = *(uint8_t***)((uint8_t*)frame + OFF_FRAME_EXTENDED_DATA);
        if (!in_data) {
            in_data = (uint8_t**)frame; // fallback to frame->data
        }

        // CRITICAL FIX: Pass outSamples (samples per channel), NOT bufferOutSize (bytes)!
        int convertedSamples = fn_swr_convert(
            resampleContext,
            &outputBuffer,
            outSamples,
            (const uint8_t**)in_data,
            sampleCount
        );
        fn_av_frame_free(&frame);

        if (convertedSamples < 0) {
            LOGE("swr_convert failed: %d", convertedSamples);
            return AUDIO_DECODER_ERROR_INVALID_DATA;
        }

        // CRITICAL FIX: Advance buffer pointer and outSize strictly by actual converted bytes!
        int actualBytes = convertedSamples * outChannels * outSampleSize;
        outputBuffer += actualBytes;
        outSize += actualBytes;
    }

    return outSize;
}

static jint JNICALL hook_ffmpegGetChannelCount(JNIEnv* env, jobject thiz, jlong context_handle) {
    (void)env;
    (void)thiz;
    if (!context_handle) return 0;
    uint8_t* ctx = (uint8_t*)(intptr_t)context_handle;
    int ch = *(int32_t*)(ctx + OFF_CTX_NB_CHANNELS);
    // Multichannel (> 2 channels) is downmixed to stereo
    if (ch > 2) {
        return 2;
    }
    return ch;
}

JNIEXPORT jboolean JNICALL
Java_com_ztftrue_music_play_NativeFfmpegFix_installNativeHook(JNIEnv *env, jclass clazz, jstring libPathStr) {
    (void)clazz;
    const char* libPathUtf = NULL;
    if (libPathStr != NULL) {
        libPathUtf = (*env)->GetStringUTFChars(env, libPathStr, NULL);
    }

    int resolved = resolve_ffmpeg_symbols(libPathUtf);

    if (libPathUtf != NULL) {
        (*env)->ReleaseStringUTFChars(env, libPathStr, libPathUtf);
    }

    if (!resolved) {
        LOGW("Could not resolve all FFmpeg symbols from libffmpegJNI.so");
        return JNI_FALSE;
    }

    jclass decoderClass = (*env)->FindClass(env, "androidx/media3/decoder/ffmpeg/FfmpegAudioDecoder");
    if (!decoderClass) {
        LOGE("Could not find class androidx/media3/decoder/ffmpeg/FfmpegAudioDecoder");
        (*env)->ExceptionClear(env);
        return JNI_FALSE;
    }

    growOutputBufferMethod = (*env)->GetMethodID(
        env,
        decoderClass,
        "growOutputBuffer",
        "(Landroidx/media3/decoder/SimpleDecoderOutputBuffer;I)Ljava/nio/ByteBuffer;"
    );
    if (!growOutputBufferMethod) {
        LOGE("Could not get growOutputBuffer method ID");
        (*env)->ExceptionClear(env);
        return JNI_FALSE;
    }

    JNINativeMethod methods[] = {
        {
            "ffmpegDecode",
            "(JLjava/nio/ByteBuffer;ILandroidx/media3/decoder/SimpleDecoderOutputBuffer;Ljava/nio/ByteBuffer;I)I",
            (void*)hook_ffmpegDecode
        },
        {
            "ffmpegGetChannelCount",
            "(J)I",
            (void*)hook_ffmpegGetChannelCount
        }
    };

    if ((*env)->RegisterNatives(env, decoderClass, methods, sizeof(methods)/sizeof(methods[0])) < 0) {
        LOGE("Failed to RegisterNatives for ffmpeg methods");
        (*env)->ExceptionClear(env);
        return JNI_FALSE;
    }

    LOGI("FFmpeg native decode hardening & stereo downmix hook successfully installed via RegisterNatives");
    return JNI_TRUE;
}
