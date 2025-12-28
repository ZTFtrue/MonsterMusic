package com.ztftrue.music.play.manager

import android.os.Bundle
import android.os.CountDownTimer

class SleepTimerManager(
    private val onTimerTick: (Long) -> Unit,
    private val onTimerFinish: (Boolean) -> Unit
) {
    var remainingTime = 0L
    var playCompleted = false
    var sleepTime = 0L
    private var countDownTimer: CountDownTimer? = null

    fun setTimer(extras: Bundle) {
        val t = extras.getLong("time")
        val v = extras.getBoolean("play_completed", false)
        sleepTime = t
        playCompleted = v
        stopTimer()

        if (sleepTime > 0) {
            countDownTimer = object : CountDownTimer(sleepTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingTime = millisUntilFinished
                    onTimerTick(remainingTime)
                }

                override fun onFinish() {
                    onTimerFinish(playCompleted)
                }
            }
            countDownTimer?.start()
        } else {
            // Cancel case
            onTimerTick(0)
        }
    }

    fun stopTimer() {
        remainingTime = 0
        countDownTimer?.cancel()
    }
}