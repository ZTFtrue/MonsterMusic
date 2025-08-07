package com.ztftrue.music.effects

import android.os.Bundle
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaBrowser
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import com.ztftrue.music.effects.SoundUtils.downsampleMagnitudes
import com.ztftrue.music.effects.SoundUtils.expandBuffer
import com.ztftrue.music.effects.SoundUtils.getBytePerSample
import com.ztftrue.music.play.AudioDataRepository
import com.ztftrue.music.play.PlayService.Companion.COMMAND_VISUALIZATION_DATA
import com.ztftrue.music.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.math3.util.FastMath
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.absoluteValue


/** Indicates that the output sample rate should be the same as the input.  */
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
        floatArray = FloatArray(bufferSize)
        val size = bufferSize / (bytePerSample * outputAudioFormat!!.channelCount)
        sampleBufferRealLeft = FloatArray(size)
        sampleBufferRealRight = FloatArray(size)
        visualizationBuffer = FloatArray(size)
        visualizationArrayList.clear()
        pcmToFrequencyDomain =
            PCMToFrequencyDomain(
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

    private var bytePerSample: Int = 2
    private var ind = 0
    private var leftEqualizerMax: Float = 1.0f
    private var rightEqualizerMax: Float = 1.0f

    private var leftEchoMax: Float = 1.0f
    private var rightEchoMax: Float = 1.0f
    override fun isActive(): Boolean {
        return outputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
    }

    fun isSetActive(): Boolean {
        return equalizerActive
    }

    fun setVisualizationAudioActive(active: Boolean) {
        this.visualizationAudioActive = active
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            return
        }
        if (dataBuffer.remaining() < 1 || dataBuffer.remaining() < inputBuffer.limit()) {
            dataBuffer = expandBuffer(dataBuffer.capacity() + inputBuffer.limit() * 2, dataBuffer)
        }
        dataBuffer.put(inputBuffer)
    }

    //    private fun expandBuffer(newCapacity: Int) {
