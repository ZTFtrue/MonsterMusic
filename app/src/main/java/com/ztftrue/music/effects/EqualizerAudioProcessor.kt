package com.ztftrue.music.effects

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
import androidx.media3.common.util.UnstableApi
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.qFactors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.apache.commons.math3.util.FastMath
import java.nio.ByteBuffer
import java.nio.ByteOrder


/** Indicates that the output sample rate should be the same as the input.  */
private const val SAMPLE_RATE_NO_CHANGE = -1

@UnstableApi
class EqualizerAudioProcessor : AudioProcessor {

    private var pendingOutputSampleRate = 0
    private var active = false
    private var outputAudioFormat: AudioProcessor.AudioFormat? = null

    private val sampleBufferRealLeft: DoubleArray
    private val sampleBufferRealRight: DoubleArray

    private var outputBuffer: ByteBuffer
    private var bufferSize = 4096
    private val dataBuffer: ByteBuffer
    private var inputEnded = false

    private val mCoefficientLeft: ArrayList<BiQuadraticFilter> = arrayListOf()
    private val mCoefficientRight: ArrayList<BiQuadraticFilter> = arrayListOf()
    private val gainFilterLeft = BiQuadraticFilter()
    private val highPassFilterLeft = BiQuadraticFilter()
    private val lowPassFilterLeft = BiQuadraticFilter()
    private val gainFilterRight = BiQuadraticFilter()
    private val highPassFilterRight = BiQuadraticFilter()
    private val lowPassFilterRight = BiQuadraticFilter()
    private lateinit var converter: TarsosDSPAudioFloatConverter

    init {
        outputBuffer = EMPTY_BUFFER
        repeat(Utils.kThirdOct.count()) {
            mCoefficientLeft.add(BiQuadraticFilter())
            mCoefficientRight.add(BiQuadraticFilter())
        }
        dataBuffer = ByteBuffer.allocate(bufferSize * 8)
        sampleBufferRealLeft = DoubleArray(bufferSize / 4)
        sampleBufferRealRight = DoubleArray(bufferSize / 4)
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingOutputSampleRate = SAMPLE_RATE_NO_CHANGE
    }


