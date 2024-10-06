package com.ztftrue.music.ui.home

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.play.ACTION_PlayLIST_CHANGE
import com.ztftrue.music.ui.public.AddMusicToPlayListDialog
import com.ztftrue.music.ui.public.CreatePlayListDialog
import com.ztftrue.music.ui.public.DeleteTip
import com.ztftrue.music.ui.public.RenamePlayListDialog
import com.ztftrue.music.utils.OperateType
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.operateDialogDeal
import com.ztftrue.music.utils.enumToStringForPlayListType
import com.ztftrue.music.utils.model.MusicPlayList
import com.ztftrue.music.utils.trackManager.PlaylistManager


@Composable
fun PlayListView(
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel,
    navController: NavHostController,
) {
    val listState = rememberLazyListState()
    val playList = remember { mutableStateListOf<MusicPlayList>() }
    LaunchedEffect(musicViewModel.refreshPlayList.value) {
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
                    playList.clear()
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
    if (playList.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(id = R.string.there_is_no_any_album_in_here), Modifier.padding(start = 10.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        return
    }
    LazyColumn(
        state = listState, modifier = modifier.fillMaxSize()
    ) {
        items(playList.size) { index ->
            val item = playList[index]
            PlayListItemView(
                item,
                musicViewModel,
                navController,
                PlayListType.PlayLists,
                playList,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.inverseOnSurface, thickness = 1.2.dp)
        }
    }
}

data class ListParameter(val id: Long, val type: PlayListType) : Navigator.Extras

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayListItemView(
    item: MusicPlayList,
    musicViewModel: MusicViewModel,
    navController: NavHostController,
    type: PlayListType,
    playList: SnapshotStateList<MusicPlayList>,
) {
    val context = LocalContext.current
    val number = item.trackNumber
    var showOperateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteTip by remember { mutableStateOf(false) }
    var showAddPlayListDialog by remember { mutableStateOf(false) }
    var showCreatePlayListDialog by remember { mutableStateOf(false) }

    if (showOperateDialog) {
        PlayListOperateDialog(
            musicViewModel,
            playList = item,
            onDismiss = {
                showOperateDialog = false
                when (it) {
                    OperateType.AddToPlaylist -> {
                        showAddPlayListDialog = true
                    }

                    OperateType.RenamePlayList -> {
                        showRenameDialog = true
                    }

                    OperateType.DeletePlayList -> {
                        showDeleteTip = true
                    }

                    else -> {
                        operateDialogDeal(it, item, musicViewModel)
                    }
                }
            },
        )
    }
    if (showAddPlayListDialog) {
        AddMusicToPlayListDialog(musicViewModel, null, onDismiss = {
            showAddPlayListDialog = false
            if (it != null) {
                if (it == -1L) {
                    showCreatePlayListDialog = true
                } else {
                    Utils.addTracksToPlayList(it, context, type, item.id, musicViewModel)
                }
            }
        })
    }
    if (showCreatePlayListDialog) {
        CreatePlayListDialog(onDismiss = {
            showCreatePlayListDialog = false
            if (it != null) {
                Utils.createPlayListAddTracks(it, context, type, item.id, musicViewModel)
            }
        })
    }
    if (showRenameDialog) {
        RenamePlayListDialog(onDismiss = {
            showRenameDialog = false
            if (!it.isNullOrEmpty()) {
                if (PlaylistManager.renamePlaylist(context, item.id, it)) {
                    musicViewModel.mediaBrowser?.sendCustomAction(
                        ACTION_PlayLIST_CHANGE,
                        null,
                        object : MediaBrowserCompat.CustomActionCallback() {
                            override fun onResult(
                                action: String?,
                                extras: Bundle?,
                                resultData: Bundle?
                            ) {
                                super.onResult(action, extras, resultData)
                                musicViewModel.refreshPlayList.value = !musicViewModel.refreshPlayList.value
                            }
                        }
                    )

                }
            }
        })
    }
    if (showDeleteTip) {
        DeleteTip(item.name, onDismiss = {
            showDeleteTip = false
            if (it) {
                if (PlaylistManager.deletePlaylist(context, item.id)) {
                    musicViewModel.mediaBrowser?.sendCustomAction(
                        ACTION_PlayLIST_CHANGE,
                        null,
                        object : MediaBrowserCompat.CustomActionCallback() {
                            override fun onResult(
                                action: String?,
                                extras: Bundle?,
                                resultData: Bundle?
                            ) {
                                super.onResult(action, extras, resultData)
                                musicViewModel.refreshPlayList.value = !musicViewModel.refreshPlayList.value
                            }
                        }
                    )
                }
            }
        })
    }

    Row(
        modifier = Modifier
            .padding(0.dp)
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = {
                    showOperateDialog = true
                },
                onDoubleClick = {

                }
            ) {
                navController.navigate(
                    Router.PlayListView.withArgs("${item.id}", enumToStringForPlayListType(type)),
                    navigatorExtras = ListParameter(item.id, type)
                )
            }, verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(
                if (item.id == musicViewModel.playListCurrent.value?.id && item.type == musicViewModel.playListCurrent.value?.type) {
                    R.drawable.pause
                } else {
                    R.drawable.play
                }
            ),
            contentDescription = if (musicViewModel.playStatus.value) {
                stringResource(R.string.pause)
            } else {
                stringResource(R.string.play)
            },
            modifier = Modifier
                .size(40.dp),
            colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
        )
        Column(modifier = Modifier.fillMaxWidth(0.9f)) {
            Text(
                text = item.name,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.horizontalScroll(rememberScrollState(0))
            )
            Text(
                text = stringResource(R.string.song, number, if (number <= 1L) "" else "s"),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(
            modifier = Modifier
                .width(50.dp)
                .height(40.dp), onClick = {
                showOperateDialog = true
            }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(id = R.string.operate_more_will_open_dialog),
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
//        Image(
//            painter = painterResource(
//                R.drawable.ic_queue
//            ),
//            contentDescription = if (musicViewModel.playStatus.value) {
//                "pause"
//            } else {
//                "play"
//            },
//            modifier = Modifier
//                .size(40.dp),
//            colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
//        )
    }
}

@Composable
fun PlayListOperateDialog(
    musicViewModel: MusicViewModel,
    playList: MusicPlayList,
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
                    text = stringResource(id = R.string.operate), modifier = Modifier
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(1) {
                        if (!(musicViewModel.playListCurrent.value?.type?.equals(playList.type) == true
                                    && musicViewModel.playListCurrent.value?.id?.equals(playList.id) == true)
                        ) {
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
                                        musicViewModel.playListCurrent.value = null
                                        onDismiss(OperateType.AddToQueue)
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = stringResource(id = R.string.add_to_queue),
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
                                        musicViewModel.playListCurrent.value = null
                                        onDismiss(OperateType.PlayNext)
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text =stringResource(id = R.string.play_next),
                                    Modifier.padding(start = 10.dp),
                                    color = MaterialTheme.colorScheme.onBackground
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
                                    onDismiss(OperateType.AddToPlaylist)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(id = R.string.add_to_playlist),
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
                                    onDismiss(OperateType.RenamePlayList)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(R.string.rename_playlist),
                                modifier = Modifier.padding(start = 10.dp),
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
                                    onDismiss(OperateType.DeletePlayList)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(R.string.delete_playlist),
                                modifier = Modifier.padding(start = 10.dp),
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
                        Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    )
}
