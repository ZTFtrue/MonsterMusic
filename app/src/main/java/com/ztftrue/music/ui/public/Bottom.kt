package com.ztftrue.music.ui.public

import android.graphics.Bitmap
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.sqlData.model.MusicItem

@UnstableApi
@Composable
fun Bottom(musicViewModel: MusicViewModel, navController: NavController) {
    val currentMusic: MusicItem? = musicViewModel.currentPlay.value
    val modifier = remember {
        Modifier.clickable {
            navController.navigate(Router.MusicPlayerView.withArgs())
        }
    }
    var paint by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(musicViewModel.currentPlay.value) {
        paint = musicViewModel.getCurrentMusicCover()
    }
    if (currentMusic == null) return
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(
            modifier = modifier
        ) {
            key(musicViewModel.currentPlay.value) {
                Image(
                    painter = rememberAsyncImagePainter(
                        paint ?: R.drawable.songs_thumbnail_cover
                    ),
                    contentDescription = "song cover",
                    modifier = Modifier
                        .width(50.dp)
                        .height(50.dp)
                        .aspectRatio(1f),
                )
                Column(
                    Modifier
                        .padding(start = 5.dp, end = 5.dp)
                        .fillMaxWidth(0.5f)
                ) {
                    Text(text = currentMusic.name, color = MaterialTheme.colorScheme.onBackground)
                    Text(text = currentMusic.artist, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            key(Unit) {
                Image(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = "skip previous",
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
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = "skip next",
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

    }
}