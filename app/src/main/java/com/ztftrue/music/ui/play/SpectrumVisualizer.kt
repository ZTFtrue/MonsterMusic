package com.ztftrue.music.ui.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.ztftrue.music.play.AudioDataRepository
import kotlin.math.exp
import kotlin.math.max

/**
 * Animated audio spectrum visualizer with frequency bars and studio-grade peak indicators.
 * Optimized for zero-recomposition rendering, smooth exponential attack/decay physics,
 * and flutter-free peak holding.
 */
@Composable
fun SpectrumVisualizer(
    musicViewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleActive by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isLifecycleActive = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> isLifecycleActive = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isPlaying = musicViewModel.playStatus.value

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val barCount = 32
    val rawMagnitudes = remember { FloatArray(barCount) }
    val targetMagnitudes = remember { FloatArray(barCount) }
    val spatialMagnitudes = remember { FloatArray(barCount) }
    val currentHeights = remember { FloatArray(barCount) }
    val peakHeights = remember { FloatArray(barCount) }
    val peakVelocities = remember { FloatArray(barCount) }
    val peakHoldTimers = remember { FloatArray(barCount) }

    val peakColor = remember { Color.White.copy(alpha = 0.9f) }
    val peakCornerRadius = remember { CornerRadius(1.5f, 1.5f) }

    var cachedWidth by remember { mutableStateOf(0f) }
    var cachedHeight by remember { mutableStateOf(0f) }
    var cachedPrimary by remember { mutableStateOf(Color.Unspecified) }
    var cachedSecondary by remember { mutableStateOf(Color.Unspecified) }
    var cachedTertiary by remember { mutableStateOf(Color.Unspecified) }
    var cachedBarBrush by remember { mutableStateOf<Brush?>(null) }
    var cachedReflectionBrush by remember { mutableStateOf<Brush?>(null) }

    var frameTick by remember { mutableLongStateOf(0L) }

    fun hasActiveBars(): Boolean {
        for (i in 0 until barCount) {
            if (currentHeights[i] > 0.001f || peakHeights[i] > 0.001f) return true
        }
        return false
    }

    LaunchedEffect(isLifecycleActive, isPlaying) {
        if (!isLifecycleActive) return@LaunchedEffect
        var lastTime = 0L
        while (isLifecycleActive && (isPlaying || hasActiveBars())) {
            withFrameMillis { frameTime ->
                if (lastTime != 0L) {
                    val dt = ((frameTime - lastTime).coerceIn(1L, 100L)) / 1000f

                    // 1. Fetch latest audio magnitudes directly from repository (zero heap allocations)
                    if (isPlaying) {
                        AudioDataRepository.getLatestVisualizationData(rawMagnitudes)
                    }

                    // 2. Pre-filter target magnitudes to attenuate frame-to-frame phase flutter
                    val riseFilter = 1f - exp(-45f * dt)
                    val fallFilter = 1f - exp(-22f * dt)
                    for (i in 0 until barCount) {
                        val raw = if (isPlaying) rawMagnitudes[i].coerceIn(0f, 1f) else 0f
                        if (raw > targetMagnitudes[i]) {
                            targetMagnitudes[i] += (raw - targetMagnitudes[i]) * riseFilter
                        } else {
                            targetMagnitudes[i] += (raw - targetMagnitudes[i]) * fallFilter
                        }
                    }

                    // 3. Subtle spatial smoothing across adjacent frequency bands (0.07 - 0.86 - 0.07)
                    // Prevents 1-pixel needle flicker between adjacent FFT bins
                    for (i in 0 until barCount) {
                        val prev = if (i > 0) targetMagnitudes[i - 1] else targetMagnitudes[i]
                        val next = if (i < barCount - 1) targetMagnitudes[i + 1] else targetMagnitudes[i]
                        spatialMagnitudes[i] = targetMagnitudes[i] * 0.86f + (prev + next) * 0.07f
                    }

                    // 4. Physical bar dynamics (punchy exponential attack, smooth logarithmic decay)
                    val decayFactor = exp(-4.2f * dt)
                    for (i in 0 until barCount) {
                        val target = spatialMagnitudes[i]
                        if (target > currentHeights[i]) {
                            val diff = target - currentHeights[i]
                            val attackSpeed = if (diff > 0.25f) 42f else 30f
                            val attack = 1f - exp(-attackSpeed * dt)
                            currentHeights[i] += diff * attack
                        } else {
                            currentHeights[i] = max(0f, currentHeights[i] * decayFactor - dt * 0.12f)
                        }

                        // 5. Studio peak hold (holds peak steadily for 300ms, then falls under gravity)
                        if (currentHeights[i] >= peakHeights[i]) {
                            peakHeights[i] = currentHeights[i]
                            peakVelocities[i] = 0f
                            peakHoldTimers[i] = 0.30f // 300ms hold duration
                        } else {
                            if (peakHoldTimers[i] > 0f) {
                                peakHoldTimers[i] -= dt
                            } else {
                                peakVelocities[i] += dt * 3.6f // Gravity acceleration
                                peakHeights[i] = max(0f, peakHeights[i] - peakVelocities[i] * dt)
                            }
                        }
                    }
                    frameTick++
                }
                lastTime = frameTime
            }
        }

        // When paused and all bars have gracefully settled, clean arrays
        if (!isPlaying) {
            rawMagnitudes.fill(0f)
            targetMagnitudes.fill(0f)
            spatialMagnitudes.fill(0f)
            currentHeights.fill(0f)
            peakHeights.fill(0f)
            peakVelocities.fill(0f)
            peakHoldTimers.fill(0f)
            frameTick++
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

        // Cache brushes to avoid per-frame native shader allocation in onDraw
        if (w != cachedWidth || h != cachedHeight ||
            primaryColor != cachedPrimary ||
            secondaryColor != cachedSecondary ||
            tertiaryColor != cachedTertiary ||
            cachedBarBrush == null
        ) {
            cachedWidth = w
            cachedHeight = h
            cachedPrimary = primaryColor
            cachedSecondary = secondaryColor
            cachedTertiary = tertiaryColor
            cachedBarBrush = Brush.verticalGradient(
                colors = listOf(tertiaryColor, secondaryColor, primaryColor),
                startY = baseY - maxBarH,
                endY = baseY
            )
            cachedReflectionBrush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                startY = baseY,
                endY = h
            )
        }

        val barBrush = cachedBarBrush ?: return@Canvas
        val reflectionBrush = cachedReflectionBrush ?: return@Canvas
        val barCornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        for (i in 0 until barCount) {
            val x = i * slotWidth + spacing / 2f
            val curH = currentHeights[i]
            val barH = max(4f, curH * maxBarH)
            val barTop = baseY - barH

            // 1. Draw main spectrum bar
            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, barTop),
                size = Size(barWidth, barH),
                cornerRadius = barCornerRadius
            )

            // 2. Draw ground reflection
            val reflH = barH * 0.25f
            if (reflH > 2f) {
                drawRoundRect(
                    brush = reflectionBrush,
                    topLeft = Offset(x, baseY + 2f),
                    size = Size(barWidth, reflH),
                    cornerRadius = barCornerRadius
                )
            }

            // 3. Draw peak-hold indicator
            val peakH = peakHeights[i] * maxBarH
            if (peakH > 4f) {
                val peakTop = (baseY - peakH - 4f).coerceAtLeast(0f)
                drawRoundRect(
                    color = peakColor,
                    topLeft = Offset(x, peakTop),
                    size = Size(barWidth, 3f),
                    cornerRadius = peakCornerRadius
                )
            }
        }
    }
}
