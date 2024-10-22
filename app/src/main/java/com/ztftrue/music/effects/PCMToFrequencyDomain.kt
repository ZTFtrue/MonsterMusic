package com.ztftrue.music.effects

import be.tarsos.dsp.util.fft.FFT


class PCMToFrequencyDomain(private val bufferSize: Int, private val sampleRate: Float) {
    private val fft: FFT = FFT(bufferSize)
    private val fftSize: Int = bufferSize / 2
    fun process(pcmData: FloatArray): FloatArray {
        val paddedData = pcmData.copyOf(bufferSize)
        fft.forwardTransform(paddedData)
        // Retrieve magnitudes
        val amplitudes = FloatArray(fftSize)
        fft.modulus(paddedData, amplitudes)
        // Optionally: Print frequencies and magnitudes
//        val r=FloatArray(fftSize)
//        for (i in amplitudes.indices) {
//            val frequency = fft.binToHz(i, sampleRate) // assuming 44100 Hz sample rate
//            r[i] = amplitudes[i]
////            println("Frequency: %.2f Hz, Magnitude: %.2f".format(frequency, amplitudes[i]))
//        }
//        Log.d("amplitudes", r.contentToString())
        return amplitudes
    }

}