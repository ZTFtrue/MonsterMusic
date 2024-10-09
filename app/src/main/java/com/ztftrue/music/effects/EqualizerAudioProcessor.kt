package com.ztftrue.music.effects

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
import androidx.media3.common.util.UnstableApi
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import com.ztftrue.music.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.apache.commons.math3.util.FastMath
import uk.me.berndporr.iirj.Butterworth
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.locks.ReentrantLock


/** Indicates that the output sample rate should be the same as the input.  */
private const val SAMPLE_RATE_NO_CHANGE = -1

@UnstableApi
class EqualizerAudioProcessor : AudioProcessor {

    private var pendingOutputSampleRate = 0
    private var active = false
    private var outputAudioFormat: AudioProcessor.AudioFormat? = null

    private var sampleBufferRealLeft: DoubleArray = DoubleArray(0)
    private var sampleBufferRealRight: DoubleArray = DoubleArray(0)

    private var bufferSize = 2048
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var dataBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false
    private val gainDBAbsArray: DoubleArray =
        doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    private val gainDBArray: IntArray = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    private val mCoefficientLeftBiQuad: ArrayList<BiQuadraticFilter> = arrayListOf()
    private val mCoefficientRightBiQuad: ArrayList<BiQuadraticFilter> = arrayListOf()
    private val butterWorthLeftBandPass: ArrayList<Butterworth> = arrayListOf()
    private val butterWorthRightBandPass: ArrayList<Butterworth> = arrayListOf()

    private lateinit var converter: TarsosDSPAudioFloatConverter

    init {
        outputBuffer = EMPTY_BUFFER
        repeat(Utils.bandsCenter.count()) {
            mCoefficientLeftBiQuad.add(BiQuadraticFilter())
            mCoefficientRightBiQuad.add(BiQuadraticFilter())
            butterWorthLeftBandPass.add(Butterworth())
            butterWorthRightBandPass.add(Butterworth())
        }
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingOutputSampleRate = SAMPLE_RATE_NO_CHANGE
    }


    fun setActive(active: Boolean) {
        lock.lock()
        if (this.active != active) {
            mCoefficientRightBiQuad.forEachIndexed { index, biQuadraticFilter ->
                setBand(index, gainDBArray[index])
            }
            this.active = active
        }
        lock.unlock()
    }

