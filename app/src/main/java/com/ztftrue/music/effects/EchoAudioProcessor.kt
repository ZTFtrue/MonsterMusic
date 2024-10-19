package com.ztftrue.music.effects

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
import androidx.media3.common.util.UnstableApi
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import com.ztftrue.music.effects.SoundUtils.getBytePerSample
import com.ztftrue.music.effects.SoundUtils.getOutputSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.nio.ByteOrder


@UnstableApi
class EchoAudioProcessor : AudioProcessor {
    private var active = false
    private var pendingOutputAudioFormat: AudioProcessor.AudioFormat? =
        AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat: AudioProcessor.AudioFormat? = AudioProcessor.AudioFormat.NOT_SET

    private var sampleBufferRealLeft: FloatArray = FloatArray(0)
    private var sampleBufferRealRight: FloatArray = FloatArray(0)

    private var bufferSize = 512
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var dataBuffer: ByteBuffer = EMPTY_BUFFER
    private var floatArray = FloatArray(bufferSize)

    private var inputEnded = false
    var BYTES_PER_SAMPLE: Int = 2

    private lateinit var converter: TarsosDSPAudioFloatConverter
    private var delayTime = 0.5f
    private var decay = 1.0f
    private var echoFeedBack = false

    fun setActive(active: Boolean) {
        if (this.active != active) {
            this.active = active
        }
    }

    private var delayEffectLeft: DelayEffect? = null
    private var delayEffectRight: DelayEffect? = null

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // TODO need support more encoding
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            return AudioProcessor.AudioFormat.NOT_SET
        }
        pendingOutputAudioFormat = inputAudioFormat
        // https://stackoverflow.com/questions/68776031/playing-a-wav-file-with-tarsosdsp-on-android
        val tarsosDSPAudioFormat = TarsosDSPAudioFormat(
            inputAudioFormat.sampleRate.toFloat(),
            16,  //based on the screenshot from Audacity, should this be 32?
            inputAudioFormat.channelCount,
            true,
            ByteOrder.BIG_ENDIAN == ByteOrder.nativeOrder()
        )
        outputAudioFormat = inputAudioFormat
        BYTES_PER_SAMPLE = getBytePerSample(outputAudioFormat!!.encoding)
        bufferSize = getOutputSize(outputAudioFormat!!, BYTES_PER_SAMPLE)
        dataBuffer = ByteBuffer.allocate(bufferSize * 8)
        floatArray = FloatArray(bufferSize)
        val size = bufferSize / BYTES_PER_SAMPLE / outputAudioFormat!!.channelCount
        sampleBufferRealLeft = FloatArray(size)
        sampleBufferRealRight = FloatArray(size)
        delayEffectLeft = DelayEffect(
            delayTime.toDouble(),
            decay,
            pendingOutputAudioFormat!!.sampleRate.toDouble()
        )
        delayEffectLeft?.isWithFeedBack = echoFeedBack
        delayEffectRight = DelayEffect(
            delayTime.toDouble(),
            decay,
            pendingOutputAudioFormat!!.sampleRate.toDouble()
        )
        delayEffectRight?.isWithFeedBack = echoFeedBack
        converter =
            TarsosDSPAudioFloatConverter.getConverter(
                tarsosDSPAudioFormat
            )
        return pendingOutputAudioFormat!!

    }

    override fun isActive(): Boolean {
        return pendingOutputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            return
        }
        inputBuffer.order(ByteOrder.nativeOrder())
        if (dataBuffer.remaining() < 1 || dataBuffer.remaining() < inputBuffer.limit()) {
            expandBuffer(dataBuffer.capacity() + inputBuffer.limit() * 2);
            Log.d("ExpandBuffer1", dataBuffer.remaining().toString())
        }

        dataBuffer.put(inputBuffer)
    }

    private fun expandBuffer(newCapacity: Int) {
        val newBuffer = ByteBuffer.allocate(newCapacity)
        dataBuffer.flip() // 切换到读取模式
        newBuffer.put(dataBuffer) // 复制内容
        dataBuffer = newBuffer // 替换旧的 ByteBuffer
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

    private var ind = 0
    private fun processData() {
        if (active) {
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
                        bufferSize / BYTES_PER_SAMPLE
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
                                delayEffectLeft?.process(sampleBufferRealLeft)
                            },
                            async(Dispatchers.IO) {
                                delayEffectRight?.process(sampleBufferRealRight)
                            }
                        )
                    }
                    ind = 0
                    // TODO need support more channel count
                    for (i in 0 until halfLength step outputAudioFormat!!.channelCount) {
                        floatArray[i] = sampleBufferRealLeft[ind].toFloat()
                        floatArray[i + 1] = sampleBufferRealRight[ind].toFloat()
                        ind += 1
                    }
                    converter.toByteArray(floatArray, halfLength, byteArray)
                    processedBuffer.clear()
                    processedBuffer.put(byteArray)
                    processedBuffer.position(bufferSize)
                    processedBuffer.order(ByteOrder.nativeOrder())
                    this.outputBuffer = processedBuffer
                }
                dataBuffer.compact()
            }
        } else {
            dataBuffer.flip()
            dataBuffer.position(0)
            val processedBuffer: ByteBuffer = ByteBuffer.allocate(dataBuffer.limit())
            processedBuffer.put(dataBuffer)
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
        pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        inputEnded = false
    }

    /**
     * Set the delay time in milliseconds
     */
    fun setDaleyTime(value: Float) {
        delayTime = value
        delayEffectLeft?.setEchoLength(delayTime.toDouble())
        delayEffectRight?.setEchoLength(delayTime.toDouble())
    }

    fun setDecay(value: Float) {
        if (decay > 1.0 || decay < 0.0) {
            return
        }
        this.decay = value
        delayEffectLeft?.setDecay(value)
        delayEffectRight?.setDecay(value)
    }

    fun setFeedBack(value: Boolean) {
        echoFeedBack = value
        this.echoFeedBack = value
        delayEffectLeft?.isWithFeedBack = value
        delayEffectRight?.isWithFeedBack = value
    }


    fun getDelayTime(): Float {
        return delayTime
    }


}