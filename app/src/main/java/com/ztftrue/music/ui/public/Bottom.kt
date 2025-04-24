package com.ztftrue.music.ui.public

import android.graphics.Bitmap
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.sqlData.model.MusicItem

@UnstableApi
@Composable
fun Bottom(musicViewModel: MusicViewModel, navController: NavHostController) {
    var currentMusic by remember { mutableStateOf<MusicItem?>(null) }
    val modifier = remember {
        Modifier.clickable {
            navController.navigate(Router.MusicPlayerView.withArgs()) {
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    var paint by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    LaunchedEffect(musicViewModel.currentPlay.value) {
        currentMusic = musicViewModel.currentPlay.value
        paint = musicViewModel.getCurrentMusicCover(context)
    }
    if (currentMusic == null) return
    BottomAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp), // padding 为 0
        containerColor = Color.Transparent, // 透明背景
        tonalElevation = 0.dp, // 阴影去掉（可选）
        contentPadding = PaddingValues(0.dp, 0.dp), // 内容也没有内边距
        actions = {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(0.dp)
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                key(musicViewModel.currentPlay.value) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            paint ?: R.drawable.songs_thumbnail_cover
                        ),
                        contentDescription = "song cover",
                        modifier = Modifier
                            .width(60.dp)
                            .height(60.dp)
                            .aspectRatio(1f),
                    )
                    val configuration = LocalConfiguration.current
                    Column(
                        Modifier
                            .padding(start = 5.dp, end = 5.dp, top = 5.dp, bottom = 5.dp)
                            .width((configuration.screenWidthDp - 220).dp)
                    ) {
                        Text(
                            text = currentMusic?.name ?: "",
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.horizontalScroll(
                                rememberScrollState(0)
                            )
                        )
                        Text(
                            text = currentMusic?.artist ?: "",
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.horizontalScroll(rememberScrollState(0))
                        )
                    }
                }

                key(Unit) {
                    Image(
                        painter = painterResource(R.drawable.play_previous_song),
                        contentDescription = "play previous song",
                        modifier = Modifier
                            .clickable {
                                musicViewModel.mediaController?.transportControls?.skipToPrevious()
                            }
                            .width(50.dp)
                            .height(50.dp)
                            .padding(10.dp),
                        colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)

                    )
                    Image(
                        painter = painterResource(
                            if (musicViewModel.playStatus.value) {
                                R.drawable.pause
                            } else {
                                R.drawable.play
                            }
                        ),
                        contentDescription = if (musicViewModel.playStatus.value) {
                            "pause"
                        } else {
                            "play"
                        },
                        modifier = Modifier
                            .clickable {
                                val pbState = musicViewModel.mediaController?.playbackState?.state
                                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                                    musicViewModel.mediaController?.transportControls?.pause()
                                } else {
                                    musicViewModel.mediaController?.transportControls?.play()
                                }
                            }
                            .width(50.dp)
                            .height(50.dp)
                            .padding(10.dp),
                        colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                    )
                    Image(
                        painter = painterResource(R.drawable.play_next_song),
                        contentDescription = "Play next song",
                        modifier = Modifier
                            .clickable {
                                musicViewModel.mediaController?.transportControls?.skipToNext()
                            }
                            .width(50.dp)
                            .height(50.dp)
                            .padding(10.dp),
                        colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                    )
                }
            }

        },
    )

}