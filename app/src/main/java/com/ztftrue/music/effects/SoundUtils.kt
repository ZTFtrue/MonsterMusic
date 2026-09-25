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
        magnitudes: FloatArray,
        targetSize: Int = 12,
        minDb: Float = -60f,
        needNormalize: Boolean = true,
        needPositive: Boolean = true,
        refValue: Float = 1.0f,
        tiltFactor: Float = 0f
    ): FloatArray {
        if (magnitudes.isEmpty() || targetSize <= 0) {
            return FloatArray(maxOf(0, targetSize))
        }
        val downsampled = FloatArray(targetSize)
        val totalSize = magnitudes.size
        val normalizationRange = abs(minDb)

        // Logarithmic frequency distribution covering musical spectrum (sub-bass ~25Hz to Nyquist)
        val minRatio = 0.0015
        val maxRatio = 1.0
        val logRatio = FastMath.log(maxRatio / minRatio)

        for (i in downsampled.indices) {
            val startFrac = if (i == 0) 0.0 else minRatio * FastMath.exp(logRatio * (i.toDouble() / targetSize))
            val endFrac = if (i == targetSize - 1) 1.0 else minRatio * FastMath.exp(logRatio * ((i + 1).toDouble() / targetSize))

            val startIdx = (startFrac * totalSize).toInt()
            val endIdx = (endFrac * totalSize).toInt()

            // Ensure distinct non-empty bin ranges: early bass bands get distinct bins
            val start = maxOf(minOf(i, totalSize - 1), startIdx.coerceIn(0, totalSize - 1))
            val end = maxOf(start + 1, endIdx.coerceIn(0, totalSize))

            var sum = 0f
            var maxVal = 0f
            for (j in start until end) {
                val v = magnitudes[j]
                sum += v
                if (v > maxVal) maxVal = v
            }
            val count = end - start
            val avg = if (count > 0) sum / count else 0f
            // Combine average and peak in wider bands to avoid drowning sharp instrument tones
            val bandVal = if (count > 1) maxOf(avg, maxVal * 0.75f) else avg

            // Optional acoustic tilt (compensates for natural pink noise rolloff in music)
            val tilt = if (tiltFactor > 0f && targetSize > 1) {
                1.0f + (i.toFloat() / (targetSize - 1)) * tiltFactor
            } else {
                1.0f
            }
            val effectiveVal = bandVal * tilt

            val db = if (effectiveVal > 0) {
                // dB = 20 * log10(amplitude / ref)
                val rawDb = 20 * log10((effectiveVal / refValue).toDouble()).toFloat()
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