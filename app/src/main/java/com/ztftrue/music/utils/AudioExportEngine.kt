package com.ztftrue.music.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.effects.EqualizerAudioProcessor
import com.ztftrue.music.sqlData.model.Auxr
import com.ztftrue.music.sqlData.model.MusicItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
object AudioExportEngine {

    private const val TAG = "AudioExportEngine"

    enum class ExportFormat {
        WAV,
        M4A
    }

    fun sanitizeFilename(name: String): String {
        val sanitized = name.replace(Regex("""[\\\\/:*?"<>|]"""), "_").trim()
        val withoutUnderscores = sanitized.replace("_", "").trim()
        return if (sanitized.isEmpty() || withoutUnderscores.isEmpty()) "audio_processed" else sanitized
    }

    fun isRawPcmMime(mime: String?): Boolean {
        if (mime == null) return false
        return mime.equals("audio/raw", ignoreCase = true) ||
                mime.equals("audio/wav", ignoreCase = true) ||
                mime.equals("audio/x-wav", ignoreCase = true)
    }

    /**
     * Interface for writing processed PCM buffers into destination format.
     */
    interface AudioWriter : AutoCloseable {
        fun write(pcmBuffer: ByteBuffer)
        fun finish()
    }

    /**
     * Lossless 16-bit PCM WAV writer with 44-byte RIFF header.
     */
    class WavWriter(
        private val outputFile: File,
        private val sampleRate: Int,
        private val channelCount: Int,
        private val bitsPerSample: Int = 16
    ) : AudioWriter {
        private val raf = RandomAccessFile(outputFile, "rw")
        private var totalPcmBytes = 0L

        init {
            // Reserve 44 bytes for WAV header
            raf.write(ByteArray(44))
        }

        override fun write(pcmBuffer: ByteBuffer) {
            val remaining = pcmBuffer.remaining()
            if (remaining <= 0) return

            if (pcmBuffer.hasArray()) {
                val array = pcmBuffer.array()
                val offset = pcmBuffer.arrayOffset() + pcmBuffer.position()
                raf.write(array, offset, remaining)
                pcmBuffer.position(pcmBuffer.position() + remaining)
            } else {
                val chunk = ByteArray(minOf(remaining, 16384))
                while (pcmBuffer.hasRemaining()) {
                    val toRead = minOf(pcmBuffer.remaining(), chunk.size)
                    pcmBuffer.get(chunk, 0, toRead)
                    raf.write(chunk, 0, toRead)
                }
            }
            totalPcmBytes += remaining
        }

        override fun finish() {
            raf.seek(0)
            val header = createWavHeader(sampleRate, channelCount, bitsPerSample, totalPcmBytes)
            raf.write(header)
        }

        override fun close() {
            try {
                raf.close()
            } catch (_: Throwable) {}
        }

        companion object {
            fun createWavHeader(
                sampleRate: Int,
                channels: Int,
                bitsPerSample: Int,
                dataLength: Long
            ): ByteArray {
                val totalDataLen = dataLength + 36
                val byteRate = (sampleRate * channels * bitsPerSample) / 8
                val blockAlign = (channels * bitsPerSample) / 8

                val header = ByteArray(44)
                // RIFF descriptor
                header[0] = 'R'.code.toByte()
                header[1] = 'I'.code.toByte()
                header[2] = 'F'.code.toByte()
                header[3] = 'F'.code.toByte()
                header[4] = (totalDataLen and 0xff).toByte()
                header[5] = ((totalDataLen shr 8) and 0xff).toByte()
                header[6] = ((totalDataLen shr 16) and 0xff).toByte()
                header[7] = ((totalDataLen shr 24) and 0xff).toByte()
                header[8] = 'W'.code.toByte()
                header[9] = 'A'.code.toByte()
                header[10] = 'V'.code.toByte()
                header[11] = 'E'.code.toByte()

                // "fmt " sub-chunk
                header[12] = 'f'.code.toByte()
                header[13] = 'm'.code.toByte()
                header[14] = 't'.code.toByte()
                header[15] = ' '.code.toByte()
                header[16] = 16 // Subchunk1Size = 16 for PCM
                header[17] = 0
                header[18] = 0
                header[19] = 0
                header[20] = 1 // AudioFormat = 1 (Linear PCM)
                header[21] = 0
                header[22] = (channels and 0xff).toByte()
                header[23] = ((channels shr 8) and 0xff).toByte()
                header[24] = (sampleRate and 0xff).toByte()
                header[25] = ((sampleRate shr 8) and 0xff).toByte()
                header[26] = ((sampleRate shr 16) and 0xff).toByte()
                header[27] = ((sampleRate shr 24) and 0xff).toByte()
                header[28] = (byteRate and 0xff).toByte()
                header[29] = ((byteRate shr 8) and 0xff).toByte()
                header[30] = ((byteRate shr 16) and 0xff).toByte()
                header[31] = ((byteRate shr 24) and 0xff).toByte()
                header[32] = (blockAlign and 0xff).toByte()
                header[33] = ((blockAlign shr 8) and 0xff).toByte()
                header[34] = (bitsPerSample and 0xff).toByte()
                header[35] = ((bitsPerSample shr 8) and 0xff).toByte()

                // "data" sub-chunk
                header[36] = 'd'.code.toByte()
                header[37] = 'a'.code.toByte()
                header[38] = 't'.code.toByte()
                header[39] = 'a'.code.toByte()
                header[40] = (dataLength and 0xff).toByte()
                header[41] = ((dataLength shr 8) and 0xff).toByte()
                header[42] = ((dataLength shr 16) and 0xff).toByte()
                header[43] = ((dataLength shr 24) and 0xff).toByte()

                return header
            }
        }
    }

