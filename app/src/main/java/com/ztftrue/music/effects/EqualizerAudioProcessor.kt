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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

@UnstableApi
class EqualizerAudioProcessor : AudioProcessor {

    companion object {
        private const val TAG = "EqualizerProcessor"
        private const val PCM_16_BIT_MAX = 32767.0f
        // 缓冲区扩容时的余量，避免频繁扩容
        private const val BUFFER_HEADROOM = 4096
    }

    private var equalizerActive = false
    private var echoActive = false
    private var visualizationAudioActive = false

    private var inputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET

    // ===============================================================
    // Buffers (持久化持有，避免 GC)
    // ===============================================================
    // 1. 持有原生内存的容器
    private var bufferContainer: ByteBuffer = EMPTY_BUFFER
    // 2. 指向当前处理结果的引用 (API 要求)
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    // 3. 浮点运算用的声道缓冲区 [Channel][Sample]
    private var channelBuffers: Array<FloatArray> = emptyArray()

    // 4. 用户要求的 Max 记录 (用于 Limiter 逻辑)
    private var channelEqualizerMaxs = FloatArray(2) { 1.0f }

    private var inputEnded = false

    // Effects
    private var delayEffectLeft: DelayEffect = DelayEffect(0.5f, 1.0f, 44100.0f)
    private var delayEffectRight: DelayEffect = DelayEffect(0.5f, 1.0f, 44100.0f)
    private var channelDelays = arrayOf(delayEffectLeft, delayEffectRight)

    // EQ Filters
    private val mCoefficientLeftBiQuad: ArrayList<BiQuadraticFilter> = arrayListOf()
    private val mCoefficientRightBiQuad: ArrayList<BiQuadraticFilter> = arrayListOf()
    // 方便循环处理 [Channel][BandIndex]
    private var channelFilters = arrayOf(mCoefficientLeftBiQuad, mCoefficientRightBiQuad)

    // EQ Configuration
    private val gainDBArray: IntArray = IntArray(10) { 0 }
    // 用于记录分贝值的绝对系数 (用于 UI 或逻辑判断，暂保留)
    private val gainDBAbsArray: FloatArray = FloatArray(10) { 1.0f }

    private var Q = Utils.Q
    private var changeDb = false

    // Visualization Ring Buffer
    private val visRingBufferLen = 4096
    private var visRingBuffer = FloatArray(visRingBufferLen)
    private var visWritePos = 0
    private var pcmToFrequencyDomain: PCMToFrequencyDomain? = null
    private var visTempArray: FloatArray = FloatArray(0)

    private val lock = ReentrantLock()

