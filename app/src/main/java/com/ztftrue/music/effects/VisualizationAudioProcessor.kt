package com.ztftrue.music.effects

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
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
    var BYTES_PER_SAMPLE: Int = 2
    private var sampleBuffer: FloatArray = FloatArray(0)

    private var bufferSize = 512
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var dataBuffer: ByteBuffer = EMPTY_BUFFER
    private var ttfBuffer: ByteBuffer = EMPTY_BUFFER
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
        BYTES_PER_SAMPLE = getBytePerSample(outputAudioFormat!!.encoding)
        ttfBuffer = ByteBuffer.allocate(bufferSize * 8)
        val size = bufferSize / BYTES_PER_SAMPLE / outputAudioFormat!!.channelCount
        readSize = getOutputSize(outputAudioFormat!!, BYTES_PER_SAMPLE) / 2
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



        dataBuffer = ByteBuffer.allocate(readSize)
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
        if (active) {
            if (ttfBuffer.remaining() < 1 || ttfBuffer.remaining() < inputBuffer.limit()) {
                ttfBuffer =
                    SoundUtils.expandBuffer(
                        ttfBuffer.capacity() + inputBuffer.limit() * 2,
                        ttfBuffer
                    );
                Log.d(
                    "2${VisualizationAudioProcessor::class.simpleName}ExpandBuffer",
                    ttfBuffer.remaining().toString()
                )
            }
            ttfBuffer.put(inputBuffer.array())
        }

        if (dataBuffer.remaining() < 1 || dataBuffer.remaining() < inputBuffer.limit()) {
            dataBuffer =
                SoundUtils.expandBuffer(dataBuffer.capacity() + inputBuffer.limit() * 2, dataBuffer)
            Log.d(
                "1${VisualizationAudioProcessor::class.simpleName}ExpandBuffer",
                dataBuffer.remaining().toString()
            )
        }
        dataBuffer.put(inputBuffer)
    }


    override fun queueEndOfStream() {
        // TODO
        val processedBuffer = ByteBuffer.allocate(dataBuffer.limit())
        processedBuffer.put(dataBuffer)
        this.outputBuffer = processedBuffer
        dataBuffer.compact()
        this.outputBuffer = EMPTY_BUFFER
        inputEnded = true
    }


    override fun getOutput(): ByteBuffer {
        processData()
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
        val outputBuffer: ByteBuffer = this.outputBuffer
        this.outputBuffer = EMPTY_BUFFER
        outputBuffer.flip()
        return outputBuffer
    }

    private fun processData() {
        if (active) {
            if (ttfBuffer.position() >= readSize) {
                ttfBuffer.flip()
                if (ttfBuffer.hasRemaining()) {
                    val dataRemaining = ttfBuffer.remaining() // 获取 dataBuffer 中剩余的有效数据量
                    val readSize =
                        if (dataRemaining > readSize) readSize else dataRemaining // 确定实际读取量,TODO 正常不会发生，代码需要修改
                    val processedBuffer = ByteBuffer.allocate(readSize)
                    val oldLimit = ttfBuffer.limit() // 记录旧的 limit
                    ttfBuffer.limit(ttfBuffer.position() + readSize) // 设置新 limit 来控制读取量
                    processedBuffer.put(ttfBuffer)
                    ttfBuffer.limit(oldLimit)
                    CoroutineScope(Dispatchers.IO).launch {
                        val floatArray = FloatArray(bufferSize / BYTES_PER_SAMPLE)
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
                ttfBuffer.compact()
            }
        } else {
            ttfBuffer.clear()
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
        ttfBuffer.clear()
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