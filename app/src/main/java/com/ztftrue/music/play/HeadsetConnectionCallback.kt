package com.ztftrue.music.play

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import com.ztftrue.music.utils.SharedPreferencesUtils

class HeadsetConnectionCallback(private val browser: Player?, private val context: Context) :
    AudioDeviceCallback() {
    override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo?>?) {
        super.onAudioDevicesAdded(addedDevices)
        if (addedDevices != null) {
            for (device in addedDevices) {
                val type = device?.type
                if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                    waitForDeviceRealConnected(context)
                } else if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    waitForDeviceRealConnected(context)
                }
            }
        }
    }

    /**
     *
     */
    private fun waitForDeviceRealConnected(context: Context) {
        val waitTime = SharedPreferencesUtils.getAutoPlayWaitTime(context)
        Handler(Looper.getMainLooper()).postDelayed({
            browser?.play()
        }, waitTime)
    }
}