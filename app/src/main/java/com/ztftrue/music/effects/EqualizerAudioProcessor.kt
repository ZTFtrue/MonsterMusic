package com.ztftrue.music.effects

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.effects.SoundUtils.downsampleMagnitudes
import com.ztftrue.music.play.AudioDataRepository
import com.ztftrue.music.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.pow

@UnstableApi
class EqualizerAudioProcessor : AudioProcessor {

    companion object {
        private const val TAG = "EqualizerProcessor"
        private const val BUFFER_HEADROOM = 4096
        private const val FILTER_TYPE_PEAK = 3

        private var isNativeLoaded = false

        init {
            try {
                System.loadLibrary("monster_audio")
                isNativeLoaded = true
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load monster_audio library", t)
                isNativeLoaded = false
            }
        }
    }

    // Native Equalizer & PCM processing JNI
    private var nativeEqualizerHandle: Long = 0L

    private external fun initNativeEqualizer(channelCount: Int, bandCount: Int, sampleRate: Float): Long
    private external fun freeNativeEqualizer(handle: Long)
    private external fun resetNativeEqualizer(handle: Long)
    private external fun resetLimiterNative(handle: Long)
    private external fun configureBandNative(
        handle: Long,
        channel: Int,
        band: Int,
        type: Int,
        centerFreq: Float,
        sampleRate: Float,
        q: Float,
        gainDb: Float
    )
    private external fun setEchoParamsNative(
        handle: Long,
        delayTime: Float,
        decay: Float,
        feedback: Boolean,
        sampleRate: Float
    )
    private external fun processPcmDirectNative(
        handle: Long,
        inputBuffer: ByteBuffer,
        outputBuffer: ByteBuffer,
        framesCount: Int,
        channelCount: Int,
        encoding: Int,
        eqActive: Boolean,
        echoActive: Boolean,
        visArray: FloatArray?
    ): Int
    private external fun setEqualizerTypeNative(handle: Long, type: Int)
    private external fun setVirtualizerNative(handle: Long, enabled: Boolean, strength: Float)
    private external fun setReverbNative(
        handle: Long,
        enabled: Boolean,
        roomSize: Float,
        damping: Float,
        mix: Float
    )
    private external fun setChorusNative(
        handle: Long,
        enabled: Boolean,
        rate: Float,
        depth: Float,
        mix: Float
    )
    private external fun setFlangerNative(
        handle: Long,
        enabled: Boolean,
        rate: Float,
        depth: Float,
        feedback: Float,
        mix: Float
    )
    private external fun setPolyphonyNative(
        handle: Long,
        enabled: Boolean,
        semitones: Int,
        detuneCents: Float,
        mix: Float
    )
    private external fun setDelayNative(
        handle: Long,
        enabled: Boolean,
        delayTime: Float,
        feedback: Float,
        mix: Float
    )

    // Native FFT JNI
    private var nativeFftHandle: Long = 0L
    private var equalizerType: Int = 0

    private external fun initNativeFft(size: Int): Long
    private external fun freeNativeFft(handle: Long)
    private external fun processNativeFft(
        handle: Long,
        inputArray: FloatArray,
        outputArray: FloatArray
    )

    private var equalizerActive = false
    private var echoActive = false
    private var visualizationAudioActive = false

    // Effect states
    private var virtualizerActive = false
    private var virtualizerStrength = 0.0f

    private var reverbActive = false
    private var reverbRoomSize = 0.5f
    private var reverbDamping = 0.5f
    private var reverbMix = 0.3f

    private var chorusActive = false
    private var chorusRate = 1.5f
    private var chorusDepth = 0.5f
    private var chorusMix = 0.5f

    private var flangerActive = false
    private var flangerRate = 0.5f
    private var flangerDepth = 0.7f
    private var flangerFeedback = 0.5f
    private var flangerMix = 0.5f

    private var polyphonyActive = false
    private var polyphonySemitones = 0
    private var polyphonyDetune = 0.0f
    private var polyphonyMix = 0.5f

    private var delayActive = false
    private var delayEffectTime = 0.35f
    private var delayEffectFeedback = 0.4f
    private var delayEffectMix = 0.4f

    private var inputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET

    private var bufferContainer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER

    private var inputEnded = false
    private var echoDelay = 0.0f
    private var echoDecay = 0.0f
    private var isWithFeedBack: Boolean = false

