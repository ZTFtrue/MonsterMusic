package com.ztftrue.music.effects

// 移除协程相关引用，改用单线程
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import com.ztftrue.music.effects.SoundUtils.downsampleMagnitudes
import com.ztftrue.music.effects.SoundUtils.expandBuffer
import com.ztftrue.music.effects.SoundUtils.getBytePerSample
import com.ztftrue.music.play.AudioDataRepository
import com.ztftrue.music.utils.Utils
import org.apache.commons.math3.util.FastMath
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.absoluteValue

private const val SAMPLE_RATE_NO_CHANGE = -1

@UnstableApi
class EqualizerAudioProcessor : AudioProcessor {
    private var delayEffectLeft: DelayEffect = DelayEffect(0.0f, 1.0f, 44100.0f)
    private var delayEffectRight: DelayEffect = DelayEffect(0.0f, 1.0f, 44100.0f)
    private var pendingOutputSampleRate = 0
    private var equalizerActive = false
    private var echoActive = false
    private var visualizationAudioActive = false

    private var outputAudioFormat: AudioProcessor.AudioFormat? = null

    private var sampleBufferRealLeft: FloatArray = FloatArray(0)
    private var sampleBufferRealRight: FloatArray = FloatArray(0)

    private var bufferSize = 512
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var dataBuffer: ByteBuffer = EMPTY_BUFFER
    private var floatArray = FloatArray(bufferSize)
    private var inputEnded = false
    private val gainDBAbsArray: FloatArray =
        floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
    private val gainDBArray: IntArray = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    private val mCoefficientLeftBiQuad: ArrayList<BiQuadraticFilter> = arrayListOf()
    private val mCoefficientRightBiQuad: ArrayList<BiQuadraticFilter> = arrayListOf()
    private var delayTime = 0.5f
    private var decay = 1.0f
    private var echoFeedBack = false
    private lateinit var converter: TarsosDSPAudioFloatConverter
    private var pcmToFrequencyDomain = PCMToFrequencyDomain(bufferSize, 44100f)
    private var Q = Utils.Q

    // --- 状态变量 ---
    private var bytePerSample: Int = 2
    private var ind = 0
    private var leftEqualizerMax: Float = 1.0f
    private var rightEqualizerMax: Float = 1.0f
    private var leftEchoMax: Float = 1.0f
    private var rightEchoMax: Float = 1.0f
    private var changeDb = false
    private val lock = ReentrantLock()

    // 暂时保留您的实现，虽然 Queue 会有 GC，但我们先优先保证声音正常
    private var blockingQueue = LinkedBlockingQueue<Float>(bufferSize * 4)
    private var visualizationBuffer: FloatArray = FloatArray(0)

