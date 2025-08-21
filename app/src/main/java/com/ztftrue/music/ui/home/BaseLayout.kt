package com.ztftrue.music.ui.home

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
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
import com.ztftrue.music.utils.stringToEnumForPlayListType

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun BaseLayout(
    musicViewModel: MusicViewModel,
    activity: MainActivity
) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                // GIF 解碼
                add(coil3.gif.GifDecoder.Factory())
                add(coil3.svg.SvgDecoder.Factory())
            }
            .build()
    }
    val window = LocalActivity.current!!.window
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars =
        MaterialTheme.colorScheme.background.luminance() > 0.5
//    window.navigationBarColor = MaterialTheme.colorScheme.background.toArgb()

    val navController: NavHostController = rememberNavController()
    musicViewModel.navController = navController
    val context = LocalContext.current

    CompositionLocalProvider(LocalContext provides context) {
        NavHost(
            navController = navController, startDestination = Router.MainView.route,
        ) {
            composable(route = Router.MainView.route) {
                key(Unit) {
                    if (musicViewModel.mainTabList.isNotEmpty()) {
                        MainView(musicViewModel, activity, navController)
                    }
                }
            }
            composable(
                route = Router.MusicPlayerView.route,
            ) { _ ->
                key(Unit) {
                    PlayingPage(navController, musicViewModel = musicViewModel)
                }
            }
            composable(
                route = Router.PlayListView.withArgs("id" to "{id}", "path" to "{path}", "itemType" to "{itemType}"),
            ) { backStackEntry ->

                val arg = backStackEntry.arguments
                if (arg != null) {
                    val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
                    TracksListPage(
                        musicViewModel = musicViewModel,
                        navController,
                        stringToEnumForPlayListType(arg.getString("itemType") ?: ""),
                        arg.getString("id")?.toLong() ?: 0,
                        encodedPath
                    )
                }
            }
            composable(
                route = Router.TracksSelectPage.withArgs("id" to "{id}", "name" to "{name}"), arguments = listOf(),
            ) { backStackEntry ->
                val arg = backStackEntry.arguments
                key(Unit) {
                    if (arg != null) {
                        TracksSelectPage(
                            musicViewModel = musicViewModel,
                            navController,
                            arg.getString("name"),
                            arg.getString("id")?.toLong()
                        )
                    }
                }
            }
            composable(
                route = Router.EditTrackPage.withArgs("id" to "{id}"), arguments = listOf(),
            ) { backStackEntry ->
                val arg = backStackEntry.arguments
                key(Unit) {
                    if (arg != null) {
                        val id = arg.getString("id")?.toLong()
                        if (id != null) {
                            EditTrackPage(
                                musicViewModel = musicViewModel,
                                navController,
                                id
                            )
                        }
                    }
                }
            }
            composable(
                route = Router.SettingsPage.route, arguments = listOf(),
            ) { _ ->
                key(Unit) {
                    SettingsPage(
                        musicViewModel = musicViewModel,
                        navController,
                    )
                }
            }
            composable(
                route = Router.QueuePage.route, arguments = listOf(),
            ) { _ ->
                key(Unit) {
                    QueuePage(
                        musicViewModel = musicViewModel,
                        navController,
                    )
                }
            }
            composable(
                route = Router.SearchPage.route, arguments = listOf(),
            ) { _ ->
                key(Unit) {
                    SearchPage(
                        musicViewModel = musicViewModel,
                        navController,
                    )
                }
            }
        }
    }

}