package com.ztftrue.music.utils

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer

class BluetoothConnectionReceiver(private val exoPlayer: ExoPlayer) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return
        val waitTime = SharedPreferencesUtils.getAutoPlayWaitTime(context)
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            // Check if the connected device is your intended Bluetooth device (optional)
            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            if (device != null) {
                // Headset or Bluetooth device is connected, introduce a delay
                Handler(Looper.getMainLooper()).postDelayed({
                    // Now start playback or check again if the device can play audio
                    waitForAudioRouteAndPlay(context, waitTime)
                }, waitTime) // Adjust the delay as necessary
            }
        } else if (intent.action == Intent.ACTION_HEADSET_PLUG) {
            val state = intent.getIntExtra("state", -1)
//            val name = intent.getStringExtra("name")
//            val microphone = intent.getIntExtra("microphone", -1)
            /**
             * state - 0 for unplugged, 1 for plugged.
             * name - Headset type, human readable string
             * microphone - 1 if headset has a microphone, 0 otherwise
             */
            when (state) {
                0 -> {
                    // Headset is unplugged
//                    Log.d("HeadsetPlugReceiver", "Headset unplugged")
                }

                1 -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Now start playback or check again if the device can play audio
                        waitForAudioRouteAndPlay(context, waitTime)
                    }, 1000) // Adj
                }

                else -> {
                    // Unknown state
//                    Log.d("HeadsetPlugReceiver", "Unknown headset state")
                }
            }
        }

    }

    var times = 0

    private fun waitForAudioRouteAndPlay(context: Context, waitTime: Long) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val deviceInfo = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                var isWiredHeadsetConnected = false
                var isBluetoothConnected = false
                for (it in deviceInfo) {
                    when (it.type) {
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                        AudioDeviceInfo.TYPE_WIRED_HEADSET -> {
                            isWiredHeadsetConnected = true
                            break
                        }

                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                            isBluetoothConnected = true
                        }

                        else -> {

                        }
                    }
                }
                if (isWiredHeadsetConnected
                    || isBluetoothConnected
                ) {
                    exoPlayer.playWhenReady = true
                    exoPlayer.prepare()
                    exoPlayer.play()
                    times = 0
                } else {
                    times++
                    if (times <= 3) {
                        handler.postDelayed(this, waitTime)
                    }
                }

            }
        }
        handler.post(runnable)
    }
}
