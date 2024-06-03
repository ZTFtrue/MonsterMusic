package com.ztftrue.music.ui.play

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.ACTION_CHANGE_PITCH
import com.ztftrue.music.play.ACTION_DSP_BAND
import com.ztftrue.music.play.ACTION_DSP_BANDS_SET
import com.ztftrue.music.play.ACTION_DSP_BAND_FLATTEN
import com.ztftrue.music.play.ACTION_DSP_ENABLE
import com.ztftrue.music.play.ACTION_ECHO_DECAY
import com.ztftrue.music.play.ACTION_ECHO_DELAY
import com.ztftrue.music.play.ACTION_ECHO_ENABLE
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.equalizerMax
import com.ztftrue.music.utils.Utils.equalizerMin
import com.ztftrue.music.utils.model.EqualizerBand
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
    var selectedIndex by remember {
        mutableStateOf(Utils.custom)
    }
    LaunchedEffect(key1 = Unit) {
        selectedIndex = context.getSharedPreferences(
            "SelectedPreset",
            Context.MODE_PRIVATE
        ).getString("SelectedPreset", Utils.custom) ?: Utils.custom
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
                            musicViewModel.mediaBrowser?.sendCustomAction(
                                ACTION_CHANGE_PITCH,
                                bundle,
                                null
                            )
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.reset),
                            color = MaterialTheme.colorScheme.onBackground
                        )
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
                        text = stringResource(R.string.speed) + (speed.floatValue).toString(),
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
                        Text(
                            text = stringResource(R.string.reset),
                            color = MaterialTheme.colorScheme.onBackground
                        )
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
                    text = stringResource(R.string.delay) + (delayTime.floatValue) + stringResource(
                        R.string.seconds
                    ),
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
                    text = stringResource(R.string.decay) + (decay.floatValue).toString(),
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
                        Text(
                            text = stringResource(R.string.equalizer),
                            color = MaterialTheme.colorScheme.onBackground
                        )
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
                                                        context.getString(R.string.flatten_failed),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.flatten),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                LazyRow {
                    items(bands.size, key = { it }) { index ->
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
                                    selectedIndex = Utils.custom
                                    context.getSharedPreferences(
                                        "SelectedPreset",
                                        Context.MODE_PRIVATE
                                    ).edit().putString("SelectedPreset", Utils.custom).apply()
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
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(top = 5.dp, bottom = 5.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    val offset = remember { mutableIntStateOf(200) }
                    var expanded by remember { mutableStateOf(false) }
                    BackHandler(enabled = expanded) {
                        if (expanded) {
                            expanded = false
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp)
                            .height(60.dp)
                            .clickable { expanded = !expanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.preset),
                            Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (selectedIndex == Utils.custom) stringResource(
                                id = Utils.translateMap[selectedIndex] ?: R.string.app_name
                            ) else selectedIndex,
                            Modifier.padding(end = 10.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((LocalView.current.height / 2f).dp)
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer
                            ),
                        offset = DpOffset(
                            x = 0.dp,
                            y = with(LocalDensity.current) { offset.intValue.toDp() }
                        )
                    ) {
                        Utils.eqPreset.forEach { (key, value) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        key,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                },
                                onClick = {
                                    selectedIndex = key
                                    expanded = false
                                    value.forEachIndexed { i, v ->
                                        tempBandValue[i].floatValue = v.toFloat()
                                        bands[i] = EqualizerBand(bands[i].id, bands[i].name, v)
                                    }
                                    musicViewModel.mediaBrowser?.sendCustomAction(
                                        ACTION_DSP_BANDS_SET,
                                        Bundle().apply {
                                            putIntArray(
                                                "value",
                                                value
                                            )
                                        },
                                        null
                                    )
                                    context.getSharedPreferences(
                                        "SelectedPreset",
                                        Context.MODE_PRIVATE
                                    ).edit().putString("SelectedPreset", key).apply()
                                })
                        }
                    }
                }
            }

        }
    }
}