    init {
        // 初始化滤波器列表
        repeat(Utils.bandsCenter.count()) {
            this.mCoefficientLeftBiQuad.add(BiQuadraticFilter())
            this.mCoefficientRightBiQuad.add(BiQuadraticFilter())
        }
        // 初始化 channelFilters 引用
        channelFilters = arrayOf(mCoefficientLeftBiQuad, mCoefficientRightBiQuad)
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat

        // 预分配内存 (预估 200ms 数据量)
        val frameSize = inputAudioFormat.bytesPerFrame
        val initialSize = inputAudioFormat.sampleRate * frameSize / 5
        if (bufferContainer.capacity() < initialSize) {
            Log.d(TAG, "allocateDirect1")
            bufferContainer = ByteBuffer.allocateDirect(initialSize).order(ByteOrder.nativeOrder())
        }

        // 初始化/重置效果器
        val sampleRate = inputAudioFormat.sampleRate.toFloat()
        delayEffectLeft = DelayEffect(0.5f, 1.0f, sampleRate)
        delayEffectRight = DelayEffect(0.5f, 1.0f, sampleRate)
        channelDelays = arrayOf(delayEffectLeft, delayEffectRight)

        // 重置 Max 记录
        channelEqualizerMaxs = FloatArray(inputAudioFormat.channelCount) { 1.0f }

        // 初始化 FFT
        val fftSize = 1024
        pcmToFrequencyDomain = PCMToFrequencyDomain(fftSize, sampleRate)
        visTempArray = FloatArray(fftSize)
        visRingBuffer = FloatArray(4096)

        // 应用当前的 EQ 设置到新采样率的 Filter
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
        val needsProcessing = equalizerActive || echoActive || visualizationAudioActive

        // 1. 获取输出容器 (复用)
        val resultBuffer = replaceOutputBuffer(length)

        // 2. Fast Path
        if (!needsProcessing) {
            resultBuffer.put(data)
            resultBuffer.flip()
            return
        }

        val channelCount = inputAudioFormat.channelCount
        val encoding = inputAudioFormat.encoding
        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val sampleCountTotal = length / bytesPerSample
        val framesCount = sampleCountTotal / channelCount

        // 3. 准备声道浮点数组 (只增不减策略)
        if (channelBuffers.isEmpty() || channelBuffers[0].size < framesCount) {
            val newSize = framesCount + BUFFER_HEADROOM
            if (channelBuffers.size != channelCount) {
                Log.d("CHANNEL","BUFFRT")
                channelBuffers = Array(channelCount) { FloatArray(newSize) }
            } else {
                for (i in 0 until channelCount) {
                    channelBuffers[i] = FloatArray(newSize)
                }
            }
        }

        // 4. De-interleave & Convert to Float
        val startPos = data.position()
        if (encoding == C.ENCODING_PCM_FLOAT) {
            for (i in 0 until framesCount) {
                for (ch in 0 until channelCount) {
                    val offset = startPos + (i * channelCount + ch) * 4
                    channelBuffers[ch][i] = data.getFloat(offset)
                }
            }
        } else {
            // 16-bit PCM
            for (i in 0 until framesCount) {
                for (ch in 0 until channelCount) {
                    val offset = startPos + (i * channelCount + ch) * 2
                    val byte1 = data.get(offset).toInt() and 0xFF
                    val byte2 = data.get(offset + 1).toInt() shl 8
                    val shortVal = (byte1 or byte2).toShort()
                    channelBuffers[ch][i] = shortVal / PCM_16_BIT_MAX
                }
            }
        }

        // 5. 重置 Filter 状态 (如果参数发生了变化)
        if (changeDb) {
            lock.lock()
            try {
                for (chFilters in channelFilters) {
                    for (filter in chFilters) filter.reset()
                }
                // 重置 Max 值，防止之前的爆音记录持续压低音量
                channelEqualizerMaxs.fill(1.0f)
                changeDb = false
            } finally {
                lock.unlock()
            }
        }

        // 6. 应用音效 (EQ & Echo) & Limiter Logic
        for (ch in 0 until channelCount) {
            val samples = channelBuffers[ch]

            // A. Echo
            if (echoActive) {
                channelDelays[ch].process(samples, framesCount)
            }

            // B. EQ
            if (equalizerActive) {
                applyEqualizer(samples, channelFilters[ch], framesCount)
            }

            // C. Limiter Logic (用户要求保留的部分)
            if (echoActive || equalizerActive) {
                // 2. 更新历史 Max
                channelEqualizerMaxs[ch] = max(channelEqualizerMaxs[ch],   Limiter.process(samples))

                // 3. 如果超过 1.0，进行全局归一化
                if (channelEqualizerMaxs[ch] > 1.0f) {
                    val invMax = 1.0f / channelEqualizerMaxs[ch]
                    for (i in 0 until framesCount) {
                        samples[i] = samples[i] * invMax
                    }
                }
            }
        }

        // 7. Visualization
        if (visualizationAudioActive) {
            processVisualization(channelBuffers[0], channelBuffers[1], framesCount)
        }

        // 8. Interleave & Output
        if (encoding == C.ENCODING_PCM_FLOAT) {
            for (i in 0 until framesCount) {
                for (ch in 0 until channelCount) {
                    resultBuffer.putFloat(channelBuffers[ch][i])
                }
            }
        } else {
            for (i in 0 until framesCount) {
                for (ch in 0 until channelCount) {
                    var valFloat = channelBuffers[ch][i] * PCM_16_BIT_MAX
                    if (valFloat > 32767f) valFloat = 32767f
                    else if (valFloat < -32768f) valFloat = -32768f

                    val shortVal = valFloat.toInt()
                    resultBuffer.put(shortVal.toByte())
                    resultBuffer.put((shortVal shr 8).toByte())
                }
            }
        }

        resultBuffer.flip()
    }

    private fun replaceOutputBuffer(count: Int): ByteBuffer {
        if (bufferContainer.capacity() < count) {
            val newSize = count + BUFFER_HEADROOM
            Log.d(TAG, "allocateDirect2")
            bufferContainer = ByteBuffer.allocateDirect(newSize).order(ByteOrder.nativeOrder())
        }
        bufferContainer.clear()
        outputBuffer = bufferContainer
        return outputBuffer
    }