    private var outputContainer: ByteBuffer =
        ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())

    // 替代原来的 processedBuffer.array()
    private var reusableByteArray: ByteArray = ByteArray(0)

    init {
        outputBuffer = EMPTY_BUFFER
        repeat(Utils.bandsCenter.count()) {
            mCoefficientLeftBiQuad.add(BiQuadraticFilter())
            mCoefficientRightBiQuad.add(BiQuadraticFilter())
        }
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingOutputSampleRate = SAMPLE_RATE_NO_CHANGE
    }


    fun setEqualizerActive(active: Boolean) {
        lock.lock()
        if (this.equalizerActive != active) {
            mCoefficientRightBiQuad.forEachIndexed { index, _ ->
                setBand(index, gainDBArray[index])
            }
            this.equalizerActive = active
        }
        lock.unlock()
    }

    fun setEchoActive(active: Boolean) {
        if (this.echoActive != active) {
            this.echoActive = active
        }
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // TODO need support more encoding
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            return AudioProcessor.AudioFormat.NOT_SET
        }
        outputAudioFormat = inputAudioFormat
        // single channel
        bytePerSample = getBytePerSample(outputAudioFormat!!.encoding)
        val perSecond = Util.getPcmFrameSize(
            outputAudioFormat!!.encoding,
            outputAudioFormat!!.channelCount
        ) * outputAudioFormat!!.sampleRate
        bufferSize = perSecond
        dataBuffer = ByteBuffer.allocate(bufferSize * 8)

        // 2. 初始化输出复用容器 (预分配，防止 processData 里 allocate)
        // 给大一点，防止越界
        outputContainer = ByteBuffer.allocateDirect(bufferSize * 4).order(ByteOrder.nativeOrder())
        reusableByteArray = ByteArray(bufferSize * 4)

        // 3. 初始化数组
        floatArray = FloatArray(bufferSize)
        val size = bufferSize / (bytePerSample * outputAudioFormat!!.channelCount)
        sampleBufferRealLeft = FloatArray(size)
        sampleBufferRealRight = FloatArray(size)
        visualizationBuffer = FloatArray(size)

        pcmToFrequencyDomain = PCMToFrequencyDomain(
            size / 2,
            outputAudioFormat!!.sampleRate.toFloat()
        )
        blockingQueue.clear()
        blockingQueue = LinkedBlockingQueue(bufferSize * 4)
        // https://stackoverflow.com/questions/68776031/playing-a-wav-file-with-tarsosdsp-on-android
        val tarsosDSPAudioFormat = TarsosDSPAudioFormat(
            if (inputAudioFormat.sampleRate.toFloat() <= 0) {
                44100.0f
            } else {
                inputAudioFormat.sampleRate.toFloat()
            },
            16,  //based on the screenshot from Audacity, should this be 32?
            inputAudioFormat.channelCount,
            true,
            ByteOrder.BIG_ENDIAN == ByteOrder.nativeOrder()
        )
        converter =
            TarsosDSPAudioFloatConverter.getConverter(
                tarsosDSPAudioFormat
            )
        mCoefficientRightBiQuad.forEachIndexed { index, _ ->
            setBand(index, gainDBArray[index])
        }

        delayEffectLeft = DelayEffect(
            delayTime,
            decay,
            outputAudioFormat!!.sampleRate.toFloat()
        )
        delayEffectLeft.isWithFeedBack = echoFeedBack
        delayEffectRight = DelayEffect(
            delayTime,
            decay,
            outputAudioFormat!!.sampleRate.toFloat()
        )
        delayEffectRight.isWithFeedBack = echoFeedBack
        leftEqualizerMax = 1.0f
        rightEqualizerMax = 1.0f
        leftEchoMax = 1.0f
        rightEchoMax = 1.0f

        return outputAudioFormat!!

    }

    override fun isActive(): Boolean {
        return outputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
    }

    fun isSetActive(): Boolean = equalizerActive
    fun setVisualizationAudioActive(active: Boolean) {
        this.visualizationAudioActive = active
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        if (dataBuffer.remaining() < 1 || dataBuffer.remaining() < inputBuffer.limit()) {
            dataBuffer = expandBuffer(dataBuffer.capacity() + inputBuffer.limit() * 2, dataBuffer)
        }
        dataBuffer.put(inputBuffer)
    }

    override fun queueEndOfStream() {
        dataBuffer.flip()
        outputContainer.clear()
        if (outputContainer.capacity() < dataBuffer.limit()) {
            outputContainer =
                ByteBuffer.allocateDirect(dataBuffer.limit()).order(ByteOrder.nativeOrder())
        }
        outputContainer.put(dataBuffer)
        outputContainer.flip()
        this.outputBuffer = outputContainer
        dataBuffer.compact()
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        // 先处理数据，把 this.outputBuffer 填满
        processData()
        val outputBuffer = this.outputBuffer
        this.outputBuffer = EMPTY_BUFFER
        return outputBuffer
    }

    private fun processData() {
        if (equalizerActive or echoActive or visualizationAudioActive) {
            lock.lock()
            try {
                if (changeDb) {
                    leftEqualizerMax = 1.0f
                    rightEqualizerMax = 1.0f
                    for (i in 0 until 10) {
                        mCoefficientLeftBiQuad[i].reset()
                        mCoefficientRightBiQuad[i].reset()
                    }
                    changeDb = false
                }

                // 2. 只有当数据量 >= bufferSize 时才处理 (恢复原有逻辑！)
                if (dataBuffer.position() >= bufferSize) {
                    dataBuffer.flip()

                    if (dataBuffer.hasRemaining()) {
                        val dataRemaining = dataBuffer.remaining()
                        // 确定读取量，保持和原来一样
                        val readSize = if (dataRemaining > bufferSize) bufferSize else dataRemaining

                        // --- 修改点：复用内存，不再 allocate ---
                        if (outputContainer.capacity() < readSize) {
                            outputContainer = ByteBuffer.allocateDirect(readSize + 4096)
                                .order(ByteOrder.nativeOrder())
                        }
                        if (reusableByteArray.size < readSize) {
                            reusableByteArray = ByteArray(readSize + 4096)
                        }
                        outputContainer.clear()
                        // ------------------------------------

                        // 记录旧 limit，只读取 readSize
                        val oldLimit = dataBuffer.limit()
                        dataBuffer.limit(dataBuffer.position() + readSize)

                        // 从 dataBuffer 读取到 outputContainer
                        outputContainer.put(dataBuffer)
                        dataBuffer.limit(oldLimit) // 恢复 limit

                        // 准备 ByteArray 进行处理
                        outputContainer.flip()
                        outputContainer.get(reusableByteArray, 0, readSize)

                        // 转换 Float
                        converter.toFloatArray(
                            reusableByteArray,
                            floatArray,
                            bufferSize / bytePerSample
                        )

                        ind = 0
                        val halfLength = floatArray.size / 2

                        // 分离声道
                        for (i in 0 until halfLength step outputAudioFormat!!.channelCount) {
                            sampleBufferRealLeft[ind] = floatArray[i]
                            sampleBufferRealRight[ind] = floatArray[i + 1]
                            ind += 1
                        }

                        // --- DSP 处理 (改为单线程顺序执行，避免协程卡顿) ---
                        // 左声道
                        if (echoActive) {
                            // 保留您原有的逻辑
                            leftEchoMax = FastMath.max(
                                delayEffectLeft.process(sampleBufferRealLeft),
                                leftEchoMax.absoluteValue
                            )
                            if (leftEchoMax > 1.0) {
                                sampleBufferRealLeft.forEachIndexed { index, it ->
                                    sampleBufferRealLeft[index] = it / leftEchoMax
                                }
                            }
                            rightEchoMax = FastMath.max(
                                delayEffectRight.process(sampleBufferRealRight),
                                rightEchoMax.absoluteValue
                            )
                            if (rightEchoMax > 1.0) {
                                sampleBufferRealRight.forEachIndexed { index, it ->
                                    sampleBufferRealRight[index] = it / rightEchoMax
                                }
                            }
                        }
                        if (equalizerActive) {
                            sampleBufferRealLeft.forEachIndexed { index, it ->
                                var outY: Float = it
                                mCoefficientLeftBiQuad.forEach { filter ->
                                    outY = filter.filter(outY)
                                }
                                sampleBufferRealLeft[index] = outY
                            }
                            leftEqualizerMax = FastMath.max(
                                leftEqualizerMax,
                                Limiter.Limiter.process(sampleBufferRealLeft)
                            )
                            if (leftEqualizerMax > 1.0) {
                                sampleBufferRealLeft.forEachIndexed { index, it ->
                                    sampleBufferRealLeft[index] = it / leftEqualizerMax
                                }
                            }

                            sampleBufferRealRight.forEachIndexed { index, it ->
                                var outY: Float = it
                                mCoefficientRightBiQuad.forEach { filter ->
                                    outY = filter.filter(outY)
                                }
                                sampleBufferRealRight[index] = outY
                            }
                            rightEqualizerMax = FastMath.max(
                                rightEqualizerMax,
                                Limiter.Limiter.process(sampleBufferRealRight)
                            )
                            if (rightEqualizerMax > 1.0) {
                                sampleBufferRealRight.forEachIndexed { index, it ->
                                    sampleBufferRealRight[index] = it / rightEqualizerMax
                                }
                            }
                        }

                        if (visualizationAudioActive) {
                            sampleBufferRealLeft.forEachIndexed { index, it ->
                                blockingQueue.offer((it + sampleBufferRealRight[index]) / 2f)
                            }
                            if (blockingQueue.size >= visualizationBuffer.size) {
                                visualizationBuffer.forEachIndexed { index, _ ->
                                    visualizationBuffer[index] = blockingQueue.poll() ?: 0f
                                }
                                val magnitude: FloatArray =
                                    pcmToFrequencyDomain.process(visualizationBuffer)
                                val m = downsampleMagnitudes(
                                    magnitude, 32, -60f, needNormalize = false,
                                    needPositive = true
                                )
                                AudioDataRepository.postVisualizationData(m)
                            }
                        }

                        ind = 0
                        // 合并声道
                        for (i in 0 until halfLength step outputAudioFormat!!.channelCount) {
                            floatArray[i] = sampleBufferRealLeft[ind]
                            floatArray[i + 1] = sampleBufferRealRight[ind]
                            ind += 1
                        }

                        // 转回 Byte
                        converter.toByteArray(floatArray, halfLength, reusableByteArray)

                        // 写入输出容器
                        outputContainer.clear()
                        outputContainer.put(reusableByteArray, 0, readSize)

                        // 准备输出
                        // 注意：getOutput 会调用 flip，所以这里要处于写模式(position at end)吗？
                        // 看原代码：processedBuffer.position(bufferSize)，然后 outputBuffer.flip() 在 getOutput 里
                        // 所以这里我们需要模拟原代码的状态
                        outputContainer.flip() // 变为读模式

                        this.outputBuffer = outputContainer
                    }
                    dataBuffer.compact()
                }
            } finally {
                lock.unlock()
            }
        } else {
            dataBuffer.flip()
            if (dataBuffer.hasRemaining()) {
                val dataRemaining = dataBuffer.remaining()
                val readSize = if (dataRemaining > bufferSize) bufferSize else dataRemaining

                if (outputContainer.capacity() < readSize) {
                    outputContainer =
                        ByteBuffer.allocateDirect(readSize + 4096).order(ByteOrder.nativeOrder())
                }
                outputContainer.clear()

                val oldLimit = dataBuffer.limit()
                dataBuffer.limit(dataBuffer.position() + readSize)
                outputContainer.put(dataBuffer)
                dataBuffer.limit(oldLimit)
                outputContainer.flip()
                this.outputBuffer = outputContainer
            }
            dataBuffer.compact()
        }
    }

    override fun isEnded(): Boolean {
        return inputEnded && !this.outputBuffer.hasRemaining()
    }

    override fun flush() {
        lock.lock()
        outputBuffer = EMPTY_BUFFER
        outputContainer.clear() // 清理复用容器
        dataBuffer.clear()
        blockingQueue.clear()
        inputEnded = false
        lock.unlock()
    }

    override fun reset() {
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingOutputSampleRate = SAMPLE_RATE_NO_CHANGE
        outputBuffer = EMPTY_BUFFER
        outputContainer.clear()
        inputEnded = false
    }

    // ... (Bottom Setters: setQ, setBand, flatBand, etc. 保持不变) ...
    // ... 请把原文件底部的 setQ, setBand 等方法原样复制过来 ...
    // 为了节省篇幅，这里假设它们还在
    fun setQ(value: Float, needChange: Boolean = true) {
        this.Q = value
        if (needChange) {
            for (i in 0 until 10) {
                setBand(i, gainDBArray[i])
            }
        }
    }

    fun setBand(index: Int, value: Int) {
        lock.lock()
        if (outputAudioFormat != null) {
            changeDb = true
            gainDBAbsArray[index] = FastMath.pow(10.0, (value.toDouble() / 20.0)).toFloat()
            gainDBArray[index] = value
            if (outputAudioFormat!!.sampleRate.toFloat() > 0) {
                mCoefficientLeftBiQuad[index].configure(
                    BiQuadraticFilter.PEAK,
                    Utils.bandsCenter[index],
                    outputAudioFormat!!.sampleRate.toFloat(),
                    Q,
                    value.toFloat(),
                )
                mCoefficientRightBiQuad[index].configure(
                    BiQuadraticFilter.PEAK,
                    Utils.bandsCenter[index],
                    outputAudioFormat!!.sampleRate.toFloat(),
                    Q,
                    value.toFloat(),
                )
            }
        }
        lock.unlock()
    }

    fun flatBand(): Boolean {
        if (outputAudioFormat != null) {
            for (index in mCoefficientLeftBiQuad.indices) {
                setBand(index, 0)
            }
            return true
        } else {
            return false
        }
    }

    fun getBandLevels(): IntArray {
        val bandLevels = IntArray(gainDBAbsArray.size)
        gainDBArray.forEachIndexed { index: Int, biQuadraticFilter: Int ->
            bandLevels[index] = biQuadraticFilter
        }
        return bandLevels
    }

    fun setDaleyTime(value: Float) {
        delayTime = value
        delayEffectLeft.setEchoLength(delayTime)
        delayEffectRight.setEchoLength(delayTime)
    }

    fun setDecay(value: Float) {
        if (decay > 1.0 || decay < 0.0) {
            return
        }
        leftEchoMax = 1.0f
        rightEchoMax = 1.0f
        this.decay = value
        delayEffectLeft.setDecay(value)
        delayEffectRight.setDecay(value)
    }

    fun setFeedBack(value: Boolean) {
        leftEchoMax = 1.0f
        rightEchoMax = 1.0f
        echoFeedBack = value
        this.echoFeedBack = value
        delayEffectLeft.isWithFeedBack = value
        delayEffectRight.isWithFeedBack = value
    }
}