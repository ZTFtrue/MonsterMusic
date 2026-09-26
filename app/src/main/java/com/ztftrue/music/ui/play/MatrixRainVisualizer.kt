package com.ztftrue.music.ui.play

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ztftrue.music.MusicViewModel
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * High-performance Matrix Digital Rain visualizer.
 * - Allocation-free simulation engine using primitive arrays.
 * - Dynamic beat reactivity: bass frequencies surge column speeds, treble creates code flicker.
 * - Scales smoothly across all screen orientations and resolutions (portrait, landscape, fullscreen).
 */
@Composable
fun MatrixRainVisualizer(
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

    val magnitudes by musicViewModel.visualizationData.observeAsState(initial = emptyList())
    val density = LocalDensity.current
    val columnSpacingPx = with(density) { 15.dp.toPx() }
    val charHeightPx = with(density) { 16.dp.toPx() }
    val textSizePx = with(density) { 13.dp.toPx() }

    val simulation = remember(columnSpacingPx, charHeightPx, textSizePx) {
        MatrixRainSimulation(
            columnSpacing = columnSpacingPx,
            charHeight = charHeightPx,
            textSize = textSizePx
        )
    }

    var frameTick by remember { mutableLongStateOf(0L) }

    val isPlaying = musicViewModel.playStatus.value

    LaunchedEffect(isLifecycleActive, isPlaying) {
        if (!isLifecycleActive || !isPlaying) return@LaunchedEffect
        var lastTime = 0L
        while (isLifecycleActive && isPlaying) {
            withFrameMillis { frameTime ->
                if (lastTime != 0L) {
                    val dt = (frameTime - lastTime).coerceIn(1L, 100L)
                    try {
                        simulation.update(dt.toFloat(), magnitudes, isPlaying)
                    } catch (_: Exception) {
                    }
                    frameTick++
                }
                lastTime = frameTime
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize().clipToBounds()) {
        // Read frameTick to trigger redraw per frame
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTick

        val width = size.width
        val height = size.height
        if (width > 0 && height > 0) {
            simulation.ensureDimensions(width, height)
            simulation.draw(drawContext.canvas.nativeCanvas, width, height)
        }
    }
}

class MatrixRainSimulation(
    private val columnSpacing: Float,
    private val charHeight: Float,
    private val textSize: Float
) {
    private val lock = Any()
    private var width = 0f
    private var height = 0f
    private var columnCount = 0

    // Simulation primitive arrays
    private var headY = FloatArray(0)
    private var speeds = FloatArray(0)
    private var trailLengths = IntArray(0)
    private var chars = Array(0) { CharArray(0) }
    private var mutationCounters = IntArray(0)

    // Smoothed audio envelope
    private val smoothedEnergy = FloatArray(32)

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = this@MatrixRainSimulation.textSize
        typeface = Typeface.MONOSPACE
        style = Paint.Style.FILL
    }

    // Pre-calculated color palette for trail (index 0 is head, followed by fading greens)
    private val maxTrail = 30
    private val trailColors = IntArray(maxTrail) { index ->
        when (index) {
            0 -> 0xFFFFFFFF.toInt()          // Bright white head
            1 -> 0xFFD0FFD0.toInt()          // Pale glowing green
            2 -> 0xFF55FF88.toInt()          // Neon cyber green
            3 -> 0xFF00EE55.toInt()          // Vibrant green
            else -> {
                val factor = (maxTrail - index).toFloat() / (maxTrail - 3)
                val alpha = (255 * factor.coerceIn(0.08f, 1f)).toInt()
                val green = (220 * factor.coerceIn(0.15f, 1f)).toInt()
                (alpha shl 24) or (green shl 8)
            }
        }
    }

    companion object {
        private val GLYPH_CHARS = (
            "0123456789" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "ｦｱｳｴｵｶｷｹｺｻｼｽｾｿﾀﾂﾃﾅﾆﾇﾈﾊﾋﾎﾏﾐﾑﾒﾓﾔﾕﾗﾘﾜ" +
            "αβγδεζηθλμξπρστυφχψωΩΣΔ#%*+=-/<>:;"
        ).toCharArray()

        fun getRandomGlyph(): Char {
            return GLYPH_CHARS[Random.nextInt(GLYPH_CHARS.size)]
        }
    }

    fun ensureDimensions(newWidth: Float, newHeight: Float) {
        synchronized(lock) {
            if (width == newWidth && height == newHeight && columnCount > 0) return

            width = newWidth
            height = newHeight
            val newColumnCount = max(1, (width / columnSpacing).toInt())

            if (newColumnCount != columnCount) {
                columnCount = newColumnCount
                headY = FloatArray(columnCount)
                speeds = FloatArray(columnCount)
                trailLengths = IntArray(columnCount)
                chars = Array(columnCount) { CharArray(maxTrail) }
                mutationCounters = IntArray(columnCount)

                for (c in 0 until columnCount) {
                    resetColumn(c, randomizeStart = true)
                }
            }
        }
    }

    private fun resetColumn(col: Int, randomizeStart: Boolean = false) {
        val freqRatio = if (columnCount > 1) col.toFloat() / (columnCount - 1) else 0.5f
        // Wavelength tracks frequency: Bass has longer trails (20-28), treble has shorter crisp trails (10-16)
        val maxLen = (28 - freqRatio * 14).toInt().coerceIn(12, maxTrail)
        val minLen = (maxLen - 5).coerceAtLeast(8)
        val trailLen = Random.nextInt(minLen, maxLen + 1)
        trailLengths[col] = trailLen
        // Subtle column speed variation (0.95f..1.05f) so columns are natural
        speeds[col] = Random.nextFloat() * 0.1f + 0.95f

        // Fill trail glyphs
        for (i in 0 until maxTrail) {
            chars[col][i] = getRandomGlyph()
        }

        mutationCounters[col] = Random.nextInt(3, 12)

        if (randomizeStart) {
            headY[col] = Random.nextFloat() * (height + 300f) - 300f
        } else {
            headY[col] = -Random.nextFloat() * 150f
        }
    }

    fun update(dtMs: Float, rawMagnitudes: List<Float>, isPlaying: Boolean) {
        synchronized(lock) {
            val dtSec = dtMs / 1000f

            // 1. Smooth audio magnitudes (attack fast, decay smoothly)
            val magSize = rawMagnitudes.size
            for (i in 0 until 32) {
                val target = if (i < magSize) rawMagnitudes[i].coerceIn(0f, 1f) else 0f
                if (target > smoothedEnergy[i]) {
                    smoothedEnergy[i] = target // Instant attack
                } else {
                    smoothedEnergy[i] = max(0f, smoothedEnergy[i] - dtSec * 3.5f) // Smooth decay
                }
            }

            // 2. Update columns: movement speed directly tracks the frequency
            for (c in 0 until columnCount) {
                val freqRatio = if (columnCount > 1) c.toFloat() / (columnCount - 1) else 0.5f
                val bandPos = freqRatio * 31f
                val bandLow = bandPos.toInt().coerceIn(0, 31)
                val bandHigh = minOf(31, bandLow + 1)
                val frac = bandPos - bandLow
                val energy = if (isPlaying) {
                    (smoothedEnergy[bandLow] * (1f - frac) + smoothedEnergy[bandHigh] * frac).coerceIn(0f, 1f)
                } else 0f

                // Movement speed directly tracks the frequency magnitude:
                // Fluid base speed, accelerating dynamically on loud frequency hits
                val idleSpeed = 45f + freqRatio * 20f
                val peakSpeed = 650f + freqRatio * 220f // Bass has heavy drops, treble has snappy darts
                val speedFactor = energy * energy * 0.3f + energy * 0.7f
                val currentSpeed = (idleSpeed + (peakSpeed - idleSpeed) * speedFactor) * speeds[c]

                headY[c] += currentSpeed * dtSec

                // Periodic glyph mutations (flicker rate tracks frequency energy)
                mutationCounters[c]--
                val mutationThreshold = if (energy > 0.5f) 2 else if (energy > 0.2f) 1 else 0
                if (mutationCounters[c] <= mutationThreshold) {
                    mutationCounters[c] = Random.nextInt(3, 14)
                    val mutateIdx = Random.nextInt(trailLengths[c])
                    chars[c][mutateIdx] = getRandomGlyph()
                }

                // Recycle column when trail has fully passed bottom
                val trailHeight = trailLengths[c] * charHeight
                if (headY[c] - trailHeight > height) {
                    resetColumn(c, randomizeStart = false)
                }
            }
        }
    }

    fun draw(canvas: android.graphics.Canvas, currentWidth: Float, currentHeight: Float) {
        synchronized(lock) {
            for (c in 0 until columnCount) {
                val x = c * columnSpacing + (columnSpacing - textSize) / 2f
                val head = headY[c]
                val len = trailLengths[c]

                for (i in 0 until len) {
                    val y = head - i * charHeight
                    // Only draw visible characters strictly within canvas height
                    if (y in -charHeight..currentHeight) {
                        textPaint.color = trailColors[min(i, maxTrail - 1)]
                        canvas.drawText(chars[c], i, 1, x, y, textPaint)
                    }
                }
            }
        }
    }
}
