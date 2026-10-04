package androidx.media3.decoder.ffmpeg;

import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.TraceUtil;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.CryptoConfig;
import androidx.media3.decoder.DecoderException;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.audio.AudioRendererEventListener;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DecoderAudioRenderer;

/**
 * An audio renderer that uses FFmpeg to decode audio, hardened against native AArch64
 * NEON crashes in libffmpegJNI (swri_oldapi_conv_fltp_to_s16_nch_neon).
 */
@UnstableApi
public final class SafeFfmpegAudioRenderer extends DecoderAudioRenderer<FfmpegAudioDecoder> {

  private static final String TAG = "SafeFfmpegAudioRenderer";
  private static final int NUM_BUFFERS = 16;
  private static final int DEFAULT_INPUT_BUFFER_SIZE = 5760;

  public SafeFfmpegAudioRenderer() {
    this(null, null, new AudioProcessor[0]);
  }

  public SafeFfmpegAudioRenderer(
      @Nullable Handler eventHandler,
      @Nullable AudioRendererEventListener eventListener,
      AudioSink audioSink) {
    super(eventHandler, eventListener, audioSink);
  }

  public SafeFfmpegAudioRenderer(
      @Nullable Handler eventHandler,
      @Nullable AudioRendererEventListener eventListener,
      AudioProcessor... audioProcessors) {
    super(eventHandler, eventListener, audioProcessors);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  protected int supportsFormatInternal(Format format) {
    String sampleMimeType = Assertions.checkNotNull(format.sampleMimeType);
    if (!FfmpegLibrary.isAvailable() || !MimeTypes.isAudio(sampleMimeType)) {
      return C.FORMAT_UNSUPPORTED_TYPE;
    }
    if (!FfmpegLibrary.supportsFormat(sampleMimeType)) {
      return C.FORMAT_UNSUPPORTED_SUBTYPE;
    }
    int channelCount = format.channelCount != Format.NO_VALUE ? format.channelCount : 2;
    int sampleRate = format.sampleRate != Format.NO_VALUE ? format.sampleRate : 44100;
    if (!sinkSupportsFormat(Util.getPcmFormat(C.ENCODING_PCM_FLOAT, channelCount, sampleRate))
        && !sinkSupportsFormat(Util.getPcmFormat(C.ENCODING_PCM_16BIT, channelCount, sampleRate))) {
      return C.FORMAT_UNSUPPORTED_SUBTYPE;
    }
    if (format.cryptoType != C.CRYPTO_TYPE_NONE) {
      return C.FORMAT_UNSUPPORTED_DRM;
    }
    return C.FORMAT_HANDLED;
  }

  @Override
  public int supportsMixedMimeTypeAdaptation() {
    return RendererCapabilities.ADAPTIVE_SEAMLESS;
  }

  @Override
  protected FfmpegAudioDecoder createDecoder(Format format, @Nullable CryptoConfig cryptoConfig)
      throws DecoderException {
    TraceUtil.beginSection("createSafeFfmpegAudioDecoder");
    int initialInputBufferSize =
        format.maxInputSize != Format.NO_VALUE ? format.maxInputSize : DEFAULT_INPUT_BUFFER_SIZE;

    // Force outputFloat = true.
    // In libffmpegJNI.so on ARM64, libswresample's NEON conversion routine
    // (swri_oldapi_conv_fltp_to_s16_nch_neon) crashes with SIGSEGV (fault addr 0x0)
    // when converting multichannel planar float to 16-bit PCM.
    // Requesting float output outputs AV_SAMPLE_FMT_FLT, bypassing the buggy NEON
    // s16 conversion routine completely. Media3's AudioSink natively handles float PCM
    // on Android 5.0+ and safely downsamples/converts in Java if needed.
    boolean outputFloat = true;

    FfmpegAudioDecoder decoder =
        new FfmpegAudioDecoder(
            format,
            NUM_BUFFERS,
            NUM_BUFFERS,
            initialInputBufferSize,
            outputFloat);
    TraceUtil.endSection();
    return decoder;
  }

  @Override
  protected Format getOutputFormat(FfmpegAudioDecoder decoder) {
    Assertions.checkNotNull(decoder);
    return new Format.Builder()
        .setSampleMimeType(MimeTypes.AUDIO_RAW)
        .setChannelCount(decoder.getChannelCount())
        .setSampleRate(decoder.getSampleRate())
        .setPcmEncoding(decoder.getEncoding())
        .build();
  }
}
