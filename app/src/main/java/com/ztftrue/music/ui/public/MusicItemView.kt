package com.ztftrue.music.ui.public

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.SwipeVertical
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.play.ACTION_AddPlayQueue
import com.ztftrue.music.play.ACTION_PLAY_MUSIC
import com.ztftrue.music.play.ACTION_PlayLIST_CHANGE
import com.ztftrue.music.play.ACTION_RemoveFromQueue
import com.ztftrue.music.play.ACTION_TRACKS_DELETE
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.OperateType
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.TracksUtils
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.deleteTrackUpdate
import com.ztftrue.music.utils.Utils.toPx
import com.ztftrue.music.utils.enumToStringForPlayListType
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.model.MusicPlayList
import com.ztftrue.music.utils.trackManager.PlaylistManager
import com.ztftrue.music.utils.trackManager.SongsUtils
import com.ztftrue.music.utils.trackManager.TracksManager

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicItemView(
    music: MusicItem,
    index: Int,
    viewModel: MusicViewModel,
    playList: AnyListBase,
    modifier: Modifier = Modifier,
    musicList: SnapshotStateList<MusicItem>,
    selectStatus: Boolean = false,
    selectList: SnapshotStateList<MusicItem>?,
) {
    val playStatusIcon: Int =
        if (viewModel.playStatus.value && music.id == viewModel.currentPlay.value?.id) {
            R.drawable.pause
        } else {
            R.drawable.play
        }
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var showAddPlayListDialog by remember { mutableStateOf(false) }
    var showCreatePlayListDialog by remember { mutableStateOf(false) }
    var showDeleteTip by remember { mutableStateOf(false) }
    if (showDeleteTip) {
        DeleteTip(music.name, onDismiss = {
            showDeleteTip = false
            if (it) {
                if (TracksManager.removeMusicById(context, music.id)) {
                    val bundle = Bundle()
                    bundle.putLong("id", music.id)
                    viewModel.mediaBrowser?.sendCustomAction(
                        ACTION_TRACKS_DELETE,
                        bundle,
                        object : MediaBrowserCompat.CustomActionCallback() {
                            override fun onResult(
                                action: String?,
                                extras: Bundle?,
                                resultData: Bundle?
                            ) {
                                super.onResult(action, extras, resultData)
                                if (ACTION_TRACKS_DELETE == action) {
                                    deleteTrackUpdate(viewModel, resultData)
                                }
                            }
                        }
                    )
                }

            }
        })
    }
    if (showDialog) {
        OperateDialog(
            viewModel,
            music = music,
            playList,
            onDismiss = {
                showDialog = false
                when (it) {
                    OperateType.AddToQueue -> {
                        viewModel.musicQueue.add(music)
                        val bundle = Bundle()
                        bundle.putParcelable("musicItem", music)
                        viewModel.mediaBrowser?.sendCustomAction(ACTION_AddPlayQueue, bundle, null)
                    }

                    OperateType.PlayNext -> {
                        val indexAdd =
                            if (viewModel.musicQueue.isEmpty()) 0 else viewModel.currentPlayQueueIndex.intValue + 1
                        viewModel.musicQueue.add(
                            indexAdd,
                            music
                        )
                        val bundle = Bundle()
                        bundle.putParcelable("musicItem", music)
                        bundle.putInt("index", indexAdd)
                        viewModel.mediaBrowser?.sendCustomAction(ACTION_AddPlayQueue, bundle, null)
                    }

                    OperateType.AddToPlaylist -> {
                        showAddPlayListDialog = true
                    }

                    OperateType.RemoveFromPlaylist -> {
                        if (playList is MusicPlayList) {
                            if (index < musicList.size)
                                musicList.removeAt(index)
                            val playListPath =
                                PlaylistManager.resortOrRemoveTrackFromPlayList(
                                    context,
                                    playList.id,
                                    ArrayList(musicList.toList()),
                                    playList.path
                                )
                            if (!playListPath.isNullOrEmpty()) {
                                MediaScannerConnection.scanFile(
                                    context,
                                    arrayOf(playListPath),
                                    arrayOf("*/*"),
                                    object : MediaScannerConnection.MediaScannerConnectionClient {
                                        override fun onMediaScannerConnected() {}
                                        override fun onScanCompleted(path: String, uri: Uri) {
                                            viewModel.mediaBrowser?.sendCustomAction(
                                                ACTION_PlayLIST_CHANGE,
                                                null,
                                                object : MediaBrowserCompat.CustomActionCallback() {
                                                    override fun onResult(
                                                        action: String?,
                                                        extras: Bundle?,
                                                        resultData: Bundle?
                                                    ) {
                                                        super.onResult(action, extras, resultData)
                                                        viewModel.refreshPlayList.value =
                                                            !viewModel.refreshPlayList.value
                                                    }
                                                }
                                            )
                                        }
                                    })
                            }
                        }
                    }

                    OperateType.AddToFavorite -> {
                        // create playlist name is  MY favorite
                    }

                    OperateType.Artist -> {
                        viewModel.navController?.navigate(
                            Router.PlayListView.withArgs(
                             "id" to   "${music.artistId}",
                             "itemType" to   enumToStringForPlayListType(PlayListType.Artists)
                            ),
                        ) {
                            popUpTo(Router.MainView.route) {
                                // Inclusive means the start destination is also popped
                                inclusive = false
                            }
                        }
                    }

                    OperateType.Album -> {
                        viewModel.navController?.navigate(
                            Router.PlayListView.withArgs(
                               "id" to "${music.albumId}",
                              "itemType" to  enumToStringForPlayListType(PlayListType.Albums)
                            )
                        ) {
                            popUpTo(Router.MainView.route) {
                                // Inclusive means the start destination is also popped
                                inclusive = false
                            }
                        }
                    }

                    OperateType.RemoveFromQueue -> {
                        val indexM =
                            viewModel.musicQueue.indexOfFirst { musicItem -> musicItem.id == music.id }
                        if (indexM == -1) return@OperateDialog
                        val bundle = Bundle()
                        bundle.putInt("index", indexM)
                        viewModel.mediaBrowser?.sendCustomAction(
                            ACTION_RemoveFromQueue,
                            bundle,
                            object : MediaBrowserCompat.CustomActionCallback() {
                                override fun onResult(
                                    action: String?,
                                    extras: Bundle?,
                                    resultData: Bundle?
                                ) {
                                    super.onResult(action, extras, resultData)
                                    if (ACTION_RemoveFromQueue == action && resultData == null) {
                                        if (viewModel.currentPlay.value?.id == music.id) {
                                            viewModel.currentMusicCover.value = null
                                            viewModel.currentPlayQueueIndex.intValue =
                                                (index) % (viewModel.musicQueue.size + 1)
                                            viewModel.currentPlay.value =
                                                viewModel.musicQueue[viewModel.currentPlayQueueIndex.intValue]
                                        }
                                    }
                                }
                            }
                        )
                        viewModel.musicQueue.removeAt(indexM)
                    }

                    OperateType.EditMusicInfo -> {
                        viewModel.navController?.navigate(Router.EditTrackPage.withArgs("id" to "${music.id}"))
                    }

                    OperateType.DeleteFromStorage -> {
                        showDeleteTip = true
                    }

                    OperateType.No -> {

                    }

                    else -> {

                    }
                }
            },
        )
    }
    if (showAddPlayListDialog) {
        AddMusicToPlayListDialog(viewModel, music, onDismiss = { playListId, removeDuplicate ->
            showAddPlayListDialog = false
            if (playListId != null) {
                if (playListId == -1L) {
                    showCreatePlayListDialog = true
                } else {
                    val musics = ArrayList<MusicItem>()
                    musics.add(music)
                    if(PlaylistManager.addMusicsToPlaylist(
                        context,
                        playListId,
                        musics,
                        removeDuplicate
                    )){
                        SongsUtils.refreshPlaylist(viewModel)
                    }
                    if (playList.id == playListId) {
                        musicList.add(music)
                    }

                }
            }
        })
    }
    if (showCreatePlayListDialog) {
        CreatePlayListDialog(onDismiss = {
            showCreatePlayListDialog = false
            if (!it.isNullOrEmpty()) {
                Utils.createPlayListAddTrack(it, context, music, viewModel, false)
            }
        })
    }
    var offset by remember { mutableFloatStateOf(0f) }
    Row(
        modifier
            .height(80.dp)
            .graphicsLayer(
                translationY = offset,
            )
            .combinedClickable(
                onClick = {
                    // in select tracks status for add playlist
                    if (selectStatus) {
                        if (selectList?.contains(music) == true) {
                            selectList.remove(music)
                        } else {
                            selectList?.add(music)
                        }
                    } else {
                        val bundle = Bundle()
                        if (playList.type != PlayListType.Queue) {
                            viewModel.playListCurrent.value = playList
                            viewModel.musicQueue.clear()
                            viewModel.currentPlayQueueIndex.intValue = -1
                            viewModel.musicQueue.addAll(musicList)
                            bundle.putBoolean("switch_queue", true)
                        } else {
                            bundle.putBoolean("switch_queue", false)
                        }
                        viewModel.enableShuffleModel.value = false
                        if (viewModel.playListCurrent.value == null) {
                            viewModel.playListCurrent.value = playList
                            viewModel.currentMusicCover.value = null
                            viewModel.currentPlay.value = music
                        }
                        bundle.putParcelable("musicItem", music)
                        bundle.putParcelable("playList", playList)
                        bundle.putParcelableArrayList("musicItems", ArrayList(musicList))
                        bundle.putInt("index", index)
                        viewModel.mediaBrowser?.sendCustomAction(
                            ACTION_PLAY_MUSIC,
                            bundle,
                            null
                        )
                    }
                },
                onLongClick = {
                    if (!selectStatus) {
                        showDialog = true
                    }
                }
            ), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier
                .wrapContentHeight(Alignment.CenterVertically)
                .padding(top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectStatus) {
                Checkbox(
                    checked = selectList?.contains(music) == true,
                    onCheckedChange = { v ->
                        if (v) {
                            selectList?.add(music)
                        } else {
                            selectList?.remove(music)
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                Image(
                    painter = painterResource(playStatusIcon),
                    contentDescription = "Operate More, will open dialog",
                    modifier = Modifier
                        .size(30.dp)
                        .padding(5.dp)
                        .clip(CircleShape),
                    colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 5.dp, end = 5.dp)
                ) {
                    Text(
                        text = music.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.horizontalScroll(rememberScrollState(0))
                    )
                    Text(
                        text = music.artist,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.horizontalScroll(rememberScrollState(0))
                    )
                }
                if (playList.type == PlayListType.Queue || playList.type == PlayListType.PlayLists) {
                    Box(
                        modifier = Modifier
                            .height(45.dp)
                            .width(45.dp)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    offset += delta
                                },
                                onDragStopped = { _ ->
                                    var targetPosition =
                                        index + (offset / 80.dp.toPx(context)).toInt()
                                    if (targetPosition < 0) {
                                        targetPosition = 0
                                    }
                                    if (targetPosition > musicList.size - 1) {
                                        targetPosition = musicList.size - 1
                                    }
                                    if (targetPosition != index) {
                                        saveSortResult(
                                            playList,
                                            musicList,
                                            context,
                                            music,
                                            viewModel,
                                            index,
                                            targetPosition
                                        )
                                    }
                                    offset = 0f

                                }
                            )) {
                        Icon(
                            imageVector = Icons.Outlined.SwipeVertical,
                            contentDescription = "${music.name} sort",
                            modifier = Modifier
                                .size(40.dp)
                                .padding(5.dp)
                                .clip(
                                    CircleShape
                                ),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                if (!selectStatus) {
                    IconButton(
                        modifier = Modifier
                            .width(45.dp), onClick = {
                            showDialog = true
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
    }
}

fun saveSortResult(
    playList: AnyListBase,
    musicList: SnapshotStateList<MusicItem>,
    context: Context,
    music: MusicItem,
    viewModel: MusicViewModel,
    index: Int,
    targetIndex: Int
) {

    if (playList.type == PlayListType.PlayLists) {
        viewModel.mediaBrowser?.let {
            TracksUtils.sortPlayLists(
                it, context,
                playList,
                musicList,
                music,
                targetIndex
            )
        }
    } else if (playList.type == PlayListType.Queue) {
        viewModel.mediaBrowser?.let {
            TracksUtils.sortQueue(
                it, musicList,
                music,
                index, targetIndex
            )
        }
    }
}

@Composable
fun OperateDialog(
    musicViewModel: MusicViewModel,
    music: MusicItem,
    playList: AnyListBase?,
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
                    text = stringResource(R.string.operate_music, music.name), modifier = Modifier
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
                        if (musicViewModel.musicQueue.any { it.id == music.id }) {
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
                                        onDismiss(OperateType.RemoveFromQueue)
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = stringResource(R.string.remove_from_queue),
                                    modifier = Modifier.padding(start = 10.dp),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        } else {
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
                                    modifier = Modifier.padding(start = 10.dp),
                                    color = MaterialTheme.colorScheme.onBackground,
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
                                    text = stringResource(id = R.string.play_next),
                                    modifier = Modifier.padding(start = 10.dp),
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
                                modifier = Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        if (playList != null && playList.type == PlayListType.PlayLists) {
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
                                        onDismiss(OperateType.RemoveFromPlaylist)
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = stringResource(R.string.remove_from_current_playlist),
                                    modifier = Modifier.padding(start = 10.dp),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(50.dp)
//                                .padding(0.dp)
//                                .drawBehind {
//                                    drawLine(
//                                        color = color,
//                                        start = Offset(0f, size.height - 1.dp.toPx()),
//                                        end = Offset(size.width, size.height - 1.dp.toPx()),
//                                        strokeWidth = 1.dp.toPx()
//                                    )
//                                }
//                                .clickable {
//                                    onDismiss(OperateType.Ad_to_favorite)
//                                },
//                            contentAlignment = Alignment.CenterStart
//                        ) {
//                            Text(
//                                text = "Add to favorite", Modifier.padding(start = 10.dp),
//                                color = MaterialTheme.colorScheme.onBackground
//                            )
//                        }
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
                                    onDismiss(OperateType.Artist)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(id = R.string.artist, music.artist),
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
                                    onDismiss(OperateType.Album)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(id = R.string.album, music.album),
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
                                    onDismiss(OperateType.EditMusicInfo)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(R.string.music_info),
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
                                    onDismiss(OperateType.DeleteFromStorage)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(R.string.delete_from_storage),
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

