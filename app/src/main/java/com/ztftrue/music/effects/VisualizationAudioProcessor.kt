package com.ztftrue.music.effects

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import com.ztftrue.music.effects.SoundUtils.downsampleMagnitudes
import com.ztftrue.music.effects.SoundUtils.getBytePerSample
import com.ztftrue.music.effects.SoundUtils.getOutputSize
import com.ztftrue.music.play.EVENT_Visualization_Change
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder


@UnstableApi
class VisualizationAudioProcessor(private var mediaSession: MediaSessionCompat?) : AudioProcessor {

    private var active = false
    private var outputAudioFormat: AudioProcessor.AudioFormat? = null
    var r = 2
    var BYTES_PER_SAMPLE: Int = 2
    private var sampleBuffer: FloatArray = FloatArray(0)

    private var bufferSize = 2048
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var dataBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false
    private lateinit var converter: TarsosDSPAudioFloatConverter
    private var readSize = 2048
    var pcmToFrequencyDomain = PCMToFrequencyDomain(readSize, 44100f)

    init {
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    }

    fun setMediaSession(mediaSession: MediaSessionCompat) {
        this.mediaSession = mediaSession
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
        // ENCODING_PCM_16BIT, is two byte to one float
        r = if (outputAudioFormat!!.encoding == C.ENCODING_PCM_16BIT) 2 else 1
        dataBuffer = ByteBuffer.allocate(bufferSize * 8)
        val size = bufferSize / r / outputAudioFormat!!.channelCount
        sampleBuffer = FloatArray(size)
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

        BYTES_PER_SAMPLE = getBytePerSample(outputAudioFormat!!.encoding)
        readSize = getOutputSize(outputAudioFormat!!, BYTES_PER_SAMPLE) / 10
        pcmToFrequencyDomain =
            PCMToFrequencyDomain(
                readSize / (outputAudioFormat!!.channelCount * BYTES_PER_SAMPLE),
                outputAudioFormat!!.sampleRate.toFloat()
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
        this.outputBuffer = inputBuffer
        if (dataBuffer.remaining() < 1 || dataBuffer.remaining() < inputBuffer.limit()) {
            expandBuffer(dataBuffer.capacity() + inputBuffer.limit() * 2);
            Log.d("ExpandBuffer", dataBuffer.remaining().toString())
        }
        dataBuffer.put(inputBuffer.array())
    }

    private fun expandBuffer(newCapacity: Int) {
        val newBuffer = ByteBuffer.allocate(newCapacity)
        dataBuffer.flip() // 切换到读取模式
        newBuffer.put(dataBuffer) // 复制内容
        dataBuffer = newBuffer // 替换旧的 ByteBuffer
    }

    override fun queueEndOfStream() {
        // TODO
//        val processedBuffer = ByteBuffer.allocate(dataBuffer.limit())
//        processedBuffer.put(dataBuffer)
//        this.outputBuffer = processedBuffer
//        dataBuffer.compact()
        inputEnded = true
    }


    override fun getOutput(): ByteBuffer {
        processData()
        val outputBuffer: ByteBuffer = this.outputBuffer
//        this.outputBuffer = AudioProcessor.EMPTY_BUFFER
        return outputBuffer
    }

    private fun processData() {
        if (active) {
            if (dataBuffer.position() >= readSize) {
                dataBuffer.flip()
                if (dataBuffer.hasRemaining()) {
                    val dataRemaining = dataBuffer.remaining() // 获取 dataBuffer 中剩余的有效数据量
                    val readSize =
                        if (dataRemaining > readSize) readSize else dataRemaining // 确定实际读取量,TODO 正常不会发生，代码需要修改
                    val processedBuffer = ByteBuffer.allocate(readSize)
                    val oldLimit = dataBuffer.limit() // 记录旧的 limit
                    dataBuffer.limit(dataBuffer.position() + readSize) // 设置新 limit 来控制读取量
                    processedBuffer.put(dataBuffer)
                    dataBuffer.limit(oldLimit)
                    CoroutineScope(Dispatchers.IO).launch {
                        val floatArray = FloatArray(bufferSize / r)
                        converter.toFloatArray(processedBuffer.array(), floatArray)
                        var ind = 0
                        // TODO need support more channel count
                        for (i in floatArray.indices step outputAudioFormat!!.channelCount) {
                            sampleBuffer[ind] = (floatArray[i] + floatArray[i + 1]) / 2
                            ind += 1
                        }
                        val magnitude: FloatArray =
                            pcmToFrequencyDomain.process(sampleBuffer)
                        val m = downsampleMagnitudes(magnitude, 32)
                        val bundle = Bundle()
                        bundle.putInt("type", EVENT_Visualization_Change)
                        bundle.putFloatArray("magnitude", m)
                        mediaSession?.setExtras(bundle)
                    }
                }
                dataBuffer.compact()
            }
        }
    }

    override fun isEnded(): Boolean {
        return inputEnded && !this.outputBuffer.hasRemaining()
    }

    override fun flush() {
        if (outputBuffer != AudioProcessor.EMPTY_BUFFER) {
            outputBuffer.clear()
        }
        dataBuffer.clear()
        inputEnded = false
    }


    override fun reset() {
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputBuffer.clear()
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }

}