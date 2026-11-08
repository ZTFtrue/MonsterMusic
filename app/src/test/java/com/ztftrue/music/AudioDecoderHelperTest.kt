package com.ztftrue.music

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import com.ztftrue.music.play.AudioDecoderHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(UnstableApi::class)
class AudioDecoderHelperTest {

    private fun createCodecInfo(
        name: String,
        mimeType: String = MimeTypes.AUDIO_MPEG,
        hardwareAccelerated: Boolean,
        softwareOnly: Boolean,
        vendor: Boolean
    ): MediaCodecInfo {
        return MediaCodecInfo.newInstance(
            name,
            mimeType,
            mimeType,
            /* capabilities= */ null,
            hardwareAccelerated,
            softwareOnly,
            vendor,
            /* forceDisableAdaptive= */ false,
            /* forceSecure= */ false
        )
    }

    @Test
    fun filterDecodersForFormat_mpeg2LowSampleRate_removesHardwareDecoders() {
        val samsungHw = createCodecInfo(
            name = "c2.sec.mp3.decoder",
            hardwareAccelerated = true,
            softwareOnly = false,
            vendor = true
        )
        val qualcommHw = createCodecInfo(
            name = "c2.qti.mp3.decoder",
            hardwareAccelerated = true,
            softwareOnly = false,
            vendor = true
        )
        val googleSw = createCodecInfo(
            name = "c2.android.mp3.decoder",
            hardwareAccelerated = false,
            softwareOnly = true,
            vendor = false
        )

        val decoders = listOf(samsungHw, qualcommHw, googleSw)

        // 22050 Hz is MPEG-2 Layer 3 (Track.01.mp3)
        val format22k = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_MPEG)
            .setSampleRate(22050)
            .setChannelCount(2)
            .build()

        val filtered = AudioDecoderHelper.filterDecodersForFormat(decoders, format22k)
        assertEquals(1, filtered.size)
        assertEquals("c2.android.mp3.decoder", filtered[0].name)
    }

    @Test
    fun filterDecodersForFormat_mpeg2LowSampleRate_noSoftwareDecoder_returnsEmptyForFfmpegFallback() {
        val samsungHw = createCodecInfo(
            name = "c2.sec.mp3.decoder",
            hardwareAccelerated = true,
            softwareOnly = false,
            vendor = true
        )
        val decoders = listOf(samsungHw)

        val format16k = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_MPEG)
            .setSampleRate(16000)
            .setChannelCount(1)
            .build()

        val filtered = AudioDecoderHelper.filterDecodersForFormat(decoders, format16k)
        assertTrue("Should return empty list to enable FFmpeg fallback when no software MediaCodec exists", filtered.isEmpty())
    }

    @Test
    fun filterDecodersForFormat_mpeg1StandardSampleRate_retainsAllDecoders() {
        val samsungHw = createCodecInfo(
            name = "c2.sec.mp3.decoder",
            hardwareAccelerated = true,
            softwareOnly = false,
            vendor = true
        )
        val googleSw = createCodecInfo(
            name = "c2.android.mp3.decoder",
            hardwareAccelerated = false,
            softwareOnly = true,
            vendor = false
        )
        val decoders = listOf(samsungHw, googleSw)

        val format44k = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_MPEG)
            .setSampleRate(44100)
            .setChannelCount(2)
            .build()

        val filtered = AudioDecoderHelper.filterDecodersForFormat(decoders, format44k)
        assertEquals(2, filtered.size)
    }

    @Test
    fun filterDecodersForFormat_nonMp3_retainsAllDecoders() {
        val flacHw = createCodecInfo(
            name = "c2.qti.flac.decoder",
            mimeType = MimeTypes.AUDIO_FLAC,
            hardwareAccelerated = true,
            softwareOnly = false,
            vendor = true
        )
        val decoders = listOf(flacHw)

        val formatFlac = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_FLAC)
            .setSampleRate(22050)
            .setChannelCount(2)
            .build()

        val filtered = AudioDecoderHelper.filterDecodersForFormat(decoders, formatFlac)
        assertEquals(1, filtered.size)
        assertEquals("c2.qti.flac.decoder", filtered[0].name)
    }

    @Test
    fun mediaCodecSelector_sortsSoftwareOnlyDecodersFirst() {
        val samsungHw = createCodecInfo(
            name = "c2.sec.mp3.decoder",
            hardwareAccelerated = true,
            softwareOnly = false,
            vendor = true
        )
        val googleSw = createCodecInfo(
            name = "c2.android.mp3.decoder",
            hardwareAccelerated = false,
            softwareOnly = true,
            vendor = false
        )
        val unsorted = listOf(samsungHw, googleSw)

        val sorted = MediaCodecUtil.getDecoderInfosSortedBySoftwareOnly(unsorted)
        assertEquals(2, sorted.size)
        assertEquals("c2.android.mp3.decoder", sorted[0].name)
        assertEquals("c2.sec.mp3.decoder", sorted[1].name)
    }
}
