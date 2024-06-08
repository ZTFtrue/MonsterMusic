package com.ztftrue.music.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.ztftrue.music.MainActivity
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.QueuePlayList
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.SongsPlayList
import com.ztftrue.music.play.ACTION_CLEAR_QUEUE
import com.ztftrue.music.play.ACTION_PlayLIST_CHANGE
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.ui.other.DrawMenu
import com.ztftrue.music.ui.public.AddMusicToPlayListDialog
import com.ztftrue.music.ui.public.Bottom
import com.ztftrue.music.ui.public.CreatePlayListDialog
import com.ztftrue.music.ui.public.QueueOperateDialog
import com.ztftrue.music.ui.public.SleepTimeDialog
import com.ztftrue.music.ui.public.TracksListView
import com.ztftrue.music.utils.OperateType
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.trackManager.PlaylistManager
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainView(
    musicViewModel: MusicViewModel,
    activity: MainActivity,
    navController: NavHostController,
) {
    val tabList = remember {
        mutableStateListOf<MainTab>()
    }
    LaunchedEffect(key1 = musicViewModel.mainTabList) {
        tabList.clear()
        tabList.addAll(musicViewModel.mainTabList)
    }

    val pagerState = rememberPagerState { tabList.size }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) {
        if (drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "MainView" },
        color = MaterialTheme.colorScheme.background
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DrawMenu(
                    pagerState,
                    drawerState,
                    navController,
                    musicViewModel,
                    activity
                )
            },
        ) {
            Scaffold(modifier = Modifier  .semantics {
                contentDescription =
                    "This is main page"
            },
                topBar = {
                    MainTopBar(musicViewModel, drawerState, pagerState, navController)
                }, bottomBar = {
                    Bottom(
                        musicViewModel, navController
                    )
                }, content = {
                    HorizontalPager(
                        state = pagerState,
                        beyondBoundsPageCount = tabList.size,
                        modifier = Modifier
                            .padding(it)
                            .semantics {
                                contentDescription =
                                    "current page is ${musicViewModel.mainTabList[pagerState.currentPage].name}"
                            },
                    ) { page ->
                        when (tabList[page].type) {
                            PlayListType.Songs -> {
                                TracksListView(
                                    Modifier.fillMaxHeight(),
                                    musicViewModel,
                                    SongsPlayList,
                                    musicViewModel.songsList
                                )
                            }

                            PlayListType.PlayLists -> {
                                PlayListView(
                                    Modifier.fillMaxHeight(),
                                    musicViewModel,
                                    navController,
                                )
                            }

                            PlayListType.Queue -> {
                                TracksListView(
                                    Modifier.fillMaxHeight(),
                                    musicViewModel,
                                    QueuePlayList,
                                    tracksList = musicViewModel.musicQueue
                                )
                            }

                            PlayListType.Albums -> {
                                AlbumGridView(
                                    Modifier.fillMaxHeight(),
                                    musicViewModel,
                                    navController
                                )
                            }

                            PlayListType.Artists -> {
                                ArtistsGridView(
                                    Modifier.fillMaxHeight(),
                                    musicViewModel,
                                    navController,
                                )
                            }

                            PlayListType.Genres -> {
                                GenreGridView(
                                    Modifier.fillMaxHeight(),
                                    musicViewModel,
                                    navController,
                                )
                            }

                            PlayListType.Folders -> {
                                FolderListView(
                                    Modifier.fillMaxHeight(),
                                    musicViewModel,
                                    navController,
                                )
                            }

                            else -> {

                            }
                        }
                    }
                })
        }
    }

}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    val timerIcon: Int = if (musicViewModel.remainTime.longValue == 0L) {
        R.drawable.set_timer
    } else {
        R.drawable.setted_timer
    }

    Column {
        if (showSleepDialog) {
            SleepTimeDialog(musicViewModel, onDismiss = {
                showSleepDialog = false
            })
        }
        if (showCreatePlayListDialog) {
            CreatePlayListDialog(musicViewModel, onDismiss = {
                showCreatePlayListDialog = false
                if (!it.isNullOrEmpty()) {
                    navController.navigate(
                        Router.TracksSelectPage.withArgs("-1", it)
                    )
                }
            })
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
                    Icon(Icons.Filled.Menu, contentDescription = "menu")
                }
            },
            title = { },
            actions = {

                if (musicViewModel.mainTabList[pagerState.currentPage].type == PlayListType.PlayLists) {
                    IconButton(modifier = Modifier.semantics {
                        contentDescription = "Add PlayList"
                    }, onClick = {
                        showCreatePlayListDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add PlayList",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape),
                        )
                    }
                } else if (musicViewModel.mainTabList[pagerState.currentPage].type == PlayListType.Queue) {
                    var showDialogForQueue by remember { mutableStateOf(false) }
                    var showAddPlayListDialog by remember { mutableStateOf(false) }
                    var showCreatePlayListDialogForQueue by remember { mutableStateOf(false) }

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
                                    val ids = ArrayList<Long>(musicViewModel.musicQueue.size)
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
                        CreatePlayListDialog(musicViewModel, onDismiss = { playListName ->
                            showCreatePlayListDialogForQueue = false
                            if (!playListName.isNullOrEmpty()) {
                                val ids = ArrayList<Long>(musicViewModel.musicQueue.size)
                                musicViewModel.musicQueue.forEach {
                                    ids.add(it.id)
                                }
                                val idPlayList =
                                    PlaylistManager.createPlaylist(context, playListName)
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
                                    Toast.makeText(context,    context.getString(R.string.create_failed), Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        })
                    }
                    IconButton(
                        modifier = Modifier.width(50.dp), onClick = {
                            showDialogForQueue = true
                        }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription =stringResource(id = R.string.operate),
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape),
                        )
                    }
                }
                IconButton(modifier = Modifier.semantics {
                    contentDescription = "Set sleep time"
                }, onClick = {
                    showSleepDialog = true
                }) {
                    Image(
                        painter = painterResource(timerIcon),
                        contentDescription = "Sleeper time",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape),
                        colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
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
                            text =stringResource(id = Utils.translateMap[item.name] ?:R.string.app_name) ,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                        )
                    })
            }
        }

    }
}

