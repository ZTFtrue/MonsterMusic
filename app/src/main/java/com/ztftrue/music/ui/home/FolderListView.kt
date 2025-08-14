package com.ztftrue.music.ui.home

import android.content.Context
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import androidx.navigation.NavHostController
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.play.CustomMetadataKeys
import com.ztftrue.music.ui.public.AddMusicToPlayListDialog
import com.ztftrue.music.ui.public.CreatePlayListDialog
import com.ztftrue.music.utils.OperateType
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.enumToStringForPlayListType
import com.ztftrue.music.utils.model.FolderList


@Composable
fun FolderListView(
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel,
    navController: NavHostController,
    type: PlayListType = PlayListType.Folders
) {

    val folderList = remember { mutableStateListOf<FolderList>() }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    LaunchedEffect(Unit, musicViewModel.refreshFolder.value ) {
        folderList.clear()
        val futureResult: ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>? =
            musicViewModel.browser?.getChildren("folders_root", 0, Integer.MAX_VALUE, null)
        futureResult?.addListener({
            try {
                val result: LibraryResult<ImmutableList<MediaItem>>? = futureResult.get()
                if (result == null || result.resultCode != LibraryResult.RESULT_SUCCESS) {
                    return@addListener
                }
                val albumMediaItems: List<MediaItem> = result.value ?: listOf()
                albumMediaItems.forEach { mediaItem ->
                    val album = FolderList(
                        id = mediaItem.mediaId.toLong(),
                        name = mediaItem.mediaMetadata.title.toString(),
                        trackNumber = mediaItem.mediaMetadata.totalTrackCount ?: 0,
                        isShow = mediaItem.mediaMetadata.extras?.getBoolean(
                            CustomMetadataKeys.FOLDER_IS_SHOW,
                            true
                        ) ?: true
                    )
                    folderList.add(album)
                }
            } catch (e: Exception) {
                // 处理在获取结果过程中可能发生的异常 (如 ExecutionException)
                Log.e("Client", "Failed to toggle favorite status", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Column {
        Text(
            text = stringResource(R.string.warning_for_tracks_tab),
            modifier = Modifier.padding(10.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyColumn(
            state = listState, modifier = modifier.fillMaxSize()
        ) {
            items(folderList.size) { index ->
                val item = folderList[index]
                FolderItemView(
                    item,
                    musicViewModel,
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth(),
                    navController
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    thickness = 1.2.dp
                )
            }
        }
    }

}


@Composable
fun FolderItemView(
    item: FolderList,
    musicViewModel: MusicViewModel,
    modifier: Modifier,
    navController: NavHostController,
    type: PlayListType = PlayListType.Folders,
) {
    val context = LocalContext.current
    var showOperateDialog by remember { mutableStateOf(false) }
    var showAddPlayListDialog by remember { mutableStateOf(false) }
    var showCreatePlayListDialog by remember { mutableStateOf(false) }
    if (showOperateDialog) {
        FolderListOperateDialog(
            musicViewModel,
            playList = item,
            onDismiss = {
                showOperateDialog = false
                when (it) {
                    OperateType.AddToPlaylist -> {
                        showAddPlayListDialog = true
                    }

                    OperateType.IgnoreFolder -> {
                        val sharedPreferences =
                            context.getSharedPreferences("scan_config", Context.MODE_PRIVATE)
                        val ignoreFolders = sharedPreferences.getString("ignore_folders", "")
                        val newIgnoreFolders: String = if (ignoreFolders.isNullOrEmpty()) {
                            item.id.toString()
                        } else {
                            "$ignoreFolders,${item.id}"
                        }
                        sharedPreferences.edit {
                            putString("ignore_folders", newIgnoreFolders)
                        }
                        Toast.makeText(
                            context,
                            context.getString(R.string.ignored_this_folder_please_restart_the_app_to_take_effect),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {
                        Utils.operateDialogDeal(it, item, musicViewModel)
                    }
                }
            },
        )
    }
    if (showAddPlayListDialog) {
        AddMusicToPlayListDialog(musicViewModel, null, onDismiss = {playListId,removeDuplicate ->
            showAddPlayListDialog = false
            if (playListId != null) {
                if (playListId == -1L) {
                    showCreatePlayListDialog = true
                } else {
                    Utils.addTracksToPlayList(playListId, context, type, item.id, musicViewModel,removeDuplicate)
                }
            }
        })
    }
    if (showCreatePlayListDialog) {
        CreatePlayListDialog(onDismiss = {
            showCreatePlayListDialog = false
            if (it != null) {
                Utils.createPlayListAddTracks(it, context, type, item.id, musicViewModel,false)
            }
        })
    }
    val number = item.trackNumber
    Row(
        modifier = Modifier
            .combinedClickable(
                onLongClick = {
                    showOperateDialog = true
                }
            ) {
                navController.navigate(
                    Router.PlayListView.withArgs("id" to "${item.id}", "itemType" to enumToStringForPlayListType(type)),
                    navigatorExtras = ListParameter(item.id, type)
                )
            }, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(
                    if (item.id == musicViewModel.playListCurrent.value?.id && item.type == musicViewModel.playListCurrent.value?.type) {
                        R.drawable.pause
                    } else {
                        R.drawable.play
                    }
                ),
                contentDescription = if (musicViewModel.playStatus.value) {
                    val s = "pause"
                    s
                } else {
                    "play"
                },
                modifier = Modifier
                    .size(30.dp)
                    .padding(5.dp),
                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
            )
            Column(modifier = Modifier.fillMaxWidth(0.9f)) {
                Text(
                    text = item.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.horizontalScroll(rememberScrollState(0)),
                )
                Text(
                    text = stringResource(R.string.song, number, if (number <= 1L) "" else stringResource(id = R.string.s)),
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
                    contentDescription = "Operate More, will open dialog",
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }


    }
}

@Composable
fun FolderListOperateDialog(
    musicViewModel: MusicViewModel,
    playList: FolderList,
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
                    text = stringResource(R.string.operate), modifier = Modifier
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
                                    text = stringResource(R.string.add_to_queue),
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
                                        musicViewModel.playListCurrent.value = null
                                        onDismiss(OperateType.PlayNext)
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = stringResource(R.string.play_next),
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
                                text = stringResource(R.string.add_to_playlist),
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
                                    onDismiss(OperateType.IgnoreFolder)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(R.string.ignore_this_folder),
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
