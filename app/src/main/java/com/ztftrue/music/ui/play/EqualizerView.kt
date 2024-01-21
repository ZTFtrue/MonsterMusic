package com.ztftrue.music.ui.play

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.play.ACTION_CHANGE_PITCH
import com.ztftrue.music.play.ACTION_DSP_BAND
import com.ztftrue.music.play.ACTION_DSP_BAND_FLATTEN
import com.ztftrue.music.play.ACTION_DSP_ENABLE
import com.ztftrue.music.play.ACTION_ECHO_DECAY
import com.ztftrue.music.play.ACTION_ECHO_DELAY
import com.ztftrue.music.play.ACTION_ECHO_ENABLE
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.equalizerMax
import com.ztftrue.music.utils.Utils.equalizerMin
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.roundToInt

@Composable
fun EqualizerView(musicViewModel: MusicViewModel) {
    val listState = rememberLazyListState()
    val pitch = remember { mutableFloatStateOf(musicViewModel.pitch.floatValue) }
    val speed = remember { mutableFloatStateOf(musicViewModel.speed.floatValue) }
    val bands = remember { musicViewModel.equalizerBands }
    val context = LocalContext.current
    val minEQLevel = remember { equalizerMin }
    val maxEQLevel = remember { equalizerMax }
    val tempBandValue = ArrayList<MutableFloatState>(Utils.kThirdOct.size)
    bands.forEach { band ->
        val bandValue = remember { mutableFloatStateOf(band.value.toFloat()) }
        tempBandValue.add(bandValue)
    }

    val delayTime = remember { mutableFloatStateOf(musicViewModel.delayTime.floatValue) }
    val decay = remember { mutableFloatStateOf(musicViewModel.decay.floatValue) }
    val color = MaterialTheme.colorScheme.onBackground
    val df = DecimalFormat("#.#")
    df.roundingMode = RoundingMode.FLOOR
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
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pitch" + (pitch.floatValue).toString(),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedButton(
                        modifier = Modifier.padding(0.dp),
                        onClick = {
                            musicViewModel.pitch.floatValue = 1f
                            pitch.floatValue = 1f
                            val bundle = Bundle()
                            bundle.putFloat("pitch", musicViewModel.pitch.floatValue)
                            musicViewModel.mediaBrowser?.sendCustomAction(
                                ACTION_CHANGE_PITCH,
                                bundle,
                                null
                            )
                        },
                    ) {
                        Text(text = "Reset", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Slider(
                    modifier = Modifier
                        .semantics { contentDescription = "Pitch Slider" },
                    value = pitch.floatValue,
                    onValueChange = {
                        pitch.floatValue = df.format(it).toFloat()
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    onValueChangeFinished = {
                        musicViewModel.pitch.floatValue = pitch.floatValue
                        val bundle = Bundle()
                        bundle.putFloat("pitch", musicViewModel.pitch.floatValue)
                        musicViewModel.mediaBrowser?.sendCustomAction(
                            ACTION_CHANGE_PITCH,
                            bundle,
                            null
                        )
                    },
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Speed" + (speed.floatValue).toString(),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedButton(
                        onClick = {
                            musicViewModel.speed.floatValue = 1f
                            speed.floatValue = 1f
                            musicViewModel.mediaController?.transportControls?.setPlaybackSpeed(
                                speed.floatValue
                            )
                        },
                    ) {
                        Text(text = "Reset", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Slider(
                    modifier = Modifier
                        .semantics { contentDescription = "Speed Slider" },
                    value = speed.floatValue,
                    onValueChange = {
                        speed.floatValue = df.format(it).toFloat()
                    },
                    valueRange = 0.5f..2f,
                    steps = 15,
                    onValueChangeFinished = {
                        musicViewModel.speed.floatValue = speed.floatValue
                        musicViewModel.mediaController?.transportControls?.setPlaybackSpeed(
                            speed.floatValue
                        )
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
                        Text(text = "Echo", color = MaterialTheme.colorScheme.onBackground)
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
                                bundle.putBoolean("enable", it)
                                musicViewModel.mediaBrowser
                                    ?.sendCustomAction(
                                        ACTION_ECHO_ENABLE, bundle, null
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
                    text = "Delay: " + (delayTime.floatValue).toString() + "seconds",
                    color = MaterialTheme.colorScheme.onBackground
                )
                Slider(
                    modifier = Modifier
                        .semantics { contentDescription = "Localized Description" },
                    value = delayTime.floatValue,
                    onValueChange = {
                        delayTime.floatValue = df.format(it).toFloat()
                    },
                    enabled = musicViewModel.enableEcho.value,
                    valueRange = 0.1f..2f,
                    steps = 20,
                    onValueChangeFinished = {
                        musicViewModel.delayTime.floatValue = delayTime.floatValue
                        val bundle = Bundle()
                        bundle.putFloat("delay", musicViewModel.delayTime.floatValue)
                        musicViewModel.mediaBrowser?.sendCustomAction(
                            ACTION_ECHO_DELAY,
                            bundle,
                            null
                        )
                    },
                )
                Text(
                    text = "Decay: " + (decay.floatValue).toString(),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Slider(
                    modifier = Modifier
                        .semantics { contentDescription = "Localized Description" },
                    value = decay.floatValue,
                    onValueChange = {
                        decay.floatValue = df.format(it).toFloat()
                    },
                    valueRange = 0f..1f,
                    steps = 10,
                    enabled = musicViewModel.enableEcho.value,
                    onValueChangeFinished = {
                        musicViewModel.decay.floatValue = decay.floatValue
                        val bundle = Bundle()
                        bundle.putFloat("decay", musicViewModel.decay.floatValue)
                        musicViewModel.mediaBrowser?.sendCustomAction(
                            ACTION_ECHO_DECAY,
                            bundle,
                            null
                        )
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
                    modifier = Modifier
                        .absolutePadding(
                            left = 0.dp,
                            top = 0.dp,
                            right = 0.dp,
                            bottom = 5.dp
                        )
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Equalizer", color = MaterialTheme.colorScheme.onBackground)
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(2.dp)
                        )
                        Switch(
                            modifier = Modifier.clip(MaterialTheme.shapes.small),
                            checked = musicViewModel.enableEqualizer.value,
                            onCheckedChange = {
                                musicViewModel.enableEqualizer.value = it
                                val bundle = Bundle()
                                bundle.putBoolean("enable", it)
                                musicViewModel.mediaBrowser
                                    ?.sendCustomAction(
                                        ACTION_DSP_ENABLE, bundle, null
                                    )
                            },
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(2.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            enabled = musicViewModel.enableEqualizer.value,
                            onClick = {
                                musicViewModel.mediaBrowser?.sendCustomAction(
                                    ACTION_DSP_BAND_FLATTEN,
                                    Bundle().apply {
                                        putInt("value", 0)
                                    },
                                    object : MediaBrowserCompat.CustomActionCallback() {
                                        override fun onResult(
                                            action: String?,
                                            extras: Bundle?,
                                            resultData: Bundle?
                                        ) {
                                            super.onResult(action, extras, resultData)
                                            if (ACTION_DSP_BAND_FLATTEN == action) {
                                                if (resultData?.getBoolean("result") == true) {
                                                    bands.forEach {
                                                        it.value = 0
                                                    }
                                                    tempBandValue.forEach {
                                                        it.floatValue = 0f
                                                    }
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "flatten failed",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            },
                        ) {
                            Text(text = "Flatten", color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
                LazyRow {
                    items(Utils.kThirdOct.size) { index ->
                        val band = bands[index]
                        Column {
                            Text(
                                text = " ${band.name} ",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Slider(
                                modifier = Modifier
                                    .graphicsLayer {
                                        rotationZ = 270f
                                        transformOrigin = TransformOrigin(0f, 0f)
                                    }
                                    .layout { measurable, constraints ->
                                        val placeable = measurable.measure(
                                            Constraints(
                                                minWidth = constraints.minHeight,
                                                maxWidth = constraints.maxHeight,
                                                minHeight = constraints.minWidth,
                                                maxHeight = constraints.maxHeight,
                                            )
                                        )
                                        layout(placeable.height, placeable.width) {
                                            placeable.place(-placeable.width, 0)
                                        }
                                    }
                                    .width(220.dp)
                                    .height(60.dp),
                                enabled = musicViewModel.enableEqualizer.value,
                                value = tempBandValue[index].floatValue,
                                onValueChange = {
                                    tempBandValue[index].floatValue = it
                                },
                                valueRange = minEQLevel.toFloat()..maxEQLevel.toFloat(),
                                steps = 21,
                                onValueChangeFinished = {
                                    band.value = tempBandValue[index].floatValue.roundToInt()
                                    musicViewModel.mediaBrowser?.sendCustomAction(
                                        ACTION_DSP_BAND,
                                        Bundle().apply {
                                            putInt("index", index)
                                            putInt(
                                                "value",
                                                tempBandValue[index].floatValue.roundToInt()
                                            )
                                        },
                                        null
                                    )
                                },
                            )
                            Text(
                                text = "${tempBandValue[index].floatValue.roundToInt()}db",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

        }
    }
}
