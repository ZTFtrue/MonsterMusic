package com.ztftrue.music.effects;

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

object SoundUtils {

    fun downsampleMagnitudes(magnitudes: FloatArray, targetSize: Int = 12): FloatArray {

            val downsampled = FloatArray(targetSize)
            val totalSize = magnitudes.size

            for (i in downsampled.indices) {
                // Exponential scaling
                val startIdx = Math.pow((i.toDouble() / targetSize), 2.0) * totalSize
                val endIdx = Math.pow(((i + 1).toDouble() / targetSize), 2.0) * totalSize

                // Convert to valid index
                val start = startIdx.toInt().coerceAtLeast(0).coerceAtMost(totalSize - 1)
                val end = endIdx.toInt().coerceAtLeast(0).coerceAtMost(totalSize - 1)

                // Compute average magnitude over this range
                var sum = 0f
                for (j in start until end) {
                    sum += magnitudes[j]
                }

                downsampled[i] = if (end > start) sum / (end - start) else 0f
            }
            return downsampled
    }

    @OptIn(UnstableApi::class)
    fun getOutputSize(outputAudioFormat: AudioProcessor.AudioFormat,BYTES_PER_SAMPLE:Int): Int {
        return outputAudioFormat.sampleRate * outputAudioFormat.channelCount * BYTES_PER_SAMPLE
    }

    @OptIn(UnstableApi::class)
    fun getBytePerSample(bitDepth: Int): Int {
        when (bitDepth) {
            Format.NO_VALUE -> {
                return 1;
            }

            C.ENCODING_INVALID -> {
                return 1;
            }

            C.ENCODING_PCM_8BIT -> {
                return 1;
            }

            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_16BIT_BIG_ENDIAN -> {
                return 2;
            }

            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_24BIT_BIG_ENDIAN -> {
                return 3;
            }

            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_32BIT_BIG_ENDIAN -> {
                return 4;
            }

            C.ENCODING_PCM_FLOAT -> {
                return 4;
            }
        }
        return 1;
    }

    fun adjustFrequencies(magnitudes: FloatArray, sampleRate: Int, fftSize: Int): FloatArray {
        val adjustedMagnitudes = mutableListOf<Float>()
        val totalBands = 32 // 例如，将可视化划分为 32 段
        val lowFreqCutoff = 200 // 200 Hz 以下为低频
        val highFreqCutoff = 2000 // 2000 Hz 以上为高频

        val bandStepLow = (lowFreqCutoff / sampleRate.toFloat()) * fftSize
        val bandStepHigh = (highFreqCutoff / sampleRate.toFloat()) * fftSize

        // 低频分段 (更细致)
        for (i in 0 until bandStepLow.toInt()) {
            adjustedMagnitudes.add(magnitudes[i])
        }

        // 高频合并 (将多个频段合并)
        var i = bandStepLow.toInt()
        while (i < magnitudes.size) {
            var sum = 0f
            var count = 0
            for (j in 0 until bandStepHigh.toInt()) {
                if (i + j < magnitudes.size) {
                    sum += magnitudes[i + j]
                    count++
                }
            }
            adjustedMagnitudes.add(sum / count) // 合并
            i += bandStepHigh.toInt()
        }

        return adjustedMagnitudes.toFloatArray()
    }
      fun expandBuffer(newCapacity: Int, oldBuffer: ByteBuffer): ByteBuffer {
        val newBuffer = ByteBuffer.allocate(newCapacity)
        oldBuffer.flip() // 切换到读取模式
        newBuffer.put(oldBuffer) // 复制内容
        return newBuffer
    }

}