package com.ztftrue.music.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.ztftrue.music.MainActivity
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.QueuePlayList
import com.ztftrue.music.SongsPlayList
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.ui.other.DrawMenu
import com.ztftrue.music.ui.other.MainTopBar
import com.ztftrue.music.ui.public.Bottom
import com.ztftrue.music.ui.public.TracksListView
import com.ztftrue.music.utils.PlayListType
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainView(
    musicViewModel: MusicViewModel,
    activity: MainActivity,
    navController: NavHostController,
) {
    val showIndicator = remember { mutableStateOf<Boolean>(false) }

    val tabList = remember {
        mutableStateListOf<MainTab>()
    }
    LaunchedEffect(key1 = musicViewModel.mainTabList) {
        tabList.clear()
        tabList.addAll(musicViewModel.mainTabList)
    }
//    LaunchedEffect(key1 = musicViewModel.showIndicatorMap) {
        showIndicator.value =
            musicViewModel.showIndicatorMap.getOrDefault(PlayListType.Songs.toString(), false)
//    }
    val pagerState = rememberPagerState { tabList.size }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val queueD = remember { mutableStateOf(false) }
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
                                    musicViewModel.songsList,
                                    showIndicator
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
                                    tracksList = musicViewModel.musicQueue,
                                    queueD
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



