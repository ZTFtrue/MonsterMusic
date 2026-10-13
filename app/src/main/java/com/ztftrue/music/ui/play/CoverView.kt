package com.ztftrue.music.ui.play

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ztftrue.music.ImageSource
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R

@OptIn(UnstableApi::class)
@Composable
fun CoverView(
    musicViewModel: MusicViewModel,
    isSelected: Boolean = true,
    onEnterFullscreen: (() -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val musicVisualizationEnable = remember { musicViewModel.musicVisualizationEnable }
    val showOtherMessage = remember { mutableStateOf(true) }
    val imageModel: ImageSource by musicViewModel.currentMusicCover
    val lifecycleOwner = LocalLifecycleOwner.current
    val windowInfo = LocalWindowInfo.current
    val isWindowFocused = windowInfo.isWindowFocused
    val context = LocalContext.current
    var isLifecycleActive by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, musicVisualizationEnable.value, isSelected, isWindowFocused) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    isLifecycleActive = true
                    if (musicVisualizationEnable.value && isSelected && isWindowFocused) {
                        musicViewModel.setVisualizationActive("CoverView", true)
                    } else {
                        musicViewModel.setVisualizationActive("CoverView", false)
                    }
                }

                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    isLifecycleActive = false
                    musicViewModel.setVisualizationActive("CoverView", false)
                }

                else -> Unit
            }
        }

        lifecycle.addObserver(observer)
        isLifecycleActive = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (isLifecycleActive && musicVisualizationEnable.value && isSelected && isWindowFocused) {
            musicViewModel.setVisualizationActive("CoverView", true)
        } else {
            musicViewModel.setVisualizationActive("CoverView", false)
        }
        onDispose {
            lifecycle.removeObserver(observer)
            isLifecycleActive = false
            musicViewModel.setVisualizationActive("CoverView", false)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clipToBounds()
            ) {
                val isCoverVisible = !musicVisualizationEnable.value || musicViewModel.showMusicCover.value
                if (isCoverVisible) {
                    key(musicViewModel.currentPlay.value) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageModel.asModel())
                                .crossfade(true)
                                .size(600, 600)
                                .build(),
                            contentDescription = stringResource(R.string.cover),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = Color.Black)
                    )
                }

                // If both cover and visualizer are shown, add a translucent dark scrim for vivid contrast
                if (musicVisualizationEnable.value && musicViewModel.showMusicCover.value && isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            onLongClick = {
                                showOtherMessage.value = !showOtherMessage.value
                            },
                            onClick = {
                            }
                        )
                )

                if (musicVisualizationEnable.value && isSelected) {
                    val mode = musicViewModel.visualizationMode.value
                    if (mode == "Spectrum") {
                        SpectrumVisualizer(
                            musicViewModel = musicViewModel,
                            modifier = Modifier.fillMaxSize().clipToBounds(),
                            maxHeightRatio = 0.48f,
                            baseYRatio = 0.96f
                        )
                    } else {
                        MatrixRainVisualizer(
                            musicViewModel = musicViewModel,
                            modifier = Modifier.fillMaxSize().clipToBounds()
                        )
                    }

                    // Fullscreen Button Overlay
                    if (onEnterFullscreen != null) {
                        IconButton(
                            onClick = onEnterFullscreen,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = stringResource(R.string.fullscreen),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            if (showOtherMessage.value && musicViewModel.currentPlay.value != null) {
                Column(Modifier.padding(15.dp)) {
                    AudioChainView(musicViewModel = musicViewModel)
                }
            }
        }
    }
}
