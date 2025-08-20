package com.ztftrue.music.effects

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import org.apache.commons.math3.util.FastMath
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.log10

object SoundUtils {
    fun downsampleMagnitudes(
        magnitudes: FloatArray, targetSize: Int = 12, minDb: Float = -60f,
        needNormalize: Boolean = true,
        needPositive: Boolean = true
    ): FloatArray {
        // 定义一个参考值，防止 log(0) 出现。同时它也定义了0dB的位置。
        val refValue = 1.0f
        val downsampled = FloatArray(targetSize)
        val totalSize = magnitudes.size
        val normalizationRange = abs(minDb)
        // The short answer is: Decibels are negative
        // because they represent how much quieter
        // a sound is compared to the LOUDEST POSSIBLE sound.
        for (i in downsampled.indices) {
            // Exponential scaling
            val startIdx = FastMath.pow((i.toDouble() / targetSize), 2.0) * totalSize
            val endIdx = FastMath.pow(((i + 1).toDouble() / targetSize), 2.0) * totalSize

            // Convert to valid index
            val start = startIdx.toInt().coerceAtLeast(0).coerceAtMost(totalSize - 1)
            val end = endIdx.toInt().coerceAtLeast(0).coerceAtMost(totalSize - 1)

            var sum = 0f
            for (j in start until end) {
                sum += magnitudes[j]
            }
            val avg = if (end > start) sum / (end - start) else 0f
            downsampled[i] = if (avg > 0) {
                //  dB = 20 * log10(amplitude / ref)
                val db = 20 * log10((avg / refValue).toDouble()).toFloat()
                val dbResult = if (db < minDb) minDb else db
                if (needPositive) abs(dbResult) else dbResult
            } else {
                if (needPositive) abs(minDb) else minDb
            }
            if (needNormalize) {
                if (normalizationRange > 1e-6f) {
                    downsampled[i] = (downsampled[i] - minDb) / normalizationRange
                } else {
                    downsampled[i] = 0.0f
                }
            }
        }

        return downsampled
    }

    @OptIn(UnstableApi::class)
    fun getOutputSize(outputAudioFormat: AudioProcessor.AudioFormat, bytePerSample: Int): Int {
        return outputAudioFormat.sampleRate * outputAudioFormat.channelCount * bytePerSample
    }

    @OptIn(UnstableApi::class)
    fun getBytePerSample(bitDepth: Int): Int {
        when (bitDepth) {
            Format.NO_VALUE -> {
                return 1
            }

            C.ENCODING_INVALID -> {
                return 1
            }

            C.ENCODING_PCM_8BIT -> {
                return 1
            }

            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_16BIT_BIG_ENDIAN -> {
                return 2
            }

            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_24BIT_BIG_ENDIAN -> {
                return 3
            }

            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_32BIT_BIG_ENDIAN -> {
                return 4
            }

            C.ENCODING_PCM_FLOAT -> {
                return 4
            }
        }
        return 1
    }

    fun expandBuffer(newCapacity: Int, oldBuffer: ByteBuffer): ByteBuffer {
        val newBuffer = ByteBuffer.allocate(newCapacity)
        oldBuffer.flip() // 切换到读取模式
        newBuffer.put(oldBuffer) // 复制内容
        return newBuffer
    }

}