    /**
     * AAC / M4A writer using MediaCodec AAC LC encoder and MediaMuxer.
     */
    class AacWriter(
        outputFile: File,
        private val sampleRate: Int,
        private val channelCount: Int,
        bitrate: Int = 256000
    ) : AudioWriter {
        private val encoder: MediaCodec
        private val muxer: MediaMuxer
        private var trackIndex = -1
        private var muxerStarted = false
        private var presentationTimeUs = 0L
        private val bufferInfo = MediaCodec.BufferInfo()
        private val timeoutUs = 5000L

        init {
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount)
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount)
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }

        override fun write(pcmBuffer: ByteBuffer) {
            val bytesPerFrame = maxOf(1, channelCount * 2)
            while (pcmBuffer.hasRemaining()) {
                val inIndex = encoder.dequeueInputBuffer(timeoutUs)
                if (inIndex >= 0) {
                    val inputBuffer = encoder.getInputBuffer(inIndex) ?: continue
                    inputBuffer.clear()
                    val bytesToCopy = minOf(pcmBuffer.remaining(), inputBuffer.capacity())
                    val frameAlignedBytes = (bytesToCopy / bytesPerFrame) * bytesPerFrame
                    if (frameAlignedBytes == 0) break
                    val originalLimit = pcmBuffer.limit()
                    pcmBuffer.limit(pcmBuffer.position() + frameAlignedBytes)
                    inputBuffer.put(pcmBuffer)
                    pcmBuffer.limit(originalLimit)

                    val sampleCount = frameAlignedBytes / bytesPerFrame
                    val bufferDurationUs = if (sampleRate > 0) (sampleCount * 1_000_000L) / sampleRate else 0L
                    encoder.queueInputBuffer(inIndex, 0, frameAlignedBytes, presentationTimeUs, 0)
                    presentationTimeUs += bufferDurationUs
                }
                drainEncoder(false)
            }
        }

        private fun drainEncoder(endOfStream: Boolean) {
            var retryCount = 0
            while (true) {
                val outIndex = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        val newFormat = encoder.outputFormat
                        trackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                } else if (outIndex >= 0) {
                    retryCount = 0
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    val encodedData = encoder.getOutputBuffer(outIndex)
                    if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) {
                        break
                    } else {
                        retryCount++
                        if (retryCount > 30) {
                            break
                        }
                    }
                }
            }
        }

        override fun finish() {
            var queued = false
            var retryCount = 0
            while (!queued && retryCount < 20) {
                val inIndex = encoder.dequeueInputBuffer(timeoutUs)
                if (inIndex >= 0) {
                    encoder.queueInputBuffer(inIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    queued = true
                } else {
                    retryCount++
                    drainEncoder(false)
                }
            }
            drainEncoder(true)
        }

        override fun close() {
            try { encoder.stop() } catch (_: Throwable) {}
            try { encoder.release() } catch (_: Throwable) {}
            if (muxerStarted) {
                try { muxer.stop() } catch (_: Throwable) {}
            }
            try { muxer.release() } catch (_: Throwable) {}
        }
    }

    /**
     * Offline AudioProcessor pipeline chain.
     */
    class ProcessorPipeline(private val processors: List<AudioProcessor>) {
        var outputFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
            private set

        fun configure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            var format = inputFormat
            for (p in processors) {
                val out = p.configure(format)
                if (p.isActive) {
                    format = out
                }
            }
            flush()
            outputFormat = format
            return format
        }

        fun flush() {
            for (p in processors) {
                try {
                    p.flush(AudioProcessor.StreamMetadata.DEFAULT)
                } catch (_: Throwable) {
                    try {
                        @Suppress("DEPRECATION")
                        p.flush()
                    } catch (_: Throwable) {}
                }
            }
        }

        fun process(input: ByteBuffer, onOutput: (ByteBuffer) -> Unit) {
            val nativeInput = if (input.order() != ByteOrder.nativeOrder()) {
                input.duplicate().order(ByteOrder.nativeOrder())
            } else {
                input
            }
            feed(nativeInput, 0, onOutput)
        }

        private fun feed(buffer: ByteBuffer, processorIndex: Int, onOutput: (ByteBuffer) -> Unit) {
            if (processorIndex >= processors.size) {
                if (buffer.hasRemaining()) {
                    onOutput(buffer)
                }
                return
            }

            val p = processors[processorIndex]
            if (!p.isActive) {
                feed(buffer, processorIndex + 1, onOutput)
                return
            }

            p.queueInput(buffer)
            while (true) {
                val out = p.output
                if (!out.hasRemaining()) break
                feed(out, processorIndex + 1, onOutput)
            }
        }

        fun drainEndOfStream(onOutput: (ByteBuffer) -> Unit) {
            for (i in processors.indices) {
                val p = processors[i]
                if (!p.isActive) continue
                p.queueEndOfStream()
                while (!p.isEnded) {
                    val out = p.output
                    if (out.hasRemaining()) {
                        feed(out, i + 1, onOutput)
                    } else {
                        break
                    }
                }
            }
        }

        fun reset() {
            for (p in processors) {
                p.reset()
            }
        }
    }

    /**
     * Decodes source track audio, routes through DSP effects, and writes to public MediaStore.
     */
    suspend fun exportAudio(
        context: Context,
        musicItem: MusicItem,
        auxr: Auxr,
        outputFileName: String,
        format: ExportFormat,
        onProgress: (Float) -> Unit
    ): Uri = withContext(Dispatchers.IO) {
        var cleanName = sanitizeFilename(outputFileName)
        if (format == ExportFormat.WAV && cleanName.endsWith(".wav", ignoreCase = true)) {
            cleanName = cleanName.dropLast(4)
        } else if (format == ExportFormat.M4A && cleanName.endsWith(".m4a", ignoreCase = true)) {
            cleanName = cleanName.dropLast(4)
        }
        val ext = if (format == ExportFormat.WAV) "wav" else "m4a"
        val finalFileName = "$cleanName.$ext"

        val tempFile = File(context.cacheDir, "export_${System.currentTimeMillis()}.$ext")
        var writer: AudioWriter? = null
        var destinationUri: Uri? = null

        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null

        val processors = mutableListOf<AudioProcessor>()
        var pipeline: ProcessorPipeline? = null

        try {
            val sourceUri = if (musicItem.path.startsWith("content://") ||
                musicItem.path.startsWith("http://") ||
                musicItem.path.startsWith("https://")
            ) {
                musicItem.path.toUri()
            } else {
                val f = File(musicItem.path)
                if (f.exists()) {
                    f.toUri()
                } else {
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicItem.id)
                }
            }
            extractor.setDataSource(context, sourceUri, null)

            var audioTrackIndex = -1
            var inputMediaFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    inputMediaFormat = trackFormat
                    break
                }
            }

            if (audioTrackIndex == -1 || inputMediaFormat == null) {
                throw IllegalStateException("No audio track found in media source")
            }

            extractor.selectTrack(audioTrackIndex)

            val durationUs = if (inputMediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
                inputMediaFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                musicItem.duration * 1000L
            }

            val mime = inputMediaFormat.getString(MediaFormat.KEY_MIME)!!
            val sourceRate = if (inputMediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            val sourceChannels = if (inputMediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                inputMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            // Prepare Sonic Audio Processor (for speed, pitch, and high sample-rate AAC downsampling)
            val sonic = SonicAudioProcessor()
            if (format == ExportFormat.M4A && sourceRate > 48000) {
                val targetRate = if (sourceRate % 44100 == 0) 44100 else 48000
                sonic.setOutputSampleRateHz(targetRate)
            }
            sonic.setSpeed(auxr.speed)
            sonic.setPitch(auxr.pitch)
            processors.add(sonic)

            // Prepare 10-band Equalizer and effects
            val eqProcessor = EqualizerAudioProcessor().apply {
                setEqualizerActive(auxr.equalizer)
                setEqualizerType(auxr.equalizerType)
                setQ(auxr.equalizerQ, false)
                for (i in auxr.equalizerBand.indices) {
                    if (i < 10) setBand(i, auxr.equalizerBand[i])
                }
                setDelay(auxr.delayEnabled, auxr.delayTime, auxr.delayFeedback, auxr.delayMix)
                setDelayTime(auxr.echoDelay)
                setDecay(auxr.echoDecay)
                setFeedBack(auxr.echoRevert)
                setEchoActive(auxr.echo)
                setBassBoost(auxr.bassBoostEnabled, auxr.bassBoostStrength / 1000f)
                setVirtualizer(auxr.virtualizerEnabled, auxr.virtualizerStrength / 1000f)
                setReverb(auxr.reverbEnabled, auxr.reverbRoomSize, auxr.reverbDamping, auxr.reverbMix)
                setChorus(auxr.chorusEnabled, auxr.chorusRate, auxr.chorusDepth, auxr.chorusMix)
                setFlanger(auxr.flangerEnabled, auxr.flangerRate, auxr.flangerDepth, auxr.flangerFeedback, auxr.flangerMix)
                setPolyphony(auxr.polyphonyEnabled, auxr.polyphonySemitones, auxr.polyphonyDetune, auxr.polyphonyMix)
            }
            processors.add(eqProcessor)

            val activePipeline = ProcessorPipeline(processors)
            pipeline = activePipeline

            fun initWriter(rate: Int, channels: Int) {
                if (writer == null) {
                    writer = if (format == ExportFormat.WAV) {
                        WavWriter(tempFile, rate, channels)
                    } else {
                        AacWriter(tempFile, rate, channels)
                    }
                }
            }

            // Check if source is already uncompressed PCM (e.g. WAV / raw PCM)
            val isRawPcm = isRawPcmMime(mime)
            if (isRawPcm) {
                val pcmEncoding = if (inputMediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                    inputMediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                } else {
                    C.ENCODING_PCM_16BIT
                }

                val procFormat = activePipeline.configure(
                    AudioProcessor.AudioFormat(sourceRate, sourceChannels, pcmEncoding)
                )
                initWriter(procFormat.sampleRate, procFormat.channelCount)

                val maxInputSize = if (inputMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    inputMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                } else {
                    65536
                }
                val bufferCapacity = maxOf(65536, maxInputSize)
                val readBuffer = ByteBuffer.allocateDirect(bufferCapacity).order(ByteOrder.nativeOrder())
                val bytesPerSample = if (pcmEncoding == C.ENCODING_PCM_FLOAT) 4 else 2
                val frameSize = maxOf(1, sourceChannels * bytesPerSample)
                while (true) {
                    if (!currentCoroutineContext().isActive) {
                        throw CancellationException("Audio export cancelled")
                    }
                    readBuffer.clear()
                    val sampleSize = extractor.readSampleData(readBuffer, 0)
                    if (sampleSize < 0) {
                        break
                    }
                    val validBytes = (sampleSize / frameSize) * frameSize
                    readBuffer.position(0)
                    readBuffer.limit(validBytes)

                    if (validBytes > 0) {
                        activePipeline.process(readBuffer) { chunk ->
                            writer?.write(chunk)
                        }
                    }

                    val sampleTimeUs = extractor.sampleTime
                    if (durationUs > 0 && sampleTimeUs > 0) {
                        val progress = (sampleTimeUs.toFloat() / durationUs).coerceIn(0f, 1f)
                        onProgress(progress * 0.95f)
                    }
                    extractor.advance()
                }
            } else {
                // Compressed audio: decode via MediaCodec
                decoder = MediaCodec.createDecoderByType(mime)
                val cleanFormat = MediaFormat.createAudioFormat(mime, sourceRate, sourceChannels)
                if (inputMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    cleanFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, inputMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                }
                for (key in listOf("csd-0", "csd-1", "csd-2")) {
                    if (inputMediaFormat.containsKey(key)) {
                        cleanFormat.setByteBuffer(key, inputMediaFormat.getByteBuffer(key))
                    }
                }
                decoder.configure(cleanFormat, null, null, 0)
                decoder.start()

                val bufferInfo = MediaCodec.BufferInfo()
                var isExtractorEOS = false
                var isDecoderEOS = false
                val timeoutUs = 5000L

                while (!isDecoderEOS) {
                    if (!currentCoroutineContext().isActive) {
                        throw CancellationException("Audio export cancelled")
                    }

                    // Feed input buffer
                    if (!isExtractorEOS) {
                        val inIndex = decoder.dequeueInputBuffer(timeoutUs)
                        if (inIndex >= 0) {
                            val inputBuffer = decoder.getInputBuffer(inIndex)!!
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isExtractorEOS = true
                            } else {
                                val sampleTime = extractor.sampleTime
                                decoder.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    // Drain output buffer
                    val outIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                    if (outIndex >= 0) {
                        val outBuffer = decoder.getOutputBuffer(outIndex)
                        if (outBuffer != null && bufferInfo.size > 0) {
                            outBuffer.position(bufferInfo.offset)
                            outBuffer.limit(bufferInfo.offset + bufferInfo.size)

                            if (writer == null) {
                                val rate = decoder.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                                val channels = decoder.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                                if (format == ExportFormat.M4A && rate > 48000) {
                                    val targetRate = if (rate % 44100 == 0) 44100 else 48000
                                    sonic.setOutputSampleRateHz(targetRate)
                                }
                                val procFormat = activePipeline.configure(
                                    AudioProcessor.AudioFormat(rate, channels, C.ENCODING_PCM_16BIT)
                                )
                                initWriter(procFormat.sampleRate, procFormat.channelCount)
                            }

                            val slice = outBuffer.slice().order(ByteOrder.nativeOrder())
                            activePipeline.process(slice) { chunk ->
                                writer?.write(chunk)
                            }

                            if (durationUs > 0) {
                                val progress = (bufferInfo.presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 1f)
                                onProgress(progress * 0.95f)
                            }
                        }
                        decoder.releaseOutputBuffer(outIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            isDecoderEOS = true
                        }
                    } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val newFormat = decoder.outputFormat
                        val sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        val channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        if (format == ExportFormat.M4A && sampleRate > 48000) {
                            val targetRate = if (sampleRate % 44100 == 0) 44100 else 48000
                            sonic.setOutputSampleRateHz(targetRate)
                        }
                        val procFormat = activePipeline.configure(
                            AudioProcessor.AudioFormat(sampleRate, channelCount, C.ENCODING_PCM_16BIT)
                        )
                        initWriter(procFormat.sampleRate, procFormat.channelCount)
                    }
                }
            }

            // Drain remaining audio processor buffers
            activePipeline.drainEndOfStream { chunk ->
                writer?.write(chunk)
            }
            writer?.finish()
            writer?.close()
            writer = null

            onProgress(0.98f)

            // Insert into MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, finalFileName)
                put(MediaStore.Audio.Media.TITLE, cleanName)
                put(MediaStore.Audio.Media.ARTIST, musicItem.artist)
                put(MediaStore.Audio.Media.ALBUM, musicItem.album)
                put(MediaStore.Audio.Media.MIME_TYPE, if (format == ExportFormat.WAV) "audio/wav" else "audio/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/MonsterMusic")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            destinationUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IOException("Failed to create MediaStore entry for $finalFileName")

            resolver.openOutputStream(destinationUri)?.use { outStream ->
                tempFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            } ?: throw IOException("Failed to open MediaStore output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(destinationUri, contentValues, null, null)
            }

            MediaScannerConnection.scanFile(
                context,
                arrayOf(destinationUri.toString()),
                arrayOf(if (format == ExportFormat.WAV) "audio/wav" else "audio/mp4"),
                null
            )

            onProgress(1.0f)
            return@withContext destinationUri
        } catch (e: Exception) {
            Log.e(TAG, "Audio export failed", e)
            destinationUri?.let { uri ->
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (_: Throwable) {}
            }
            throw e
        } finally {
            try { writer?.close() } catch (_: Throwable) {}
            try { decoder?.stop() } catch (_: Throwable) {}
            try { decoder?.release() } catch (_: Throwable) {}
            try { extractor.release() } catch (_: Throwable) {}
            pipeline?.reset()
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
}
