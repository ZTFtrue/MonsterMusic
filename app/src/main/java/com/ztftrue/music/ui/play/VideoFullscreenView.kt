package com.ztftrue.music.ui.play

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.PlayService
import com.ztftrue.music.utils.CustomSlider
import com.ztftrue.music.utils.SharedPreferencesUtils
import com.ztftrue.music.utils.Utils
import kotlinx.coroutines.delay
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds

/**
 * Fullscreen video player supporting landscape and portrait orientations,
 * auto-hiding playback controls, seek slider, and immersive system bars handling.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoFullscreenView(
    musicViewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showControls by remember { mutableStateOf(true) }
    var repeatModel by remember { mutableIntStateOf(musicViewModel.repeatModel.intValue) }

    // Immersive fullscreen: hide system status and navigation bars
    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())

            onDispose {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        } else {
            onDispose {}
        }
    }

    // Keep screen on during fullscreen video playback
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleActive by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }

    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
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

    // Handle back gesture to exit fullscreen
    BackHandler {
        onDismiss()
    }

    // Auto-hide controls after 3.5 seconds when not touching slider
    LaunchedEffect(showControls, musicViewModel.sliderTouching) {
        if (showControls && !musicViewModel.sliderTouching) {
            delay(3500.milliseconds)
            showControls = false
        }
    }

    // Automatically exit fullscreen if track changes to non-video or video support disabled
    LaunchedEffect(musicViewModel.currentPlay.value, musicViewModel.videoSupportEnable.value) {
        val currentTrack = musicViewModel.currentPlay.value
        val isVideo = musicViewModel.videoSupportEnable.value &&
            currentTrack != null &&
            (Utils.isVideoPath(currentTrack.path) || currentTrack.id < 0)
        if (!isVideo) {
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Video Surface
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
                    setOnClickListener {
                        showControls = !showControls
                    }
                }
            },
            update = { playerView ->
                val player = PlayService.exoPlayerInstance ?: musicViewModel.browser
                if (isLifecycleActive) {
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

        // Top Overlay Header
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .statusBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {} // Prevent click-through
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Exit Fullscreen Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = stringResource(R.string.exit_fullscreen),
                        tint = Color.White
                    )
                }

                // Track Title & Artist
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = musicViewModel.currentPlay.value?.name ?: "",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = musicViewModel.currentPlay.value?.artist ?: "",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Orientation Toggle Button (Landscape / Portrait)
                IconButton(
                    onClick = {
                        if (activity != null) {
                            if (isLandscape) {
                                activity.requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                activity.requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = if (isLandscape) stringResource(R.string.portrait) else stringResource(R.string.landscape),
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Overlay Playback Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.70f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {} // Prevent click-through
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Seek Bar + Time display
                val duration = musicViewModel.currentPlay.value?.duration ?: 0L
                if (duration > 0) {
                    CustomSlider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .semantics { contentDescription = "Video Fullscreen Slider" }
                            .motionEventSpy {
                                when (it.action) {
                                    MotionEvent.ACTION_DOWN -> musicViewModel.sliderTouching = true
                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                        musicViewModel.browser?.seekTo(musicViewModel.sliderPosition.floatValue.toLong())
                                        musicViewModel.sliderTouching = false
                                    }
                                }
                            },
                        value = musicViewModel.sliderPosition.floatValue.coerceIn(0f, duration.toFloat()),
                        onValueChange = {
                            musicViewModel.sliderPosition.floatValue = it.roundToLong().toFloat()
                        },
                        valueRange = 0f..duration.toFloat(),
                        steps = 100,
                        onValueChangeFinished = {
                            musicViewModel.browser?.seekTo(musicViewModel.sliderPosition.floatValue.toLong())
                            musicViewModel.sliderTouching = false
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = Utils.formatTime(musicViewModel.sliderPosition.floatValue.toLong()),
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = Utils.formatTime(duration),
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                // Playback Control Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = {
                        val newMode = !musicViewModel.enableShuffleModel.value
                        musicViewModel.enableShuffleModel.value = newMode
                        musicViewModel.browser?.shuffleModeEnabled = newMode
                        SharedPreferencesUtils.enableShuffle(context, newMode)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (musicViewModel.enableShuffleModel.value) MaterialTheme.colorScheme.primary else Color.LightGray
                        )
                    }

                    // Previous
                    IconButton(onClick = {
                        if (musicViewModel.trackEffectEnabled.value) {
                            musicViewModel.resetAudioEffectsToDefault()
                        }
                        musicViewModel.browser?.seekToPreviousMediaItem()
                    }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }

                    // Play / Pause
                    IconButton(
                        onClick = {
                            if (musicViewModel.playStatus.value) {
                                musicViewModel.browser?.pause()
                            } else {
                                musicViewModel.browser?.play()
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (musicViewModel.playStatus.value) Icons.Outlined.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (musicViewModel.playStatus.value) "Pause" else "Play",
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }

                    // Next
                    IconButton(onClick = {
                        if (musicViewModel.trackEffectEnabled.value) {
                            musicViewModel.resetAudioEffectsToDefault()
                        }
                        musicViewModel.browser?.seekToNextMediaItem()
                    }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }

                    // Repeat Mode
                    IconButton(onClick = {
                        when (repeatModel) {
                            Player.REPEAT_MODE_ALL -> {
                                repeatModel = Player.REPEAT_MODE_ONE
                                musicViewModel.repeatModel.intValue = Player.REPEAT_MODE_ONE
                                musicViewModel.browser?.setRepeatMode(Player.REPEAT_MODE_ONE)
                            }
                            Player.REPEAT_MODE_ONE -> {
                                repeatModel = Player.REPEAT_MODE_OFF
                                musicViewModel.repeatModel.intValue = Player.REPEAT_MODE_OFF
                                musicViewModel.browser?.setRepeatMode(Player.REPEAT_MODE_OFF)
                            }
                            else -> {
                                repeatModel = Player.REPEAT_MODE_ALL
                                musicViewModel.repeatModel.intValue = Player.REPEAT_MODE_ALL
                                musicViewModel.browser?.setRepeatMode(Player.REPEAT_MODE_ALL)
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (repeatModel == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat Mode",
                            tint = if (repeatModel != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.LightGray
                        )
                    }
                }
            }
        }
    }
}
