package com.ztftrue.music.effects

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Optimized FFT implementation for Audio Visualization.
 * Uses Lookup Tables (LUT) for Windowing, Trigonometry, and Bit-reversal.
 */
class PCMToFrequencyDomain(private val bufferSize: Int, private val sampleRate: Float) {

    private val fftSize: Int = bufferSize
    private val halfSize: Int = bufferSize / 2

    // Processing Buffers
    private val real: FloatArray = FloatArray(bufferSize)
    private val imag: FloatArray = FloatArray(bufferSize)

    // Output Buffer
    private val amplitudes: FloatArray = FloatArray(halfSize)

    // Lookup Tables
    private val window: FloatArray = FloatArray(bufferSize)
    private val sinTable: FloatArray
    private val cosTable: FloatArray
    private val reverseTable: IntArray

    init {
        // 1. Pre-calculate Hann Window
        // w[n] = 0.5 * (1 - cos(2*PI*n / (N-1)))
        for (i in 0 until bufferSize) {
            window[i] = (0.5 * (1.0 - cos(2.0 * PI * i / (bufferSize - 1)))).toFloat()
        }

        // 2. Pre-calculate Bit-Reversal Table
        // This avoids calculating bit reversals in the hot loop
        reverseTable = IntArray(bufferSize)
        val levels = (Math.log(bufferSize.toDouble()) / Math.log(2.0)).toInt()
        for (i in 0 until bufferSize) {
            var rev = 0
            var temp = i
            for (j in 0 until levels) {
                rev = (rev shl 1) or (temp and 1)
                temp = temp shr 1
            }
            reverseTable[i] = rev
        }

        // 3. Pre-calculate Trigonometry (Twiddle Factors)
        // We need sin/cos for each stage.
        // We can pre-calculate the standard twiddle factors.
        // However, a simple LUT for the specific angles used in loops is tricky because the stride changes.
        // A common optimization is to pre-calc a large sin/cos table, but here we will use a specific structure
        // compatible with the specific loops below for max speed.
        // Actually, for a fixed size FFT, we can cache the specific W_N^k values.
        // But to keep code readable and reasonably sized, we will pre-calculate the main W array.
        // Size is bufferSize / 2 because k goes from 0 to N/2
        sinTable = FloatArray(halfSize)
        cosTable = FloatArray(halfSize)

        for (i in 0 until halfSize) {
            val angle = -2.0 * PI * i / bufferSize
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }
    }

    /**
     * Processes PCM data into Frequency Magnitudes.
     * @param pcmData Input float array (Real audio data).
     * @return FloatArray containing magnitudes (Size = bufferSize / 2).
     */
    fun process(pcmData: FloatArray): FloatArray {
        // 1. Prepare Data: Copy, Window, Zero Imaginary
        // We use Math.min to handle cases where input might be smaller than bufferSize
        val len = minOf(pcmData.size, bufferSize)

        for (i in 0 until len) {
            real[i] = pcmData[i] * window[i]
            imag[i] = 0f
        }
        // Zero pad if input was short
        if (len < bufferSize) {
            for (i in len until bufferSize) {
                real[i] = 0f
                imag[i] = 0f
            }
        }

        // 2. Perform FFT
        performFFT()

        // 3. Calculate Magnitudes (Modulus)
        // Only first half (Nyquist) is needed
        for (i in 0 until halfSize) {
            val r = real[i]
            val iVal = imag[i]
            amplitudes[i] = sqrt(r * r + iVal * iVal)
        }

        return amplitudes
    }

    private fun performFFT() {
        // A. Bit-Reverse Permutation using LUT
        for (i in 0 until bufferSize) {
            val j = reverseTable[i]
            if (j > i) {
                // Swap Real
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR
                // Swap Imag
                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
        }

        // B. Cooley-Tukey Butterfly Operations
        var size = 2
        while (size <= bufferSize) {
            val halfsize = size / 2
            val tabStep = bufferSize / size

            for (i in 0 until bufferSize step size) {
                var k = 0
                for (j in i until i + halfsize) {
                    // Fetch pre-calculated sin/cos
                    // The index logic maps the current butterfly stage to our LUT
                    val l = k * tabStep
                    val c = cosTable[l]
                    val s = sinTable[l]

                    val jPlusHalf = j + halfsize
                    val tReal = real[jPlusHalf] * c - imag[jPlusHalf] * s
                    val tImag = real[jPlusHalf] * s + imag[jPlusHalf] * c

                    real[jPlusHalf] = real[j] - tReal
                    imag[jPlusHalf] = imag[j] - tImag
                    real[j] += tReal
                    imag[j] += tImag

                    k++
                }
            }
            size *= 2
        }
    }
}