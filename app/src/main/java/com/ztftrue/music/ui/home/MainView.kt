package com.ztftrue.music.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
import com.ztftrue.music.play.PlayUtils
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.dao.SortFiledDao
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.sqlData.model.SortFiledData
import com.ztftrue.music.ui.other.DrawMenu
import com.ztftrue.music.ui.other.MainTopBar
import com.ztftrue.music.ui.play.toPx
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
            Scaffold(modifier = Modifier.semantics {
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