    fun setActive(active: Boolean) {
        if (this.active != active) {
            this.active = active
        }
    }


    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // TODO need support more encoding
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            return AudioProcessor.AudioFormat.NOT_SET
        }
        outputAudioFormat = inputAudioFormat
        // https://stackoverflow.com/questions/68776031/playing-a-wav-file-with-tarsosdsp-on-android
        val tarsosDSPAudioFormat = TarsosDSPAudioFormat(
            inputAudioFormat.sampleRate.toFloat(),
            16,  //based on the screenshot from Audacity, should this be 32?
            inputAudioFormat.channelCount,
            true,
            ByteOrder.BIG_ENDIAN == ByteOrder.nativeOrder()
        )
        converter =
            TarsosDSPAudioFloatConverter.getConverter(
                tarsosDSPAudioFormat
            )
        mCoefficientLeft.forEachIndexed { index, filter ->
            filter.configure(
                BiQuadraticFilter.PEAK, Utils.kThirdOct[index],
                outputAudioFormat!!.sampleRate.toDouble(), qFactors[index], filter.gainDB
            )
            filter.reset()
        }
        mCoefficientRight.forEachIndexed { index, filter ->
            filter.configure(
                BiQuadraticFilter.PEAK, Utils.kThirdOct[index],
                outputAudioFormat!!.sampleRate.toDouble(), qFactors[index], filter.gainDB
            )
            filter.reset()
        }

        gainFilterLeft.configureBw(
            BiQuadraticFilter.Gain,
            BiQuadraticFilter.BIND_TYPE.BW,
            0.0,
            outputAudioFormat!!.sampleRate.toDouble(),
            0.0,
            -9.0
        )
        gainFilterRight.configureBw(
            BiQuadraticFilter.Gain,
            BiQuadraticFilter.BIND_TYPE.BW,
            0.0,
            outputAudioFormat!!.sampleRate.toDouble(),
            0.0,
            -9.0
        )
        highPassFilterLeft.configure(
            BiQuadraticFilter.HIGHPASS,
            100.0,
            outputAudioFormat!!.sampleRate.toDouble(),
            0.707,
            0.0
        )
        highPassFilterRight.configure(
            BiQuadraticFilter.HIGHPASS,
            100.0,
            outputAudioFormat!!.sampleRate.toDouble(),
            0.707,
            0.0
        )
        lowPassFilterLeft.configure(
            BiQuadraticFilter.LOWPASS,
            5000.0,
            outputAudioFormat!!.sampleRate.toDouble(),
            0.707,
            0.0
        )
        lowPassFilterRight.configure(
            BiQuadraticFilter.LOWPASS,
            5000.0,
            outputAudioFormat!!.sampleRate.toDouble(),
            0.707,
            0.0
        )
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

    private fun processData() {
        if (active) {
            if (dataBuffer.position() >= bufferSize) {
                // limit  设置为当前位置 (position) , position 设置为 0
                dataBuffer.flip()
                val processedBuffer = ByteBuffer.allocate(bufferSize)
                processedBuffer.put(dataBuffer.array(), 0, bufferSize)
                processedBuffer.flip()
                dataBuffer.position(bufferSize)
                dataBuffer.compact()
                val floatArray = FloatArray(bufferSize / outputAudioFormat!!.channelCount)
                converter.toFloatArray(processedBuffer.array(), floatArray)
                // TODO need support more channel count
                for (i in 0 until bufferSize / 2) {
                    if (i % 2 == 0) {
                        sampleBufferRealLeft[i / 2] = floatArray[i].toDouble()*0.6
                    } else {
                        sampleBufferRealRight[FastMath.floor((i / 2).toDouble()).toInt()] =
                            floatArray[i].toDouble()*0.6
                    }
                }
                runBlocking {
                    awaitAll(
                        async(Dispatchers.IO) {
                            sampleBufferRealLeft.forEachIndexed { index, it ->
                                var outY: Double = it
//                                outY = gainFilterLeft.filter(outY)
                                outY = highPassFilterLeft.filter(outY)
                                outY = lowPassFilterLeft.filter(outY)
                                mCoefficientLeft.forEach { filter ->
                                    outY = filter.filter(outY)
                                }
                                sampleBufferRealLeft[index] =
                                    (if (outY > 1.0) 1.0 else if (outY < -1.0) -1.0 else outY)
                            }
                        },
                        async(Dispatchers.IO) {
                            sampleBufferRealRight.forEachIndexed { index, it ->
                                var outY: Double = it
//                                outY = gainFilterRight.filter(outY)
                                outY = highPassFilterRight.filter(outY)
                                outY = lowPassFilterRight.filter(outY)
                                mCoefficientRight.forEach { filter ->
                                    outY = filter.filter(outY)
                                }
                                sampleBufferRealRight[index] =
                                    (if (outY > 1.0) 1.0 else if (outY < -1.0) -1.0 else outY)
                            }
                        }
                    )
                }
                val outD = FloatArray(bufferSize / outputAudioFormat!!.channelCount)
                var pI = 0
                // TODO need support more channel count
                for (i in 0 until sampleBufferRealLeft.size) {
                    outD[pI] = sampleBufferRealLeft[i].toFloat()
                    outD[pI + 1] = sampleBufferRealRight[i].toFloat()
                    pI = pI + 2
                }
                val outB = ByteArray(bufferSize)
                converter.toByteArray(outD, outB)
                val processedBuffer2 = ByteBuffer.wrap(outB)
                processedBuffer2.position(bufferSize)
                processedBuffer2.order(ByteOrder.nativeOrder())
                this.outputBuffer = processedBuffer2
            }
        } else {
            dataBuffer.flip()
            val processedBuffer = ByteBuffer.allocate(dataBuffer.limit())
            val a = dataBuffer.array()
            val floatArray = FloatArray(a.size / outputAudioFormat!!.channelCount)
            converter.toFloatArray(a, floatArray)
            for (i in floatArray.indices) {
                floatArray[i] = floatArray[i] * 0.6f
            }
            val outB = ByteArray(a.size)
            converter.toByteArray(floatArray, outB)
            processedBuffer.put(outB, 0, dataBuffer.limit())
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
        gainFilterLeft.reset()
        highPassFilterLeft.reset()
        gainFilterRight.reset()
        highPassFilterRight.reset()
        lowPassFilterRight.reset()
        lowPassFilterLeft.reset()
        mCoefficientLeft.forEach {
            it.reset()
        }
        mCoefficientRight.forEach {
            it.reset()
        }
        inputEnded = false
    }


    override fun reset() {
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingOutputSampleRate = SAMPLE_RATE_NO_CHANGE
        inputEnded = false
    }


    fun setBand(index: Int, value: Int) {
        if (outputAudioFormat != null) {
            val filterLeft = mCoefficientLeft[index]
            val filterRight = mCoefficientRight[index]
            filterLeft.configure(
                BiQuadraticFilter.PEAK, Utils.kThirdOct[index],
                outputAudioFormat!!.sampleRate.toDouble(), qFactors[index], value.toDouble()
            )
            filterRight.configure(
                BiQuadraticFilter.PEAK, Utils.kThirdOct[index],
                outputAudioFormat!!.sampleRate.toDouble(), qFactors[index], value.toDouble()
            )
            filterLeft.reset()
            filterRight.reset()
        }
    }

    fun flatBand(): Boolean {
        if (outputAudioFormat != null) {
            for (index in mCoefficientLeft.indices) {
                val filterLeft = mCoefficientLeft[index]
                val filterRight = mCoefficientRight[index]
                filterLeft.configure(
                    BiQuadraticFilter.PEAK, Utils.kThirdOct[index],
                    outputAudioFormat!!.sampleRate.toDouble(), qFactors[index], 0.0
                )
                filterRight.configure(
                    BiQuadraticFilter.PEAK, Utils.kThirdOct[index],
                    outputAudioFormat!!.sampleRate.toDouble(), qFactors[index], 0.0
                )
                filterLeft.reset()
                filterRight.reset()
            }
            return true
        } else {
            return false
        }
    }

    fun getBandLevels(): IntArray {
        val bandLevels = IntArray(mCoefficientLeft.size)
        mCoefficientLeft.forEachIndexed { index: Int, biQuadraticFilter: BiQuadraticFilter ->
            bandLevels[index] = biQuadraticFilter.gainDB.toInt()
        }
        return bandLevels
    }
}