    // EQ Configuration
    private val gainDBArray: IntArray = IntArray(Utils.bandsCenter.size) { 0 }
    private val gainDBAbsArray: FloatArray = FloatArray(Utils.bandsCenter.size) { 1.0f }
    private var Q = Utils.Q
    private var changeDb = false

    // Visualization
    private val fftSize = 1024
    private val halfFftSize = fftSize / 2
    private val visRingBufferLen = 4096
    private var visRingBuffer = FloatArray(visRingBufferLen)
    private var visWritePos = 0
    private var visPcmBuffer = FloatArray(visRingBufferLen)
    private var visTempArray = FloatArray(fftSize)
    private var nativeFftMagnitudes = FloatArray(halfFftSize)

    private val lock = ReentrantLock()

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat

        val frameSize = inputAudioFormat.bytesPerFrame
        val initialSize = inputAudioFormat.sampleRate * frameSize / 5
        if (bufferContainer.capacity() < initialSize) {
            bufferContainer = ByteBuffer.allocateDirect(initialSize).order(ByteOrder.nativeOrder())
        }

        val sampleRate = inputAudioFormat.sampleRate.toFloat()
        val channelCount = inputAudioFormat.channelCount

        if (isNativeLoaded) {
            if (nativeEqualizerHandle != 0L) {
                freeNativeEqualizer(nativeEqualizerHandle)
            }
            val allocChannels = maxOf(channelCount, 2)
            nativeEqualizerHandle = initNativeEqualizer(allocChannels, Utils.bandsCenter.count(), sampleRate)
            setEqualizerTypeNative(nativeEqualizerHandle, equalizerType)
            setEchoParamsNative(nativeEqualizerHandle, echoDelay, echoDecay, isWithFeedBack, sampleRate)
            setVirtualizerNative(nativeEqualizerHandle, virtualizerActive, virtualizerStrength)
            setReverbNative(nativeEqualizerHandle, reverbActive, reverbRoomSize, reverbDamping, reverbMix)
            setChorusNative(nativeEqualizerHandle, chorusActive, chorusRate, chorusDepth, chorusMix)
            setFlangerNative(nativeEqualizerHandle, flangerActive, flangerRate, flangerDepth, flangerFeedback, flangerMix)
            setPolyphonyNative(nativeEqualizerHandle, polyphonyActive, polyphonySemitones, polyphonyDetune, polyphonyMix)
            setDelayNative(nativeEqualizerHandle, delayActive, delayEffectTime, delayEffectFeedback, delayEffectMix)

            if (nativeFftHandle != 0L) {
                freeNativeFft(nativeFftHandle)
            }
            nativeFftHandle = initNativeFft(fftSize)
        }

        visTempArray = FloatArray(fftSize)
        visRingBuffer = FloatArray(visRingBufferLen)
        visPcmBuffer = FloatArray(visRingBufferLen)
        nativeFftMagnitudes = FloatArray(halfFftSize)

        restoreBands()

