package com.ztftrue.music.ui.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ztftrue.music.MusicViewModel
import kotlin.math.max

/**
 * Animated audio spectrum visualizer with frequency bars and falling peak indicators.
 */
@Composable
fun SpectrumVisualizer(
    musicViewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleActive by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isLifecycleActive = event.targetState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val magnitudes by musicViewModel.visualizationData.observeAsState(initial = emptyList())
    val isPlaying = musicViewModel.playStatus.value

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val barCount = 32
    val currentHeights = remember { FloatArray(barCount) }
    val peakHeights = remember { FloatArray(barCount) }
    val peakVelocities = remember { FloatArray(barCount) }

    var frameTick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isLifecycleActive, isPlaying) {
        if (!isLifecycleActive) return@LaunchedEffect
        var lastTime = 0L
        while (isLifecycleActive) {
            withFrameMillis { frameTime ->
                if (lastTime != 0L) {
                    val dt = ((frameTime - lastTime).coerceIn(1L, 100L)) / 1000f

                    for (i in 0 until barCount) {
                        val target = if (isPlaying && i < magnitudes.size) {
                            magnitudes[i].coerceIn(0f, 1f)
                        } else 0f

                        // Attack fast, decay smoothly
                        if (target > currentHeights[i]) {
                            currentHeights[i] = target
                        } else {
                            currentHeights[i] = max(0f, currentHeights[i] - dt * 2.2f)
                        }

                        // Peak hold and gravity fall
                        if (currentHeights[i] >= peakHeights[i]) {
                            peakHeights[i] = currentHeights[i]
                            peakVelocities[i] = 0f
                        } else {
                            peakVelocities[i] += dt * 1.8f // Gravity acceleration
                            peakHeights[i] = max(0f, peakHeights[i] - peakVelocities[i] * dt)
                        }
                    }
                    frameTick++
                }
                lastTime = frameTime
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize().clipToBounds()) {
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTick

        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val totalBarSpacingRatio = 0.3f
        val slotWidth = w / barCount
        val barWidth = slotWidth * (1f - totalBarSpacingRatio)
        val spacing = slotWidth * totalBarSpacingRatio
        val maxBarH = h * 0.85f
        val baseY = h * 0.92f

        val barBrush = Brush.verticalGradient(
            colors = listOf(tertiaryColor, secondaryColor, primaryColor),
            startY = baseY - maxBarH,
            endY = baseY
        )

        val reflectionBrush = Brush.verticalGradient(
            colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
            startY = baseY,
            endY = h
        )

        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        for (i in 0 until barCount) {
            val x = i * slotWidth + spacing / 2f
            val barH = max(4f, currentHeights[i] * maxBarH)
            val barTop = baseY - barH

            // 1. Draw main spectrum bar
            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, barTop),
                size = Size(barWidth, barH),
                cornerRadius = cornerRadius
            )

            // 2. Draw ground reflection
            val reflH = barH * 0.25f
            if (reflH > 2f) {
                drawRoundRect(
                    brush = reflectionBrush,
                    topLeft = Offset(x, baseY + 2f),
                    size = Size(barWidth, reflH),
                    cornerRadius = cornerRadius
                )
            }

            // 3. Draw peak-hold indicator
            val peakH = peakHeights[i] * maxBarH
            if (peakH > 4f) {
                val peakTop = (baseY - peakH - 4f).coerceAtLeast(0f)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = Offset(x, peakTop),
                    size = Size(barWidth, 3f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
            }
        }
    }
}
