package com.ztftrue.music.ui.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.utils.AudioPathHelper
import com.ztftrue.music.utils.SharedPreferencesUtils
import com.ztftrue.music.utils.Utils

private data class ChainStage(
    val title: String,
    val icon: ImageVector,
    val items: List<Pair<String, String>>
)

@Composable
fun AudioChainView(
    musicViewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPlay = musicViewModel.currentPlay.value
    val formatMap = musicViewModel.currentInputFormat
    val tags = musicViewModel.tags
    val equalizerType = musicViewModel.equalizerType.intValue
    val enableEqualizer = musicViewModel.enableEqualizer.value
    val virtualStrength = musicViewModel.virtualStrength.intValue

    // --- 1. Track Info ---
    val filePath = currentPlay?.path ?: ""
    val rawMime = formatMap["MimeType"] ?: ""
    val rawCodec = formatMap["Codec"] ?: ""
    val tagFormat = tags["audioFormat"] ?: ""
    val formatName = AudioPathHelper.formatFormatName(
        rawMime.ifEmpty { rawCodec },
        filePath,
        tagFormat
    )

    val tagBits = tags["bitsPerSample"] ?: ""
    val mapBits = formatMap["BitDepth"] ?: ""
    val bitDepth = AudioPathHelper.formatBitDepth(tagBits, formatName, mapBits)

    val sampleRateInt = formatMap["SampleRate"]?.toIntOrNull()
        ?: tags["audioSampleRate"]?.toIntOrNull()
        ?: 48000
    val sampleRateStr = "$sampleRateInt Hz"

    val rawBitrate = formatMap["Bitrate"]?.toLongOrNull() ?: 0L
    val tagBitrate = tags["audioBitRate"]
    val bitrateStr = AudioPathHelper.formatBitrate(rawBitrate, tagBitrate)

    val channelCount = formatMap["ChannelCount"]?.toIntOrNull()
        ?: tags["audioChannels"]?.toIntOrNull()
        ?: 2
    val channelsStr = AudioPathHelper.formatChannels(channelCount)

    // --- 2. Decoder ---
    val decoderName = AudioPathHelper.resolveDecoderName(
        rawMime.ifEmpty { rawCodec },
        formatName
    )

    // --- 3. Resampler ---
    val hwRate = AudioPathHelper.getHardwareSampleRate(context)
    val ioRateStr = "$sampleRateInt Hz ➔ $hwRate Hz"
    val resamplerType = "Hardware (HAL)"
    val cutoffStr = "${minOf(sampleRateInt, hwRate) / 2} Hz, 25%"
    val qualityStr = "HAL Native"

    // --- 4. DSP ---
    val filterTypeStr = if (equalizerType == 0) "IIR" else "FFT"
    val pcmFormatStr = "PCM 16-bit"
    val dspSampleRateStr = "$sampleRateInt Hz"
    val eqPresetName = if (!enableEqualizer) {
        "Bypassed"
    } else {
        val preset = SharedPreferencesUtils.getCurrentEqualizerName(context)
        if (preset == Utils.custom) {
            stringResource(Utils.translateMap[preset] ?: R.string.app_name)
        } else {
            preset
        }
    }
    val stereoExpandStr = "${virtualStrength / 10}%"
    val buffersStr = AudioPathHelper.calculateBuffers(sampleRateInt)
    val latencyStr = "~255 ms"
    val visualizerLatencyStr = "~255 ms"
    val outputApiStr = "AudioTrack"

    // --- 5. Output Device ---
    val deviceNameStr = AudioPathHelper.getActiveOutputDeviceName(context)
    val outputBitDepthStr = "In: $bitDepth Out: 16-bit"
    val outputSampleRateStr = "$hwRate Hz"

    val stages = listOf(
        ChainStage(
            title = stringResource(R.string.track_info),
            icon = Icons.Default.MusicNote,
            items = listOf(
                stringResource(R.string.format_label) to formatName,
                stringResource(R.string.bit_depth) to bitDepth,
                stringResource(R.string.sample_rate_label) to sampleRateStr,
                stringResource(R.string.bitrate_label) to bitrateStr,
                stringResource(R.string.channels_label) to channelsStr
            )
        ),
        ChainStage(
            title = stringResource(R.string.decoder),
            icon = Icons.Default.Memory,
            items = listOf(
                stringResource(R.string.decoder_name) to decoderName
            )
        ),
        ChainStage(
            title = stringResource(R.string.resampler),
            icon = Icons.Default.GraphicEq,
            items = listOf(
                stringResource(R.string.io_rate) to ioRateStr,
                stringResource(R.string.type_label) to resamplerType,
                stringResource(R.string.cutoff) to cutoffStr,
                stringResource(R.string.quality) to qualityStr
            )
        ),
        ChainStage(
            title = stringResource(R.string.dsp),
            icon = Icons.Default.Equalizer,
            items = listOf(
                stringResource(R.string.pcm_format) to pcmFormatStr,
                stringResource(R.string.sample_rate_label) to dspSampleRateStr,
                stringResource(R.string.equalizer_type) to filterTypeStr,
                stringResource(R.string.eq_preset) to eqPresetName,
                stringResource(R.string.stereo_expand) to stereoExpandStr,
                stringResource(R.string.buffers) to buffersStr,
                stringResource(R.string.latency) to latencyStr,
                stringResource(R.string.visualizer_latency) to visualizerLatencyStr,
                stringResource(R.string.output_api) to outputApiStr
            )
        ),
        ChainStage(
            title = stringResource(R.string.output_device),
            icon = Icons.AutoMirrored.Outlined.VolumeUp,
            items = listOf(
                stringResource(R.string.device_name) to deviceNameStr,
                stringResource(R.string.bit_depth) to outputBitDepthStr,
                stringResource(R.string.sample_rate_label) to outputSampleRateStr
            )
        )
    )

    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Column(modifier = modifier.fillMaxWidth()) {
        stages.forEachIndexed { index, stage ->
            val isLast = index == stages.size - 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                // Left Column: Icon + Timeline connecting line with arrow indicator
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (!isLast) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val iconRadius = 14.dp.toPx()
                            val startY = iconRadius * 2 + 4.dp.toPx()
                            val endY = size.height - 4.dp.toPx()
                            val centerX = size.width / 2f

                            if (endY > startY) {
                                // Vertical line
                                drawLine(
                                    color = lineColor,
                                    start = Offset(centerX, startY),
                                    end = Offset(centerX, endY),
                                    strokeWidth = 1.5.dp.toPx()
                                )

                                // Downward chevron arrow
                                val arrowSize = 4.dp.toPx()
                                val path = Path().apply {
                                    moveTo(centerX - arrowSize, endY - arrowSize)
                                    lineTo(centerX, endY)
                                    lineTo(centerX + arrowSize, endY - arrowSize)
                                }
                                drawPath(
                                    path = path,
                                    color = lineColor,
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = stage.icon,
                        contentDescription = stage.title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopCenter)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Column: Title and Key-Value attributes
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isLast) 12.dp else 24.dp)
                ) {
                    Text(
                        text = stage.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    stage.items.forEach { (key, value) ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("$key: ")
                                    }
                                    append(value)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}
