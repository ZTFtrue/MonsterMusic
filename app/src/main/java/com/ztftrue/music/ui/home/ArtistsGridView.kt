package com.ztftrue.music.ui.home

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
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
import com.ztftrue.music.utils.ScrollDirectionType
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.enumToStringForPlayListType
import com.ztftrue.music.utils.model.ArtistList


@Composable
fun ArtistsGridView(
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel,
    navController: NavHostController,
    artistListDefault: SnapshotStateList<ArtistList>? = null,
    type: PlayListType = PlayListType.Artists,
    scrollDirection: ScrollDirectionType? = null
) {
    val listState = rememberLazyGridState()
    val context = LocalContext.current
    var artistLists = remember { mutableStateListOf<ArtistList>() }
    if (artistListDefault != null) {
        artistLists = artistListDefault
    }
    LaunchedEffect(musicViewModel.refreshArtist.value) {
        // if there has artist, don't get new artist
        if (artistListDefault == null) {
            artistLists.clear()
            val futureResult: ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>? =
                musicViewModel.browser?.getChildren("artists_root", 0, Integer.MAX_VALUE, null)
            futureResult?.addListener({
                try {
                    val result: LibraryResult<ImmutableList<MediaItem>>? = futureResult.get()
                    if (result == null || result.resultCode != LibraryResult.RESULT_SUCCESS) {
                        return@addListener
                    }
                    val albumMediaItems: List<MediaItem> = result.value ?: listOf()
                    val list = mutableListOf<ArtistList>()
                    albumMediaItems.forEach { mediaItem ->
                        val album = ArtistList(
                            id = mediaItem.mediaId.toLong(),
                            name = mediaItem.mediaMetadata.title.toString(),
                            trackNumber = mediaItem.mediaMetadata.totalTrackCount ?: 0,
                            albumNumber = mediaItem.mediaMetadata.extras?.getInt(
                                CustomMetadataKeys.KEY_ALBUM_COUNT,
                                0
                            ) ?: 0
                        )
                        list.add(album)
                    }
                    artistLists.addAll(list)
                } catch (e: Exception) {
                    // 处理在获取结果过程中可能发生的异常 (如 ExecutionException)
                    Log.e("Client", "Failed to toggle favorite status", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
    if (artistLists.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.there_is_no_any_artist_in_here),
                Modifier.padding(start = 10.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        return
    }
    when (scrollDirection) {
        ScrollDirectionType.LIST_VERTICAL -> {
//            LazyColumn(
//
//            )
        }

        ScrollDirectionType.GRID_HORIZONTAL -> {
            val rowListSate = rememberLazyListState()
            val windowInfo = LocalWindowInfo.current
            val density = LocalDensity.current
            val containerWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
            val width = (containerWidthDp) / 2.8f
            LazyRow(
                contentPadding = PaddingValues(5.dp),
                state = rowListSate,
                modifier = modifier
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxWidth()
            ) {
                items(artistLists.size) { index ->
                    val item = artistLists[index]
                    Box(
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxSize()
                            .width(width)
                    ) {
                        ArtistItemView(
                            item,
                            musicViewModel,
                            navController,
                            type
                        )
                    }
                }
            }
        }

        else -> {
            // ScrollDirectionType.GRID_VERTICAL
            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp), // Number of columns in the grid
                contentPadding = PaddingValues(5.dp),
                state = listState,
                modifier = modifier
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxSize()
            ) {
                items(artistLists.size) { index ->
                    val item = artistLists[index]
                    Box(modifier = Modifier.padding(5.dp)) {
                        ArtistItemView(
                            item,
                            musicViewModel,
                            navController,
                            type
                        )
                    }
                }
            }
        }
    }

}


@Composable
fun ArtistItemView(
    item: ArtistList,
    musicViewModel: MusicViewModel,
    navController: NavHostController,
    type: PlayListType = PlayListType.Artists
) {
    val context = LocalContext.current
    var showOperateDialog by remember { mutableStateOf(false) }
    var showAddPlayListDialog by remember { mutableStateOf(false) }
    var showCreatePlayListDialog by remember { mutableStateOf(false) }
    if (showOperateDialog) {
        ArtistsOperateDialog(
            musicViewModel,
            playList = item,
            onDismiss = {
                showOperateDialog = false
                when (it) {
                    OperateType.AddToPlaylist -> {
                        showAddPlayListDialog = true
                    }

                    else -> {
                        Utils.operateDialogDeal(it, item, musicViewModel)
                    }
                }
            },
        )
    }
    if (showAddPlayListDialog) {
        AddMusicToPlayListDialog(musicViewModel, null, onDismiss = { playListId, removeDuplicate ->
            showAddPlayListDialog = false
            if (playListId != null) {
                if (playListId == -1L) {
                    showCreatePlayListDialog = true
                } else {
                    Utils.addTracksToPlayList(
                        playListId,
                        context,
                        type,
                        item.id,
                        musicViewModel,
                        removeDuplicate
                    )
                }
            }
        })
    }
    if (showCreatePlayListDialog) {
        CreatePlayListDialog(onDismiss = {
            showCreatePlayListDialog = false
            if (it != null) {
                Utils.createPlayListAddTracks(it, context, type, item.id, musicViewModel, false)
            }
        })
    }
    val number = item.trackNumber
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))
            .clip(RoundedCornerShape(5.dp))
            .combinedClickable(
                onLongClick = {
                    showOperateDialog = true
                }
            ) {
                navController.navigate(
                    Router.PlayListView.withArgs(
                        "id" to "${item.id}",
                        "itemType" to enumToStringForPlayListType(type)
                    ),
                    navigatorExtras = ListParameter(item.id, type)
                )
            }) {
        ConstraintLayout {
            val (playIndicator) = createRefs()
            val model: Any? = musicViewModel.artistCover[item.name.lowercase().trim()]
            Image(
                painter = rememberAsyncImagePainter(
                    model ?: R.drawable.ic_artist
                ),
                contentDescription = stringResource(R.string.album_cover),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .aspectRatio(1f),
                colorFilter = if (model == null) ColorFilter.tint(color = MaterialTheme.colorScheme.onPrimaryContainer) else null
            )
            if (item.id == musicViewModel.playListCurrent.value?.id && item.type == musicViewModel.playListCurrent.value?.type) {
                Image(
                    painter = painterResource(
                        R.drawable.pause
                    ),
                    contentDescription = stringResource(R.string.playing),
                    modifier = Modifier
                        .width(40.dp)
                        .height(40.dp)
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                        .constrainAs(playIndicator) {
                            bottom.linkTo(anchor = parent.bottom, margin = 10.dp)
                            end.linkTo(anchor = parent.end, margin = 10.dp)
                        },
                    colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onPrimary)
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(5.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            Text(
                text = item.name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.horizontalScroll(rememberScrollState(0))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(
                        R.string.song,
                        number,
                        if (number <= 1L) "" else stringResource(id = R.string.s)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(
                    modifier = Modifier
                        .width(50.dp)
                        .height(20.dp), onClick = {
                        showOperateDialog = true
                    }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(id = R.string.operate_more_will_open_dialog),
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

    }
}

@Composable
fun ArtistsOperateDialog(
    musicViewModel: MusicViewModel,
    playList: ArtistList,
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
                                    text = stringResource(id = R.string.play_next),
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
