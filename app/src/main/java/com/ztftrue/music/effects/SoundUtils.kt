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
        if (magnitudes.isEmpty() || targetSize <= 0) {
            return FloatArray(maxOf(0, targetSize))
        }
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
            val start = startIdx.toInt().coerceIn(0, totalSize - 1)
            val end = maxOf(start + 1, endIdx.toInt().coerceIn(0, totalSize))

            var sum = 0f
            for (j in start until end) {
                sum += magnitudes[j]
            }
            val avg = if (end > start) sum / (end - start) else 0f
            val db = if (avg > 0) {
                // dB = 20 * log10(amplitude / ref)
                val rawDb = 20 * log10((avg / refValue).toDouble()).toFloat()
                if (rawDb < minDb) minDb else if (rawDb > 0f) 0f else rawDb
            } else {
                minDb
            }
            if (needNormalize) {
                // Normalize [minDb..0dB] to [0.0..1.0] where 1.0 is loudest (0dB) and 0.0 is silence (minDb)
                downsampled[i] = if (normalizationRange > 1e-6f) {
                    ((db - minDb) / normalizationRange).coerceIn(0f, 1f)
                } else {
                    0.0f
                }
            } else {
                downsampled[i] = if (needPositive) abs(db) else db
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
        val newBuffer = ByteBuffer.allocateDirect(newCapacity)
        oldBuffer.flip() // 切换到读取模式
        newBuffer.put(oldBuffer) // 复制内容
        return newBuffer
    }

}