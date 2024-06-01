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
import uk.me.berndporr.iirj.Butterworth
import java.nio.ByteBuffer
import java.nio.ByteOrder


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

    private val mCoefficientLeft: ArrayList<BiQuadraticFilter> = arrayListOf()
    private val mCoefficientRight: ArrayList<BiQuadraticFilter> = arrayListOf()
    private val highPassFilterLeft = BiQuadraticFilter()
    private val lowPassFilterLeft = BiQuadraticFilter()
    private val highPassFilterRight = BiQuadraticFilter()
    private val lowPassFilterRight = BiQuadraticFilter()

    private val mCoefficientLeftBandPass: ArrayList<BiQuadraticFilter> = arrayListOf()
    private val mCoefficientRightBandPass: ArrayList<BiQuadraticFilter> = arrayListOf()
    private val butterWorthLeftBandPass: ArrayList<Butterworth> = arrayListOf()
    private val butterWorthRightBandPass: ArrayList<Butterworth> = arrayListOf()


    private lateinit var converter: TarsosDSPAudioFloatConverter

    init {
        outputBuffer = EMPTY_BUFFER
        repeat(Utils.kThirdOct.count()) {
            mCoefficientLeft.add(BiQuadraticFilter())
            mCoefficientRight.add(BiQuadraticFilter())
            mCoefficientLeftBandPass.add(BiQuadraticFilter())
            mCoefficientRightBandPass.add(BiQuadraticFilter())
            butterWorthLeftBandPass.add(Butterworth())
            butterWorthRightBandPass.add(Butterworth())
        }
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingOutputSampleRate = SAMPLE_RATE_NO_CHANGE
    }


    fun setActive(active: Boolean) {
        mCoefficientLeft.forEach {
            it.reset()
        }
        mCoefficientRight.forEach {
            it.reset()
        }
        mCoefficientLeftBandPass.forEach {
            it.reset()
        }
        mCoefficientRightBandPass.forEach {
            it.reset()
        }

        highPassFilterLeft.reset()
        lowPassFilterLeft.reset()
        highPassFilterRight.reset()
        lowPassFilterRight.reset()
        if (this.active != active) {
            this.active = active
        }
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
        mCoefficientLeftBandPass.forEachIndexed { index, biQuadraticFilter ->
            biQuadraticFilter.configure(
                BiQuadraticFilter.BANDPASS, Utils.kThirdOct[index],
                outputAudioFormat!!.sampleRate.toDouble(), 2.0, biQuadraticFilter.gainDB
            )
        }
        mCoefficientRightBandPass.forEachIndexed { index, biQuadraticFilter ->
            biQuadraticFilter.configure(
                BiQuadraticFilter.BANDPASS, Utils.kThirdOct[index],
                outputAudioFormat!!.sampleRate.toDouble(), 2.0, biQuadraticFilter.gainDB
            )
        }
        if (outputAudioFormat!!.sampleRate.toDouble() > 0) {
            butterWorthLeftBandPass.forEachIndexed { index, butter ->
                butter.highPass(0, outputAudioFormat!!.sampleRate.toDouble(), 100.0)
                butter.lowPass(1, outputAudioFormat!!.sampleRate.toDouble(), 10000.0)
                butter.bandPass(
                    2,
                    outputAudioFormat!!.sampleRate.toDouble(),
                    Utils.kThirdOct[index],
                    Utils.kThirdBW[index]
                )
            }
            butterWorthRightBandPass.forEachIndexed { index, butter ->
                butter.highPass(0, outputAudioFormat!!.sampleRate.toDouble(), 100.0)
                butter.lowPass(1, outputAudioFormat!!.sampleRate.toDouble(), 10000.0)
                butter.bandPass(
                    2,
                    outputAudioFormat!!.sampleRate.toDouble(),
                    Utils.kThirdOct[index],
                    Utils.kThirdBW[index]
                )

            }
        }

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
            10000.0,
            outputAudioFormat!!.sampleRate.toDouble(),
            0.707,
            0.0
        )
        lowPassFilterRight.configure(
            BiQuadraticFilter.LOWPASS,
            10000.0,
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
//                                outY = highPassFilterLeft.filter(outY)
//                                outY = lowPassFilterLeft.filter(outY)
//                                mCoefficientLeft.forEach { filter ->
//                                    outY = filter.filter(outY)
//                                }
                                var sum = 0.0
//                                mCoefficientLeftBandPass.forEach { filter ->
//                                    // only used for peaking and shelving filters
//                                    sum += filter.gain_abs * filter.filter(
//                                        outY
//                                    )
//                                }
                                butterWorthLeftBandPass.forEachIndexed { index1, filter ->
                                    // only used for peaking and shelving filters
                                    sum += mCoefficientLeftBandPass[index1].gain_abs * filter.filter(
                                        outY
                                    )
                                }
                                outY = sum
                                sampleBufferRealLeft[index] =
                                    (if (outY > 1.0) 1.0 else if (outY < -1.0) -1.0 else outY)
                            }
                        },
                        async(Dispatchers.IO) {
                            sampleBufferRealRight.forEachIndexed { index, it ->
                                var outY: Double = it
//                                outY = highPassFilterRight.filter(outY)
//                                outY = lowPassFilterRight.filter(outY)
//                                mCoefficientRight.forEach { filter ->
//                                    outY = filter.filter(outY)
//                                }
                                var sum = 0.0
//                                mCoefficientRightBandPass.forEach() { filter ->
//                                    // only used for peaking and shelving filters
//                                    sum += filter.gain_abs * filter.filter(
//                                        outY
//                                    )
//                                }
                                butterWorthRightBandPass.forEachIndexed { index1, filter ->
                                    // only used for peaking and shelving filters
                                    sum += mCoefficientLeftBandPass[index1].gain_abs * filter.filter(
                                        outY
                                    )
                                }
                                outY = sum
                                sampleBufferRealRight[index] =
                                    (if (outY > 1.0) 1.0 else if (outY < -1.0) -1.0 else outY)
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
        } else {
            dataBuffer.flip()
            val processedBuffer = ByteBuffer.allocate(dataBuffer.limit())
            processedBuffer.put(dataBuffer.array(), 0, dataBuffer.limit())
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
        highPassFilterLeft.reset()
        highPassFilterRight.reset()
        lowPassFilterRight.reset()
        lowPassFilterLeft.reset()
        mCoefficientLeft.forEach {
            it.reset()
        }
        mCoefficientRight.forEach {
            it.reset()
        }
        mCoefficientLeftBandPass.forEach {
            it.reset()
        }
        mCoefficientRightBandPass.forEach {
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
            mCoefficientLeftBandPass[index].configure(
                BiQuadraticFilter.BANDPASS, Utils.kThirdOct[index],
                outputAudioFormat!!.sampleRate.toDouble(), 2.0, value.toDouble()
            )
            mCoefficientRightBandPass[index].configure(
                BiQuadraticFilter.BANDPASS, Utils.kThirdOct[index],
                outputAudioFormat!!.sampleRate.toDouble(), 2.0, value.toDouble()
            )
            val s =
                if (outputAudioFormat!!.sampleRate.toDouble() > 0) outputAudioFormat!!.sampleRate.toDouble() else 441000.0
            butterWorthLeftBandPass[index].bandPass(
                2,
                s,
                Utils.kThirdOct[index],
                Utils.kThirdBW[index]
            )
            butterWorthRightBandPass[index].bandPass(
                2,
                s,
                Utils.kThirdOct[index],
                Utils.kThirdBW[index]
            )

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
                mCoefficientLeftBandPass[index].configure(
                    BiQuadraticFilter.BANDPASS, Utils.kThirdOct[index],
                    outputAudioFormat!!.sampleRate.toDouble(), 2.0, 0.0
                )
                mCoefficientRightBandPass[index].configure(
                    BiQuadraticFilter.BANDPASS, Utils.kThirdOct[index],
                    outputAudioFormat!!.sampleRate.toDouble(), 2.0, 0.0
                )
                val s =
                    if (outputAudioFormat!!.sampleRate.toDouble() > 0) outputAudioFormat!!.sampleRate.toDouble() else 441000.0
                butterWorthLeftBandPass[index].bandPass(
                    2,
                    s,
                    Utils.kThirdOct[index],
                    Utils.kThirdBW[index]
                )
                butterWorthRightBandPass[index].bandPass(
                    2,
                    s,
                    Utils.kThirdOct[index],
                    Utils.kThirdBW[index]
                )
            }

            return true
        } else {
            return false
        }
    }

    fun getBandLevels(): IntArray {
        val bandLevels = IntArray(mCoefficientLeftBandPass.size)
        mCoefficientLeftBandPass.forEachIndexed { index: Int, biQuadraticFilter: BiQuadraticFilter ->
            bandLevels[index] = biQuadraticFilter.gainDB.toInt()
        }
        return bandLevels
    }
}