    var r = 2

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // TODO need support more encoding
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            return AudioProcessor.AudioFormat.NOT_SET
        }
        outputAudioFormat = inputAudioFormat
        // ENCODING_PCM_16BIT, is two byte to one float
        r = if (outputAudioFormat!!.encoding == C.ENCODING_PCM_16BIT) 2 else 1
        dataBuffer = ByteBuffer.allocate(bufferSize * 16)
        val size = bufferSize / r / outputAudioFormat!!.channelCount
        sampleBufferRealLeft = DoubleArray(size)
        sampleBufferRealRight = DoubleArray(size)
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
        mCoefficientRightBiQuad.forEachIndexed { index, biQuadraticFilter ->
            setBand(index, gainDBArray[index])
        }
        leftMax = 1.0
        rightMax = 1.0
        return outputAudioFormat!!

    }

    override fun isActive(): Boolean {
        return outputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
    }

    fun isSetActive(): Boolean {
        return active
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            return
        }
        dataBuffer.put(inputBuffer)
    }


    override fun queueEndOfStream() {
        // TODO
        dataBuffer.flip()
        val processedBuffer = ByteBuffer.allocate(dataBuffer.limit())
        processedBuffer.put(dataBuffer)
        this.outputBuffer = processedBuffer
        dataBuffer.compact()
        inputEnded = true
    }

    val BYTES_PER_SAMPLE: Int = 2

    private var leftMax = 1.0
    private var rightMax = 1.0
    fun getOutputSize(): Int {
        return outputAudioFormat!!.sampleRate * outputAudioFormat!!.channelCount * BYTES_PER_SAMPLE
    }

    private var changeDb = false
    override fun getOutput(): ByteBuffer {
        processData()
//        if (outputBuffer.position() == 0) {
//            return EMPTY_BUFFER;
//        }
        val outputBuffer: ByteBuffer = this.outputBuffer
        this.outputBuffer = EMPTY_BUFFER
        outputBuffer.flip()
        return outputBuffer
    }

    private fun processData() {
        if (active) {
            lock.lock()
            if (changeDb) {
                leftMax = 1.0
                rightMax = 1.0
                for (i in 0 until 10) {
                    butterWorthRightBandPass[i].reset()
                    butterWorthLeftBandPass[i].reset()
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
                    val floatArray = FloatArray(bufferSize / r)
                    converter.toFloatArray(processedBuffer.array(), floatArray)
                    var ind = 0
                    // TODO need support more channel count
                    for (i in floatArray.indices step outputAudioFormat!!.channelCount) {
                        sampleBufferRealLeft[ind] = floatArray[i].toDouble()
                        sampleBufferRealRight[ind] = floatArray[i + 1].toDouble()
                        ind += 1
                    }
                    runBlocking {
                        awaitAll(
                            async(Dispatchers.IO) {
                                // https://stackoverflow.com/questions/24003887/how-properly-implement-equalization-using-band-pass-filer
                                sampleBufferRealLeft.forEachIndexed { index, it ->
                                    var outY: Double = it
                                    var sum = 0.0
//                                butterWorthLeftBandPass.forEachIndexed { index1, filter ->
//                                    // only used for peaking and shelving filters
//                                    sum += gainDBAbsArray[index1] * filter.filter(
//                                        outY
//                                    )
//                                }
//                                mCoefficientLeftBiQuad.forEachIndexed { index1, filter ->
//                                    // only used for peaking and shelving filters
//                                    sum += gainDBAbsArray[index1] * filter.filter(
//                                        outY
//                                    )
//                                }
//                                outY = sum
                                    mCoefficientLeftBiQuad.forEach { filter ->
                                        outY = filter.filter(
                                            outY
                                        )
                                    }
                                    leftMax = FastMath.max(leftMax, FastMath.abs(outY))
                                    sampleBufferRealLeft[index] = outY
                                }
                                if (leftMax > 1.0) {
                                    sampleBufferRealLeft.forEachIndexed { index, it ->
                                        sampleBufferRealLeft[index] = it / leftMax
                                    }
                                }
//                            (if (outY > 1.0) 1.0 else if (outY < -1.0) -1.0 else outY)
                            },
                            async(Dispatchers.IO) {
                                sampleBufferRealRight.forEachIndexed { index, it ->
                                    var outY: Double = it
//                                var sum = 0.0
//                                butterWorthRightBandPass.forEachIndexed { index1, filter ->
//                                    // only used for peaking and shelving filters
//                                    sum += gainDBAbsArray[index1] * filter.filter(
//                                        outY
//                                    )
//                                }
//                                mCoefficientRightBiQuad.forEachIndexed { index1, filter ->
//                                    // only used for peaking and shelving filters
//                                    sum += gainDBAbsArray[index1] * filter.filter(
//                                        outY
//                                    )
//                                }
//                                outY = sum
                                    mCoefficientRightBiQuad.forEach { filter ->
                                        outY = filter.filter(
                                            outY
                                        )
                                    }
                                    rightMax = FastMath.max(rightMax, FastMath.abs(outY))
                                    sampleBufferRealRight[index] = outY
//                                    (if (outY > 1.0) 1.0 else if (outY < -1.0) -1.0 else outY)
                                }
                                if (rightMax > 1.0)
                                    sampleBufferRealRight.forEachIndexed { index, it ->
                                        sampleBufferRealRight[index] = it / rightMax
                                    }
                            }
                        )
                    }
                    val outDoubleArray = FloatArray(sampleBufferRealLeft.size * 2)
                    var pI = 0
                    // TODO need support more channel count
                    for (i in floatArray.indices step outputAudioFormat!!.channelCount) {
                        outDoubleArray[i] = sampleBufferRealLeft[pI].toFloat()
                        outDoubleArray[i + 1] = sampleBufferRealRight[pI].toFloat()
                        pI += 1
                    }
                    val outB = ByteArray(bufferSize)
                    converter.toByteArray(outDoubleArray, outB)
                    val processedResultBuffer = ByteBuffer.wrap(outB)
                    processedResultBuffer.position(bufferSize)
                    processedResultBuffer.order(ByteOrder.nativeOrder())
                    this.outputBuffer = processedResultBuffer
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
        if (outputBuffer != EMPTY_BUFFER) {
            outputBuffer.clear()
        }
        dataBuffer.clear()

        inputEnded = false
    }


    override fun reset() {
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingOutputSampleRate = SAMPLE_RATE_NO_CHANGE
        outputBuffer.clear()
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
    }

    private val lock = ReentrantLock()
    fun setBand(index: Int, value: Int) {
        lock.lock()
        if (outputAudioFormat != null) {
            changeDb = true
            gainDBAbsArray[index] = FastMath.pow(10.0, (value.toDouble() / 20))
            gainDBArray[index] = value
            if (outputAudioFormat!!.sampleRate.toDouble() > 0) {
                butterWorthLeftBandPass[index].bandPass(
                    Utils.order,
                    outputAudioFormat!!.sampleRate.toDouble(),
                    Utils.bandsCenter[index],
                    Utils.kThirdBW[index]
                )
                butterWorthRightBandPass[index].bandPass(
                    Utils.order,
                    outputAudioFormat!!.sampleRate.toDouble(),
                    Utils.bandsCenter[index],
                    Utils.kThirdBW[index]
                )
                mCoefficientLeftBiQuad[index].configure(
                    BiQuadraticFilter.PEAK,
                    Utils.bandsCenter[index],
                    outputAudioFormat!!.sampleRate.toDouble(),
                    Utils.qs[index],
                    value.toDouble(),
                )
                mCoefficientRightBiQuad[index].configure(
                    BiQuadraticFilter.PEAK,
                    Utils.bandsCenter[index],
                    outputAudioFormat!!.sampleRate.toDouble(),
                    Utils.qs[index],
                    value.toDouble(),
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
}