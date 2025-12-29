package com.ztftrue.music.effects

import java.util.Arrays

class DelayEffect(
        echoLength: Float,
        decay: Float,
        private val sampleRate: Float
) {
    private var echoBuffer: FloatArray = FloatArray(0)
    private var position: Int = 0
    private var decay: Float = 0f

    var isWithFeedBack: Boolean = false
    private var pendingEchoLength: Float = -1f

    init {
        setDecay(decay)
        setEchoLength(echoLength)
        checkPendingLength()
    }

    fun setEchoLength(newEchoLength: Float) {
        this.pendingEchoLength = newEchoLength
    }

    fun setDecay(newDecay: Float) {
        this.decay = newDecay.coerceIn(0f, 1f)
    }

    fun process(floatBuffer: FloatArray, length: Int) {
        checkPendingLength()

        if (echoBuffer.isEmpty()) return

        val bufferLen = echoBuffer.size
        var cursor = position
        val localDecay = decay
        val feedback = isWithFeedBack

        for (i in 0 until length) {
            val inputSample = floatBuffer[i]
            val delaySample = echoBuffer[cursor]

            val outputSample = inputSample + (delaySample * localDecay)
            floatBuffer[i] = outputSample

            echoBuffer[cursor] = if (feedback) outputSample else inputSample

            cursor++
            if (cursor >= bufferLen) {
                cursor = 0
            }
        }
        position = cursor
    }

    // 兼容旧调用
    fun process(floatBuffer: FloatArray) {
        process(floatBuffer, floatBuffer.size)
    }

    private fun checkPendingLength() {
        if (pendingEchoLength != -1f) {
            val newSize = (sampleRate * pendingEchoLength).toInt()
            if (newSize <= 0) {
                pendingEchoLength = -1f
                return
            }

            if (newSize != echoBuffer.size) {
                val newBuffer = FloatArray(newSize)
                if (echoBuffer.isNotEmpty()) {
                    val oldSize = echoBuffer.size
                    val lengthToCopy = minOf(oldSize, newSize)
                    for(i in 0 until lengthToCopy) {
                        val srcIndex = (position + i) % oldSize
                        newBuffer[i] = echoBuffer[srcIndex]
                    }
                }
                this.echoBuffer = newBuffer
                this.position = 0
            }
            pendingEchoLength = -1f
        }
    }

    fun flush() {
        Arrays.fill(echoBuffer, 0f)
        position = 0
    }
}