    private fun applyEqualizer(samples: FloatArray, filters: ArrayList<BiQuadraticFilter>, length: Int) {
        // 内层循环优化：避免迭代器
        val filterSize = filters.size
        for (i in 0 until length) {
            var s = samples[i]
            for (j in 0 until filterSize) {
                s = filters[j].filter(s)
            }
            samples[i] = s
        }
    }

    private fun processVisualization(left: FloatArray, right: FloatArray, count: Int) {
        for (i in 0 until count) {
            val mix = (left[i] + right[i]) / 2f
            visRingBuffer[visWritePos] = mix
            visWritePos = (visWritePos + 1) and (visRingBufferLen - 1)
        }
        val fftSize = visTempArray.size
        var readPos = (visWritePos - fftSize)
        if (readPos < 0) readPos += visRingBufferLen
        for (i in 0 until fftSize) {
            visTempArray[i] = visRingBuffer[readPos]
            readPos = (readPos + 1) and (visRingBufferLen - 1)
        }
        pcmToFrequencyDomain?.let { fft ->
            val magnitudes = fft.process(visTempArray)
            val m = downsampleMagnitudes(
                magnitudes, 32, -60f, needNormalize = false,
                needPositive = true
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
            for (chFilters in channelFilters) {
                for (filter in chFilters) filter.reset()
            }
            channelEqualizerMaxs.fill(1.0f) // Reset Limiter state
            delayEffectLeft.flush()
            delayEffectRight.flush()
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
    }

    // ===================================
    // Configuration & Getters
    // ===================================
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

    /**
     * 设置特定频段的增益
     */
    fun setBand(index: Int, value: Int) {
        lock.lock()
        try {
            if (index in gainDBArray.indices) {
                gainDBArray[index] = value
                gainDBAbsArray[index] = 10.0.pow(value.toDouble() / 20.0).toFloat()
                configureBandFilter(index, value)
                // 标记变化，以便 processChunk 重置滤波器历史状态
                changeDb = true
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * 将所有频段重置为 0dB
     */
    fun flatBand(): Boolean {
        lock.lock()
        try {
            if (!isActive()) return false

            for (i in gainDBArray.indices) {
                gainDBArray[i] = 0
                gainDBAbsArray[i] = 1.0f
                configureBandFilter(i, 0)
            }

            // 重要的：重置限幅器最大值，否则之前的高音量会一直压制声音
            channelEqualizerMaxs.fill(1.0f)
            changeDb = true
            return true
        } finally {
            lock.unlock()
        }
    }

    /**
     * 设置 Q 值 (Quality Factor) 并重新应用到所有滤波器
     */
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

    /**
     * 获取当前所有频段的增益数组副本
     */
    fun getBandLevels(): IntArray {
        lock.lock()
        try {
            return gainDBArray
        } finally {
            lock.unlock()
        }
    }

    fun setDelayTime(value: Float) {
        delayEffectLeft.setEchoLength(value)
        delayEffectRight.setEchoLength(value)
    }

    fun setDecay(value: Float) {
        delayEffectLeft.setDecay(value)
        delayEffectRight.setDecay(value)
    }

    fun setFeedBack(value: Boolean) {
        delayEffectLeft.isWithFeedBack = value
        delayEffectRight.isWithFeedBack = value
    }

    // ===================================
    // Internal Helper
    // ===================================

    private fun restoreBands() {
        for (i in gainDBArray.indices) {
            configureBandFilter(i, gainDBArray[i])
        }
    }

    private fun configureBandFilter(index: Int, value: Int) {
        // 只有当采样率有效时才配置，否则 configure() 会在初始化时调用 restoreBands
        if (outputAudioFormat.sampleRate > 0) {
            val freq = Utils.bandsCenter[index]
            val rate = outputAudioFormat.sampleRate.toFloat()
            // 为左右声道配置相同的参数
            mCoefficientLeftBiQuad[index].configure(BiQuadraticFilter.PEAK, freq, rate, Q, value.toFloat())
            mCoefficientRightBiQuad[index].configure(BiQuadraticFilter.PEAK, freq, rate, Q, value.toFloat())
        }
    }
}