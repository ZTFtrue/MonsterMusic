package com.ztftrue.music.ui.play

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun CoverView(musicViewModel: MusicViewModel) {
    val listState = rememberLazyListState()
    var paint by remember { mutableStateOf<Bitmap?>(null) }

    val showOtherMessage = remember { mutableStateOf(false) }
    LaunchedEffect(musicViewModel.currentPlay.value) {
        paint = musicViewModel.getCurrentMusicCover()
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        items(1) {
            Image(
                painter = rememberAsyncImagePainter(
                    paint ?: R.drawable.songs_thumbnail_cover
                ), contentDescription = stringResource(R.string.cover),
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)
                    .background(color = Color.Black)
                    .combinedClickable(
                        onLongClick = {
                            showOtherMessage.value = !showOtherMessage.value
                        },
                        onClick = {

                        }
                    )
            )
            if (showOtherMessage.value) {
                Column(Modifier.padding(15.dp)) {
                    musicViewModel.currentPlay.value?.let { it1 ->
                        Text(
                            text = it1.path, modifier =
                            Modifier
                                .padding(0.dp)
                                .horizontalScroll(rememberScrollState(0))
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,// Set the text color here
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        )
                        Text(
                            text = stringResource(R.string.artist, it1.artist), modifier =
                            Modifier
                                .padding(0.dp)
                                .height(30.dp)
                                .horizontalScroll(rememberScrollState(0))
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,// Set the text color here
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        )
                        Text(
                            text = stringResource(R.string.album, it1.album), modifier =
                            Modifier
                                .padding(top = 10.dp)
                                .horizontalScroll(rememberScrollState(0))
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,// Set the text color here
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        )
                    }
                }
            }
        }
    }
}