package com.ztftrue.music.utils

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer

class BluetoothConnectionReceiver(private val exoPlayer: ExoPlayer) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("TAG", "onReceive")
        if (intent == null) return
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            // Check if the connected device is your intended Bluetooth device (optional)
            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            if (device != null) {
                Log.d(
                    "TAG",
                    "Connected to device: ${device.name}, ${device.address}, ${device.type}, ${device.alias}"
                )
                // Optionally, filter by device name or address
//            if (device.name == "YourBluetoothDeviceName") {
                // Autoplay logic
                if (!exoPlayer.isPlaying) {
                    exoPlayer.playWhenReady = true
                    exoPlayer.prepare()
                    exoPlayer.play()
                }
//            }
            }
        } else if (intent.action == Intent.ACTION_HEADSET_PLUG) {
            val state = intent.getIntExtra("state", -1)
            val name = intent.getStringExtra("name")
            val microphone = intent.getIntExtra("microphone", -1)
            /**
             * state - 0 for unplugged, 1 for plugged.
             * name - Headset type, human readable string
             * microphone - 1 if headset has a microphone, 0 otherwise
             */
            when (state) {
                0 -> {
                    // Headset is unplugged
                    Log.d("HeadsetPlugReceiver", "Headset unplugged")
                }

                1 -> {
                    // Headset is plugged in
                    if (!exoPlayer.isPlaying && microphone == 0) {
                        exoPlayer.playWhenReady = true
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                }

                else -> {
                    // Unknown state
                    Log.d("HeadsetPlugReceiver", "Unknown headset state")
                }
            }
        }

    }
}
