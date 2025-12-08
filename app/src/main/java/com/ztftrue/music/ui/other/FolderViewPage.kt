package com.ztftrue.music.ui.other

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SnippetFolder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.SessionResult
import coil3.compose.rememberAsyncImagePainter
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
import com.ztftrue.music.ui.public.AddMusicToPlayListDialog
import com.ztftrue.music.ui.public.Bottom
import com.ztftrue.music.ui.public.CreatePlayListDialog
import com.ztftrue.music.ui.public.QueueOperateDialog
import com.ztftrue.music.ui.public.TopBar
import com.ztftrue.music.ui.public.TracksListView
import com.ztftrue.music.utils.OperateType
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.toPx
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.model.FolderList
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

@UnstableApi
@Composable
fun FolderListPage(
    musicViewModel: MusicViewModel,
    navController: SnapshotStateList<Any>,
    folderList: FolderList
) {
    val tracksList = remember { mutableStateListOf<MusicItem>() }
    val showIndicator = remember { mutableStateOf(false) }
    val durationAll = remember { mutableStateOf("") }
    val musicPlayList = remember { mutableStateOf(AnyListBase(2, PlayListType.None)) }
    val childrenFolderList = remember { mutableStateListOf<FolderList>() }
    childrenFolderList.addAll(folderList.children)
    var refreshCurrentValueList by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showMoreOperateDialog by remember { mutableStateOf(false) }
    var showOperatePopup by remember { mutableStateOf(false) }
    var showAddPlayListDialog by remember { mutableStateOf(false) }
    var showCreatePlayListDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    val musicList = remember { musicViewModel.musicQueue }
    var showDialog by remember { mutableStateOf(false) }
    showIndicator.value =
        musicViewModel.showIndicatorMap.getOrDefault(folderList.type.toString(), false)
    if (showSortDialog) {
        val sortFiledOptions =
            PlayUtils.trackSortFiledMap[folderList.type.name + "@Tracks"]
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
                    sortDb?.findSortByType(folderList.type.name + "@Tracks")
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
                                            sortDb.findSortByType(folderList.type.name + "@Tracks")
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
                                                folderList.type.name + "@Tracks",
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
                            musicViewModel.showIndicatorMap[folderList.type.name] =
                                filedSelected == "Alphabetical"
                            showIndicator.value =
                                musicViewModel.showIndicatorMap.getOrDefault(
                                    folderList.type.toString(),
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
                                folderList.type.name + "@Tracks"
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

        val item = FolderList(path = "", name = "", id = folderList.id, trackNumber = 0)
        FolderListOperateDialog(
            musicViewModel,
            playList = item,
            onDismiss = {
                showMoreOperateDialog = false
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
                        folderList.type,
                        folderList.id,
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
                    folderList.type,
                    folderList.id,
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
                        if (!PlayUtils.trackSortFiledMap[folderList.type.name + "@Tracks"].isNullOrEmpty()) {
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

    if (showDialog) {
        QueueOperateDialog(onDismiss = {
            if (it == OperateType.ClearQueue) {
                musicViewModel.browser?.sendCustomCommand(
                    MediaCommands.COMMAND_CLEAR_QUEUE,
                    Bundle.EMPTY
                )
                musicViewModel.musicQueue.clear()
                musicViewModel.currentPlay.value = null
                musicViewModel.playListCurrent.value = null
                musicViewModel.currentCaptionList.clear()
                musicList.clear()
            } else if (it == OperateType.SaveQueueToPlayList) {
                showAddPlayListDialog = true
            }
        })
    }
    if (showAddPlayListDialog) {
        AddMusicToPlayListDialog(musicViewModel, null) { playListId, removeDuplicate ->
            if (playListId != null) {
                if (playListId == -1L) {
                    showCreatePlayListDialog = true
                } else {
                    val ids = ArrayList<MusicItem>(musicList.size)
                    musicList.forEach {
                        ids.add(it)
                    }
                    if (PlaylistManager.addMusicsToPlaylist(
                            context,
                            playListId,
                            ids,
                            removeDuplicate
                        )
                    ) {
                        SongsUtils.refreshPlaylist(musicViewModel)
                    }
                }
            }
        }
    }
    if (showCreatePlayListDialog) {
        CreatePlayListDialog(onDismiss = { playListName ->
            if (!playListName.isNullOrEmpty()) {
                val ids = ArrayList<MusicItem>(musicList.size)
                musicList.forEach {
                    ids.add(it)
                }
                val idPlayList = PlaylistManager.createPlaylist(context, playListName, ids, false)
                if (idPlayList != null) {
                    musicViewModel.browser?.sendCustomCommand(
                        MediaCommands.COMMAND_PlAY_LIST_CHANGE,
                        Bundle().apply {},
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


    LaunchedEffect(musicViewModel.refreshPlayList.value, refreshCurrentValueList) {
        if (musicViewModel.browser == null) {
            navController.removeLastOrNull()
            return@LaunchedEffect
        } else {
            musicViewModel.loadingTracks.value = true
            val futureResult: ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>? =
                musicViewModel.browser?.getChildren(
                    folderList.type.name + "_track_" + folderList.id,
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
                            showOperatePopup = true
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
                                musicViewModel.customMusicCover.value
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
                                        navController.add(Router.TracksSelectPage(mListPlay as MusicPlayList))
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
                TracksListView(
                    musicViewModel,
                    folderList, tracksList, showIndicator,
                    folderData = if (musicViewModel.folderViewTree.value) {
                        childrenFolderList
                    } else {
                        null
                    }
                )
            }

        },
    )
}

@Composable
fun FolderItemView(
    item: FolderList,
    musicViewModel: MusicViewModel,
    modifier: Modifier,
    navController: SnapshotStateList<Any>,
    type: PlayListType = PlayListType.Folders,
) {
    val context = LocalContext.current
    var showOperateDialog by remember { mutableStateOf(false) }
    var showAddPlayListDialog by remember { mutableStateOf(false) }
    var showCreatePlayListDialog by remember { mutableStateOf(false) }
    var showFolderPath by remember { mutableStateOf(musicViewModel.folderViewShowPath) }
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
    val folderNumber = item.children.size
    Row(
        modifier = modifier
            .combinedClickable(
                onLongClick = {
                    showOperateDialog = true
                }
            ) {
//                navController.add(Router.PlayListView(item))
                navController.add(Router.FolderListPage(item))
//                navController.navigate(
//                    Router.PlayListView.withArgs(
//                        "id" to "${item.id}",
//                        "itemType" to enumToStringForPlayListType(type)
//                    ),
//                    navigatorExtras = ListParameter(item.id, type)
//                )
            }, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.padding(top = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.id == musicViewModel.playListCurrent.value?.id && item.type == musicViewModel.playListCurrent.value?.type) {
                    Icons.Outlined.SnippetFolder
                } else {
                    Icons.Outlined.Folder
                },
                contentDescription = if (musicViewModel.playStatus.value) {
                    "pause"
                } else {
                    "play"
                },
                modifier = Modifier
                    .size(30.dp)
                    .padding(end = 5.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
            Column(modifier = Modifier.fillMaxWidth(0.9f)) {
                Text(
                    text = item.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.horizontalScroll(rememberScrollState(0)),
                )
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (musicViewModel.folderViewTree.value) {
                        if (number != 0) {
                            Text(
                                text = stringResource(
                                    R.string.song,
                                    number,
                                    if (number <= 1L) "" else stringResource(id = R.string.s)
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.size(10.dp))
                        }
                    } else {
                        Text(
                            text = stringResource(
                                R.string.song,
                                number,
                                if (number <= 1L) "" else stringResource(id = R.string.s)
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.size(10.dp))
                    }

                    if (musicViewModel.folderViewTree.value && folderNumber != 0) {
                        Text(
                            text = "$folderNumber Folder${
                                if (folderNumber <= 1L) "" else stringResource(
                                    id = R.string.s
                                )
                            }",
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                }

                if (showFolderPath.value) {
                    Text(
                        text = item.path,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.80f),
                        modifier = Modifier.horizontalScroll(rememberScrollState(0)),
                    )
                }
            }
            Spacer(Modifier.size(20.dp))
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
