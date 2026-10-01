package com.ztftrue.music.ui.play

import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.PlayService
import com.ztftrue.music.utils.Utils

@OptIn(UnstableApi::class)
@Composable
fun VideoView(
    musicViewModel: MusicViewModel,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onEnterFullscreen: (() -> Unit)? = null
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var isLifecycleActive by remember {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    isLifecycleActive = true
                }

                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    isLifecycleActive = false
                }

                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        isLifecycleActive = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        onDispose {
            lifecycle.removeObserver(observer)
            isLifecycleActive = false
        }
    }

    val isFocused = isSelected && isLifecycleActive
    val currentTrack = musicViewModel.currentPlay.value
    val isVideo = currentTrack != null && (Utils.isVideoPath(currentTrack.path) || currentTrack.id < 0)
    val videoSupportEnabled = musicViewModel.videoSupportEnable.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!videoSupportEnabled) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.VideocamOff,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = stringResource(R.string.video_support_disabled_tip),
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else if (!isVideo) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = stringResource(R.string.video_not_supported_or_not_video),
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
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
                        playerView.visibility = View.GONE
                    }
                },
                onRelease = { playerView ->
                    playerView.player = null
                }
            )

            if (onEnterFullscreen != null && isFocused) {
                IconButton(
                    onClick = onEnterFullscreen,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = stringResource(R.string.fullscreen),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
