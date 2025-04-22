package com.ztftrue.music.ui.home

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.ztftrue.music.MainActivity
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.QueuePlayList
import com.ztftrue.music.R
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
    val showIndicator = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val sharedPreferences =
        context.getSharedPreferences("list_indicator_config", Context.MODE_PRIVATE)
    val queueD = remember {
        mutableStateOf(sharedPreferences.getBoolean("show_queue_indicator", false))
    }
    val tabList = remember {
        mutableStateListOf<MainTab>()
    }
    LaunchedEffect(key1 = musicViewModel.mainTabList, key2 = musicViewModel.mainTabList.size) {
        tabList.clear()
        tabList.addAll(musicViewModel.mainTabList)
    }
    val pagerState: PagerState = rememberPagerState(initialPage = 0, pageCount = { tabList.size })
    LaunchedEffect(tabList, tabList.size) {
        if (pagerState.currentPage >= tabList.size) {
            scope.launch {
                pagerState.scrollToPage(0) // Reset to page 0 if currentPage is out of bounds
            }
        }
    }

//    LaunchedEffect(key1 = musicViewModel.showIndicatorMap) {
    showIndicator.value =
        musicViewModel.showIndicatorMap.getOrDefault(PlayListType.Songs.toString(), false)
//    }

    BackHandler(enabled = drawerState.isOpen) {
        if (drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        }
    }
    val paddingModifier = Modifier
        .padding(
            WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues()
        )
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
        Scaffold(
            modifier = Modifier
                .semantics {
                    contentDescription =
                        context.getString(R.string.this_is_main_page)
                },
            topBar = {
                MainTopBar(musicViewModel, drawerState, pagerState, navController)
            }, bottomBar = {
                Bottom(
                    musicViewModel, navController
                )
            }, content = {innerPadding->
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = tabList.size,
                    modifier = Modifier
                        .padding(innerPadding)
                        .semantics {
                            context
                                .getString(
                                    R.string.current_page_is,
                                    if (musicViewModel.mainTabList.size > pagerState.currentPage) musicViewModel.mainTabList[pagerState.currentPage].name else ""
                                )
                                .also { contentDescription = it }
                        },
                ) { page ->
                    when (tabList[page].type) {
                        PlayListType.Songs -> {
                            TracksListView(
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



