package com.ztftrue.music.play

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil

@OptIn(UnstableApi::class)
object AudioDecoderHelper {

    /**
     * Creates a [MediaCodecSelector] that prioritizes software decoders (e.g. c2.android.mp3.decoder,
     * OMX.google.mp3.decoder) for MP3 playback over vendor hardware decoders (e.g. c2.sec.mp3.decoder,
     * c2.qti.mp3.decoder).
     *
     * Hardware MP3 decoders on certain OEM devices (such as Samsung Galaxy S23 / Snapdragon 8 Gen 2)
     * suffer from vendor DSP firmware bugs, including silent stalls on MPEG-2 Layer 3 half-rate streams,
     * dropped gapless padding, and playback freezes. Software decoding on modern mobile CPUs consumes
     * less than 0.3% of a single CPU core while offering 100% format compatibility.
     */
    fun createMediaCodecSelector(): MediaCodecSelector {
        return MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val decoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder
            )
            if (MimeTypes.AUDIO_MPEG.equals(mimeType, ignoreCase = true)) {
                MediaCodecUtil.getDecoderInfosSortedBySoftwareOnly(decoders)
            } else {
                decoders
            }
        }
    }

    /**
     * For MPEG-2 and MPEG-2.5 Layer 3 audio (sample rate < 32000 Hz, e.g. 22050 Hz, 16000 Hz, 24000 Hz),
     * vendor hardware decoders (c2.sec.mp3.decoder on Samsung, c2.qti.mp3.decoder on Qualcomm)
     * have a known bug where they decode the first 576-sample frame (~26 ms) and then stall or output
     * continuous silence without throwing an exception.
     *
     * This method filters the decoder list to strictly use software decoders (c2.android.*, OMX.google.*)
     * for MPEG-2 / MPEG-2.5 Layer 3 streams. If no system software decoder is present, it returns an
     * empty list so ExoPlayer can fall back to SafeFfmpegAudioRenderer.
     */
    fun filterDecodersForFormat(
        decoderInfos: List<MediaCodecInfo>,
        format: Format
    ): List<MediaCodecInfo> {
        val mimeType = format.sampleMimeType ?: return decoderInfos
        if (MimeTypes.AUDIO_MPEG.equals(mimeType, ignoreCase = true)) {
            val sampleRate = format.sampleRate
            val isMpeg2OrLower = sampleRate != Format.NO_VALUE && sampleRate in 1 until 32000
            if (isMpeg2OrLower) {
                val swDecoders = decoderInfos.filter {
                    it.softwareOnly || it.name.startsWith("c2.android.") || it.name.startsWith("OMX.google.")
                }
                if (swDecoders.isNotEmpty()) {
                    return swDecoders
                }
                // Return empty list so MediaCodecAudioRenderer rejects format, enabling FFmpeg fallback
                return emptyList()
            }
        }
        return decoderInfos
    }
}
