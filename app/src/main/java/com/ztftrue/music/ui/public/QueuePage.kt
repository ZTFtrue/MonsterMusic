package com.ztftrue.music.ui.public

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.QueuePlayList
import com.ztftrue.music.R
import com.ztftrue.music.play.ACTION_CLEAR_QUEUE
import com.ztftrue.music.play.ACTION_PlayLIST_CHANGE
import com.ztftrue.music.utils.OperateType
import com.ztftrue.music.utils.trackManager.PlaylistManager


/**
 * show all music of playlist
 */
@UnstableApi
@Composable
fun QueuePage(
    musicViewModel: MusicViewModel,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val musicList = remember { musicViewModel.musicQueue }
    var showDialog by remember { mutableStateOf(false) }
    var showAddPlayListDialog by remember { mutableStateOf(false) }
    var showCreatePlayListDialog by remember { mutableStateOf(false) }
    val sharedPreferences =
        context.getSharedPreferences("list_indicator_config", Context.MODE_PRIVATE)
    val showIndicator = remember {
        mutableStateOf(sharedPreferences.getBoolean("show_queue_indicator", false))
    }

    if (showDialog) {
        QueueOperateDialog(onDismiss = {
            showDialog = false
            if (it == OperateType.ClearQueue) {
                musicViewModel.mediaBrowser?.sendCustomAction(ACTION_CLEAR_QUEUE, null, null)
                musicViewModel.musicQueue.clear()
                musicList.clear()
                musicViewModel.currentPlay.value = null
                musicViewModel.playListCurrent.value = null
                musicViewModel.currentPlayQueueIndex.intValue = 0
                musicViewModel.currentCaptionList.clear()
            } else if (it == OperateType.SaveQueueToPlayList) {
                showAddPlayListDialog = true
            } else if (it == OperateType.QueueSwipeSort) {

            }
        })
    }
    if (showAddPlayListDialog) {
        AddMusicToPlayListDialog(musicViewModel, null) { playListId ->
            showAddPlayListDialog = false
            if (playListId != null) {
                if (playListId == -1L) {
                    showCreatePlayListDialog = true
                } else {
                    val ids = ArrayList<Long>(musicList.size)
                    musicList.forEach {
                        ids.add(it.id)
                    }
                    PlaylistManager.addMusicsToPlaylist(context, playListId, ids)
                    musicViewModel.mediaBrowser?.sendCustomAction(
                        ACTION_PlayLIST_CHANGE, null, null
                    )
                }
            }
        }
    }
    if (showCreatePlayListDialog) {
        CreatePlayListDialog(musicViewModel, onDismiss = { playListName ->
            showCreatePlayListDialog = false
            if (!playListName.isNullOrEmpty()) {
                val ids = ArrayList<Long>(musicList.size)
                musicList.forEach {
                    ids.add(it.id)
                }
                val idPlayList = PlaylistManager.createPlaylist(context, playListName)
                if (idPlayList != -1L) {
                    PlaylistManager.addMusicsToPlaylist(context, idPlayList, ids)
                    musicViewModel.mediaBrowser?.sendCustomAction(
                        ACTION_PlayLIST_CHANGE, null, null
                    )
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.create_failed),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        })
    }
    Scaffold(
        modifier = Modifier.padding(all = 0.dp),
        topBar = {
            TopBar(navController, musicViewModel, content = {
                IconButton(
                    modifier = Modifier.width(50.dp), onClick = {
                        showDialog = true
                    }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Operate",
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            })
        },
        bottomBar = { Bottom(musicViewModel, navController) },
        floatingActionButton = {},
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                TracksListView(
                    modifier = Modifier
                        .fillMaxSize(),
                    musicViewModel, QueuePlayList, musicList, showIndicator
                )
            }

        },
    )
}

@Composable
fun QueueOperateDialog(
    onDismiss: (value: OperateType) -> Unit
) {
    val onConfirmation = ({
        onDismiss(OperateType.No)
    })

    val color = MaterialTheme.colorScheme.onBackground
    Dialog(
        onDismissRequest = onConfirmation,
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
                    color = MaterialTheme.colorScheme.onBackground,
                    text = stringResource(R.string.operate_current_queue), modifier = Modifier
                        .padding(2.dp)
                )
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(1) {
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
                                    onDismiss(OperateType.SaveQueueToPlayList)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(R.string.save_current_queue_to_playlist),
                                Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
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
                                    onDismiss(
                                        OperateType.ClearQueue
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(R.string.clear_current_queue),
                                Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = {
                            onConfirmation()
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    )
}

