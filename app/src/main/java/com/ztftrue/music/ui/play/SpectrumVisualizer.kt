package com.ztftrue.music.ui.play

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
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
    modifier: Modifier = Modifier,
    maxHeightRatio: Float? = null,
    baseYRatio: Float? = null
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val effectiveMaxHeightRatio = maxHeightRatio ?: if (isLandscape) 0.78f else 0.48f
    val effectiveBaseYRatio = baseYRatio ?: if (isLandscape) 0.92f else 0.96f

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

    var cachedWidth by remember { mutableFloatStateOf(0f) }
    var cachedHeight by remember { mutableFloatStateOf(0f) }
    var cachedMaxBarH by remember { mutableFloatStateOf(0f) }
    var cachedBaseY by remember { mutableFloatStateOf(0f) }
    var cachedPrimary by remember { mutableStateOf(Color.Unspecified) }
    var cachedSecondary by remember { mutableStateOf(Color.Unspecified) }
    var cachedTertiary by remember { mutableStateOf(Color.Unspecified) }
    var cachedBarBrush by remember { mutableStateOf<Brush?>(null) }
    var cachedReflectionBrush by remember { mutableStateOf<Brush?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val windowInfo = LocalWindowInfo.current
    val isWindowFocused = windowInfo.isWindowFocused
    val isPlaying = musicViewModel.playStatus.value

    var isResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    isResumed = true
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    isResumed = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var isSettled by remember { mutableStateOf(!isPlaying) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            isSettled = false
        }
    }

    val shouldRun = isWindowFocused && isResumed && (isPlaying || !isSettled)

    // VSYNC-locked continuous frame clock (60Hz / 90Hz / 120Hz)
    var frameTick by remember { mutableLongStateOf(0L) }
    val lastFrameTimeHolder = remember { LongArray(1) { 0L } }

    LaunchedEffect(shouldRun) {
        if (!shouldRun) {
            lastFrameTimeHolder[0] = 0L
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos { frameNanos ->
                frameTick = frameNanos
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize().clipToBounds()) {
        if (!shouldRun && frameTick == 0L) return@Canvas
        val currentFrameNanos = frameTick
        if (currentFrameNanos == 0L) return@Canvas

        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val lastTime = lastFrameTimeHolder[0]
        val dt = if (lastTime != 0L) {
            ((currentFrameNanos - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        } else {
            0.016f
        }
        lastFrameTimeHolder[0] = currentFrameNanos

        val isPlaying = musicViewModel.playStatus.value

        // 1. Fetch latest audio magnitudes directly from repository (zero heap allocations)
        if (isPlaying) {
            val hasData = AudioDataRepository.getLatestVisualizationData(rawMagnitudes)
            if (!hasData) {
                rawMagnitudes.fill(0f)
            }
        } else {
            rawMagnitudes.fill(0f)
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
        // Prevents needle flicker between adjacent FFT bins
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

        // When paused, stop running once all bars and peaks have cleanly settled to 0
        if (!isPlaying && !isSettled) {
            var allZero = true
            for (i in 0 until barCount) {
                if (currentHeights[i] > 0.001f || peakHeights[i] > 0.001f) {
                    allZero = false
                    break
                }
            }
            if (allZero) {
                for (i in 0 until barCount) {
                    currentHeights[i] = 0f
                    peakHeights[i] = 0f
                    peakVelocities[i] = 0f
                    peakHoldTimers[i] = 0f
                    targetMagnitudes[i] = 0f
                    spatialMagnitudes[i] = 0f
                }
                isSettled = true
            }
        }

        val totalBarSpacingRatio = 0.3f
        val slotWidth = w / barCount
        val barWidth = slotWidth * (1f - totalBarSpacingRatio)
        val spacing = slotWidth * totalBarSpacingRatio
        val maxBarH = h * effectiveMaxHeightRatio
        val baseY = h * effectiveBaseYRatio

        // Cache brushes to avoid per-frame native shader allocation in onDraw
        if (w != cachedWidth || h != cachedHeight ||
            maxBarH != cachedMaxBarH || baseY != cachedBaseY ||
            primaryColor != cachedPrimary ||
            secondaryColor != cachedSecondary ||
            tertiaryColor != cachedTertiary ||
            cachedBarBrush == null
        ) {
            cachedWidth = w
            cachedHeight = h
            cachedMaxBarH = maxBarH
            cachedBaseY = baseY
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
