package com.ztftrue.music.ui.home

import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.gif.GifDecoder
import coil3.svg.SvgDecoder
import com.ztftrue.music.MainActivity
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.Router
import com.ztftrue.music.ui.other.EditTrackPage
import com.ztftrue.music.ui.other.SearchPage
import com.ztftrue.music.ui.other.SettingsPage
import com.ztftrue.music.ui.other.TracksSelectPage
import com.ztftrue.music.ui.play.PlayingPage
import com.ztftrue.music.ui.public.QueuePage
import com.ztftrue.music.ui.public.TracksListPage

@OptIn(UnstableApi::class)
@Composable
fun BaseLayout(
    musicViewModel: MusicViewModel,
    activity: MainActivity
) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                // GIF 解碼
                add(GifDecoder.Factory())
                add(SvgDecoder.Factory())
            }
            .build()
    }
    val window = LocalActivity.current!!.window
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars =
        MaterialTheme.colorScheme.background.luminance() > 0.5
//    window.navigationBarColor = MaterialTheme.colorScheme.background.toArgb()
    val navController = remember { mutableStateListOf<Any>(Router.MainView) }
    musicViewModel.navController = navController
    val context = LocalContext.current
    CompositionLocalProvider(LocalContext provides context) {
        NavDisplay(
            backStack = navController,
            onBack = { navController.removeLastOrNull() },
            entryProvider = { key: Any ->
                when (key) {
                    is Router.MainView -> NavEntry(key) {
                        if (musicViewModel.mainTabList.isNotEmpty()) {
                            MainView(musicViewModel, activity, navController)
                        }
                    }

                    is Router.MusicPlayerView -> NavEntry(key) {
                        PlayingPage(navController, musicViewModel = musicViewModel)
                    }

                    is Router.PlayListView -> NavEntry(key) {
                        TracksListPage(
                            musicViewModel = musicViewModel,
                            navController,
                            key.listBase
                        )
                    }

                    is Router.TracksSelectPage -> NavEntry(key) {
                        TracksSelectPage(
                            musicViewModel = musicViewModel,
                            navController,
                            key.listBase.name,
                            key.listBase.id,
                        )
                    }

                    is Router.EditTrackPage -> NavEntry(key) {
                        EditTrackPage(
                            musicViewModel = musicViewModel,
                            navController,
                            key.music
                        )
                    }

                    is Router.SettingsPage -> NavEntry(key) {
                        SettingsPage(
                            musicViewModel = musicViewModel,
                            navController,
                        )
                    }

                    is Router.QueuePage -> NavEntry(key) {
                        QueuePage(
                            musicViewModel = musicViewModel,
                            navController,
                        )
                    }
                    is Router.SearchPage -> NavEntry(key) {
                        SearchPage(
                            musicViewModel = musicViewModel,
                            navController,
                        )
                    }
                    else -> NavEntry(Unit) { Text("Unknown route") }
                }
            },
        )
    }

}