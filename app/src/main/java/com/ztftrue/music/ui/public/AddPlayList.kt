package com.ztftrue.music.ui.public

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.MusicPlayList
import com.ztftrue.music.utils.PlayListType

@Composable
fun CreatePlayListDialog(
    musicViewModel: MusicViewModel,
    onDismiss: (value: String?) -> Unit
) {
    var playListName by remember { mutableStateOf("") }

    val onConfirmation = ({
        onDismiss(playListName)
    })
    val onDis = ({
        onDismiss(null)
    })
    val color = MaterialTheme.colorScheme.onBackground

    Dialog(
        onDismissRequest = onDis,
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
                    text = "Create PlayList",
                    modifier = Modifier
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.primary)
                )
                Row {
                    TextField(
                        value = playListName,
                        onValueChange = {
                            playListName = it
                        },
                        label = {
                            Text(
                                "Enter name",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
//                            startTimer(inputMinutes.toInt())
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        suffix = {
//                            Text("")
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            disabledTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedSupportingTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedSupportingTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(50.dp)
                            .padding(0.dp)
                            .drawBehind {
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height - 1.dp.toPx()),
                                    end = Offset(size.width, size.height - 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .clickable {
                                onDis()
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Cancel",
                            Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxWidth(0.1f)
                            .height(50.dp)
                            .background(color = MaterialTheme.colorScheme.secondary)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(0.dp)
                            .drawBehind {
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height - 1.dp.toPx()),
                                    end = Offset(size.width, size.height - 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .clickable {
                                onConfirmation()
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Ok",
                            Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

            }
        }
    )
}


@Composable
fun RenamePlayListDialog(
    musicViewModel: MusicViewModel,
    onDismiss: (value: String?) -> Unit
) {
    var playListName by remember { mutableStateOf("") }

    val onConfirmation = ({
        onDismiss(playListName)
    })
    val onDis = ({
        onDismiss(null)
    })
    val color = MaterialTheme.colorScheme.onBackground

    Dialog(
        onDismissRequest = onDis,
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
                    text = "Rename PlayList name",
                    modifier = Modifier
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )
                Row {
                    TextField(
                        value = playListName,
                        onValueChange = {
                            playListName = it
                        },
                        label = { Text("Enter name") },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
//                            startTimer(inputMinutes.toInt())
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        suffix = {
                        },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(50.dp)
                            .padding(0.dp)
                            .drawBehind {
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height - 1.dp.toPx()),
                                    end = Offset(size.width, size.height - 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .clickable {
                                onDis()
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Cancel",
                            Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxWidth(0.1f)
                            .height(50.dp)
                            .background(color = MaterialTheme.colorScheme.onBackground)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(0.dp)
                            .drawBehind {
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height - 1.dp.toPx()),
                                    end = Offset(size.width, size.height - 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .clickable {
                                onConfirmation()
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Ok",
                            Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

            }
        }
    )
}

@Composable
fun DeleteTip(
    musicViewModel: MusicViewModel,
    titleTip: String,
    onDismiss: (value: Boolean) -> Unit
) {

    val onConfirmation = ({
        onDismiss(true)
    })
    val onDis = ({
        onDismiss(false)
    })
    val color = MaterialTheme.colorScheme.onBackground

    Dialog(
        onDismissRequest = onDis,
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
                    text = "Confirm to delete $titleTip?",
                    modifier = Modifier
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(50.dp)
                            .padding(0.dp)
                            .drawBehind {
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height - 1.dp.toPx()),
                                    end = Offset(size.width, size.height - 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .clickable {
                                onDis()
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Cancel", Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxWidth(0.1f)
                            .height(50.dp)
                            .background(color = MaterialTheme.colorScheme.onBackground)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(0.dp)
                            .drawBehind {
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height - 1.dp.toPx()),
                                    end = Offset(size.width, size.height - 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .clickable {
                                onConfirmation()
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Confirm",
                            Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

            }
        }
    )
}

@Composable
fun AddMusicToPlayListDialog(
    musicViewModel: MusicViewModel,
    musicItem: MusicItem? = null,
    onDismiss: (playListId: Long?) -> Unit
) {

    fun onConfirmation(id: Long) {
        onDismiss(id)
    }

    val onDis = {
        onDismiss(null)
    }
    val color = MaterialTheme.colorScheme.onBackground

    val playList = remember { mutableStateListOf<MusicPlayList>() }
    LaunchedEffect(Unit) {
        musicViewModel.mediaBrowser?.sendCustomAction(
            PlayListType.PlayLists.name,
            null,
            object : MediaBrowserCompat.CustomActionCallback() {
                override fun onProgressUpdate(
                    action: String?, extras: Bundle?, data: Bundle?
                ) {
                    super.onProgressUpdate(action, extras, data)
                }

                override fun onResult(
                    action: String?, extras: Bundle?, resultData: Bundle?
                ) {
                    super.onResult(action, extras, resultData)
                    if (action == PlayListType.PlayLists.name) {
                        resultData?.getParcelableArrayList<MusicPlayList>("list")
                            ?.also { list ->
                                playList.addAll(list)
                            }
                    }
                }

                override fun onError(action: String?, extras: Bundle?, data: Bundle?) {
                    super.onError(action, extras, data)
                }
            })
    }
    Dialog(
        onDismissRequest = onDis,
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
                    text = "Add ${musicItem?.name} Music To PlayList", modifier = Modifier
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )
                Row(
                    Modifier
                        .padding(0.dp)
                        .height(50.dp)
                        .clickable {
                            onConfirmation(-1)
                        }, verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Contact profile picture",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape),
                    )
                    Text(
                        text = "Add New PlayList",
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.horizontalScroll(rememberScrollState(0))
                    )
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(playList.size) { index ->
                        val item = playList[index]
                        Row(
                            Modifier
                                .padding(0.dp)
                                .clickable {
                                    onConfirmation(item.id)
                                }, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = item.name,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.horizontalScroll(rememberScrollState(0))
                                )
                            }
                        }
                        Divider(
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            thickness = 1.2.dp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(0.dp)
                        .drawBehind {
                            drawLine(
                                color = color,
                                start = Offset(0f, size.height - 1.dp.toPx()),
                                end = Offset(size.width, size.height - 1.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .clickable {
                            onDis()
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = "Cancel", Modifier.padding(start = 10.dp))
                }
            }
        }
    )
}