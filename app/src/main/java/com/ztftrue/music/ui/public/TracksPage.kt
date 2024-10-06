package com.ztftrue.music.ui.public

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.play.ACTION_GET_ALBUM_BY_ID
import com.ztftrue.music.play.ACTION_GET_TRACKS
import com.ztftrue.music.play.ACTION_SHUFFLE_PLAY_QUEUE
import com.ztftrue.music.play.ACTION_SORT
import com.ztftrue.music.play.PlayUtils
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.dao.SortFiledDao
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.sqlData.model.SortFiledData
import com.ztftrue.music.ui.home.AlbumGridView
import com.ztftrue.music.ui.home.AlbumsOperateDialog
import com.ztftrue.music.ui.home.ArtistsOperateDialog
import com.ztftrue.music.ui.home.FolderListOperateDialog
import com.ztftrue.music.ui.home.GenreListOperateDialog
import com.ztftrue.music.ui.home.PlayListOperateDialog
import com.ztftrue.music.ui.play.toPx
import com.ztftrue.music.utils.OperateType
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.ScrollDirectionType
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.enumToStringForPlayListType
import com.ztftrue.music.utils.model.AlbumList
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.model.ArtistList
import com.ztftrue.music.utils.model.FolderList
import com.ztftrue.music.utils.model.GenresList
import com.ztftrue.music.utils.model.ListBase
import com.ztftrue.music.utils.model.MusicPlayList
import com.ztftrue.music.utils.trackManager.ArtistManager
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
    navController: NavHostController,
    type: PlayListType,
    id: Long,
) {
    val tracksList = remember { mutableStateListOf<MusicItem>() }
    val showIndicator = remember { mutableStateOf(false) }
    val musicPlayList = remember { mutableStateOf(AnyListBase(2, PlayListType.None)) }
    val albumsList = remember { mutableStateListOf<AlbumList>() }
    var refreshCurrentValueList by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showMoreOperateDialog by remember { mutableStateOf(false) }
    var showOperatePopup by remember { mutableStateOf(false) }
    var showAddPlayListDialog by remember { mutableStateOf(false) }
    var showCreatePlayListDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
//    LaunchedEffect(key1 = musicViewModel.showIndicatorMap) {
    showIndicator.value = musicViewModel.showIndicatorMap.getOrDefault(type.toString(), false)
//    }
    if (showSortDialog) {
        val sortFiledOptions =
            PlayUtils.trackSortFiledMap[type.name + "@Tracks"]
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
                    sortDb?.findSortByType(type.name + "@Tracks")
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
                                            sortDb?.findSortByType(type.name + "@Tracks")
                                        if (sortData != null) {
                                            sortData.method =
                                                PlayUtils.methodMap[methodSelected] ?: ""
                                            sortData.methodName = methodSelected
                                            sortData.filed = sortFiledOptions[filedSelected]
                                                ?: ""
                                            sortData.filedName = filedSelected
                                            sortDb?.update(sortData)
                                        } else {
                                            sortData = SortFiledData(
                                                type.name + "@Tracks",
                                                sortFiledOptions[filedSelected]
                                                    ?: "",
                                                PlayUtils.methodMap[methodSelected] ?: "",
                                                methodSelected,
                                                filedSelected
                                            )
                                            sortDb?.insert(sortData)
                                        }
                                    })
                            }
                            musicViewModel.showIndicatorMap[type.name] =
                                filedSelected == "Alphabetical"
                            showIndicator.value =
                                musicViewModel.showIndicatorMap.getOrDefault(type.toString(), false)
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
                                type.name + "@Tracks"
                            )
                            musicViewModel.mediaBrowser?.sendCustomAction(
                                ACTION_SORT,
                                bundle,
                                object : MediaBrowserCompat.CustomActionCallback() {
                                    override fun onResult(
                                        action: String?,
                                        extras: Bundle?,
                                        resultData: Bundle?
                                    ) {
                                        super.onResult(action, extras, resultData)
                                        if (ACTION_SORT == action) {
                                            refreshCurrentValueList = !refreshCurrentValueList
                                        }
                                    }
                                }
                            )
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
        if (type == PlayListType.Albums) {
            // TODO this can avoid use an error dialog
            val item =
                if (musicPlayList.value is AlbumList) {
                    val a = (musicPlayList.value as AlbumList)
                    AlbumList(id, "", a.artist, a.firstYear, a.lastYear, 0)
                } else {
                    AlbumList(id, "", "", "", "", 0)
                }

            AlbumsOperateDialog(
                musicViewModel,
                playList = item,
                onDismiss = {
                    showMoreOperateDialog = false
                    when (it) {
                        OperateType.AddToPlaylist -> {
                            showAddPlayListDialog = true
                        }

                        OperateType.ShowArtist -> {
                            val bundle = Bundle()
                            bundle.putLong("albumId", id)
                            musicViewModel.mediaBrowser?.sendCustomAction(
                                ACTION_GET_ALBUM_BY_ID,
                                bundle,
                                object : MediaBrowserCompat.CustomActionCallback() {
                                    override fun onResult(
                                        action: String?,
                                        extras: Bundle?,
                                        resultData: Bundle?
                                    ) {
                                        super.onResult(action, extras, resultData)
                                        if (action == ACTION_GET_ALBUM_BY_ID) {
                                            val albumList =
                                                resultData?.getParcelable<AlbumList>("album")
                                            if (albumList != null) {
                                                ArtistManager.getArtistIdByName(
                                                    context,
                                                    albumList.artist
                                                )?.let { artistId ->
                                                    navController.navigate(
                                                        Router.PlayListView.withArgs(
                                                            artistId.toString(),
                                                            enumToStringForPlayListType(PlayListType.Artists)
                                                        ),
                                                    ) {

                                                    }
                                                }
                                            }
                                        }
                                    }
                                })

                        }

                        else -> {
                            Utils.operateDialogDeal(
                                it,
                                item,
                                musicViewModel
                            )

                        }
                    }
                },
            )
        } else if (type == PlayListType.Artists) {
            val item = ArtistList(id, "", 0, 0)
            ArtistsOperateDialog(
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
        } else if (type == PlayListType.Folders) {
            val item = FolderList("", id, 0)
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
        } else if (type == PlayListType.Genres) {
            val item = GenresList(id, "", 0, 0)
            GenreListOperateDialog(
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
        } else if (type == PlayListType.PlayLists) {
            val item = MusicPlayList("", id, 0)
            PlayListOperateDialog(
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

    }
    if (showAddPlayListDialog) {
        AddMusicToPlayListDialog(musicViewModel, null, onDismiss = {
            showAddPlayListDialog = false
            if (it != null) {
                if (it == -1L) {
                    showCreatePlayListDialog = true
                } else {
                    Utils.addTracksToPlayList(it, context, type, id, musicViewModel)
                }
            }
        })
    }
    if (showCreatePlayListDialog) {
        CreatePlayListDialog(onDismiss = {
            showCreatePlayListDialog = false
            if (it != null) {
                Utils.createPlayListAddTracks(it, context, type, id, musicViewModel)
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
                        if (!PlayUtils.trackSortFiledMap[type.name + "@Tracks"].isNullOrEmpty()) {
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
    LaunchedEffect(musicViewModel.refreshPlayList.value, refreshCurrentValueList) {
        val bundle = Bundle()
        bundle.putString("type", type.name)
        bundle.putLong("id", id)
        musicViewModel.mediaBrowser?.sendCustomAction(
            ACTION_GET_TRACKS,
            bundle,
            object : MediaBrowserCompat.CustomActionCallback() {
                override fun onResult(
                    action: String?,
                    extras: Bundle?,
                    resultData: Bundle?
                ) {
                    super.onResult(action, extras, resultData)
                    if (ACTION_GET_TRACKS == action && resultData != null) {
                        tracksList.clear()
                        albumsList.clear()
                        val tracksListResult = resultData.getParcelableArrayList<MusicItem>("list")
                        val albumLists = resultData.getParcelableArrayList<AlbumList>("albums")
                        val parentListMessage = resultData.getParcelable<AnyListBase>("message")
                        if (parentListMessage != null) {
                            musicPlayList.value = parentListMessage
                        }
                        tracksList.addAll(
                            tracksListResult ?: emptyList()
                        )
                        albumsList.addAll(albumLists ?: emptyList())
                        if (tracksList.isEmpty() && albumsList.isEmpty() && parentListMessage == null) {
                            navController.popBackStack()
                        }
                    }
                }
            }
        )
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
                        Image(
                            painter = rememberAsyncImagePainter(
                                R.drawable.apps
                            ),
                            contentDescription = "Operate More, will open popup",
                            modifier = Modifier
                                .width(30.dp)
                                .aspectRatio(1f),
                            colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                        )
                    }
                })
                if (musicPlayList.value is ListBase) {
                    val mListPlay = musicPlayList.value as ListBase
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                when (musicPlayList.value) {
                                    is AlbumList -> {
                                        val albumList = musicPlayList.value as AlbumList
                                        musicViewModel.getAlbumCover(albumList.id, context)
                                            ?: R.drawable.songs_thumbnail_cover
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
                                        R.drawable.songs_thumbnail_cover
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
                                        if (mListPlay.trackNumber <= 1) "" else "s"
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.horizontalScroll(rememberScrollState(0)),
                                    maxLines = 1
                                )

                                if (mListPlay.type == PlayListType.PlayLists) {
                                    IconButton(onClick = {
                                        navController.navigate(
                                            Router.TracksSelectPage.withArgs(
                                                "${mListPlay.id}",
                                                "null"
                                            )
                                        )
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
                                val bundle = Bundle()
                                musicViewModel.playListCurrent.value = musicPlayList.value
                                musicViewModel.musicQueue.clear()
                                musicViewModel.currentPlayQueueIndex.intValue = -1
                                bundle.putBoolean("switch_queue", true)
                                bundle.putParcelable("playList", musicPlayList.value)
                                bundle.putParcelableArrayList("musicItems", ArrayList(tracksList))
                                musicViewModel.mediaBrowser?.sendCustomAction(
                                    ACTION_SHUFFLE_PLAY_QUEUE,
                                    bundle,
                                    object : MediaBrowserCompat.CustomActionCallback() {
                                        override fun onResult(
                                            action: String?,
                                            extras: Bundle?,
                                            resultData: Bundle?
                                        ) {
                                            super.onResult(action, extras, resultData)
                                            if (ACTION_SHUFFLE_PLAY_QUEUE == action && resultData != null) {
                                                val qList =
                                                    resultData.getParcelableArrayList<MusicItem>(
                                                        "list"
                                                    )
                                                if (qList != null) {
                                                    musicViewModel.musicQueue.addAll(qList)
                                                    if (musicViewModel.currentPlayQueueIndex.intValue == -1) {
                                                        musicViewModel.currentPlayQueueIndex.intValue =
                                                            0
                                                        musicViewModel.currentPlay.value =
                                                            musicViewModel.musicQueue[0]
                                                        musicViewModel.currentCaptionList.clear()
                                                        musicViewModel.currentMusicCover.value =
                                                            null
                                                        musicViewModel.currentPlay.value =
                                                            musicViewModel.musicQueue[0]
                                                        musicViewModel.sliderPosition.floatValue =
                                                            0f
                                                        musicViewModel.currentDuration.longValue =
                                                            musicViewModel.currentPlay.value?.duration
                                                                ?: 0
                                                        musicViewModel.dealLyrics(
                                                            context,
                                                            musicViewModel.musicQueue[0]
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }) {
                                Image(
                                    painter = painterResource(
                                        R.drawable.shuffle_model
                                    ),
                                    contentDescription = "shuffle model",
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(30.dp),
                                    colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                                )
                            }
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
                                                if (a.albumNumber <= 1) "" else "s"
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
                                            if (a.albumNumber <= 1) "" else "s"
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
                    val configuration = LocalConfiguration.current
                    val width = (configuration.screenWidthDp / 2.5) + 70
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(width.dp),
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
