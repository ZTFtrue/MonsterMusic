package com.ztftrue.music.ui.public

import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.play.manager.MediaCommands
import com.ztftrue.music.play.MediaItemUtils
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.DialogOperate
import com.ztftrue.music.utils.DialogOperate.openOpenArtistById
import com.ztftrue.music.utils.OperateType
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.SharedPreferencesUtils
import com.ztftrue.music.utils.TracksUtils
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.deleteTrackUpdate
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.model.MusicPlayList
import com.ztftrue.music.utils.trackManager.PlaylistManager
import com.ztftrue.music.utils.trackManager.SongsUtils
import com.ztftrue.music.utils.trackManager.TracksManager

@UnstableApi
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

    @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
    var showDialog by remember { mutableStateOf(false) }

    @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
    var showAddPlayListDialog by remember { mutableStateOf(false) }

    @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
    var showCreatePlayListDialog by remember { mutableStateOf(false) }

    @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
    var showDeleteTip by remember { mutableStateOf(false) }

    var showSaveAudioDialog by remember { mutableStateOf(false) }
    if (showDeleteTip) {
        DeleteTip(music.name, onDismiss = {
            showDeleteTip = false
            if (it) {
                if (TracksManager.removeMusicById(context, music.id)) {
                    val bundle = Bundle()
                    bundle.putLong("id", music.id)
                    val futureResult: ListenableFuture<SessionResult>? =
                        viewModel.browser?.sendCustomCommand(
                            MediaCommands.COMMAND_TRACK_DELETE,
                            bundle
                        )
                    futureResult?.addListener({
                        try {
                            val sessionResult = futureResult.get()
                            if (sessionResult.resultCode == SessionResult.RESULT_SUCCESS) {
                                deleteTrackUpdate(viewModel)
                            }
                        } catch (e: Exception) {
                            Log.e("Client", "Failed to toggle favorite status", e)
                        }
                    }, ContextCompat.getMainExecutor(context)) // 或者使用主线程的 Executor
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
                        val mediaItem = MediaItemUtils.musicItemToMediaItem(music)
                        viewModel.browser?.addMediaItem(
                            mediaItem
                        )
                    }

                    OperateType.PlayNext -> {
                        val l = viewModel.browser?.mediaItemCount
                        val i = if (l == null || l <= 0) {
                            C.INDEX_UNSET
                        } else {
                            viewModel.browser?.currentMediaItemIndex
                        }
                        val position = if (i != null) {
                            if (i == C.INDEX_UNSET) {
                                0
                            } else {
                                i + 1
                            }
                        } else {
                            0
                        }
                        viewModel.musicQueue.add(
                            position,
                            music
                        )
                        val mediaItem = MediaItemUtils.musicItemToMediaItem(music)
                        viewModel.browser?.addMediaItem(
                            position,
                            mediaItem
                        )
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
                                viewModel.scanAndRefreshPlaylist(context, playListPath)
                            }
                        }
                    }

                    OperateType.AddToFavorite -> {
                        // create playlist name is  MY favorite
                    }

                    OperateType.Artist -> {
                        openOpenArtistById(music.artistId, viewModel.navController)

//                        viewModel.navController?.navigate(
//                            Router.PlayListView.withArgs(
//                                "id" to "${music.artistId}",
//                                "itemType" to enumToStringForPlayListType(PlayListType.Artists)
//                            ),
//                        ) {
//                            popUpTo(Router.MainView.route) {
//                                // Inclusive means the start destination is also popped
//                                inclusive = false
//                            }
//                        }
                    }

                    OperateType.Album -> {
                        DialogOperate.openOpenAlbumById(music, viewModel.navController)
                    }

                    OperateType.RemoveFromQueue -> {
                        val indexM =
                            viewModel.musicQueue.indexOfFirst { musicItem -> musicItem.id == music.id }
                        if (indexM == -1) return@OperateDialog
                        val bundle = Bundle()
                        bundle.putInt("index", indexM)
                        viewModel.musicQueue.removeAt(indexM)
                        viewModel.browser?.removeMediaItem(indexM)
                    }

                    OperateType.EditMusicInfo -> {
                        viewModel.navController.add(Router.EditTrackPage(music))
                    }

                    OperateType.DeleteFromStorage -> {
                        showDeleteTip = true
                    }

                    OperateType.SaveProcessedAudio -> {
                        showSaveAudioDialog = true
                    }

                    OperateType.No -> {

                    }

                    else -> {

                    }
                }
            },
        )
    }
    if (showSaveAudioDialog) {
        SaveProcessedAudioDialog(
            musicViewModel = viewModel,
            music = music,
            onDismiss = {
                showSaveAudioDialog = false
            }
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
                    if (PlaylistManager.addMusicsToPlaylist(
                            context,
                            playListId,
                            musics,
                            removeDuplicate
                        )
                    ) {
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
    val isCurrent = music.id == viewModel.currentPlay.value?.id
    val isPlaying = isCurrent && viewModel.playStatus.value
    val itemHeightPx = with(LocalDensity.current) { 72.dp.roundToPx() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer(
                translationY = offset,
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                } else {
                    Color.Transparent
                }
            )
            .combinedClickable(
                onClick = {

                    if (selectStatus) {
                        // in select tracks (status) for add playlist
                        if (selectList?.contains(music) == true) {
                            selectList.remove(music)
                        } else {
                            selectList?.add(music)
                        }
                    } else {
                        // for play
                        val bundle = Bundle()
                        if (playList.type != PlayListType.Queue) {
                            viewModel.musicQueue.clear()
                            viewModel.musicQueue.addAll(musicList)
                            bundle.putBoolean("switch_queue", true)
                            SharedPreferencesUtils.enableShuffle(context, false)
                            viewModel.enableShuffleModel.value = false
                            viewModel.playListCurrent.value = playList
                            // TODO just parma for data,then get tracks in service
                            bundle.putParcelable("playList", playList)
                            viewModel.browser?.sendCustomCommand(
                                MediaCommands.COMMAND_CHANGE_PLAYLIST,
                                bundle
                            )
                            val t1 = ArrayList<MediaItem>()
                            viewModel.musicQueue.forEachIndexed { index, it ->
                                t1.add(MediaItemUtils.musicItemToMediaItem(it))
                            }
                            var needPlay = true
                            val currentPosition: Long =
                                if (viewModel.currentPlay.value?.id == music.id) {
                                    if (viewModel.browser?.isPlaying == true) {
                                        viewModel.browser?.pause()
                                        needPlay = false
                                    }
                                    viewModel.browser?.currentPosition ?: 0L
                                } else {
                                    0L
                                }
                            viewModel.browser?.shuffleModeEnabled = false
                            viewModel.browser?.clearMediaItems()
                            viewModel.browser?.setMediaItems(t1)
                            if (viewModel.trackEffectEnabled.value) {
                                viewModel.resetAudioEffectsToDefault()
                            }
                            viewModel.browser?.seekTo(index, currentPosition)
                            viewModel.browser?.playWhenReady = needPlay
                            viewModel.browser?.prepare()
                        } else {
                            // don't need switch queue
                            val index =
                                viewModel.musicQueue.indexOfFirst { musicItem -> musicItem.id == music.id }
                            if (music.id == viewModel.currentPlay.value?.id) {
                                if (viewModel.browser?.isPlaying == true) {
                                    viewModel.browser?.pause()
                                } else {
                                    viewModel.browser?.playWhenReady = true
                                    viewModel.browser?.prepare()
                                }
                            } else {
                                if (viewModel.trackEffectEnabled.value) {
                                    viewModel.resetAudioEffectsToDefault()
                                }
                                viewModel.browser?.seekTo(index, 0)
                                viewModel.browser?.playWhenReady = true
                                viewModel.browser?.prepare()
                            }
                        }
                    }
                },
                onLongClick = {
                    if (!selectStatus) {
                        showDialog = true
                    }
                }
            )
            .padding(horizontal = 8.dp),
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
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), music.albumId))
                    .crossfade(true)
                    .size(150, 150)
                    .build(),
                placeholder = painterResource(R.drawable.songs_thumbnail_cover),
                error = painterResource(R.drawable.songs_thumbnail_cover),
                contentDescription = stringResource(R.string.album_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.Pause,
                        contentDescription = if (isPlaying) stringResource(R.string.play) else stringResource(R.string.pause),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = music.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            val durationText = Utils.formatTime(music.duration)
            val subtitleText = if (music.artist.isNotBlank()) {
                "${music.artist} • $durationText"
            } else {
                durationText
            }
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (playList.type == PlayListType.Queue || playList.type == PlayListType.PlayLists) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            offset += delta
                        },
                        onDragStopped = { _ ->
                            var targetPosition =
                                index + (offset / itemHeightPx).toInt()
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
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwipeVertical,
                    contentDescription = stringResource(
                        R.string.item_sort_description,
                        music.name
                    ),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        if (!selectStatus) {
            IconButton(
                modifier = Modifier.size(40.dp),
                onClick = {
                    showDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.operate_more_will_open_dialog),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        viewModel.browser?.let {
            TracksUtils.sortPlayLists(
                it, context,
                playList,
                musicList,
                music,
                targetIndex,
                viewModel.viewModelScope
            )
        }
    } else if (playList.type == PlayListType.Queue) {
        TracksUtils.sortQueue(
            viewModel, musicList,
            music,
            index, targetIndex
        )
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
                                    onDismiss(OperateType.SaveProcessedAudio)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(R.string.save_processed_audio),
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
