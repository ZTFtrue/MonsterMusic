package com.ztftrue.music.utils

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodecList
import android.os.Build

data class AudioPathInfo(
    // Track Info
    val format: String,
    val bitDepth: String,
    val sampleRate: String,
    val bitrate: String,
    val channels: String,
    // Decoder
    val decoderName: String,
    // Resampler
    val ioRate: String,
    val resamplerType: String,
    val cutoff: String,
    val quality: String,
    // DSP
    val filterType: String,
    val pcmFormat: String,
    val dspSampleRate: String,
    val eqPreset: String,
    val stereoExpand: String,
    val buffers: String,
    val latency: String,
    val visualizerLatency: String,
    val outputApi: String,
    // Output Device
    val deviceName: String,
    val outputBitDepth: String,
    val outputSampleRate: String
)

object AudioPathHelper {

    fun resolveDecoderName(mimeType: String?, formatName: String?): String {
        val mime = mimeType?.lowercase()?.trim() ?: ""
        val format = formatName?.lowercase()?.trim() ?: ""

        if (mime.isNotEmpty()) {
            try {
                val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
                val info = codecList.codecInfos.firstOrNull {
                    !it.isEncoder && it.supportedTypes.any { type ->
                        type.equals(mime, ignoreCase = true)
                    }
                }
                if (info != null) {
                    return info.name
                }
            } catch (_: Throwable) {
            }
        }

        return when {
            mime.contains("flac") || format.contains("flac") -> "c2.android.flac.decoder"
            mime.contains("mpeg") || mime.contains("mp3") || format.contains("mp3") -> "c2.android.mp3.decoder"
            mime.contains("mp4a") || mime.contains("aac") || format.contains("aac") || format.contains("m4a") -> "c2.android.aac.decoder"
            mime.contains("opus") || format.contains("opus") -> "c2.android.opus.decoder"
            mime.contains("vorbis") || format.contains("ogg") -> "c2.android.vorbis.decoder"
            mime.contains("raw") || mime.contains("wav") || format.contains("wav") -> "c2.android.raw.decoder"
            else -> "c2.android.${format.ifEmpty { "audio" }}.decoder"
        }
    }

    fun getActiveOutputDeviceName(context: Context): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return Build.MODEL ?: "Built-in Speaker"

        try {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            // 1. Bluetooth outputs (Highest priority for music playback)
            val btDevice = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
            }
            if (btDevice != null) {
                val name = btDevice.productName?.toString()?.trim()
                if (!name.isNullOrEmpty()) return name
                return "Bluetooth Audio"
            }

            // 2. Wired / USB Headsets
            val wiredDevice = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }
            if (wiredDevice != null) {
                val name = wiredDevice.productName?.toString()?.trim()
                if (!name.isNullOrEmpty()) return name
                return if (wiredDevice.type == AudioDeviceInfo.TYPE_USB_DEVICE || wiredDevice.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                    "USB Audio"
                } else {
                    "Wired Headphones"
                }
            }

            // 3. Built-in Speaker
            val speaker = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speaker != null) {
                val name = speaker.productName?.toString()?.trim()
                if (!name.isNullOrEmpty() && name != "Android") return name
            }
        } catch (_: Throwable) {
        }

        return Build.MODEL ?: "Built-in Speaker"
    }

    fun getHardwareSampleRate(context: Context): Int {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val rateStr = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            rateStr?.toIntOrNull() ?: AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
        } catch (_: Throwable) {
            48000
        }.takeIf { it > 0 } ?: 48000
    }

    fun formatFormatName(mimeType: String?, filePath: String?, tagFormat: String?): String {
        val mime = mimeType?.lowercase() ?: ""
        val tag = tagFormat?.uppercase() ?: ""
        val ext = filePath?.substringAfterLast('.', "")?.uppercase() ?: ""

        return when {
            tag.contains("FLAC") || mime.contains("flac") || ext == "FLAC" -> "FLAC"
            tag.contains("MPEG") || mime.contains("mpeg") || mime.contains("mp3") || ext == "MP3" -> "MP3"
            tag.contains("AAC") || mime.contains("mp4a") || mime.contains("aac") || ext == "AAC" || ext == "M4A" -> "AAC"
            tag.contains("OGG") || tag.contains("VORBIS") || mime.contains("vorbis") || ext == "OGG" -> "OGG"
            tag.contains("OPUS") || mime.contains("opus") || ext == "OPUS" -> "OPUS"
            tag.contains("WAV") || mime.contains("wav") || ext == "WAV" -> "WAV"
            tag.contains("ALAC") || ext == "ALAC" -> "ALAC"
            tag.contains("DSD") || ext == "DSF" || ext == "DFF" -> "DSD"
            tag.contains("AIFF") || ext == "AIF" || ext == "AIFF" -> "AIFF"
            ext.isNotEmpty() -> ext
            tag.isNotEmpty() -> tag
            else -> "Audio"
        }
    }

    fun formatChannels(channelCount: Int): String {
        return when (channelCount) {
            1 -> "1 (Mono)"
            2 -> "2 (Stereo)"
            6 -> "6 (5.1 Surround)"
            8 -> "8 (7.1 Surround)"
            else -> if (channelCount > 0) "$channelCount (Multi-channel)" else "2 (Stereo)"
        }
    }

    fun formatBitrate(bitrateBps: Long, tagBitrate: String?): String {
        if (bitrateBps > 0) {
            val kbps = bitrateBps / 1000
            return "$kbps kbps"
        }
        val tagNum = tagBitrate?.toLongOrNull() ?: 0L
        if (tagNum > 0) {
            return if (tagNum > 10000) "${tagNum / 1000} kbps" else "$tagNum kbps"
        }
        return "—"
    }

    fun formatBitDepth(bitDepth: String?, formatName: String, pcmEncodingStr: String?): String {
        if (!bitDepth.isNullOrEmpty() && bitDepth != "0-bit") {
            return if (bitDepth.endsWith("-bit", ignoreCase = true)) bitDepth else "$bitDepth-bit"
        }
        if (!pcmEncodingStr.isNullOrEmpty()) {
            return pcmEncodingStr
        }
        return when (formatName.uppercase()) {
            "FLAC" -> "24-bit"
            "WAV", "AIFF", "ALAC" -> "16-bit"
            "DSD" -> "1-bit"
            else -> "16-bit"
        }
    }

    fun calculateBuffers(sampleRate: Int): String {
        val safeRate = if (sampleRate > 0) sampleRate else 48000
        val frames = (safeRate * 0.12).toInt()
        return "2x (120ms, $frames frames)"
    }
}
