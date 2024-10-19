package com.ztftrue.music.effects

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
import androidx.media3.common.util.UnstableApi
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.apache.commons.math3.util.FastMath
import java.nio.ByteBuffer
import java.nio.ByteOrder


@UnstableApi
class EchoAudioProcessor : AudioProcessor {
    private var active = false
    private var pendingOutputAudioFormat: AudioProcessor.AudioFormat? = null
    private var outputAudioFormat: AudioProcessor.AudioFormat? = null

    private val sampleBufferRealLeft: FloatArray
    private val sampleBufferRealRight: FloatArray

    private var outputBuffer: ByteBuffer
    private var inputEnded = false

    private val framesInBuffer = 4096
    private var bufferSize = 0
    private var dataBuffer: ByteBuffer
    private lateinit var converter: TarsosDSPAudioFloatConverter
    private var delayTime = 0.5f
    private var decay = 1.0f
    private var echoFeedBack = false

    init {
        outputBuffer = EMPTY_BUFFER
        bufferSize = framesInBuffer
        dataBuffer = ByteBuffer.allocate(bufferSize * 16)
        sampleBufferRealLeft = FloatArray(bufferSize / 4)
        sampleBufferRealRight = FloatArray(bufferSize / 4)
        pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    }

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
                    val floatArray =
                        FloatArray(bufferSize / pendingOutputAudioFormat!!.channelCount)
                    converter.toFloatArray(processedBuffer.array(), floatArray)
                    // TODO need support more channel count
                    for (i in 0 until bufferSize / 2) {
                        if (i % 2 == 0) {
                            sampleBufferRealLeft[i / 2] = floatArray[i]
                        } else {
                            sampleBufferRealRight[FastMath.floor((i / 2).toDouble()).toInt()] =
                                floatArray[i]
                        }
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
                    val outD = FloatArray(bufferSize / pendingOutputAudioFormat!!.channelCount)
                    var pI = 0
                    // TODO need support more channel count
                    for (i in 0 until sampleBufferRealLeft.size) {
                        outD[pI] = sampleBufferRealLeft[i]
                        outD[pI + 1] = sampleBufferRealRight[i]
                        pI = pI + 2
                    }
                    val outB = ByteArray(bufferSize)
                    converter.toByteArray(outD, outB)
                    val processedBuffer2 = ByteBuffer.wrap(outB)
                    processedBuffer2.position(bufferSize)
                    processedBuffer2.order(ByteOrder.nativeOrder())
                    this.outputBuffer = processedBuffer2
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