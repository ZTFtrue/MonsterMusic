package com.ztftrue.music.ui.play

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.MediaCommands
import com.ztftrue.music.utils.CustomSlider
import com.ztftrue.music.utils.Utils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectView(musicViewModel: MusicViewModel) {
    val listState = rememberLazyListState()
    val pitch = remember { mutableFloatStateOf(musicViewModel.pitch.floatValue) }
    val speed = remember { mutableFloatStateOf(musicViewModel.speed.floatValue) }
    val bands = remember { musicViewModel.equalizerBands }
    val context = LocalContext.current
    val tempBandValue = ArrayList<MutableFloatState>(Utils.bandsCenter.size)
    bands.forEach { band ->
        val bandValue = remember { mutableFloatStateOf(band.value.toFloat()) }
        tempBandValue.add(bandValue)
    }
    val delayTime = remember { mutableFloatStateOf(musicViewModel.delayTime.floatValue) }
    val decay = remember { mutableFloatStateOf(musicViewModel.decay.floatValue) }
    val color = MaterialTheme.colorScheme.onBackground
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height - 1.dp.toPx()),
                    end = Offset(size.width, size.height - 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        items(1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(color = MaterialTheme.colorScheme.primary)
            )
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.pitch) + (pitch.floatValue).toString(),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedButton(
                        modifier = Modifier.padding(0.dp),
                        onClick = {
                            musicViewModel.pitch.floatValue = 1f
                            pitch.floatValue = 1f
                            val bundle = Bundle()
                            bundle.putFloat("pitch", musicViewModel.pitch.floatValue)
                            musicViewModel.browser?.sendCustomCommand(
                                MediaCommands.COMMAND_CHANGE_PITCH,
                                bundle
                            )
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.reset),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                CustomSlider(
                    modifier = Modifier
                        .semantics {
                            contentDescription = context.getString(R.string.pitch_slider)
                        },
                    value = pitch.floatValue,
                    onValueChange = {
                        pitch.floatValue = (it * 10f).roundToInt() / 10f
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    onValueChangeFinished = {
                        musicViewModel.pitch.floatValue = pitch.floatValue
                        val bundle = Bundle()
                        bundle.putFloat("pitch", musicViewModel.pitch.floatValue)
                        musicViewModel.browser?.sendCustomCommand(
                            MediaCommands.COMMAND_CHANGE_PITCH,
                            bundle
                        )
                    },
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.speed) + (speed.floatValue).toString(),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedButton(
                        onClick = {
                            musicViewModel.speed.floatValue = 1f
                            speed.floatValue = 1f
                            musicViewModel.browser?.setPlaybackSpeed(speed.floatValue)
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.reset),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                CustomSlider(
                    modifier = Modifier
                        .semantics {
                            contentDescription = context.getString(R.string.speed_slider)
                        },
                    value = speed.floatValue,
                    onValueChange = {
                        speed.floatValue = (it * 10f).roundToInt() / 10f
                    },
                    valueRange = 0.5f..2f,
                    steps = 15,
                    onValueChangeFinished = {
                        musicViewModel.speed.floatValue = speed.floatValue
                        musicViewModel.browser?.setPlaybackSpeed(speed.floatValue)
                    },
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(color = MaterialTheme.colorScheme.primary)
            )
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.echo),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(2.dp)
                        ) {
                        }
                        Switch(
                            modifier = Modifier.clip(MaterialTheme.shapes.small),
                            checked = musicViewModel.enableEcho.value,
                            onCheckedChange = {
                                musicViewModel.enableEcho.value = it
                                val bundle = Bundle()
                                bundle.putBoolean(MediaCommands.KEY_ENABLE, it)
                                musicViewModel.browser?.sendCustomCommand(
                                    MediaCommands.COMMAND_ECHO_ENABLE,
                                    bundle
                                )
                            }
                        )
                    }
                    Box {
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Text(text = "Multiple revert",      color = MaterialTheme.colorScheme.onBackground)
//                            Box(
//                                modifier = Modifier
//                                    .width(4.dp)
//                                    .height(2.dp)
//                            ) {
//                            }
//                            Switch(
//                                enabled = musicViewModel.enableEcho.value,
//                                modifier = Modifier.clip(MaterialTheme.shapes.small),
//                                checked = musicViewModel.echoFeedBack.value,
//                                onCheckedChange = {
//                                    musicViewModel.echoFeedBack.value = it
//                                    val bundle = Bundle()
//                                    bundle.putBoolean("enable", it)
//                                    musicViewModel.mediaBrowser
//                                        ?.sendCustomAction(
//                                            ACTION_ECHO_FEEDBACK, bundle, null
//                                        )
//                                }
//                            )
//                        }
                    }
                }
                Text(
                    text = stringResource(R.string.delay) + (delayTime.floatValue) + stringResource(
                        R.string.seconds
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                CustomSlider(
                    modifier = Modifier
                        .semantics {
                            contentDescription =
                                context.getString(R.string.echo_delay_slider_description)
                        },
                    value = delayTime.floatValue,
                    onValueChange = {
                        delayTime.floatValue = (it * 10f).roundToInt() / 10f
                    },
                    enabled = musicViewModel.enableEcho.value,
                    valueRange = 0.1f..2f,
                    steps = 20,
                    onValueChangeFinished = {
                        musicViewModel.delayTime.floatValue = delayTime.floatValue
                        val bundle = Bundle()
                        bundle.putFloat("delay", musicViewModel.delayTime.floatValue)
                        musicViewModel.browser?.sendCustomCommand(
                            MediaCommands.COMMAND_ECHO_SET_DELAY,
                            bundle
                        )
                    },
                )
                Text(
                    text = stringResource(R.string.decay) + (decay.floatValue).toString(),
                    color = MaterialTheme.colorScheme.onBackground
                )
                CustomSlider(
                    modifier = Modifier
                        .semantics {
                            contentDescription =
                                context.getString(R.string.echo_decay_slider_description)
                        },
                    value = decay.floatValue,
                    onValueChange = {
                        decay.floatValue = (it * 10f).roundToInt() / 10f
                    },
                    valueRange = 0f..1f,
                    steps = 10,
                    enabled = musicViewModel.enableEcho.value,
                    onValueChangeFinished = {
                        musicViewModel.decay.floatValue = decay.floatValue
                        val bundle = Bundle()
                        bundle.putFloat("decay", musicViewModel.decay.floatValue)
                        musicViewModel.browser?.sendCustomCommand(
                            MediaCommands.COMMAND_ECHO_SET_DECAY,
                            bundle
                        )
                    },
                )
            }
        }
    }
}