        return outputAudioFormat
    }

    override fun isActive(): Boolean {
        return outputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        processChunk(inputBuffer, inputBuffer.remaining())
        if (inputBuffer.hasRemaining()) {
            inputBuffer.position(inputBuffer.limit())
        }
    }

    private fun processChunk(data: ByteBuffer, length: Int) {
        val needsProcessing = equalizerActive || echoActive || visualizationAudioActive ||
                virtualizerActive || reverbActive || chorusActive || flangerActive || polyphonyActive

        val resultBuffer = replaceOutputBuffer(length)

        if (!needsProcessing) {
            resultBuffer.put(data)
            resultBuffer.flip()
            return
        }

        val channelCount = inputAudioFormat.channelCount
        val encoding = if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) 1 else 0
        val bytesPerSample = if (encoding == 1) 4 else 2
        val sampleCountTotal = length / bytesPerSample
        val framesCount = sampleCountTotal / channelCount

        if (changeDb) {
            lock.lock()
            try {
                if (nativeEqualizerHandle != 0L) {
                    resetNativeEqualizer(nativeEqualizerHandle)
                    resetLimiterNative(nativeEqualizerHandle)
                }
                changeDb = false
            } finally {
                lock.unlock()
            }
        }

        // Process PCM directly in Native C
        val visBuffer = if (visualizationAudioActive) visPcmBuffer else null
        val visCount = if (nativeEqualizerHandle != 0L) {
            processPcmDirectNative(
                nativeEqualizerHandle,
                data,
                resultBuffer,
                framesCount,
                channelCount,
                encoding,
                equalizerActive,
                echoActive,
                visBuffer
            )
        } else {
            0
        }

        resultBuffer.position(0)
        resultBuffer.limit(length)

        // Process visualization
        if (visualizationAudioActive && visBuffer != null && visCount > 0) {
            processVisualization(visBuffer, visCount)
        }
    }

    private fun replaceOutputBuffer(count: Int): ByteBuffer {
        if (bufferContainer.capacity() < count) {
            val newSize = count + BUFFER_HEADROOM
            bufferContainer = ByteBuffer.allocateDirect(newSize).order(ByteOrder.nativeOrder())
        }
        bufferContainer.clear()
        outputBuffer = bufferContainer
        return outputBuffer
    }

    private fun processVisualization(samples: FloatArray, count: Int) {
        for (i in 0 until count) {
            visRingBuffer[visWritePos] = samples[i]
            visWritePos = (visWritePos + 1) and (visRingBufferLen - 1)
        }

        var readPos = (visWritePos - fftSize)
        if (readPos < 0) readPos += visRingBufferLen
        for (i in 0 until fftSize) {
            visTempArray[i] = visRingBuffer[readPos]
            readPos = (readPos + 1) and (visRingBufferLen - 1)
        }

        if (nativeFftHandle != 0L) {
            processNativeFft(nativeFftHandle, visTempArray, nativeFftMagnitudes)
            val m = downsampleMagnitudes(
                nativeFftMagnitudes,
                targetSize = 32,
                minDb = -60f,
                needNormalize = true,
                needPositive = false,
                refValue = (fftSize / 4f), // 256f full-scale reference for 1024-pt Hann window FFT
                tiltFactor = 1.0f          // Equal-loudness acoustic compensation across spectrum
            )
            AudioDataRepository.postVisualizationData(m)
        }
    }

    override fun getOutput(): ByteBuffer {
        val ret = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return ret
    }

    override fun isEnded(): Boolean {
        return inputEnded && !outputBuffer.hasRemaining()
    }

    override fun flush() {
        lock.lock()
        try {
            outputBuffer = EMPTY_BUFFER
            bufferContainer.clear()
            inputEnded = false
            if (nativeEqualizerHandle != 0L) {
                resetNativeEqualizer(nativeEqualizerHandle)
                resetLimiterNative(nativeEqualizerHandle)
            }
        } finally {
            lock.unlock()
        }
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        bufferContainer = EMPTY_BUFFER
        if (nativeEqualizerHandle != 0L) {
            freeNativeEqualizer(nativeEqualizerHandle)
            nativeEqualizerHandle = 0L
        }
        if (nativeFftHandle != 0L) {
            freeNativeFft(nativeFftHandle)
            nativeFftHandle = 0L
        }
    }

    fun isSetActive(): Boolean = equalizerActive

    fun setEqualizerActive(active: Boolean) {
        if (equalizerActive != active) {
            lock.lock()
            try {
                equalizerActive = active
                if (active) {
                    restoreBands()
                }
            } finally {
                lock.unlock()
            }
        }
    }

    fun setEchoActive(active: Boolean) {
        echoActive = active
    }

    fun setVisualizationAudioActive(active: Boolean) {
        visualizationAudioActive = active
    }

    fun setEqualizerType(type: Int) {
        lock.lock()
        try {
            equalizerType = type
            if (nativeEqualizerHandle != 0L) {
                setEqualizerTypeNative(nativeEqualizerHandle, type)
            }
        } finally {
            lock.unlock()
        }
    }

    fun getEqualizerType(): Int = equalizerType

    fun setBand(index: Int, value: Int) {
        lock.lock()
        try {
            if (index in gainDBArray.indices) {
                gainDBArray[index] = value
                gainDBAbsArray[index] = 10.0.pow(value.toDouble() / 20.0).toFloat()
                configureBandFilter(index, value)
                changeDb = true
            }
        } finally {
            lock.unlock()
        }
    }

    fun flatBand(): Boolean {
        lock.lock()
        try {
            if (!isSetActive()) return false

            for (i in gainDBArray.indices) {
                gainDBArray[i] = 0
                gainDBAbsArray[i] = 1.0f
                configureBandFilter(i, 0)
            }

            if (nativeEqualizerHandle != 0L) {
                resetLimiterNative(nativeEqualizerHandle)
            }
            changeDb = true
            return true
        } finally {
            lock.unlock()
        }
    }

    fun setQ(value: Float, needChange: Boolean = true) {
        lock.lock()
        try {
            this.Q = value
            if (needChange) {
                restoreBands()
                changeDb = true
            }
        } finally {
            lock.unlock()
        }
    }

    fun getBandLevels(): IntArray {
        lock.lock()
        try {
            return gainDBArray
        } finally {
            lock.unlock()
        }
    }

    fun setDelayTime(value: Float) {
        echoDelay = value
        updateNativeEcho()
    }

    fun setDecay(value: Float) {
        echoDecay = value
        updateNativeEcho()
    }

    fun setFeedBack(value: Boolean) {
        isWithFeedBack = value
        updateNativeEcho()
    }

    private fun updateNativeEcho() {
        if (nativeEqualizerHandle != 0L && outputAudioFormat.sampleRate > 0) {
            val sampleRate = outputAudioFormat.sampleRate.toFloat()
            setEchoParamsNative(nativeEqualizerHandle, echoDelay, echoDecay, isWithFeedBack, sampleRate)
        }
    }

    private fun restoreBands() {
        for (i in gainDBArray.indices) {
            configureBandFilter(i, gainDBArray[i])
        }
    }

    private fun configureBandFilter(index: Int, value: Int) {
        if (outputAudioFormat.sampleRate > 0 && nativeEqualizerHandle != 0L) {
            val freq = Utils.bandsCenter[index]
            val rate = outputAudioFormat.sampleRate.toFloat()
            val targetChannels = maxOf(inputAudioFormat.channelCount, 2)
            for (ch in 0 until targetChannels) {
                configureBandNative(
                    nativeEqualizerHandle,
                    ch,
                    index,
                    FILTER_TYPE_PEAK,
                    freq,
                    rate,
                    Q,
                    value.toFloat()
                )
            }
        }
    }

    fun setVirtualizer(enabled: Boolean, strength: Float) {
        lock.lock()
        try {
            virtualizerActive = enabled
            virtualizerStrength = strength
            if (nativeEqualizerHandle != 0L) {
                setVirtualizerNative(nativeEqualizerHandle, enabled, strength)
            }
        } finally {
            lock.unlock()
        }
    }

    fun setReverb(enabled: Boolean, roomSize: Float, damping: Float, mix: Float) {
        lock.lock()
        try {
            reverbActive = enabled
            reverbRoomSize = roomSize
            reverbDamping = damping
            reverbMix = mix
            if (nativeEqualizerHandle != 0L) {
                setReverbNative(nativeEqualizerHandle, enabled, roomSize, damping, mix)
            }
        } finally {
            lock.unlock()
        }
    }

    fun setChorus(enabled: Boolean, rate: Float, depth: Float, mix: Float) {
        lock.lock()
        try {
            chorusActive = enabled
            chorusRate = rate
            chorusDepth = depth
            chorusMix = mix
            if (nativeEqualizerHandle != 0L) {
                setChorusNative(nativeEqualizerHandle, enabled, rate, depth, mix)
            }
        } finally {
            lock.unlock()
        }
    }

    fun setFlanger(enabled: Boolean, rate: Float, depth: Float, feedback: Float, mix: Float) {
        lock.lock()
        try {
            flangerActive = enabled
            flangerRate = rate
            flangerDepth = depth
            flangerFeedback = feedback
            flangerMix = mix
            if (nativeEqualizerHandle != 0L) {
                setFlangerNative(nativeEqualizerHandle, enabled, rate, depth, feedback, mix)
            }
        } finally {
            lock.unlock()
        }
    }

    fun setPolyphony(enabled: Boolean, semitones: Int, detuneCents: Float, mix: Float) {
        lock.lock()
        try {
            polyphonyActive = enabled
            polyphonySemitones = semitones
            polyphonyDetune = detuneCents
            polyphonyMix = mix
            if (nativeEqualizerHandle != 0L) {
                setPolyphonyNative(nativeEqualizerHandle, enabled, semitones, detuneCents, mix)
            }
        } finally {
            lock.unlock()
        }
    }

    fun setDelay(enabled: Boolean, delayTime: Float, feedback: Float, mix: Float) {
        lock.lock()
        try {
            delayActive = enabled
            delayEffectTime = delayTime
            delayEffectFeedback = feedback
            delayEffectMix = mix
            if (nativeEqualizerHandle != 0L) {
                setDelayNative(nativeEqualizerHandle, enabled, delayTime, feedback, mix)
            }
        } finally {
            lock.unlock()
        }
    }
}