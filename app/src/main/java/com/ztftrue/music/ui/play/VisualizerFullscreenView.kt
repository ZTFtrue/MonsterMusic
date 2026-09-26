package com.ztftrue.music.ui.play

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ztftrue.music.ImageSource
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.manager.MediaCommands
import com.ztftrue.music.utils.CustomSlider
import com.ztftrue.music.utils.SharedPreferencesUtils
import com.ztftrue.music.utils.Utils
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

/**
 * Immersive fullscreen view for music visualization.
 * Supports full landscape and portrait display with auto-hiding playback controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerFullscreenView(
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

    // Keep screen on when in landscape mode
    DisposableEffect(isLandscape, activity) {
        val window = activity?.window
        if (isLandscape && window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (activity != null && activity.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Ensure audio processor visualization stream is connected in fullscreen view
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    musicViewModel.setVisualizationActive(true)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    musicViewModel.setVisualizationActive(false)
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            musicViewModel.setVisualizationActive(true)
        }
        onDispose {
            lifecycle.removeObserver(observer)
            musicViewModel.setVisualizationActive(false)
        }
    }

    // Handle Android system back gesture to exit fullscreen
    BackHandler {
        onDismiss()
    }

    // Auto-hide overlay controls after 3.5 seconds of inactivity
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3500)
            showControls = false
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
        // Optional album cover backdrop (when showMusicCover is enabled)
        if (musicViewModel.showMusicCover.value) {
            val imageModel: ImageSource by musicViewModel.currentMusicCover
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel.asModel())
                        .crossfade(true)
                        .size(1000, 1000)
                        .build(),
                    contentDescription = stringResource(R.string.cover),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight(if (isLandscape) 0.75f else 0.45f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }

        // Active visualizer engine filling 100% of the screen
        val mode = musicViewModel.visualizationMode.value
        if (mode == "Spectrum") {
            SpectrumVisualizer(
                musicViewModel = musicViewModel,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            MatrixRainVisualizer(
                musicViewModel = musicViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Overlay Header Controls
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
                        .padding(horizontal = 8.dp),
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

                // Options: Cover toggle, Mode Selector (Matrix / Spectrum), Orientation
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = musicViewModel.showMusicCover.value,
                        onClick = {
                            val newCoverState = !musicViewModel.showMusicCover.value
                            musicViewModel.showMusicCover.value = newCoverState
                            SharedPreferencesUtils.saveShowMusicCover(context, newCoverState)
                        },
                        label = { Text(stringResource(R.string.cover), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = Color.White.copy(alpha = 0.15f),
                            labelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = musicViewModel.visualizationMode.value == "Matrix",
                        onClick = {
                            musicViewModel.visualizationMode.value = "Matrix"
                            SharedPreferencesUtils.saveVisualizationMode(context, "Matrix")
                        },
                        label = { Text(stringResource(R.string.matrix), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = Color.White.copy(alpha = 0.15f),
                            labelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = musicViewModel.visualizationMode.value == "Spectrum",
                        onClick = {
                            musicViewModel.visualizationMode.value = "Spectrum"
                            SharedPreferencesUtils.saveVisualizationMode(context, "Spectrum")
                        },
                        label = { Text(stringResource(R.string.spectrum), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = Color.White.copy(alpha = 0.15f),
                            labelColor = Color.White
                        )
                    )

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
                            .semantics { contentDescription = "Fullscreen Slider" }
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
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (musicViewModel.playStatus.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (musicViewModel.playStatus.value) "Pause" else "Play",
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Next
                    IconButton(onClick = {
                        musicViewModel.browser?.seekToNextMediaItem()
                    }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }

                    // Repeat
                    IconButton(onClick = {
                        val nextMode = when (repeatModel) {
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                            else -> Player.REPEAT_MODE_ALL
                        }
                        repeatModel = nextMode
                        musicViewModel.repeatModel.intValue = nextMode
                        musicViewModel.browser?.repeatMode = nextMode
                    }) {
                        Icon(
                            imageVector = if (repeatModel == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatModel != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.LightGray
                        )
                    }
                }
            }
        }
    }
}
