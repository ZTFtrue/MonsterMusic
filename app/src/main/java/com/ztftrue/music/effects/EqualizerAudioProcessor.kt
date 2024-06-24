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
import kotlin.math.abs
import kotlin.math.max


/** Indicates that the output sample rate should be the same as the input.  */
private const val SAMPLE_RATE_NO_CHANGE = -1

@UnstableApi
class EqualizerAudioProcessor : AudioProcessor {

    private var pendingOutputSampleRate = 0
    private var active = false
    private var outputAudioFormat: AudioProcessor.AudioFormat? = null

    private lateinit var sampleBufferRealLeft: DoubleArray
    private lateinit var sampleBufferRealRight: DoubleArray

    private var bufferSize = 2048
    private var outputBuffer: ByteBuffer
    private lateinit var dataBuffer: ByteBuffer
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
        repeat(Utils.kThirdOct.count()) {
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
                setBand(index, biQuadraticFilter.gainDB.toInt())
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
            setBand(index, biQuadraticFilter.gainDB.toInt())
        }
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
        inputBuffer.order(ByteOrder.nativeOrder())
        dataBuffer.put(inputBuffer)
    }

    override fun getOutput(): ByteBuffer {
        processData()
        val outputBuffer: ByteBuffer = this.outputBuffer
        this.outputBuffer = EMPTY_BUFFER
        outputBuffer.flip()
        return outputBuffer
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

    // 获取浮点数组的最大绝对值
    private fun getMaxAbsValue(audioData: DoubleArray): Double {
        var maxVal = 0.0
        audioData.forEach { value ->
            maxVal = max(abs(value), maxVal)
        }
        return maxVal
    }

    // 归一化处理
    private fun normalize(audioData: DoubleArray, maxVal: Double) {
        for (i in audioData.indices) {
            audioData[i] /= maxVal
        }
    }

    private var changeDb = false
    private fun processData() {
        if (active) {
            lock.lock()
            if (changeDb) {
                for (i in 0 until 10) {
                    butterWorthRightBandPass[i].reset()
                    butterWorthLeftBandPass[i].reset()
                    mCoefficientLeftBiQuad[i].reset()
                    mCoefficientRightBiQuad[i].reset()
                }
                changeDb = false
            }
            if (dataBuffer.position() >= bufferSize) {
                // limit  设置为当前位置 (position) , position 设置为 0
                dataBuffer.flip()
                val processedBuffer = ByteBuffer.allocate(bufferSize)
                processedBuffer.put(dataBuffer.array(), 0, bufferSize)
                processedBuffer.flip()
                dataBuffer.position(bufferSize)
                dataBuffer.compact()
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
                                butterWorthLeftBandPass.forEachIndexed { index1, filter ->
                                    // only used for peaking and shelving filters
                                    sum += gainDBAbsArray[index1] * filter.filter(
                                        outY
                                    )
                                }
//                                mCoefficientLeftBandPass.forEachIndexed { index1, filter ->
//                                    // only used for peaking and shelving filters
//                                    sum += gainDBAbsArray[index1] * filter.filter(
//                                        outY
//                                    )
//                                }
                                outY = sum
//                                mCoefficientLeftBiQuad.forEach { filter ->
//                                    outY = filter.filter(
//                                        outY
//                                    )
//                                }
                                sampleBufferRealLeft[index] =
                                    (if (outY > 1.0) 1.0 else if (outY < -1.0) -1.0 else outY)
                            }
//                            val maxVal = getMaxAbsValue(sampleBufferRealLeft)
//                            if (maxVal > 1.0) {
//                                normalize(sampleBufferRealLeft, maxVal)
//                            }
                        },
                        async(Dispatchers.IO) {
                            sampleBufferRealRight.forEachIndexed { index, it ->
                                var outY: Double = it
                                var sum = 0.0
                                butterWorthRightBandPass.forEachIndexed { index1, filter ->
                                    // only used for peaking and shelving filters
                                    sum += gainDBAbsArray[index1] * filter.filter(
                                        outY
                                    )
                                }
//                                mCoefficientRightBandPass.forEachIndexed { index1, filter ->
//                                    // only used for peaking and shelving filters
//                                    sum += gainDBAbsArray[index1] * filter.filter(
//                                        outY
//                                    )
//                                }
                                outY = sum
//                                mCoefficientRightBiQuad.forEach { filter ->
//                                    outY = filter.filter(
//                                        outY
//                                    )
//                                }
                                sampleBufferRealRight[index] =
                                    (if (outY > 1.0) 1.0 else if (outY < -1.0) -1.0 else outY)
                            }
//                            val maxVal = getMaxAbsValue(sampleBufferRealRight)
//                            if (maxVal > 1.0f) {
//                                normalize(sampleBufferRealRight, maxVal)
//                            }
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
            lock.unlock()
        } else {
            dataBuffer.flip()
            val processedBuffer = ByteBuffer.allocate(dataBuffer.limit())
            val a = dataBuffer.array()
//            a.forEachIndexed{i,t->
//                a[i]= ((t)*0.6).toInt().toByte()
//            }
            processedBuffer.put(a, 0, dataBuffer.limit())
            dataBuffer.clear()
            processedBuffer.order(ByteOrder.nativeOrder())
            this.outputBuffer = processedBuffer
        }
    }

    override fun isEnded(): Boolean {
        return inputEnded && !this.outputBuffer.hasRemaining()
    }

    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        dataBuffer.clear()
        inputEnded = false
    }


    override fun reset() {
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingOutputSampleRate = SAMPLE_RATE_NO_CHANGE
        inputEnded = false
    }

    private val lock = ReentrantLock()

    fun setBand(index: Int, value: Int) {
        lock.lock()
        if (outputAudioFormat != null) {
            changeDb = true
            gainDBAbsArray[index] = FastMath.pow(10.0, (value.toDouble() / 40))
            gainDBArray[index] = value
            if (outputAudioFormat!!.sampleRate.toDouble() > 0) {
                butterWorthLeftBandPass[index].bandPass(
                    Utils.order,
                    outputAudioFormat!!.sampleRate.toDouble(),
                    Utils.kThirdOct[index],
                    Utils.kThirdBW[index]
                )
                butterWorthRightBandPass[index].bandPass(
                    Utils.order,
                    outputAudioFormat!!.sampleRate.toDouble(),
                    Utils.kThirdOct[index],
                    Utils.kThirdBW[index]
                )
                if (index == 0) {
                    // The first filter, includes all lower frequencies
                    mCoefficientLeftBiQuad[index].configure(
                        BiQuadraticFilter.LOWSHELF,
                        Utils.kThirdOct[index],
                        outputAudioFormat!!.sampleRate.toDouble(),
                        value.toDouble(),
                        Utils.qs[index],
                    )
                    mCoefficientRightBiQuad[index].configure(
                        BiQuadraticFilter.LOWSHELF,
                        Utils.kThirdOct[index],
                        outputAudioFormat!!.sampleRate.toDouble(),
                        value.toDouble(),
                        Utils.qs[index],
                    )
                } else if (index == mCoefficientRightBiQuad.size - 1) {
                    // The last filter, includes all higher frequencies
                    mCoefficientLeftBiQuad[index].configure(
                        BiQuadraticFilter.HIGHSHELF,
                        Utils.kThirdOct[index],
                        outputAudioFormat!!.sampleRate.toDouble(),
                        value.toDouble(),
                        Utils.qs[index],
                    )
                    mCoefficientRightBiQuad[index].configure(
                        BiQuadraticFilter.HIGHSHELF,
                        Utils.kThirdOct[index],
                        outputAudioFormat!!.sampleRate.toDouble(),
                        value.toDouble(),
                        Utils.qs[index],
                    )
                } else {
                    mCoefficientLeftBiQuad[index].configure(
                        BiQuadraticFilter.PEAK,
                        Utils.kThirdOct[index],
                        outputAudioFormat!!.sampleRate.toDouble(),
                        value.toDouble(),
                    )
                    mCoefficientRightBiQuad[index].configure(
                        BiQuadraticFilter.PEAK,
                        Utils.kThirdOct[index],
                        outputAudioFormat!!.sampleRate.toDouble(),
                        value.toDouble(),
                    )
                }
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