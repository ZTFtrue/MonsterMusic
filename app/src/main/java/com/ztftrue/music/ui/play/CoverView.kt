package com.ztftrue.music.ui.play

import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ztftrue.music.ImageSource
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.PlayService
import com.ztftrue.music.utils.Utils

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
    val context = LocalContext.current
    var isLifecycleActive by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, musicVisualizationEnable.value, isSelected) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isLifecycleActive = true
                    if (musicVisualizationEnable.value && isSelected) {
                        musicViewModel.setVisualizationActive(true)
                    }
                }

                Lifecycle.Event.ON_PAUSE -> {
                    isLifecycleActive = false
                    musicViewModel.setVisualizationActive(false)
                }

                else -> Unit
            }
        }

        lifecycle.addObserver(observer)
        isLifecycleActive = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (isLifecycleActive && musicVisualizationEnable.value && isSelected) {
            musicViewModel.setVisualizationActive(true)
        } else if (!musicVisualizationEnable.value || !isSelected) {
            musicViewModel.setVisualizationActive(false)
        }
        onDispose {
            lifecycle.removeObserver(observer)
            isLifecycleActive = false
            musicViewModel.setVisualizationActive(false)
        }
    }

    val isFocused = isSelected && isLifecycleActive
    val currentTrack = musicViewModel.currentPlay.value
    val isVideo = currentTrack != null && (Utils.isVideoPath(currentTrack.path) || currentTrack.id < 0)
    val showVideo = musicViewModel.videoSupportEnable.value && isVideo

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

                if (showVideo) {
                    VideoCoverView(
                        musicViewModel = musicViewModel,
                        isFocused = isFocused,
                        modifier = Modifier.fillMaxSize()
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
                            modifier = Modifier.fillMaxSize().clipToBounds()
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

@OptIn(UnstableApi::class)
@Composable
fun VideoCoverView(
    musicViewModel: MusicViewModel,
    isFocused: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { playerView ->
            val player = PlayService.exoPlayerInstance ?: musicViewModel.browser
            if (isFocused) {
                playerView.visibility = View.VISIBLE
                if (playerView.player != player) {
                    playerView.player = player
                }
            } else {
                if (playerView.player != null) {
                    playerView.player = null
                }
                PlayService.exoPlayerInstance?.clearVideoSurface()
                playerView.visibility = View.GONE
            }
        },
        onRelease = { playerView ->
            playerView.player = null
            PlayService.exoPlayerInstance?.clearVideoSurface()
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            PlayService.exoPlayerInstance?.clearVideoSurface()
        }
    }
}
