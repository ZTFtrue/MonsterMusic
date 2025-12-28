package com.ztftrue.music.effects

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
import kotlin.math.max
import kotlin.math.pow

@UnstableApi
class EqualizerAudioProcessor : AudioProcessor {

    companion object {
        // Soft clipping threshold
        private const val CLIP_THRESHOLD = 1.0f

        // Standard PCM 16-bit conversion factors
        private const val PCM_16_BIT_MAX = 32767.0f
    }

    // Effects per channel
    // index 0 = Left (or Mono), index 1 = Right, etc.
    private var channelDelays: ArrayList<DelayEffect> = arrayListOf()

    private var equalizerActive = false
    private var echoActive = false
    private var visualizationAudioActive = false

    private var inputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET

    // Audio Buffers
    private var bufferSize = 4096
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputAccumulator: ByteBuffer = EMPTY_BUFFER

    // Processing Arrays
    // channelBuffers[channelIndex][sampleIndex]
    private var channelBuffers: Array<FloatArray> = emptyArray()

    // Max values for limiter per channel
    private var channelEqualizerMaxs: FloatArray = FloatArray(0)

    private var inputEnded = false

    // EQ Configuration
    private val gainDBAbsArray: FloatArray = FloatArray(10) { 1.0f }
    private val gainDBArray: IntArray = IntArray(10) { 0 }

    // Filter coefficients per channel: channelFilters[channelIndex][bandIndex]
    private var channelFilters: ArrayList<ArrayList<BiQuadraticFilter>> = arrayListOf()

    // Echo Configuration
    private var delayTime = 0.5f
    private var decay = 1.0f
    private var echoFeedBack = false

    // FFT / Visualization
    private var pcmToFrequencyDomain: PCMToFrequencyDomain? = null

    // Lock for thread safety
    private val lock = ReentrantLock()

    // Visualization Circular Buffer
    private var visBuffer: FloatArray = FloatArray(0)
    private var visWriteIndex = 0
    private var visReadIndex = 0
    private var visCount = 0

    private var Q = Utils.Q
    private var changeDb = false

    init {
        // Pre-initialize for at least 2 channels to avoid allocation on main thread if possible,
        // though configure will handle the actual sizing.
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Check for supported encodings.
        // We support 16-bit PCM (standard) and FLOAT PCM (high quality).
        // If unsupported (e.g. PCM_24, PCM_8), return NOT_SET to let ExoPlayer bypass this processor (pass-through).
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        // We support any channel count now.
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat

        val channelCount = inputAudioFormat.channelCount

        // Calculate buffer size: ~100ms or smaller chunk for processing
        bufferSize = 2048 * channelCount * if(inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) 4 else 2

        // Initialize accumulator
        inputAccumulator = ByteBuffer.allocateDirect(bufferSize * 2).order(ByteOrder.nativeOrder())

        // Setup Processing Buffers (De-interleaved)
        // Approximate samples per channel based on buffer size
        val samplesPerChannel = 2048
        channelBuffers = Array(channelCount) { FloatArray(samplesPerChannel) }
        channelEqualizerMaxs = FloatArray(channelCount) { 1.0f }

        // Setup Filters & Effects per channel
        channelFilters.clear()
        channelDelays.clear()

        repeat(channelCount) {
            // Setup Delays
            val delay = DelayEffect(delayTime, decay, inputAudioFormat.sampleRate.toFloat())
            delay.isWithFeedBack = echoFeedBack
            channelDelays.add(delay)

            // Setup EQ Filters (Bands)
            val filters = ArrayList<BiQuadraticFilter>()
            repeat(Utils.bandsCenter.count()) {
                filters.add(BiQuadraticFilter())
            }
            channelFilters.add(filters)
        }

        // FFT Setup
        pcmToFrequencyDomain = PCMToFrequencyDomain(samplesPerChannel, inputAudioFormat.sampleRate.toFloat())

        // Visualization Buffer Setup (Ring Buffer)
        // We mix at most 2 channels, or take Mono
        visBuffer = FloatArray(samplesPerChannel * 2)
        visWriteIndex = 0
        visReadIndex = 0
        visCount = 0

        // Apply bands to all new filters
        for (index in 0 until Utils.bandsCenter.count()) {
            setBand(index, gainDBArray[index])
        }

        return outputAudioFormat
    }

