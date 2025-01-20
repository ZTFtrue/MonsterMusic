package com.ztftrue.music.ui.other

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavHostController
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.play.ACTION_CLEAR_QUEUE
import com.ztftrue.music.play.ACTION_PlayLIST_CHANGE
import com.ztftrue.music.play.ACTION_SORT
import com.ztftrue.music.play.PlayUtils
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.dao.SortFiledDao
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.sqlData.model.SortFiledData
import com.ztftrue.music.ui.public.AddMusicToPlayListDialog
import com.ztftrue.music.ui.public.CreatePlayListDialog
import com.ztftrue.music.ui.public.QueueOperateDialog
import com.ztftrue.music.ui.public.SleepTimeDialog
import com.ztftrue.music.utils.OperateType
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.toPx
import com.ztftrue.music.utils.trackManager.PlaylistManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    musicViewModel: MusicViewModel,
    drawerState: DrawerState,
    pagerState: PagerState,
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSleepDialog by remember { mutableStateOf(false) }
    var showCreatePlayListDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showOperateDialog by remember { mutableStateOf(false) }
    val timerIcon: ImageVector = if (musicViewModel.remainTime.longValue == 0L) {
        Icons.Outlined.Alarm
    } else {
        Icons.Outlined.Snooze
    }

    Column {
        if (showSleepDialog) {
            SleepTimeDialog(musicViewModel, onDismiss = {
                showSleepDialog = false
            })
        }
        if (showCreatePlayListDialog) {
            CreatePlayListDialog(onDismiss = {
                showCreatePlayListDialog = false
                if (!it.isNullOrEmpty()) {
                    navController.navigate(
                        Router.TracksSelectPage.withArgs("-1", it)
                    )
                }
            })
        }
        if (showSortDialog) {
            val sortFiledOptions =
                PlayUtils.sortFiledMap[musicViewModel.mainTabList[pagerState.currentPage].type.name]

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
                        sortDb?.findSortByType(musicViewModel.mainTabList[pagerState.currentPage].type.name)
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
                LocalConfiguration.current
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
                                contentDescription = stringResource(R.string.close_display_sort_popup),
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                sortDb?.findSortByType(musicViewModel.mainTabList[pagerState.currentPage].type.name)
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
                                                    musicViewModel.mainTabList[pagerState.currentPage].type.name,
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
                                musicViewModel.showIndicatorMap[PlayListType.Songs.name] =
                                    filedSelected == "Alphabetical"

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
                                    musicViewModel.mainTabList[pagerState.currentPage].type.name
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
                                                when (musicViewModel.mainTabList[pagerState.currentPage].type.name) {
                                                    PlayListType.PlayLists.name -> {
                                                        musicViewModel.refreshPlayList.value =
                                                            !musicViewModel.refreshPlayList.value
                                                    }

                                                    PlayListType.Albums.name -> {
                                                        musicViewModel.refreshAlbum.value =
                                                            !musicViewModel.refreshAlbum.value
                                                    }

                                                    PlayListType.Artists.name -> {
                                                        musicViewModel.refreshArtist.value =
                                                            !musicViewModel.refreshArtist.value
                                                    }

                                                    PlayListType.Genres.name -> {
                                                        musicViewModel.refreshGenre.value =
                                                            !musicViewModel.refreshGenre.value
                                                    }

                                                    PlayListType.Songs.name -> {
                                                        resultData?.getParcelableArrayList<MusicItem>(
                                                            "songsList"
                                                        )?.also {
                                                            musicViewModel.songsList.clear()
                                                            musicViewModel.songsList.addAll(it)
                                                        }
                                                    }

                                                    else -> {
                                                    }
                                                }
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                }
            }
        }
        var showDialogForQueue by remember { mutableStateOf(false) }
        var showAddPlayListDialog by remember { mutableStateOf(false) }
        var showCreatePlayListDialogForQueue by remember {
            mutableStateOf(
                false
            )
        }

        if (showDialogForQueue) {
            QueueOperateDialog(onDismiss = {
                showDialogForQueue = false
                if (it == OperateType.ClearQueue) {
                    musicViewModel.mediaBrowser?.sendCustomAction(
                        ACTION_CLEAR_QUEUE,
                        null,
                        null
                    )
                    musicViewModel.musicQueue.clear()
                    musicViewModel.currentPlay.value = null
                    musicViewModel.playListCurrent.value = null
                    musicViewModel.currentPlayQueueIndex.intValue = 0
                    musicViewModel.currentCaptionList.clear()
                } else if (it == OperateType.SaveQueueToPlayList) {
                    showAddPlayListDialog = true
                }
            })
        }
        if (showAddPlayListDialog) {
            AddMusicToPlayListDialog(musicViewModel, null) { playListId ->
                showAddPlayListDialog = false
                if (playListId != null) {
                    if (playListId == -1L) {
                        showCreatePlayListDialogForQueue = true
                    } else {
                        val ids =
                            ArrayList<Long>(musicViewModel.musicQueue.size)
                        musicViewModel.musicQueue.forEach {
                            ids.add(it.id)
                        }
                        PlaylistManager.addMusicsToPlaylist(
                            context,
                            playListId,
                            ids
                        )
                        musicViewModel.mediaBrowser?.sendCustomAction(
                            ACTION_PlayLIST_CHANGE, null, null
                        )
                    }
                }
            }
        }
        if (showCreatePlayListDialogForQueue) {
            CreatePlayListDialog(
                onDismiss = { playListName ->
                    showCreatePlayListDialogForQueue = false
                    if (!playListName.isNullOrEmpty()) {
                        val ids =
                            ArrayList<Long>(musicViewModel.musicQueue.size)
                        musicViewModel.musicQueue.forEach {
                            ids.add(it.id)
                        }
                        val idPlayList =
                            PlaylistManager.createPlaylist(
                                context,
                                playListName
                            )
                        if (idPlayList != -1L) {
                            PlaylistManager.addMusicsToPlaylist(
                                context,
                                idPlayList,
                                ids
                            )
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
        if (showOperateDialog) {
            Popup(
                // on below line we are adding
                // alignment and properties.
                alignment = Alignment.TopEnd,
                properties = PopupProperties(),
                offset = IntOffset(
                    -100.dp.toPx(context),
                    50.dp.toPx(context)
                ),
                onDismissRequest = {
                    showOperateDialog = false
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
                            if (musicViewModel.mainTabList[pagerState.currentPage].type == PlayListType.PlayLists) {
                                IconButton(modifier = Modifier.semantics {
                                    contentDescription = "Add PlayList"
                                }, onClick = {
                                    showOperateDialog = false
                                    showCreatePlayListDialog = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add PlayList",
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        item {
                            if (!PlayUtils.sortFiledMap[musicViewModel.mainTabList[pagerState.currentPage].type.name].isNullOrEmpty()) {
                                IconButton(
                                    modifier = Modifier.width(50.dp), onClick = {
                                        showOperateDialog = false
                                        showSortDialog = true
                                    }) {
                                    Text(
                                        text = stringResource(R.string.sort),
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
                                showOperateDialog = false
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
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = {
                    scope.launch {
                        drawerState.apply {
                            if (isClosed) open() else close()
                        }
                    }
                }) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "menu",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            title = { },
            actions = {
                if (musicViewModel.mainTabList.size > pagerState.currentPage && musicViewModel.mainTabList[pagerState.currentPage].type != PlayListType.Folders) {
                    IconButton(
                        modifier = Modifier.width(50.dp), onClick = {
                            if (musicViewModel.mainTabList[pagerState.currentPage].type != PlayListType.Queue) {
                                showOperateDialog = true
                            } else if (musicViewModel.mainTabList[pagerState.currentPage].type == PlayListType.Queue) {
                                showOperateDialog = false
                                showDialogForQueue = true
                            }
                        }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(id = R.string.operate),
                            modifier = Modifier
                                .size(25.dp)
                                .clip(CircleShape),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                IconButton(modifier = Modifier.semantics {
                    contentDescription = "Set sleep time"
                }, onClick = {
                    showSleepDialog = true
                }) {
                    Icon(
                        imageVector = timerIcon,
                        contentDescription = "Sleeper time",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(
                    modifier = Modifier
                        .size(50.dp)
                        .semantics {
                            contentDescription = "Search"
                        },
                    onClick = {
                        navController.navigate(
                            Router.SearchPage.route
                        )
                    }) {
                    Icon(
                        Icons.Filled.Search,
                        modifier = Modifier.size(30.dp),
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            })
        if (musicViewModel.mainTabList.size > pagerState.currentPage) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier
                                .height(3.0.dp)
                                .tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            height = 3.0.dp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    } else {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.height(3.0.dp),
                            height = 3.0.dp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
            ) {
                musicViewModel.mainTabList.forEachIndexed { index, item ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = stringResource(
                                    id = Utils.translateMap[item.name] ?: R.string.app_name
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp,
                            )
                        })
                }
            }

        }

    }
}