//        val newBuffer = ByteBuffer.allocate(newCapacity)
//        dataBuffer.flip() // 切换到读取模式
//        newBuffer.put(dataBuffer) // 复制内容
//        dataBuffer = newBuffer // 替换旧的 ByteBuffer
//    }
    override fun queueEndOfStream() {
        // TODO
        dataBuffer.flip()
        val processedBuffer = ByteBuffer.allocate(dataBuffer.limit())
        processedBuffer.put(dataBuffer)
        this.outputBuffer = processedBuffer
        dataBuffer.compact()
        inputEnded = true
    }

    private var mediaSession: MediaBrowser? = null

    fun setMediaSession(mediaSession: MediaBrowser) {
        this.mediaSession = mediaSession
    }

    private var changeDb = false
    override fun getOutput(): ByteBuffer {
        processData()
        val outputBuffer: ByteBuffer = this.outputBuffer
        this.outputBuffer = EMPTY_BUFFER
        outputBuffer.flip()
        return outputBuffer
    }

    private var blockingQueue = LinkedBlockingQueue<Float>(bufferSize * 4)
    private var visualizationArrayList = LinkedList<Float>() // 用于存储Data
    private var visualizationBuffer: FloatArray = FloatArray(0)

    private fun processData() {
        if (equalizerActive or echoActive or visualizationAudioActive) {
            lock.lock()
            if (changeDb) {
                leftEqualizerMax = 1.0f
                rightEqualizerMax = 1.0f
                for (i in 0 until 10) {
                    mCoefficientLeftBiQuad[i].reset()
                    mCoefficientRightBiQuad[i].reset()
                }
                changeDb = false
            }
            if (dataBuffer.position() >= bufferSize) {
                dataBuffer.flip()
                if (dataBuffer.hasRemaining()) {
                    val dataRemaining = dataBuffer.remaining() // 获取 dataBuffer 中剩余的有效数据量
                    val readSize =
                        if (dataRemaining > bufferSize) bufferSize else dataRemaining // 确定实际读取量
                    val processedBuffer = ByteBuffer.allocate(readSize)
                    val oldLimit = dataBuffer.limit() // 记录旧的 limit
                    dataBuffer.limit(dataBuffer.position() + readSize) // 设置新 limit 来控制读取量
                    processedBuffer.put(dataBuffer)
                    dataBuffer.limit(oldLimit)
                    val byteArray = processedBuffer.array()
                    converter.toFloatArray(
                        byteArray,
                        floatArray,
                        bufferSize / bytePerSample
                    )
                    ind = 0
                    val halfLength = floatArray.size / 2
                    // TODO need support more channel count
                    for (i in 0 until halfLength step outputAudioFormat!!.channelCount) {
                        sampleBufferRealLeft[ind] = floatArray[i]
                        sampleBufferRealRight[ind] = floatArray[i + 1]
                        ind += 1
                    }
                    runBlocking {
                        awaitAll(
                            async(Dispatchers.IO) {
                                if (echoActive) {
                                    leftEchoMax = FastMath.max(
                                        delayEffectLeft.process(sampleBufferRealLeft),
                                        leftEchoMax.absoluteValue
                                    )
                                    if (leftEchoMax > 1.0) {
                                        sampleBufferRealLeft.forEachIndexed { index, it ->
                                            sampleBufferRealLeft[index] = it / leftEchoMax
                                        }
                                    }
                                }
                                if (equalizerActive) {
                                    sampleBufferRealLeft.forEachIndexed { index, it ->
                                        var outY: Float = it
                                        mCoefficientLeftBiQuad.forEach { filter ->
                                            outY = filter.filter(
                                                outY
                                            )
                                        }
                                        sampleBufferRealLeft[index] = outY
                                    }
                                    leftEqualizerMax =
                                        FastMath.max(
                                            leftEqualizerMax,
                                            Limiter.Limiter.process(sampleBufferRealLeft)
                                        )
                                    if (leftEqualizerMax > 1.0) {
                                        sampleBufferRealLeft.forEachIndexed { index, it ->
                                            sampleBufferRealLeft[index] = it / leftEqualizerMax
                                        }
                                    }
                                }
                            },
                            async(Dispatchers.IO) {
                                if (echoActive) {
                                    rightEchoMax =
                                        FastMath.max(
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
                                    sampleBufferRealRight.forEachIndexed { index, it ->
                                        var outY: Float = it
                                        mCoefficientRightBiQuad.forEach { filter ->
                                            outY = filter.filter(
                                                outY
                                            )
                                        }
                                        sampleBufferRealRight[index] = outY
                                    }
                                    rightEqualizerMax =
                                        FastMath.max(
                                            rightEqualizerMax,
                                            Limiter.Limiter.process(sampleBufferRealRight)
                                        )
                                    if (rightEqualizerMax > 1.0) {
                                        sampleBufferRealRight.forEachIndexed { index, it ->
                                            sampleBufferRealRight[index] =
                                                it / rightEqualizerMax
                                        }
                                    }
                                }
                            }
                        )
                    }
                    if (visualizationAudioActive) {
                        sampleBufferRealLeft.forEachIndexed { index, it ->
                            blockingQueue.offer((it + sampleBufferRealRight[index]) / 2f)
                        }
                        if (blockingQueue.size >= visualizationBuffer.size) {
                            visualizationBuffer.forEachIndexed { index, _ ->
                                visualizationBuffer[index] = blockingQueue.poll() ?: 0f
                            }
                            CoroutineScope(Dispatchers.IO).launch {
                                val magnitude: FloatArray =
                                    pcmToFrequencyDomain.process(visualizationBuffer)
                                val m = downsampleMagnitudes(magnitude, 32)
//                                val bundle = Bundle()
//                                bundle.putFloatArray("magnitude", m)
                                AudioDataRepository.postVisualizationData(m)
//                                mediaSession?.sendCustomCommand(COMMAND_VISUALIZATION_DATA,bundle)
                            }
                        }
                    }
                    ind = 0
                    // TODO need support more channel count
                    for (i in 0 until halfLength step outputAudioFormat!!.channelCount) {
                        floatArray[i] = sampleBufferRealLeft[ind]
                        floatArray[i + 1] = sampleBufferRealRight[ind]
                        ind += 1
                    }
                    converter.toByteArray(floatArray, halfLength, byteArray)
                    processedBuffer.position(bufferSize)
                    processedBuffer.order(ByteOrder.nativeOrder())
                    this.outputBuffer = processedBuffer
                }
                dataBuffer.compact()
            }
            lock.unlock()
        } else {
            dataBuffer.flip()
            if (dataBuffer.hasRemaining()) {
                val dataRemaining = dataBuffer.remaining() // 获取 dataBuffer 中剩余的有效数据量
                val readSize =
                    if (dataRemaining > bufferSize) bufferSize else dataRemaining // 确定实际读取量
                val processedBuffer: ByteBuffer = ByteBuffer.allocate(readSize)
                val oldLimit = dataBuffer.limit() // 记录旧的 limit
                dataBuffer.limit(dataBuffer.position() + readSize) // 设置新 limit 来控制读取量
                processedBuffer.put(dataBuffer)
                dataBuffer.limit(oldLimit)
                processedBuffer.order(ByteOrder.nativeOrder())
                this.outputBuffer = processedBuffer
            }
            dataBuffer.compact()
        }
    }

    override fun isEnded(): Boolean {
        return inputEnded && !this.outputBuffer.hasRemaining()
    }

    override fun flush() {
        lock.lock()
//        if (outputBuffer != EMPTY_BUFFER) {
//            outputBuffer.clear()
//        }
        outputBuffer = EMPTY_BUFFER
        dataBuffer.clear()
        blockingQueue.clear()
        inputEnded = false
        lock.unlock()
    }


    override fun reset() {
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingOutputSampleRate = SAMPLE_RATE_NO_CHANGE
//        outputBuffer.clear()
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
    }

    private val lock = ReentrantLock()
    fun setQ(value: Float, needChange: Boolean = true) {
        this.Q = value
        if (needChange) {
            for (i in 0 until 10) {
                Log.d("CHANGE_Q", gainDBArray[i].toString()+","+Q)
                setBand(
                    i,
                    gainDBArray[i]
                )
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

    /**
     * Set the delay time in milliseconds
     */
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