    override fun isActive(): Boolean {
        return outputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
    }

    fun isSetActive(): Boolean = equalizerActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        lock.lock()
        try {
            while (inputBuffer.hasRemaining()) {
                if (inputAccumulator.remaining() < inputBuffer.remaining()) {
                    val newSize = (inputAccumulator.capacity() + inputBuffer.remaining()) * 2
                    val newBuffer = ByteBuffer.allocateDirect(newSize).order(ByteOrder.nativeOrder())
                    inputAccumulator.flip()
                    newBuffer.put(inputAccumulator)
                    inputAccumulator = newBuffer
                }
                val oldLimit = inputBuffer.limit()
                inputAccumulator.put(inputBuffer)
                inputBuffer.limit(oldLimit)
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * Helper to manage output buffer size
     */
    private fun replaceOutputBuffer(count: Int): ByteBuffer {
        if (outputBuffer.capacity() < count) {
            outputBuffer = ByteBuffer.allocateDirect(count).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }
        return outputBuffer
    }

    override fun getOutput(): ByteBuffer {
        lock.lock()
        try {
            if (inputAccumulator.position() >= bufferSize || (inputEnded && inputAccumulator.position() > 0)) {
                inputAccumulator.flip()

                // Align processing chunk to full frames
                val bytesPerFrame = inputAudioFormat.channelCount * if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) 4 else 2
                var bytesToProcess = inputAccumulator.remaining()
                if (bytesToProcess >= bufferSize) {
                    bytesToProcess = bufferSize
                }
                // Ensure we process full frames
                bytesToProcess = (bytesToProcess / bytesPerFrame) * bytesPerFrame

                if (bytesToProcess > 0) {
                    processChunk(inputAccumulator, bytesToProcess)
                }

                inputAccumulator.compact()
                val result = this.outputBuffer
                this.outputBuffer = EMPTY_BUFFER
                return result
            }
            return EMPTY_BUFFER
        } finally {
            lock.unlock()
        }
    }

    /**
     * Core processing logic: Bytes -> Float (De-interleave) -> Effects -> Bytes (Interleave)
     */
    private fun processChunk(data: ByteBuffer, length: Int) {
        val needsProcessing = equalizerActive || echoActive || visualizationAudioActive

        // Prepare output buffer
        val resultBuffer = replaceOutputBuffer(length)

        if (!needsProcessing) {
            // Passthrough
            val limit = data.limit()
            data.limit(data.position() + length)
            resultBuffer.put(data)
            data.limit(limit)
            resultBuffer.flip()
            return
        }

        val channelCount = inputAudioFormat.channelCount
        val encoding = inputAudioFormat.encoding
        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val sampleCountTotal = length / bytesPerSample
        val framesCount = sampleCountTotal / channelCount

        // 1. Resize Channel Buffers if needed
        if (channelBuffers[0].size < framesCount) {
            // Re-allocate for all channels
            for (i in 0 until channelCount) {
                channelBuffers[i] = FloatArray(framesCount)
            }
        }

        // 2. De-interleave & Convert to Float
        val startPos = data.position()

        if (encoding == C.ENCODING_PCM_FLOAT) {
            // Float Input
            for (i in 0 until framesCount) {
                for (ch in 0 until channelCount) {
                    // ByteBuffer.getFloat automatically advances 4 bytes
                    channelBuffers[ch][i] = data.getFloat(startPos + (i * channelCount + ch) * 4)
                }
            }
        } else {
            // 16-bit PCM Input
            for (i in 0 until framesCount) {
                for (ch in 0 until channelCount) {
                    val byteIndex = startPos + (i * channelCount + ch) * 2
                    // Manually get short (Little Endian)
                    val byte1 = data.get(byteIndex).toInt() and 0xFF
                    val byte2 = data.get(byteIndex + 1).toInt() shl 8
                    val shortVal = (byte1 or byte2).toShort()
                    channelBuffers[ch][i] = shortVal / PCM_16_BIT_MAX
                }
            }
        }
        data.position(startPos + length)

        // 3. Reset Filters if needed
        if (changeDb) {
            for (chFilters in channelFilters) {
                for (filter in chFilters) filter.reset()
            }
            changeDb = false
        }

        // 4. Apply Effects (Per Channel)
        for (ch in 0 until channelCount) {
            val samples = channelBuffers[ch]

            // Echo
            if (echoActive) {
                channelDelays[ch].process(samples)
            }

            // Equalizer
            if (equalizerActive) {
                applyEqualizer(samples, channelFilters[ch])
            }
        }

        // 5. Soft Clipping / Limiting (Per Channel)
        if (equalizerActive || echoActive) {
            for (ch in 0 until channelCount) {
                val samples = channelBuffers[ch]
                // Preserve logic: Calculate max peak using Limiter.process
                channelEqualizerMaxs[ch] = max(
                    channelEqualizerMaxs[ch],
                    Limiter.process(samples)
                )

                if (channelEqualizerMaxs[ch] > 1.0f) {
                    val invMax = 1.0f / channelEqualizerMaxs[ch]
                    for (i in 0 until framesCount) {
                        samples[i] = samples[i] * invMax
                    }
                }
            }
        }

        // 6. Visualization (Mix down to stereo or mono)
        if (visualizationAudioActive) {
            // If we have at least 2 channels, use 0 and 1. If mono, use 0 and 0.
            val left = channelBuffers[0]
            val right = if (channelCount > 1) channelBuffers[1] else channelBuffers[0]
            processVisualization(left, right, framesCount)
        }

        // 7. Interleave & Convert back to Output Format
        if (encoding == C.ENCODING_PCM_FLOAT) {
            // Float Output
            for (i in 0 until framesCount) {
                for (ch in 0 until channelCount) {
                    resultBuffer.putFloat(channelBuffers[ch][i])
                }
            }
        } else {
            // 16-bit PCM Output
            for (i in 0 until framesCount) {
                for (ch in 0 until channelCount) {
                    var valFloat = channelBuffers[ch][i] * PCM_16_BIT_MAX

                    // Hard clamp
                    if (valFloat > 32767) valFloat = 32767f else if (valFloat < -32768) valFloat = -32768f

                    val shortVal = valFloat.toInt()
                    resultBuffer.put(shortVal.toByte())
                    resultBuffer.put((shortVal shr 8).toByte())
                }
            }
        }

        resultBuffer.flip()
    }

    private fun applyEqualizer(samples: FloatArray, filters: ArrayList<BiQuadraticFilter>) {
        // Optimized loop: Process all samples per filter chain, or filter chain per sample?
        // Usually filtering is stateful per sample sequence.
        // It's safer to loop samples, then filters for stability, or maintain state correctly.
        // The original code was: Loop samples, then Loop filters.
        for (i in samples.indices) {
            var sample = samples[i]
            for (filter in filters) {
                sample = filter.filter(sample)
            }
            samples[i] = sample
        }
    }

    private fun processVisualization(left: FloatArray, right: FloatArray, count: Int) {
        // 1. Add mixed mono data to circular buffer
        for (i in 0 until count) {
            visBuffer[visWriteIndex] = (left[i] + right[i]) / 2f
            visWriteIndex = (visWriteIndex + 1) % visBuffer.size
            visCount++
        }

        // 2. Consume if we have enough data for FFT
        // Note: Assuming FFT size is fixed (e.g. 512 or 1024), using 2048 here based on buffer calc
        val fftSize = 2048.coerceAtMost(visBuffer.size / 2)

        if (visCount >= fftSize) {
            val tempFftBuffer = FloatArray(fftSize)
            for (i in 0 until fftSize) {
                tempFftBuffer[i] = visBuffer[visReadIndex]
                visReadIndex = (visReadIndex + 1) % visBuffer.size
            }
            visCount -= fftSize

            val magnitude = pcmToFrequencyDomain?.process(tempFftBuffer)
            if (magnitude != null) {
                val m = downsampleMagnitudes(
                    magnitude, 32, -60f, needNormalize = false,
                    needPositive = true
                )
                AudioDataRepository.postVisualizationData(m)
            }
        }
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun isEnded(): Boolean {
        return inputEnded && !outputBuffer.hasRemaining() && inputAccumulator.position() == 0
    }

    override fun flush() {
        lock.lock()
        try {
            outputBuffer = EMPTY_BUFFER
            inputAccumulator.clear()
            inputEnded = false

            // Reset all channel filters
            for (chFilters in channelFilters) {
                for (filter in chFilters) filter.reset()
            }

            // Flush delays
            for (delay in channelDelays) {
                delay.flush()
            }
        } finally {
            lock.unlock()
        }
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pcmToFrequencyDomain = null
        channelFilters.clear()
        channelDelays.clear()
        channelEqualizerMaxs = FloatArray(0)
    }

    // --- Configuration Setters ---

    fun setEqualizerActive(active: Boolean) {
        if (this.equalizerActive != active) {
            lock.lock()
            try {
                this.equalizerActive = active
                if (active) {
                    // Re-apply gains to all channels
                    for (index in 0 until Utils.bandsCenter.count()) {
                        setBand(index, gainDBArray[index])
                    }
                }
            } finally {
                lock.unlock()
            }
        }
    }

    fun setEchoActive(active: Boolean) {
        this.echoActive = active
    }

    fun setVisualizationAudioActive(active: Boolean) {
        this.visualizationAudioActive = active
    }

    fun flatBand(): Boolean {
        lock.lock()
        try {
            changeDb = true
            val count = Utils.bandsCenter.count()
            for (index in 0 until count) {
                gainDBAbsArray[index] = 10.0.pow(0 / 20.0).toFloat()
                gainDBArray[index] = 0

                if (outputAudioFormat.sampleRate > 0) {
                    val centerFreq = Utils.bandsCenter[index]
                    val rate = outputAudioFormat.sampleRate.toFloat()

                    // Apply to all channels
                    for (chFilters in channelFilters) {
                        if (index < chFilters.size) {
                            chFilters[index].configure(
                                BiQuadraticFilter.PEAK,
                                centerFreq,
                                rate,
                                Q,
                                0f
                            )
                        }
                    }
                }
            }
            return true
        } finally {
            lock.unlock()
        }
    }

    fun setBand(index: Int, value: Int) {
        lock.lock()
        try {
            changeDb = true
            gainDBAbsArray[index] = 10.0.pow(value.toDouble() / 20.0).toFloat()
            gainDBArray[index] = value

            if (outputAudioFormat.sampleRate > 0) {
                val centerFreq = Utils.bandsCenter[index]
                val rate = outputAudioFormat.sampleRate.toFloat()

                // Apply to all channels
                for (chFilters in channelFilters) {
                    if (index < chFilters.size) {
                        chFilters[index].configure(
                            BiQuadraticFilter.PEAK,
                            centerFreq,
                            rate,
                            Q,
                            value.toFloat()
                        )
                    }
                }
            }
        } finally {
            lock.unlock()
        }
    }

    fun setDelayTime(value: Float) {
        delayTime = value
        for (delay in channelDelays) {
            delay.setEchoLength(delayTime)
        }
    }

    fun setDecay(value: Float) {
        if (value in 0.0..1.0) {
            this.decay = value
            for (delay in channelDelays) {
                delay.setDecay(value)
            }
        }
    }

    fun setFeedBack(value: Boolean) {
        this.echoFeedBack = value
        for (delay in channelDelays) {
            delay.isWithFeedBack = value
        }
    }

    fun setQ(value: Float, needChange: Boolean = true) {
        this.Q = value
        if (needChange) {
            for (i in 0 until Utils.bandsCenter.count()) {
                setBand(i, gainDBArray[i])
            }
        }
    }

    fun getBandLevels(): IntArray = gainDBArray.clone()
}