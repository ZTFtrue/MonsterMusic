package com.ztftrue.music.ui.play

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.manager.MediaCommands
import com.ztftrue.music.utils.CustomSlider
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectView(musicViewModel: MusicViewModel) {
    val listState = rememberLazyListState()
    var selectedCategory by remember { mutableIntStateOf(0) }

    // Categories
    val catAll = stringResource(R.string.all)
    val catPitch = stringResource(R.string.pitch) + " / " + stringResource(R.string.speed)
    val catSurround = stringResource(R.string.virtual_surround)
    val catEcho = stringResource(R.string.echo)
    val catReverb = stringResource(R.string.reverb)
    val catChorus = stringResource(R.string.chorus)
    val catFlanger = stringResource(R.string.flanger)
    val catPolyphony = stringResource(R.string.polyphony)

    val categories = listOf(
        0 to catAll,
        1 to catPitch,
        2 to catSurround,
        3 to catEcho,
        4 to catReverb,
        5 to catChorus,
        6 to catFlanger,
        7 to catPolyphony
    )

    // Local mutable states synced with ViewModel
    val pitch = remember { mutableFloatStateOf(musicViewModel.pitch.floatValue) }
    val speed = remember { mutableFloatStateOf(musicViewModel.speed.floatValue) }

    val enableVirtual = remember { mutableStateOf(musicViewModel.enableVirtual.value) }
    val virtualStrength = remember { mutableIntStateOf(musicViewModel.virtualStrength.intValue) }

    val enableEcho = remember { mutableStateOf(musicViewModel.enableEcho.value) }
    val delayTime = remember { mutableFloatStateOf(musicViewModel.delayTime.floatValue) }
    val decay = remember { mutableFloatStateOf(musicViewModel.decay.floatValue) }

    val enableReverb = remember { mutableStateOf(musicViewModel.enableReverb.value) }
    val reverbRoomSize = remember { mutableFloatStateOf(musicViewModel.reverbRoomSize.floatValue) }
    val reverbDamping = remember { mutableFloatStateOf(musicViewModel.reverbDamping.floatValue) }
    val reverbMix = remember { mutableFloatStateOf(musicViewModel.reverbMix.floatValue) }

    val enableChorus = remember { mutableStateOf(musicViewModel.enableChorus.value) }
    val chorusRate = remember { mutableFloatStateOf(musicViewModel.chorusRate.floatValue) }
    val chorusDepth = remember { mutableFloatStateOf(musicViewModel.chorusDepth.floatValue) }
    val chorusMix = remember { mutableFloatStateOf(musicViewModel.chorusMix.floatValue) }

    val enableFlanger = remember { mutableStateOf(musicViewModel.enableFlanger.value) }
    val flangerRate = remember { mutableFloatStateOf(musicViewModel.flangerRate.floatValue) }
    val flangerDepth = remember { mutableFloatStateOf(musicViewModel.flangerDepth.floatValue) }
    val flangerFeedback = remember { mutableFloatStateOf(musicViewModel.flangerFeedback.floatValue) }
    val flangerMix = remember { mutableFloatStateOf(musicViewModel.flangerMix.floatValue) }

    val enablePolyphony = remember { mutableStateOf(musicViewModel.enablePolyphony.value) }
    val polyphonySemitones = remember { mutableIntStateOf(musicViewModel.polyphonySemitones.intValue) }
    val polyphonyDetune = remember { mutableFloatStateOf(musicViewModel.polyphonyDetune.floatValue) }
    val polyphonyMix = remember { mutableFloatStateOf(musicViewModel.polyphonyMix.floatValue) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        // Category Filter Chips Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (id, label) ->
                FilterChip(
                    selected = selectedCategory == id,
                    onClick = { selectedCategory = id },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color = MaterialTheme.colorScheme.outlineVariant)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Pitch & Speed
            if (selectedCategory == 0 || selectedCategory == 1) {
                item {
                    EffectCard(
                        title = stringResource(R.string.pitch) + " & " + stringResource(R.string.speed),
                        onReset = {
                            musicViewModel.pitch.floatValue = 1f
                            pitch.floatValue = 1f
                            val bundleP = Bundle().apply { putFloat("pitch", 1f) }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_CHANGE_PITCH, bundleP)

                            musicViewModel.speed.floatValue = 1f
                            speed.floatValue = 1f
                            musicViewModel.browser?.setPlaybackSpeed(1f)
                        }
                    ) {
                        // Pitch Slider
                        Text(
                            text = stringResource(R.string.pitch) + ": " + String.format(Locale.ROOT, "%.1fx", pitch.floatValue),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = pitch.floatValue,
                            onValueChange = {
                                pitch.floatValue = (it * 10f).roundToInt() / 10f
                            },
                            valueRange = 0.5f..2.0f,
                            steps = 15,
                            onValueChangeFinished = {
                                musicViewModel.pitch.floatValue = pitch.floatValue
                                val bundle = Bundle().apply { putFloat("pitch", pitch.floatValue) }
                                musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_CHANGE_PITCH, bundle)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Speed Slider
                        Text(
                            text = stringResource(R.string.speed) + ": " + String.format(Locale.ROOT, "%.1fx", speed.floatValue),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = speed.floatValue,
                            onValueChange = {
                                speed.floatValue = (it * 10f).roundToInt() / 10f
                            },
                            valueRange = 0.5f..2.0f,
                            steps = 15,
                            onValueChangeFinished = {
                                musicViewModel.speed.floatValue = speed.floatValue
                                musicViewModel.browser?.setPlaybackSpeed(speed.floatValue)
                            }
                        )
                    }
                }
            }

            // 2. 3D Virtual Surround
            if (selectedCategory == 0 || selectedCategory == 2) {
                item {
                    EffectCard(
                        title = stringResource(R.string.virtual_surround),
                        enabled = enableVirtual.value,
                        onCheckedChange = {
                            enableVirtual.value = it
                            musicViewModel.enableVirtual.value = it
                            val bundle = Bundle().apply { putBoolean(MediaCommands.KEY_ENABLE, it) }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_VIRTUALIZER_ENABLE, bundle)
                        },
                        onReset = {
                            virtualStrength.intValue = 0
                            musicViewModel.virtualStrength.intValue = 0
                            val bundle = Bundle().apply { putInt("strength", 0) }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_VIRTUALIZER_STRENGTH, bundle)
                        }
                    ) {
                        val virtualStrengthText = stringResource(R.string.strength_format, virtualStrength.intValue)
                        Text(
                            text = virtualStrengthText,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            modifier = Modifier.semantics { contentDescription = virtualStrengthText },
                            value = virtualStrength.intValue.toFloat(),
                            onValueChange = {
                                virtualStrength.intValue = it.roundToInt()
                            },
                            enabled = enableVirtual.value,
                            valueRange = 0f..1000f,
                            steps = 100,
                            onValueChangeFinished = {
                                musicViewModel.virtualStrength.intValue = virtualStrength.intValue
                                val bundle = Bundle().apply { putInt("strength", virtualStrength.intValue) }
                                musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_VIRTUALIZER_STRENGTH, bundle)
                            }
                        )
                    }
                }
            }

            // 3. Echo
            if (selectedCategory == 0 || selectedCategory == 3) {
                item {
                    EffectCard(
                        title = stringResource(R.string.echo),
                        enabled = enableEcho.value,
                        onCheckedChange = {
                            enableEcho.value = it
                            musicViewModel.enableEcho.value = it
                            val bundle = Bundle().apply { putBoolean(MediaCommands.KEY_ENABLE, it) }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_ECHO_ENABLE, bundle)
                        },
                        onReset = {
                            delayTime.floatValue = 0.5f
                            decay.floatValue = 0.5f
                            musicViewModel.delayTime.floatValue = 0.5f
                            musicViewModel.decay.floatValue = 0.5f

                            val bundleDelay = Bundle().apply { putFloat(MediaCommands.KEY_DELAY, 0.5f) }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_ECHO_SET_DELAY, bundleDelay)

                            val bundleDecay = Bundle().apply { putFloat(MediaCommands.KEY_DECAY, 0.5f) }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_ECHO_SET_DECAY, bundleDecay)
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.delay) + ": " + String.format(Locale.ROOT, "%.1f", delayTime.floatValue) + " " + stringResource(R.string.seconds),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = delayTime.floatValue,
                            onValueChange = {
                                delayTime.floatValue = (it * 10f).roundToInt() / 10f
                            },
                            enabled = enableEcho.value,
                            valueRange = 0.1f..2.0f,
                            steps = 19,
                            onValueChangeFinished = {
                                musicViewModel.delayTime.floatValue = delayTime.floatValue
                                val bundle = Bundle().apply { putFloat(MediaCommands.KEY_DELAY, delayTime.floatValue) }
                                musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_ECHO_SET_DELAY, bundle)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(R.string.decay) + ": " + String.format(Locale.ROOT, "%.1f", decay.floatValue),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = decay.floatValue,
                            onValueChange = {
                                decay.floatValue = (it * 10f).roundToInt() / 10f
                            },
                            enabled = enableEcho.value,
                            valueRange = 0.1f..1.0f,
                            steps = 9,
                            onValueChangeFinished = {
                                musicViewModel.decay.floatValue = decay.floatValue
                                val bundle = Bundle().apply { putFloat(MediaCommands.KEY_DECAY, decay.floatValue) }
                                musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_ECHO_SET_DECAY, bundle)
                            }
                        )
                    }
                }
            }

            // 4. Reverb
            if (selectedCategory == 0 || selectedCategory == 4) {
                item {
                    EffectCard(
                        title = stringResource(R.string.reverb),
                        enabled = enableReverb.value,
                        onCheckedChange = {
                            enableReverb.value = it
                            musicViewModel.enableReverb.value = it
                            val bundle = Bundle().apply { putBoolean(MediaCommands.KEY_ENABLE, it) }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_REVERB_ENABLE, bundle)
                        },
                        onReset = {
                            reverbRoomSize.floatValue = 0.5f
                            reverbDamping.floatValue = 0.5f
                            reverbMix.floatValue = 0.3f
                            musicViewModel.reverbRoomSize.floatValue = 0.5f
                            musicViewModel.reverbDamping.floatValue = 0.5f
                            musicViewModel.reverbMix.floatValue = 0.3f

                            val bundle = Bundle().apply {
                                putFloat(MediaCommands.KEY_ROOM_SIZE, 0.5f)
                                putFloat(MediaCommands.KEY_DAMPING, 0.5f)
                                putFloat(MediaCommands.KEY_MIX, 0.3f)
                            }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_REVERB_SET_PARAMS, bundle)
                        }
                    ) {
                        // Room Size
                        Text(
                            text = stringResource(R.string.room_size) + ": " + (reverbRoomSize.floatValue * 100).roundToInt() + "%",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = reverbRoomSize.floatValue,
                            onValueChange = {
                                reverbRoomSize.floatValue = (it * 20f).roundToInt() / 20f
                            },
                            enabled = enableReverb.value,
                            valueRange = 0.1f..1.0f,
                            steps = 17,
                            onValueChangeFinished = {
                                musicViewModel.reverbRoomSize.floatValue = reverbRoomSize.floatValue
                                sendReverbParams(musicViewModel, reverbRoomSize.floatValue, reverbDamping.floatValue, reverbMix.floatValue)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Damping
                        Text(
                            text = stringResource(R.string.damping) + ": " + (reverbDamping.floatValue * 100).roundToInt() + "%",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = reverbDamping.floatValue,
                            onValueChange = {
                                reverbDamping.floatValue = (it * 20f).roundToInt() / 20f
                            },
                            enabled = enableReverb.value,
                            valueRange = 0.0f..1.0f,
                            steps = 19,
                            onValueChangeFinished = {
                                musicViewModel.reverbDamping.floatValue = reverbDamping.floatValue
                                sendReverbParams(musicViewModel, reverbRoomSize.floatValue, reverbDamping.floatValue, reverbMix.floatValue)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Wet/Dry Mix
                        Text(
                            text = stringResource(R.string.wet_dry_mix) + ": " + (reverbMix.floatValue * 100).roundToInt() + "%",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = reverbMix.floatValue,
                            onValueChange = {
                                reverbMix.floatValue = (it * 20f).roundToInt() / 20f
                            },
                            enabled = enableReverb.value,
                            valueRange = 0.0f..1.0f,
                            steps = 19,
                            onValueChangeFinished = {
                                musicViewModel.reverbMix.floatValue = reverbMix.floatValue
                                sendReverbParams(musicViewModel, reverbRoomSize.floatValue, reverbDamping.floatValue, reverbMix.floatValue)
                            }
                        )
                    }
                }
            }

            // 5. Chorus
            if (selectedCategory == 0 || selectedCategory == 5) {
                item {
                    EffectCard(
                        title = stringResource(R.string.chorus),
                        enabled = enableChorus.value,
                        onCheckedChange = {
                            enableChorus.value = it
                            musicViewModel.enableChorus.value = it
                            val bundle = Bundle().apply { putBoolean(MediaCommands.KEY_ENABLE, it) }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_CHORUS_ENABLE, bundle)
                        },
                        onReset = {
                            chorusRate.floatValue = 1.5f
                            chorusDepth.floatValue = 0.5f
                            chorusMix.floatValue = 0.5f
                            musicViewModel.chorusRate.floatValue = 1.5f
                            musicViewModel.chorusDepth.floatValue = 0.5f
                            musicViewModel.chorusMix.floatValue = 0.5f

                            val bundle = Bundle().apply {
                                putFloat(MediaCommands.KEY_RATE, 1.5f)
                                putFloat(MediaCommands.KEY_DEPTH, 0.5f)
                                putFloat(MediaCommands.KEY_MIX, 0.5f)
                            }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_CHORUS_SET_PARAMS, bundle)
                        }
                    ) {
                        // Rate
                        Text(
                            text = stringResource(R.string.chorus_rate) + ": " + String.format(Locale.ROOT, "%.1f Hz", chorusRate.floatValue),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = chorusRate.floatValue,
                            onValueChange = {
                                chorusRate.floatValue = (it * 10f).roundToInt() / 10f
                            },
                            enabled = enableChorus.value,
                            valueRange = 0.1f..5.0f,
                            steps = 48,
                            onValueChangeFinished = {
                                musicViewModel.chorusRate.floatValue = chorusRate.floatValue
                                sendChorusParams(musicViewModel, chorusRate.floatValue, chorusDepth.floatValue, chorusMix.floatValue)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Depth
                        Text(
                            text = stringResource(R.string.chorus_depth) + ": " + (chorusDepth.floatValue * 100).roundToInt() + "%",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = chorusDepth.floatValue,
                            onValueChange = {
                                chorusDepth.floatValue = (it * 20f).roundToInt() / 20f
                            },
                            enabled = enableChorus.value,
                            valueRange = 0.0f..1.0f,
                            steps = 19,
                            onValueChangeFinished = {
                                musicViewModel.chorusDepth.floatValue = chorusDepth.floatValue
                                sendChorusParams(musicViewModel, chorusRate.floatValue, chorusDepth.floatValue, chorusMix.floatValue)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Mix
                        Text(
                            text = stringResource(R.string.wet_dry_mix) + ": " + (chorusMix.floatValue * 100).roundToInt() + "%",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = chorusMix.floatValue,
                            onValueChange = {
                                chorusMix.floatValue = (it * 20f).roundToInt() / 20f
                            },
                            enabled = enableChorus.value,
                            valueRange = 0.0f..1.0f,
                            steps = 19,
                            onValueChangeFinished = {
                                musicViewModel.chorusMix.floatValue = chorusMix.floatValue
                                sendChorusParams(musicViewModel, chorusRate.floatValue, chorusDepth.floatValue, chorusMix.floatValue)
                            }
                        )
                    }
                }
            }

            // 6. Flanger
            if (selectedCategory == 0 || selectedCategory == 6) {
                item {
                    EffectCard(
                        title = stringResource(R.string.flanger),
                        enabled = enableFlanger.value,
                        onCheckedChange = {
                            enableFlanger.value = it
                            musicViewModel.enableFlanger.value = it
                            val bundle = Bundle().apply { putBoolean(MediaCommands.KEY_ENABLE, it) }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_FLANGER_ENABLE, bundle)
                        },
                        onReset = {
                            flangerRate.floatValue = 0.5f
                            flangerDepth.floatValue = 0.7f
                            flangerFeedback.floatValue = 0.5f
                            flangerMix.floatValue = 0.5f
                            musicViewModel.flangerRate.floatValue = 0.5f
                            musicViewModel.flangerDepth.floatValue = 0.7f
                            musicViewModel.flangerFeedback.floatValue = 0.5f
                            musicViewModel.flangerMix.floatValue = 0.5f

                            val bundle = Bundle().apply {
                                putFloat(MediaCommands.KEY_RATE, 0.5f)
                                putFloat(MediaCommands.KEY_DEPTH, 0.7f)
                                putFloat(MediaCommands.KEY_FEEDBACK, 0.5f)
                                putFloat(MediaCommands.KEY_MIX, 0.5f)
                            }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_FLANGER_SET_PARAMS, bundle)
                        }
                    ) {
                        // Rate
                        Text(
                            text = stringResource(R.string.chorus_rate) + ": " + String.format(Locale.ROOT, "%.2f Hz", flangerRate.floatValue),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = flangerRate.floatValue,
                            onValueChange = {
                                flangerRate.floatValue = (it * 20f).roundToInt() / 20f
                            },
                            enabled = enableFlanger.value,
                            valueRange = 0.05f..3.0f,
                            steps = 29,
                            onValueChangeFinished = {
                                musicViewModel.flangerRate.floatValue = flangerRate.floatValue
                                sendFlangerParams(musicViewModel, flangerRate.floatValue, flangerDepth.floatValue, flangerFeedback.floatValue, flangerMix.floatValue)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Depth
                        Text(
                            text = stringResource(R.string.chorus_depth) + ": " + (flangerDepth.floatValue * 100).roundToInt() + "%",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = flangerDepth.floatValue,
                            onValueChange = {
                                flangerDepth.floatValue = (it * 20f).roundToInt() / 20f
                            },
                            enabled = enableFlanger.value,
                            valueRange = 0.0f..1.0f,
                            steps = 19,
                            onValueChangeFinished = {
                                musicViewModel.flangerDepth.floatValue = flangerDepth.floatValue
                                sendFlangerParams(musicViewModel, flangerRate.floatValue, flangerDepth.floatValue, flangerFeedback.floatValue, flangerMix.floatValue)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Feedback
                        Text(
                            text = stringResource(R.string.flanger_feedback) + ": " + (flangerFeedback.floatValue * 100).roundToInt() + "%",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = flangerFeedback.floatValue,
                            onValueChange = {
                                flangerFeedback.floatValue = (it * 20f).roundToInt() / 20f
                            },
                            enabled = enableFlanger.value,
                            valueRange = -0.85f..0.85f,
                            steps = 33,
                            onValueChangeFinished = {
                                musicViewModel.flangerFeedback.floatValue = flangerFeedback.floatValue
                                sendFlangerParams(musicViewModel, flangerRate.floatValue, flangerDepth.floatValue, flangerFeedback.floatValue, flangerMix.floatValue)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Mix
                        Text(
                            text = stringResource(R.string.wet_dry_mix) + ": " + (flangerMix.floatValue * 100).roundToInt() + "%",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = flangerMix.floatValue,
                            onValueChange = {
                                flangerMix.floatValue = (it * 20f).roundToInt() / 20f
                            },
                            enabled = enableFlanger.value,
                            valueRange = 0.0f..1.0f,
                            steps = 19,
                            onValueChangeFinished = {
                                musicViewModel.flangerMix.floatValue = flangerMix.floatValue
                                sendFlangerParams(musicViewModel, flangerRate.floatValue, flangerDepth.floatValue, flangerFeedback.floatValue, flangerMix.floatValue)
                            }
                        )
                    }
                }
            }

            // 7. Polyphony
            if (selectedCategory == 0 || selectedCategory == 7) {
                item {
                    EffectCard(
                        title = stringResource(R.string.polyphony),
                        enabled = enablePolyphony.value,
                        onCheckedChange = {
                            enablePolyphony.value = it
                            musicViewModel.enablePolyphony.value = it
                            val bundle = Bundle().apply { putBoolean(MediaCommands.KEY_ENABLE, it) }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_POLYPHONY_ENABLE, bundle)
                        },
                        onReset = {
                            polyphonySemitones.intValue = 0
                            polyphonyDetune.floatValue = 0.0f
                            polyphonyMix.floatValue = 0.5f
                            musicViewModel.polyphonySemitones.intValue = 0
                            musicViewModel.polyphonyDetune.floatValue = 0.0f
                            musicViewModel.polyphonyMix.floatValue = 0.5f

                            val bundle = Bundle().apply {
                                putInt(MediaCommands.KEY_SEMITONES, 0)
                                putFloat(MediaCommands.KEY_DETUNE, 0.0f)
                                putFloat(MediaCommands.KEY_MIX, 0.5f)
                            }
                            musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_POLYPHONY_SET_PARAMS, bundle)
                        }
                    ) {
                        // Semitones (-12 .. +12)
                        val stVal = polyphonySemitones.intValue
                        val stDisplay = if (stVal > 0) "+$stVal" else "$stVal"
                        Text(
                            text = stringResource(R.string.pitch_shift_semitones) + ": $stDisplay",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = stVal.toFloat(),
                            onValueChange = {
                                polyphonySemitones.intValue = it.roundToInt()
                            },
                            enabled = enablePolyphony.value,
                            valueRange = -12f..12f,
                            steps = 23,
                            onValueChangeFinished = {
                                musicViewModel.polyphonySemitones.intValue = polyphonySemitones.intValue
                                sendPolyphonyParams(musicViewModel, polyphonySemitones.intValue, polyphonyDetune.floatValue, polyphonyMix.floatValue)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Detune (-50 .. +50 cents)
                        val detuneVal = polyphonyDetune.floatValue.roundToInt()
                        val detuneDisplay = if (detuneVal > 0) "+$detuneVal" else "$detuneVal"
                        Text(
                            text = stringResource(R.string.detune) + ": $detuneDisplay",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = polyphonyDetune.floatValue,
                            onValueChange = {
                                polyphonyDetune.floatValue = (it * 2f).roundToInt() / 2f
                            },
                            enabled = enablePolyphony.value,
                            valueRange = -50f..50f,
                            steps = 49,
                            onValueChangeFinished = {
                                musicViewModel.polyphonyDetune.floatValue = polyphonyDetune.floatValue
                                sendPolyphonyParams(musicViewModel, polyphonySemitones.intValue, polyphonyDetune.floatValue, polyphonyMix.floatValue)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Mix
                        Text(
                            text = stringResource(R.string.wet_dry_mix) + ": " + (polyphonyMix.floatValue * 100).roundToInt() + "%",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CustomSlider(
                            value = polyphonyMix.floatValue,
                            onValueChange = {
                                polyphonyMix.floatValue = (it * 20f).roundToInt() / 20f
                            },
                            enabled = enablePolyphony.value,
                            valueRange = 0.0f..1.0f,
                            steps = 19,
                            onValueChangeFinished = {
                                musicViewModel.polyphonyMix.floatValue = polyphonyMix.floatValue
                                sendPolyphonyParams(musicViewModel, polyphonySemitones.intValue, polyphonyDetune.floatValue, polyphonyMix.floatValue)
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun EffectCard(
    title: String,
    enabled: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onReset: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (enabled != null && onCheckedChange != null) {
                        Switch(
                            checked = enabled,
                            onCheckedChange = onCheckedChange,
                            modifier = Modifier.clip(MaterialTheme.shapes.small)
                        )
                    }
                }

                if (onReset != null) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reset),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            content()
        }
    }
}

private fun sendReverbParams(musicViewModel: MusicViewModel, roomSize: Float, damping: Float, mix: Float) {
    val bundle = Bundle().apply {
        putFloat(MediaCommands.KEY_ROOM_SIZE, roomSize)
        putFloat(MediaCommands.KEY_DAMPING, damping)
        putFloat(MediaCommands.KEY_MIX, mix)
    }
    musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_REVERB_SET_PARAMS, bundle)
}

private fun sendChorusParams(musicViewModel: MusicViewModel, rate: Float, depth: Float, mix: Float) {
    val bundle = Bundle().apply {
        putFloat(MediaCommands.KEY_RATE, rate)
        putFloat(MediaCommands.KEY_DEPTH, depth)
        putFloat(MediaCommands.KEY_MIX, mix)
    }
    musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_CHORUS_SET_PARAMS, bundle)
}

private fun sendFlangerParams(musicViewModel: MusicViewModel, rate: Float, depth: Float, feedback: Float, mix: Float) {
    val bundle = Bundle().apply {
        putFloat(MediaCommands.KEY_RATE, rate)
        putFloat(MediaCommands.KEY_DEPTH, depth)
        putFloat(MediaCommands.KEY_FEEDBACK, feedback)
        putFloat(MediaCommands.KEY_MIX, mix)
    }
    musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_FLANGER_SET_PARAMS, bundle)
}

private fun sendPolyphonyParams(musicViewModel: MusicViewModel, semitones: Int, detune: Float, mix: Float) {
    val bundle = Bundle().apply {
        putInt(MediaCommands.KEY_SEMITONES, semitones)
        putFloat(MediaCommands.KEY_DETUNE, detune)
        putFloat(MediaCommands.KEY_MIX, mix)
    }
    musicViewModel.browser?.sendCustomCommand(MediaCommands.COMMAND_POLYPHONY_SET_PARAMS, bundle)
}
