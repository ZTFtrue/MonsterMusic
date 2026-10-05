package com.ztftrue.music.play.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.ztftrue.music.utils.SharedPreferencesUtils
import java.util.concurrent.Executors

@UnstableApi
class UsbDacManager(
    private val context: Context,
    private val effectManager: AudioEffectManager
) {
    companion object {
        private const val TAG = "UsbDacManager"

        fun isUsbAudioOutputDevice(device: AudioDeviceInfo?): Boolean {
            if (device == null || !device.isSink) return false
            return device.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
        }
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var exoPlayer: ExoPlayer? = null

    // Track state
    var isEnabled: Boolean = SharedPreferencesUtils.getBitPerfectUsbEnabled(context)
        private set
    var connectedUsbDevice: AudioDeviceInfo? = null
        private set
    var isBitPerfectActive: Boolean = false
        private set
    var statusDescription: String = ""
        private set

    private var currentFormat: Format? = null
    private var lastPreferredDevice: AudioDeviceInfo? = null

    // Callback when status changes (for MediaSession and UI updates)
    var onStatusChanged: ((UsbDacStatus) -> Unit)? = null

    data class UsbDacStatus(
        val isEnabled: Boolean,
        val isBitPerfectActive: Boolean,
        val dacName: String?,
        val statusText: String,
        val supportedSampleRates: List<Int>
    )

    private val platformAudioAttributes: AudioAttributes by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            val usbDevice = addedDevices?.firstOrNull { isUsbAudioOutputDevice(it) }
            if (usbDevice != null) {
                Log.i(TAG, "USB audio device attached: ${usbDevice.productName} (id=${usbDevice.id}, type=${usbDevice.type})")
                connectedUsbDevice = usbDevice
                applyConfiguration()
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            val removed = removedDevices?.any { it.id == connectedUsbDevice?.id } == true
            if (removed) {
                Log.i(TAG, "USB audio device detached: ${connectedUsbDevice?.productName}")
                connectedUsbDevice = findConnectedUsbDac()
                applyConfiguration()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private var mixerAttributesListener: AudioManager.OnPreferredMixerAttributesChangedListener? = null
    private val executor = Executors.newSingleThreadExecutor()

    fun init(player: ExoPlayer) {
        this.exoPlayer = player
        this.connectedUsbDevice = findConnectedUsbDac()

        try {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to register AudioDeviceCallback", t)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val listener = AudioManager.OnPreferredMixerAttributesChangedListener { attributes, device, mixerAttributes ->
                    if (attributes.usage == AudioAttributes.USAGE_MEDIA && device.id == connectedUsbDevice?.id) {
                        Log.i(TAG, "Preferred mixer attributes changed on HAL: behavior=${mixerAttributes?.mixerBehavior}")
                        if (mixerAttributes?.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT) {
                            isBitPerfectActive = true
                        }
                        notifyStatusUpdate()
                    }
                }
                mixerAttributesListener = listener
                audioManager.addOnPreferredMixerAttributesChangedListener(executor, listener)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to register OnPreferredMixerAttributesChangedListener", t)
            }
        }

        if (connectedUsbDevice != null) {
            Log.i(TAG, "Initial USB DAC found: ${connectedUsbDevice?.productName}")
            applyConfiguration()
        }
    }

    fun setBitPerfectEnabled(enabled: Boolean) {
        if (this.isEnabled == enabled) return
        this.isEnabled = enabled
        SharedPreferencesUtils.saveBitPerfectUsbEnabled(context, enabled)
        Log.i(TAG, "Bit-perfect USB passthrough enabled state set to: $enabled")
        applyConfiguration()
    }

    fun onAudioSinkConfigured(format: Format) {
        this.currentFormat = format
        if (isEnabled && connectedUsbDevice != null) {
            applyConfiguration()
        }
    }

    fun getStatus(): UsbDacStatus {
        val rates = connectedUsbDevice?.sampleRates?.toList() ?: emptyList()
        return UsbDacStatus(
            isEnabled = isEnabled,
            isBitPerfectActive = isBitPerfectActive,
            dacName = connectedUsbDevice?.productName?.toString(),
            statusText = statusDescription,
            supportedSampleRates = rates
        )
    }

    private fun findConnectedUsbDac(): AudioDeviceInfo? {
        return try {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices.firstOrNull { isUsbAudioOutputDevice(it) }
        } catch (t: Throwable) {
            Log.e(TAG, "Error getting audio devices", t)
            null
        }
    }

    private fun applyConfiguration() {
        val dac = connectedUsbDevice
        val player = exoPlayer

        if (!isEnabled || dac == null) {
            // Disabled or DAC detached: clear bit-perfect and restore normal routing
            clearBitPerfect(dac)
            return
        }

        val format = currentFormat
        if (format == null) {
            // DAC connected and enabled, but no stream format configured yet
            statusDescription = "${dac.productName} (Ready)"
            effectManager.setBitPerfectMode(false)
            notifyStatusUpdate()
            return
        }

        val sampleRate = if (format.sampleRate > 0) format.sampleRate else 44100
        val channelCount = if (format.channelCount > 0) format.channelCount else 2
        val channelMask = if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO

        val encoding = when (format.pcmEncoding) {
            C.ENCODING_PCM_16BIT -> AudioFormat.ENCODING_PCM_16BIT
            C.ENCODING_PCM_24BIT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AudioFormat.ENCODING_PCM_24BIT_PACKED
                } else {
                    AudioFormat.ENCODING_PCM_FLOAT
                }
            }
            C.ENCODING_PCM_32BIT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AudioFormat.ENCODING_PCM_32BIT
                } else {
                    AudioFormat.ENCODING_PCM_FLOAT
                }
            }
            C.ENCODING_PCM_FLOAT -> AudioFormat.ENCODING_PCM_FLOAT
            else -> AudioFormat.ENCODING_PCM_16BIT
        }

        val bitDepthStr = when (format.pcmEncoding) {
            C.ENCODING_PCM_16BIT -> "16-bit"
            C.ENCODING_PCM_24BIT -> "24-bit"
            C.ENCODING_PCM_32BIT -> "32-bit"
            C.ENCODING_PCM_FLOAT -> "32-bit float"
            else -> "PCM"
        }

        // Android 14+ (API 34+) Native Bit-Perfect Support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            var appliedBitPerfect = false
            try {
                val supportedMixerAttrs = audioManager.getSupportedMixerAttributes(dac)
                Log.d(TAG, "Supported mixer attributes count for ${dac.productName}: ${supportedMixerAttrs.size}")

                // Search for an existing bit-perfect attribute matching our stream
                val matchingAttr = supportedMixerAttrs.firstOrNull { attr ->
                    attr.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT &&
                            attr.format.sampleRate == sampleRate &&
                            attr.format.channelMask == channelMask
                }

                val mixerAttrToSet = matchingAttr ?: run {
                    val targetFormat = AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelMask)
                        .setEncoding(encoding)
                        .build()
                    AudioMixerAttributes.Builder(targetFormat)
                        .setMixerBehavior(AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT)
                        .build()
                }

                val success = audioManager.setPreferredMixerAttributes(
                    platformAudioAttributes,
                    dac,
                    mixerAttrToSet
                )

                if (success) {
                    lastPreferredDevice = dac
                    player?.setPreferredAudioDevice(dac)
                    effectManager.setBitPerfectMode(true)
                    isBitPerfectActive = true
                    appliedBitPerfect = true
                    statusDescription = "$sampleRate Hz / $bitDepthStr (Bit-Perfect)"
                    Log.i(TAG, "Successfully applied MIXER_BEHAVIOR_BIT_PERFECT: $statusDescription on ${dac.productName}")
                } else {
                    Log.w(TAG, "setPreferredMixerAttributes returned false for $sampleRate Hz on ${dac.productName}")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Exception configuring bit-perfect mixer attributes", t)
            }

            if (!appliedBitPerfect) {
                // Fallback to direct routing if bit-perfect flag was rejected by HAL
                player?.setPreferredAudioDevice(dac)
                effectManager.setBitPerfectMode(true)
                isBitPerfectActive = false
                statusDescription = "$sampleRate Hz / $bitDepthStr (Direct USB Routing)"
                Log.i(TAG, "Using direct routing fallback: $statusDescription on ${dac.productName}")
            }
        } else {
            // Android < 14 (API 23 - 33): Direct routing via setPreferredAudioDevice + DSP bypass
            player?.setPreferredAudioDevice(dac)
            effectManager.setBitPerfectMode(true)
            isBitPerfectActive = false
            statusDescription = "$sampleRate Hz / $bitDepthStr (Direct USB Routing)"
            Log.i(TAG, "Pre-Android 14 direct USB routing: $statusDescription on ${dac.productName}")
        }

        notifyStatusUpdate()
    }

    private fun clearBitPerfect(dac: AudioDeviceInfo?) {
        val targetDevice = dac ?: lastPreferredDevice
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && targetDevice != null) {
            try {
                audioManager.clearPreferredMixerAttributes(platformAudioAttributes, targetDevice)
                Log.i(TAG, "Cleared preferred mixer attributes for ${targetDevice.productName}")
            } catch (t: Throwable) {
                Log.w(TAG, "Error clearing preferred mixer attributes", t)
            }
        }
        lastPreferredDevice = null
        exoPlayer?.setPreferredAudioDevice(null)
        effectManager.setBitPerfectMode(false)
        isBitPerfectActive = false
        statusDescription = if (connectedUsbDevice != null) {
            "${connectedUsbDevice?.productName} (Passthrough Disabled)"
        } else {
            ""
        }
        notifyStatusUpdate()
    }

    private fun notifyStatusUpdate() {
        mainHandler.post {
            onStatusChanged?.invoke(getStatus())
        }
    }

    fun release() {
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (_: Throwable) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mixerAttributesListener?.let {
                try {
                    audioManager.removeOnPreferredMixerAttributesChangedListener(it)
                } catch (_: Throwable) {}
            }
        }

        clearBitPerfect(connectedUsbDevice)
        executor.shutdown()
        exoPlayer = null
    }
}
