package com.ztftrue.music.ui.public

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.SessionResult
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.play.MediaCommands
import com.ztftrue.music.play.MediaItemUtils
import com.ztftrue.music.play.PlayUtils
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.dao.SortFiledDao
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.sqlData.model.SortFiledData
import com.ztftrue.music.ui.home.AlbumGridView
import com.ztftrue.music.ui.home.AlbumsOperateDialog
import com.ztftrue.music.ui.home.ArtistsOperateDialog
import com.ztftrue.music.ui.home.GenreListOperateDialog
import com.ztftrue.music.ui.home.PlayListOperateDialog
import com.ztftrue.music.ui.other.FolderListOperateDialog
import com.ztftrue.music.utils.DialogOperate
import com.ztftrue.music.utils.OperateType
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.ScrollDirectionType
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.toPx
import com.ztftrue.music.utils.model.AlbumList
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.model.ArtistList
import com.ztftrue.music.utils.model.FolderList
import com.ztftrue.music.utils.model.GenresList
import com.ztftrue.music.utils.model.ListBase
import com.ztftrue.music.utils.model.MusicPlayList
import com.ztftrue.music.utils.trackManager.PlaylistManager
import com.ztftrue.music.utils.trackManager.SongsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


/**
 * show all music of playlist
 */
@UnstableApi
@Composable
fun TracksListPage(
    musicViewModel: MusicViewModel,
    navController: SnapshotStateList<Any>,
    anyListBase: AnyListBase
) {
    val tracksList = remember { mutableStateListOf<MusicItem>() }
    val showIndicator = remember { mutableStateOf(false) }
    val durationAll = remember { mutableStateOf("") }
    val musicPlayList = remember { mutableStateOf(AnyListBase(2, PlayListType.None)) }
    val albumsList = remember { mutableStateListOf<AlbumList>() }
    var refreshCurrentValueList by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showMoreOperateDialog by remember { mutableStateOf(false) }
    var showOperatePopup by remember { mutableStateOf(false) }
    var showAddPlayListDialog by remember { mutableStateOf(false) }
    var showCreatePlayListDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
//    LaunchedEffect(key1 = musicViewModel.showIndicatorMap) {
    showIndicator.value =
        musicViewModel.showIndicatorMap.getOrDefault(anyListBase.type.toString(), false)
//    }
    if (showSortDialog) {
        val sortFiledOptions =
            PlayUtils.trackSortFiledMap[anyListBase.type.name + "@Tracks"]
        if (sortFiledOptions.isNullOrEmpty()) {
            return
        }
        val (filedSelected, onFiledOptionSelected) = remember {
            mutableStateOf("")
        }

        val (methodSelected, onMethodOptionSelected) = remember {
            mutableStateOf("")
        }
        var sortDb: SortFiledDao?
        LaunchedEffect(key1 = Unit) {
            CoroutineScope(Dispatchers.IO).launch {
                sortDb = MusicDatabase.getDatabase(context).SortFiledDao()
                val sortData1 =
                    sortDb?.findSortByType(anyListBase.type.name + "@Tracks")
                if (sortData1 != null) {
                    val f = sortData1.filedName
                    val m = sortData1.methodName
                    onFiledOptionSelected(f)
                    onMethodOptionSelected(m)
                }
            }
        }
        Popup(
            // on below line we are adding
            // alignment and properties.
            alignment = Alignment.TopEnd,
            properties = PopupProperties(),
            offset = IntOffset(
                -10.dp.toPx(context),
                50.dp.toPx(context)
            ),
            onDismissRequest = {
                showSortDialog = false
            }
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
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
                    modifier = Modifier
                ) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .wrapContentSize()
                                .clickable {

                                }
                                .padding(all = Dp(value = 8F))
                        ) {
                            Text(
                                text = stringResource(R.string.default_set),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            RadioButton(
                                selected = ("" == filedSelected),
                                modifier = Modifier
                                    .wrapContentSize(),
                                onClick = {
                                    onFiledOptionSelected("")
                                    onMethodOptionSelected("")
                                }
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier
                                .height(1.dp)
                                .wrapContentSize(align = Alignment.BottomCenter)
                                .width(140.dp)
                                .background(color = MaterialTheme.colorScheme.onBackground)
                        )
                    }
                    val filedKeys = sortFiledOptions.keys.toList()
                    items(filedKeys.size) { i ->
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .wrapContentSize()
                                .clickable {
                                    onFiledOptionSelected(filedKeys[i])
                                    if (methodSelected.isEmpty() || methodSelected.isBlank()) {
                                        onMethodOptionSelected(PlayUtils.methodMap.entries.first().key)
                                    }
                                }
                                .padding(all = Dp(value = 8F))
                        ) {
                            Text(
                                text = filedKeys[i],
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            RadioButton(
                                selected = (filedKeys[i] == filedSelected),
                                modifier = Modifier
                                    .wrapContentSize(),
                                onClick = {
                                    onFiledOptionSelected(filedKeys[i])
                                    if (methodSelected.isEmpty() || methodSelected.isBlank()) {
                                        onMethodOptionSelected(PlayUtils.methodMap.entries.first().key)
                                    }
                                }
                            )
                        }

                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier
                                .height(1.dp)
                                .wrapContentSize(align = Alignment.BottomCenter)
                                .width(140.dp)
                                .background(color = MaterialTheme.colorScheme.onBackground)
                        )
                    }
                    val methodKeys = PlayUtils.methodMap.keys.toList()
                    items(
                        methodKeys.size
                    ) { i ->
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(all = Dp(value = 8F))
                                .clickable {
                                    onMethodOptionSelected(methodKeys[i])
                                    if (filedSelected.isEmpty() || filedSelected.isBlank()) {
                                        onFiledOptionSelected(sortFiledOptions.entries.first().key)
                                    }
                                }
                        ) {
                            Text(
                                text = methodKeys[i],
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            RadioButton(
                                selected = (methodKeys[i] == methodSelected),
                                modifier = Modifier
                                    .wrapContentSize(),
                                onClick = {
                                    onMethodOptionSelected(methodKeys[i])
                                    if (filedSelected.isEmpty() || filedSelected.isBlank()) {
                                        onFiledOptionSelected(sortFiledOptions.entries.first().key)
                                    }
                                }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier
                        .wrapContentSize()
                        .width(120.dp)
                ) {
                    IconButton(
                        modifier = Modifier.width(50.dp),
                        onClick = {
                            showSortDialog = false
                        }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close display sort popup",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        modifier = Modifier.width(50.dp),
                        onClick = {
                            showSortDialog = false
                            runBlocking {
                                awaitAll(
                                    async(Dispatchers.IO) {
                                        sortDb =
                                            MusicDatabase.getDatabase(context).SortFiledDao()
                                        var sortData =
                                            sortDb.findSortByType(anyListBase.type.name + "@Tracks")
                                        if (sortData != null) {
                                            sortData.method =
                                                PlayUtils.methodMap[methodSelected] ?: ""
                                            sortData.methodName = methodSelected
                                            sortData.filed = sortFiledOptions[filedSelected]
                                                ?: ""
                                            sortData.filedName = filedSelected
                                            sortDb.update(sortData)
                                        } else {
                                            sortData = SortFiledData(
                                                anyListBase.type.name + "@Tracks",
                                                sortFiledOptions[filedSelected]
                                                    ?: "",
                                                PlayUtils.methodMap[methodSelected] ?: "",
                                                methodSelected,
                                                filedSelected
                                            )
                                            sortDb.insert(sortData)
                                        }
                                    })
                            }
                            musicViewModel.showIndicatorMap[anyListBase.type.name] =
                                filedSelected == "Alphabetical"
                            showIndicator.value =
                                musicViewModel.showIndicatorMap.getOrDefault(
                                    anyListBase.type.toString(),
                                    false
                                )
                            val bundle = Bundle()
                            bundle.putString(
                                "method",
                                PlayUtils.methodMap[methodSelected] ?: ""
                            )
                            bundle.putString(
                                "filed", sortFiledOptions[filedSelected]
                                    ?: ""
                            )
                            bundle.putString(
                                "type",
                                anyListBase.type.name + "@Tracks"
                            )
                            val futureResult: ListenableFuture<SessionResult>? =
                                musicViewModel.browser?.sendCustomCommand(
                                    MediaCommands.COMMAND_SORT_TRACKS,
                                    bundle
                                )
                            futureResult?.addListener({
                                try {
                                    // a. 获取 SessionResult
                                    val sessionResult = futureResult.get()
                                    // b. 检查操作是否成功
                                    if (sessionResult.resultCode == SessionResult.RESULT_SUCCESS) {
                                        refreshCurrentValueList = !refreshCurrentValueList
                                    }
                                } catch (e: Exception) {
                                    // 处理在获取结果过程中可能发生的异常 (如 ExecutionException)
                                    Log.e("Client", "Failed to toggle favorite status", e)
                                }
                            }, ContextCompat.getMainExecutor(context))
                        }) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Close display sort popup and save select",
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
    if (showMoreOperateDialog) {
        when (anyListBase) {
            is AlbumList -> {
                AlbumsOperateDialog(
                    musicViewModel,
                    playList = anyListBase,
                    onDismiss = {
                        showMoreOperateDialog = false
                        when (it) {
                            OperateType.AddToPlaylist -> {
                                showAddPlayListDialog = true
                            }

                            OperateType.ShowArtist -> {
                                DialogOperate.openArtist(
                                    context, anyListBase.artist,
                                    musicViewModel.navController
                                )
                            }

                            else -> {
                                Utils.operateDialogDeal(
                                    it,
                                    anyListBase,
                                    musicViewModel
                                )

                            }
                        }
                    },
                )
            }

            is ArtistList -> {
                ArtistsOperateDialog(
                    musicViewModel,
                    playList = anyListBase,
                    onDismiss = {
                        showMoreOperateDialog = false
                        when (it) {
                            OperateType.AddToPlaylist -> {
                                showAddPlayListDialog = true
                            }

                            else -> {
                                Utils.operateDialogDeal(it, anyListBase, musicViewModel)
                            }
                        }
                    },
                )
            }

            is FolderList -> {
                FolderListOperateDialog(
                    musicViewModel,
                    playList = anyListBase,
                    onDismiss = {
                        showMoreOperateDialog = false
                        when (it) {
                            OperateType.AddToPlaylist -> {
                                showAddPlayListDialog = true
                            }

                            else -> {
                                Utils.operateDialogDeal(it, anyListBase, musicViewModel)
                            }
                        }
                    },
                )
            }

            is GenresList -> {
                GenreListOperateDialog(
                    musicViewModel,
                    playList = anyListBase,
                    onDismiss = {
                        showMoreOperateDialog = false
                        when (it) {
                            OperateType.AddToPlaylist -> {
                                showAddPlayListDialog = true
                            }

                            else -> {
                                Utils.operateDialogDeal(it, anyListBase, musicViewModel)
                            }
                        }
                    },
                )
            }

            is MusicPlayList -> {
                PlayListOperateDialog(
                    musicViewModel,
                    playList = anyListBase,
                    onDismiss = {
                        showMoreOperateDialog = false
                        when (it) {
                            OperateType.AddToPlaylist -> {
                                showAddPlayListDialog = true
                            }

                            OperateType.RemoveDuplicate -> {
                                if (PlaylistManager.cleanDuplicateTrackFromPlayList(
                                        context,
                                        anyListBase.id,
                                        anyListBase.path,
                                        musicViewModel.songsList.toList()
                                    )
                                ) {
                                    musicViewModel.scanAndRefreshPlaylist(context, anyListBase.path)
                                }

                            }

                            OperateType.RenamePlayList -> {
                                showRenameDialog = true
                            }

                            else -> {
                                Utils.operateDialogDeal(it, anyListBase, musicViewModel)
                            }

                        }
                    },
                )
            }
        }
    }
    if (showRenameDialog) {
        if (anyListBase is MusicPlayList) {
            RenamePlayListDialog(anyListBase.name, onDismiss = {
                if (!it.isNullOrEmpty()) {
                    if (PlaylistManager.renamePlaylist(context, anyListBase.id, it)) {
                        SongsUtils.refreshPlaylist(musicViewModel)
                    }
                }
            })
        }
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
                        anyListBase.type,
                        anyListBase.id,
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
                Utils.createPlayListAddTracks(
                    it,
                    context,
                    anyListBase.type,
                    anyListBase.id,
                    musicViewModel,
                    false
                )
            }
        })
    }
    if (showOperatePopup) {
        Popup(
            // on below line we are adding
            // alignment and properties.
            alignment = Alignment.TopEnd,
            properties = PopupProperties(),
            offset = IntOffset(
                -10.dp.toPx(context),
                50.dp.toPx(context)
            ),
            onDismissRequest = {
                showOperatePopup = false
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .wrapContentSize()
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
                    modifier = Modifier
                ) {
                    item {
                        IconButton(modifier = Modifier.semantics {
                            contentDescription = "Show more operate"
                        }, onClick = {
                            showOperatePopup = false
                            showMoreOperateDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Show more operate",
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    item {
                        if (!PlayUtils.trackSortFiledMap[anyListBase.type.name + "@Tracks"].isNullOrEmpty()) {
                            IconButton(
                                modifier = Modifier.width(50.dp), onClick = {
                                    showOperatePopup = false
                                    showSortDialog = true
                                }) {
                                Text(
                                    text = stringResource(id = R.string.sort),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentSize()
                ) {
                    IconButton(
                        modifier = Modifier.width(50.dp),
                        onClick = {
                            showOperatePopup = false
                        }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close display set popup",
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
    fun getPlayListMessage() {
        val futureResultItem: ListenableFuture<SessionResult>? =
            musicViewModel.browser?.sendCustomCommand(
                MediaCommands.COMMAND_GET_PLAY_LIST_ITEM,
                Bundle().apply {
                    putString("type", anyListBase.type.name)
                    putLong("id", anyListBase.id)
                }
            )
        futureResultItem?.addListener({
            try {
                val result: SessionResult? = futureResultItem.get()
                if (result == null || result.resultCode != LibraryResult.RESULT_SUCCESS) {
                    Log.e("Client", "Failed COMMAND_GET_PLAY_LIST_ITEM ${result?.resultCode}")
                    navController.removeLastOrNull()
                    return@addListener
                }
                result.extras.getParcelable<AnyListBase>("data")?.let {
                    musicPlayList.value = it
                }
            } catch (e: Exception) {
                navController.removeLastOrNull()
                Log.e("Client", "Failed to toggle favorite status", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun getHaveAlbums() {
        if (anyListBase.type.name == PlayListType.Artists.name || anyListBase.type.name == PlayListType.Genres.name) {
            albumsList.clear()
            val futureResultAlbum: ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>? =
                musicViewModel.browser?.getChildren(
                    anyListBase.type.name + "_album_" + anyListBase.id,
                    0,
                    Integer.MAX_VALUE,
                    null
                )
            futureResultAlbum?.addListener({
                try {
                    val result: LibraryResult<ImmutableList<MediaItem>>? = futureResultAlbum.get()
                    if (result == null || result.resultCode != LibraryResult.RESULT_SUCCESS) {
                        return@addListener
                    }
                    val albumMediaItems: List<MediaItem> = result.value ?: listOf()
                    val tracksListResult = ArrayList<AlbumList>()
                    albumMediaItems.forEach { mediaItem ->
                        MediaItemUtils.mediaItemToAlbumList(mediaItem)
                            ?.let { tracksListResult.add(it) }
                    }
                    albumsList.addAll(tracksListResult)
                } catch (e: Exception) {
                    // 处理在获取结果过程中可能发生的异常 (如 ExecutionException)
                    Log.e("Client", "Failed to toggle favorite status", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
    LaunchedEffect(musicViewModel.refreshPlayList.value, refreshCurrentValueList) {
        if (musicViewModel.browser == null) {
            navController.removeLastOrNull()
            return@LaunchedEffect
        } else {
            musicViewModel.loadingTracks.value = true
            val futureResult: ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>? =
                musicViewModel.browser?.getChildren(
                    anyListBase.type.name + "_track_" + anyListBase.id,
                    0,
                    Integer.MAX_VALUE,
                    null
                )
            futureResult?.addListener({
                try {
                    val result: LibraryResult<ImmutableList<MediaItem>>? = futureResult.get()
                    Log.d("Client", "result tracks: ${result?.resultCode}")
                    if (result == null || result.resultCode != LibraryResult.RESULT_SUCCESS) {
                        navController.removeLastOrNull()
                        return@addListener
                    }
                    getPlayListMessage()
                    getHaveAlbums()
                    val albumMediaItems: List<MediaItem> = result.value ?: listOf()
                    val tracksListResult = ArrayList<MusicItem>()
                    albumMediaItems.forEach { mediaItem ->
                        MediaItemUtils.mediaItemToMusicItem(mediaItem)
                            ?.let { tracksListResult.add(it) }
                    }
                    tracksList.clear()
                    tracksList.addAll(
                        tracksListResult
                    )

                    val duration = tracksList.sumOf { it.duration }
                    durationAll.value = Utils.formatTimeWithUnit(duration)
                } catch (e: Exception) {
                    navController.removeLastOrNull()
                    // 处理在获取结果过程中可能发生的异常 (如 ExecutionException)
                    Log.e("Client", "Failed to toggle favorite status", e)
                }
                musicViewModel.loadingTracks.value = false
            }, ContextCompat.getMainExecutor(context))

        }
    }

    Scaffold(
        modifier = Modifier.padding(all = 0.dp),
        topBar = {
            Column {
                TopBar(navController, musicViewModel, content = {
                    IconButton(
                        modifier = Modifier.width(50.dp), onClick = {
                        }) {
                        Icon(
                            imageVector = Icons.Outlined.Apps,
                            contentDescription = "Operate More, will open popup",
                            modifier = Modifier
                                .width(30.dp)
                                .aspectRatio(1f),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                })
                if (musicPlayList.value is ListBase) {
                    val mListPlay = musicPlayList.value as ListBase
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                when (musicPlayList.value) {
                                    is AlbumList -> {
                                        val albumList = musicPlayList.value as AlbumList
                                        val albumCoverModel by produceState(
                                            initialValue = musicViewModel.customMusicCover.value, // 初始显示默认封面
                                            key1 = albumList.id
                                        ) {
                                            value =
                                                musicViewModel.getAlbumCover(albumList.id, context)
                                        }
                                        ImageRequest.Builder(context)
                                            .data(albumCoverModel)
                                            .build()
                                    }

                                    is ArtistList -> {
                                        val artistList = musicPlayList.value as ArtistList
                                        musicViewModel.artistCover[artistList.name.lowercase()
                                            .trim()] ?: R.drawable.ic_artist
                                    }

                                    is GenresList -> {
                                        val genresList = musicPlayList.value as GenresList
                                        musicViewModel.genreCover[genresList.name.lowercase()
                                            .trim()] ?: R.drawable.ic_genres
                                    }

                                    else -> {
                                        musicViewModel.customMusicCover.value
                                    }
                                }
                            ),
                            contentDescription = "cover",
                            modifier = Modifier
                                .width(150.dp)
                                .height(150.dp),
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 10.dp)
                        ) {

                            Text(
                                text = mListPlay.name,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.horizontalScroll(rememberScrollState(0)),
                                maxLines = 1
                            )
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.song_tracks,
                                        mListPlay.trackNumber,
                                        if (mListPlay.trackNumber <= 1) "" else stringResource(id = R.string.s)
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.horizontalScroll(rememberScrollState(0)),
                                    maxLines = 1
                                )

                                if (mListPlay.type == PlayListType.PlayLists) {
                                    IconButton(onClick = {
                                        navController.add(Router.TracksSelectPage(anyListBase as MusicPlayList))
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add playlist",
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape),
                                            tint = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = {
                                musicViewModel.enableShuffleModel.value = true
                                musicViewModel.playShuffled(mListPlay.type, mListPlay.id)
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Shuffle,
                                    contentDescription = "shuffle model",
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(30.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = stringResource(R.string.duration, durationAll.value),
                                modifier = Modifier.horizontalScroll(
                                    rememberScrollState(
                                        0
                                    )
                                ),
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            when (musicPlayList.value) {
                                is AlbumList -> {
                                    val a = musicPlayList.value as AlbumList
                                    Column {
                                        Text(
                                            text = a.artist,
//                                            modifier = Modifier
//                                                .wrapContentSize(),
                                            modifier = Modifier.horizontalScroll(
                                                rememberScrollState(
                                                    0
                                                )
                                            ),
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.year_tra,
                                                a.firstYear,
                                                if (a.firstYear == a.lastYear) "" else " ~ ${a.lastYear}"
                                            ),
//                                            modifier = Modifier
//                                                .wrapContentSize(),
                                            modifier = Modifier.horizontalScroll(
                                                rememberScrollState(
                                                    0
                                                )
                                            ),
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }

                                is ArtistList -> {
                                    val a = musicPlayList.value as ArtistList
                                    Column {
                                        Text(
                                            text = stringResource(
                                                R.string.album_tracks,
                                                a.albumNumber,
                                                if (a.albumNumber <= 1) "" else stringResource(id = R.string.s)
                                            ),
//                                            modifier = Modifier
//                                                .wrapContentSize(),
                                            modifier = Modifier.horizontalScroll(
                                                rememberScrollState(
                                                    0
                                                )
                                            ),
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }

                                is GenresList -> {
                                    val a = musicPlayList.value as GenresList
                                    Text(
                                        text = stringResource(
                                            R.string.album_tracks,
                                            a.albumNumber,
                                            if (a.albumNumber <= 1) "" else stringResource(id = R.string.s)
                                        ),
//                                        modifier = Modifier
//                                            .wrapContentSize(),
                                        modifier = Modifier.horizontalScroll(rememberScrollState(0)),
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }

                    }
                }
            }
        },
        bottomBar = { Bottom(musicViewModel, navController) },
        floatingActionButton = {},
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {

                if (albumsList.isNotEmpty()) {
                    val windowInfo = LocalWindowInfo.current
                    val containerWidth = windowInfo.containerSize.width
                    val density = LocalDensity.current
                    val containerHeightDp =
                        with(density) { ((containerWidth / 2.5) + 70.dp.toPx()).toInt().toDp() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(containerHeightDp),
                    ) {
                        AlbumGridView(
                            musicViewModel = musicViewModel,
                            navController = navController,
                            albumListDefault = albumsList,
                            scrollDirection = ScrollDirectionType.GRID_HORIZONTAL
                        )
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(color = MaterialTheme.colorScheme.background)
                        )
                    }
                }
                TracksListView(
                    musicViewModel,
                    musicPlayList.value, tracksList, showIndicator
                )
            }

        },
    )
}
