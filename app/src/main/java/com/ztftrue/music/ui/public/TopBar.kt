package com.ztftrue.music.ui.public

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.text.isDigitsOnly
import androidx.navigation.NavHostController
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.play.ACTION_SET_SLEEP_TIME
import com.ztftrue.music.play.ACTION_Volume_CHANGE
import com.ztftrue.music.ui.play.toPx
import com.ztftrue.music.utils.CustomSlider
import com.ztftrue.music.utils.Utils
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    navController: NavHostController,
    musicViewModel: MusicViewModel,
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var popupVolumeWindow by remember { mutableStateOf(false) }
    val timerIcon: ImageVector = if (musicViewModel.remainTime.longValue == 0L) {
        Icons.Outlined.Alarm
    } else {
        Icons.Outlined.Snooze
    }
    val volumeIcon: ImageVector = if (musicViewModel.volume.intValue == 0) {
        Icons.AutoMirrored.Outlined.VolumeOff
    } else {
        Icons.AutoMirrored.Outlined.VolumeUp
    }
    if (showDialog) {
        SleepTimeDialog(musicViewModel, onDismiss = {
            showDialog = false
        })
    }
    if (popupVolumeWindow) {
        Popup(
            // on below line we are adding
            // alignment and properties.
            alignment = Alignment.TopCenter,
            properties = PopupProperties(),
            offset = IntOffset(
                0.dp.toPx(context),
                40.dp.toPx(context)
            ),
            onDismissRequest = {
                popupVolumeWindow = false
            }
        ) {
            val configuration = LocalConfiguration.current
            Column(
                modifier = Modifier
                    .width(
                        (configuration.screenWidthDp - 20.dp.toPx(
                            context
                        )).dp
                    )
                    .padding(top = 5.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CustomSlider(
                                modifier = Modifier
                                    .semantics { contentDescription = "Slider" },
                                value = musicViewModel.volume.intValue.toFloat(),
                                onValueChange = {
                                    musicViewModel.volume.intValue =
                                        it.roundToLong().toInt()
                                },
                                valueRange = 0f..100f,
                                steps = 100,
                                onValueChangeFinished = {
                                    val bundle = Bundle()
                                    bundle.putInt(
                                        "volume",
                                        musicViewModel.volume.intValue
                                    )
                                    musicViewModel.mediaBrowser?.sendCustomAction(
                                        ACTION_Volume_CHANGE,
                                        bundle,
                                        object : MediaBrowserCompat.CustomActionCallback() {
                                            override fun onResult(
                                                action: String?,
                                                extras: Bundle?,
                                                resultData: Bundle?
                                            ) {
                                                super.onResult(action, extras, resultData)
//
                                            }
                                        }
                                    )
                                },
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp, end = 0.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = buildString {
                                        append(musicViewModel.volume.intValue.toString())
                                        append("%")
                                    },
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                IconButton(onClick = { popupVolumeWindow = false }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove folder",
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    TopAppBar(
        navigationIcon = {
            BackButton(navController)
        },
        title = {},
        actions = {
            IconButton(
                modifier = Modifier
                    .semantics {
                        contentDescription = "Queue Page"
                    },
                onClick = {
                    navController.navigate(
                        Router.QueuePage.route
                    ) {
                        popUpTo(Router.MainView.route) {
                            // Inclusive means the start destination is also popped
                            inclusive = false
                        }
                    }
                }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                    contentDescription = "Queue Page",
                    modifier = Modifier
                        .size(30.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(
                modifier = Modifier
                    .semantics {
                        contentDescription = "Adjust Volume"
                    },
                onClick = {
                    popupVolumeWindow = true
                }) {
                Icon(
                    imageVector = volumeIcon,
                    contentDescription = "Operate More, will open popup",
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = {
                showDialog = true
            }) {
                Icon(
                    imageVector = timerIcon,
                    contentDescription = "Operate More, will open dialog",
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(
                modifier = Modifier
                    .size(50.dp)
                    .semantics {
                        contentDescription = context.getString(R.string.search)
                    },
                onClick = {
                    navController.navigate(
                        Router.SearchPage.route
                    )
                }) {
                Icon(
                    Icons.Filled.Search,
                    modifier = Modifier.size(30.dp),
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            content()
        }
    )
}

@Composable
fun SleepTimeDialog(musicViewModel: MusicViewModel, onDismiss: () -> Unit) {
    var inputMinutes by remember { mutableStateOf("") }
    fun onConfirmation(time: Long) {
        musicViewModel.sleepTime.longValue = time
        val bundle = Bundle()
        bundle.putLong("time", time)
        bundle.putBoolean("play_completed", musicViewModel.playCompleted.value)
        musicViewModel.mediaBrowser?.sendCustomAction(ACTION_SET_SLEEP_TIME, bundle, null)
        onDismiss()
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true, dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.sleep_timer), modifier = Modifier
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )
                val sleepT: IntArray = intArrayOf(5, 10, 15, 30)
                if (musicViewModel.remainTime.longValue > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.running,
                                Utils.formatTime(musicViewModel.remainTime.longValue)
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(
                            onClick = { onConfirmation(0L) },
                            modifier = Modifier.padding(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.stop),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(color = MaterialTheme.colorScheme.onBackground)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = musicViewModel.playCompleted.value, onCheckedChange = {
                        musicViewModel.playCompleted.value = it
                    })
                    Text(
                        text = stringResource(R.string.play_completed_last_song),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(10.dp),
                ) {
                    items(sleepT.size) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ElevatedButton(
                                onClick = { onConfirmation((sleepT[item] * 60 * 1000).toLong()) },
                                modifier = Modifier
                            ) {
                                Text(
                                    text = stringResource(R.string.minutes, sleepT[item]),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
                Row {
                    TextField(
                        value = inputMinutes,
                        onValueChange = {
                            if (it.isNotEmpty()) {
                                if (it.isDigitsOnly() && !it.contains(".") && it.length < 6 && (it.toLong() > 0)) {
                                    inputMinutes = it
                                }
                            } else {
                                inputMinutes = ""
                            }

                        },
                        label = {
                            Text(
                                text = stringResource(R.string.enter_minutes),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Number
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
//                            startTimer(inputMinutes.toInt())
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            errorTextColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            unfocusedTextColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            errorCursorColor = MaterialTheme.colorScheme.error,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            errorIndicatorColor = MaterialTheme.colorScheme.error,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            ),
                            errorLeadingIconColor = MaterialTheme.colorScheme.error,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            ),
                            errorTrailingIconColor = MaterialTheme.colorScheme.error,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            errorLabelColor = MaterialTheme.colorScheme.error,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            )
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        suffix = {
                            Text(
                                text = stringResource(id = R.string.minutes, ""),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(
                            stringResource(id = R.string.cancel),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    TextButton(
                        onClick = {
                            if (inputMinutes.isNotEmpty()) {
                                onConfirmation(inputMinutes.toLong() * 60 * 1000)
                            }
                        },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(
                            stringResource(id = R.string.confirm),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

            }

        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    navController: NavHostController,
    text: String
) {
    TopAppBar(
        navigationIcon = { BackButton(navController) },
        title = {
            Text(text = text, color = MaterialTheme.colorScheme.onBackground)
        })
}