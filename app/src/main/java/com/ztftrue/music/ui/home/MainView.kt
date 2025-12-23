package com.ztftrue.music.ui.home

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.fastFilterNotNull
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.ztftrue.music.MainActivity
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.QueuePlayList
import com.ztftrue.music.R
import com.ztftrue.music.SongsPlayList
import com.ztftrue.music.play.MediaItemUtils
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.ui.other.DrawMenu
import com.ztftrue.music.ui.other.MainTopBar
import com.ztftrue.music.ui.public.Bottom
import com.ztftrue.music.ui.public.TracksListView
import com.ztftrue.music.utils.PlayListType
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MainView(
    musicViewModel: MusicViewModel,
    activity: MainActivity,
    navController: SnapshotStateList<Any>,
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
    LaunchedEffect(musicViewModel.songsList) {
        if (musicViewModel.songsList.isEmpty()) {
            musicViewModel.loadingTracks.value = true
            val futureResult: ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>? =
                musicViewModel.browser?.getChildren("songs_root", 0, Integer.MAX_VALUE, null)
            futureResult?.addListener({
                try {
                    val result: LibraryResult<ImmutableList<MediaItem>>? = futureResult.get()
                    if (result == null || result.resultCode != LibraryResult.RESULT_SUCCESS) {
                        return@addListener
                    }
                    val albumMediaItems: List<MediaItem> = result.value ?: listOf()
                    val list = albumMediaItems.map { mediaItem ->
                        MediaItemUtils.mediaItemToMusicItem(mediaItem)
                    }.fastFilterNotNull()
                    musicViewModel.songsList.addAll(list)
                } catch (e: Exception) {
                    // 处理在获取结果过程中可能发生的异常 (如 ExecutionException)
                    Log.e("Client", "Failed to toggle favorite status", e)
                }
                musicViewModel.loadingTracks.value = false
            }, ContextCompat.getMainExecutor(context))

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
            }, content = { innerPadding ->
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



