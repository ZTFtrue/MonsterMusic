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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavHostController
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.ui.public.AddMusicToPlayListDialog
import com.ztftrue.music.ui.public.CreatePlayListDialog
import com.ztftrue.music.utils.OperateType
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.ScrollDirectionType
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.addTracksToPlayList
import com.ztftrue.music.utils.Utils.createPlayListAddTracks
import com.ztftrue.music.utils.enumToStringForPlayListType
import com.ztftrue.music.utils.model.GenresList


@Composable
fun GenreGridView(
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel,
    navController: NavHostController,
    type: PlayListType = PlayListType.Genres,
    scrollDirection: ScrollDirectionType? = null
) {
    val listState = rememberLazyGridState()
    val genreList = remember { mutableStateListOf<GenresList>() }
    LaunchedEffect(Unit) {
        genreList.clear()
        musicViewModel.mediaBrowser?.sendCustomAction(
            type.name,
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
                    if (action == type.name) {
                        resultData?.getParcelableArrayList<GenresList>("list")
                            ?.also { list ->
                                genreList.addAll(list)
                            }
                    }
                }

                override fun onError(action: String?, extras: Bundle?, data: Bundle?) {
                    super.onError(action, extras, data)
                }
            })
    }
    when (scrollDirection) {
        ScrollDirectionType.LIST_VERTICAL -> {
//            LazyColumn(
//
//            )
        }

        ScrollDirectionType.GRID_HORIZONTAL -> {
            val rowListSate = rememberLazyListState()
            val configuration = LocalConfiguration.current
            LazyRow(
                contentPadding = PaddingValues(5.dp),
                state = rowListSate,
                modifier = modifier
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxSize()
            ) {
                items(genreList.size) { index ->
                    val item = genreList[index]
                    Box(
                        modifier = Modifier
                            .padding(5.dp)
                            .width(((configuration.screenWidthDp - 80) / musicViewModel.albumItemsCount.intValue).dp)
                    ) {
                        GenreItemView(
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
                columns = GridCells.Fixed(musicViewModel.genreItemsCount.intValue), // Number of columns in the grid
                contentPadding = PaddingValues(5.dp),
                state = listState,
                modifier = modifier
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxSize()
            ) {
                items(genreList.size) { index ->
                    val item = genreList[index]
                    Box(modifier = Modifier.padding(5.dp)) {
                        GenreItemView(
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GenreItemView(
    item: GenresList,
    musicViewModel: MusicViewModel,
    navController: NavHostController,
    type: PlayListType = PlayListType.Genres
) {
    val context = LocalContext.current
    var showOperateDialog by remember { mutableStateOf(false) }
    var showAddPlayListDialog by remember { mutableStateOf(false) }
    var showCreatePlayListDialog by remember { mutableStateOf(false) }
    if (showOperateDialog) {
        GenreListOperateDialog(
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
        AddMusicToPlayListDialog(musicViewModel, null, onDismiss = {
            showAddPlayListDialog = false
            if (it != null) {
                if (it == -1L) {
                    showCreatePlayListDialog = true
                } else {
                    addTracksToPlayList(it, context, type, item.id, musicViewModel)
                }
            }
        })
    }
    if (showCreatePlayListDialog) {
        CreatePlayListDialog(musicViewModel, onDismiss = {
            showCreatePlayListDialog = false
            if (it != null) {
                createPlayListAddTracks(it, context, type, item.id, musicViewModel)
            }
        })
    }
    val number = item.trackNumber
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))
            .clip(RoundedCornerShape(5.dp))
            .combinedClickable(onLongClick = {
                showOperateDialog = true
            }, onClick = {
                navController.navigate(
                    Router.PlayListView.withArgs("${item.id}", enumToStringForPlayListType(type)),
                    navigatorExtras = ListParameter(item.id, type)
                )
            })
    ) {
        ConstraintLayout {
            val (playIndicator) = createRefs()
            Image(
                painter = painterResource(
                    id = R.drawable.ic_genres
                ),
                contentDescription = "Album cover",
                modifier = Modifier
                    .zIndex(0f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .aspectRatio(1f),
                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onPrimaryContainer)
            )
            if (item.id == musicViewModel.playListCurrent.value?.id && item.type == musicViewModel.playListCurrent.value?.type) {
                Image(
                    painter = painterResource(
                        R.drawable.pause
                    ),
                    contentDescription = "playing",
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
                    text = stringResource(R.string.song, number),
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
                        contentDescription = "Operate More, will open dialog",
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
fun GenreListOperateDialog(
    musicViewModel: MusicViewModel,
    playList: GenresList,
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
                        Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    )
}

