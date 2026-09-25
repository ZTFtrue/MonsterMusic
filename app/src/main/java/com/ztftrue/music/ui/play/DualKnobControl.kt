package com.ztftrue.music.ui.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ztftrue.music.R
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure calculation logic for dual knob controls.
 * Ensures deterministic values, avoids floating-point precision errors,
 * and strictly constrains the fine knob to the tenths bracket of the coarse knob.
 */
object DualKnobHelper {
    const val MIN_BIG_TENTHS = 5   // 0.5x
    const val MAX_BIG_TENTHS = 20  // 2.0x

    /**
     * Converts a float value into total hundredths integer.
     * e.g. 1.15f -> 115
     */
    fun toTotalHundredths(value: Float): Int {
        return (value * 100f).roundToInt().coerceIn(MIN_BIG_TENTHS * 10, MAX_BIG_TENTHS * 10 + 9)
    }

    /**
     * Extracts the big knob tenths value as float.
     * e.g. 115 -> 1.1f
     */
    fun getBigValue(totalHundredths: Int): Float {
        val bigTenths = (totalHundredths / 10).coerceIn(MIN_BIG_TENTHS, MAX_BIG_TENTHS)
        return bigTenths / 10f
    }

    /**
     * Extracts the big knob tenths integer.
     * e.g. 115 -> 11
     */
    fun getBigTenths(totalHundredths: Int): Int {
        return (totalHundredths / 10).coerceIn(MIN_BIG_TENTHS, MAX_BIG_TENTHS)
    }

    /**
     * Extracts the fine hundredths offset (0..9).
     * e.g. 115 -> 5
     */
    fun getSmallOffset(totalHundredths: Int): Int {
        return (totalHundredths % 10).coerceIn(0, 9)
    }

    /**
     * Returns the small knob min range for the current big knob setting.
     * e.g. for big=1.1, min is 1.10f
     */
    fun getSmallMin(totalHundredths: Int): Float {
        val bigTenths = getBigTenths(totalHundredths)
        return (bigTenths * 10) / 100f
    }

    /**
     * Returns the small knob max range for the current big knob setting.
     * e.g. for big=1.1, max is 1.19f
     */
    fun getSmallMax(totalHundredths: Int): Float {
        val bigTenths = getBigTenths(totalHundredths)
        return (bigTenths * 10 + 9) / 100f
    }

    /**
     * Steps the big knob by deltaSteps (+1 or -1).
     * Changes the value by exactly 0.1, clamped between 0.5 and 2.0.
     * Preserves the fine hundredths offset.
     */
    fun stepBig(currentValue: Float, deltaSteps: Int): Float {
        val totalHundredths = toTotalHundredths(currentValue)
        val bigTenths = getBigTenths(totalHundredths)
        val smallOffset = getSmallOffset(totalHundredths)
        val newBigTenths = (bigTenths + deltaSteps).coerceIn(MIN_BIG_TENTHS, MAX_BIG_TENTHS)
        val newTotalHundredths = newBigTenths * 10 + smallOffset
        return newTotalHundredths / 100f
    }

    /**
     * Steps the small knob by deltaSteps (+1 or -1).
     * Changes the value by exactly 0.01, strictly clamped within [B, B + 0.09].
     */
    fun stepSmall(currentValue: Float, deltaSteps: Int): Float {
        val totalHundredths = toTotalHundredths(currentValue)
        val bigTenths = getBigTenths(totalHundredths)
        val smallOffset = getSmallOffset(totalHundredths)
        val newSmallOffset = (smallOffset + deltaSteps).coerceIn(0, 9)
        val newTotalHundredths = bigTenths * 10 + newSmallOffset
        return newTotalHundredths / 100f
    }
}

/**
 * A horizontal Edge Knob (roller / thumbwheel) composable.
 * Displays a recessed horizontal cylinder with 3D metallic vertical ribs that roll as you drag,
 * and a vertical accent indicator line representing the active position.
 * If the target range limit (min or max) is reached, the rolling animation stops.
 */
@Composable
fun HorizontalEdgeKnob(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    width: Dp? = null,
    height: Dp = 32.dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    stepThreshold: Dp = 14.dp,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    onStep: (Int) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val currentOnStep by rememberUpdatedState(onStep)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    var scrollOffsetPx by remember { mutableFloatStateOf(0f) }

    val fraction = if (valueRange.endInclusive > valueRange.start) {
        ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    } else {
        0f
    }

    val isAtMax = value >= valueRange.endInclusive - 0.0005f
    val isAtMin = value <= valueRange.start + 0.0005f

    val boxModifier = if (width != null) {
        modifier.size(width = width, height = height)
    } else {
        modifier.height(height)
    }

    Box(
        modifier = boxModifier
            .pointerInput(enabled, stepThreshold, isAtMax, isAtMin) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var accumulated = 0f
                    var isDrag = false
                    val pointerId = down.id
                    val touchSlop = viewConfiguration.touchSlop
                    val stepThresholdPx = stepThreshold.toPx()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) {
                            if (!isDrag) {
                                // Tap: right half = +1 step, left half = -1 step
                                val isRightHalf = change.position.x >= this@pointerInput.size.width / 2f
                                val step = if (isRightHalf) 1 else -1
                                val canTap = (step > 0 && !isAtMax) || (step < 0 && !isAtMin)
                                if (canTap) {
                                    currentOnStep(step)
                                    currentOnValueChangeFinished()
                                }
                            } else {
                                currentOnValueChangeFinished()
                            }
                            break
                        }

                        val dragAmount = change.position - change.previousPosition
                        // Dragging RIGHT (+x) increases value, dragging LEFT (-x) decreases value
                        val delta = dragAmount.x
                        if (kotlin.math.abs(change.position.x - down.position.x) > touchSlop ||
                            kotlin.math.abs(change.position.y - down.position.y) > touchSlop
                        ) {
                            isDrag = true
                        }

                        if (isDrag) {
                            change.consume()

                            // Stop animation if target range limit is reached
                            val canMove = when {
                                delta > 0 -> !isAtMax
                                delta < 0 -> !isAtMin
                                else -> true
                            }

                            if (canMove) {
                                scrollOffsetPx += dragAmount.x
                                accumulated += delta
                                if (accumulated >= stepThresholdPx) {
                                    val steps = (accumulated / stepThresholdPx).toInt()
                                    accumulated -= steps * stepThresholdPx
                                    currentOnStep(steps)
                                } else if (accumulated <= -stepThresholdPx) {
                                    val steps = ((-accumulated) / stepThresholdPx).toInt()
                                    accumulated += steps * stepThresholdPx
                                    currentOnStep(-steps)
                                }
                            } else {
                                // Target limit reached: stop animation and reset accumulated delta
                                accumulated = 0f
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

            // 1. Recessed dark slot background
            drawRoundRect(
                color = Color(0xFF141416),
                size = size,
                cornerRadius = cornerRadius
            )

            // 2. Vertical cylindrical shading across the height (simulating horizontal cylinder)
            val cylindricalShading = Brush.verticalGradient(
                0.0f to Color(0xFF0C0C0E),
                0.20f to Color(0xFF1C1C20),
                0.5f to Color(0xFF2C2C32),
                0.80f to Color(0xFF1C1C20),
                1.0f to Color(0xFF0C0C0E)
            )
            drawRoundRect(
                brush = cylindricalShading,
                size = size,
                cornerRadius = cornerRadius
            )

            // 3. Cylindrical vertical ribs (serrations on horizontal cylinder)
            val ribSpacing = 6.dp.toPx()
            val numRibs = (w / ribSpacing).toInt() + 2
            val normalizedOffset = (scrollOffsetPx % ribSpacing)
            val ribPaddingV = 2.dp.toPx()

            for (i in -1..numRibs) {
                val x = i * ribSpacing + normalizedOffset
                if (x in -2f..w + 2f) {
                    val distFromEdge = minOf(x, w - x).coerceAtLeast(0f)
                    val edgeAlpha = (distFromEdge / (10.dp.toPx())).coerceIn(0f, 1f)

                    if (edgeAlpha > 0.05f) {
                        // Highlight vertical line (left of rib)
                        val ribHighlightBrush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.2f to Color(0xFF70727A).copy(alpha = 0.4f * edgeAlpha),
                            0.5f to Color(0xFFB0B2BC).copy(alpha = 0.85f * edgeAlpha),
                            0.8f to Color(0xFF70727A).copy(alpha = 0.4f * edgeAlpha),
                            1.0f to Color.Transparent
                        )
                        drawLine(
                            brush = ribHighlightBrush,
                            start = Offset(x, ribPaddingV),
                            end = Offset(x, h - ribPaddingV),
                            strokeWidth = 1.2.dp.toPx()
                        )

                        // Shadow vertical line (right of rib)
                        val ribShadowBrush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.2f to Color(0xFF050508).copy(alpha = 0.5f * edgeAlpha),
                            0.5f to Color(0xFF08080C).copy(alpha = 0.85f * edgeAlpha),
                            0.8f to Color(0xFF050508).copy(alpha = 0.5f * edgeAlpha),
                            1.0f to Color.Transparent
                        )
                        drawLine(
                            brush = ribShadowBrush,
                            start = Offset(x + 1.2.dp.toPx(), ribPaddingV),
                            end = Offset(x + 1.2.dp.toPx(), h - ribPaddingV),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }

            // 4. Subtle left and right depth shadow vignettes
            val leftVignette = Brush.horizontalGradient(
                0f to Color(0xB3000000),
                8.dp.toPx() to Color.Transparent
            )
            drawRoundRect(
                brush = leftVignette,
                size = size,
                cornerRadius = cornerRadius
            )
            val rightVignette = Brush.horizontalGradient(
                (w - 8.dp.toPx()) to Color.Transparent,
                w to Color(0xB3000000)
            )
            drawRoundRect(
                brush = rightVignette,
                size = size,
                cornerRadius = cornerRadius
            )

            // 5. Active Indicator Bar (Vertical accent line moving horizontally)
            val indicatorWidth = 3.dp.toPx()
            val indicatorPadding = 3.dp.toPx()
            val usableWidth = w - 2 * indicatorPadding - indicatorWidth
            val indicatorX = indicatorPadding + fraction * usableWidth

            // Draw accent vertical indicator line
            drawRoundRect(
                color = if (enabled) indicatorColor else indicatorColor.copy(alpha = 0.4f),
                topLeft = Offset(indicatorX, 2.dp.toPx()),
                size = Size(indicatorWidth, h - 4.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )

            // Glow around indicator
            drawRoundRect(
                color = if (enabled) indicatorColor.copy(alpha = 0.35f) else Color.Transparent,
                topLeft = Offset(indicatorX - 1.dp.toPx(), 1.5.dp.toPx()),
                size = Size(indicatorWidth + 2.dp.toPx(), h - 3.dp.toPx()),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )

            // 6. Outer bezel border (light metallic specular rim)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                size = size,
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

/**
 * Single control block for Pitch or Speed.
 * Displayed as a full row with title/readout and dual horizontal Edge Knobs with increased diameter (32.dp).
 */
@Composable
fun SinglePitchOrSpeedEdgeControl(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    rollerHeight: Dp = 32.dp
) {
    var liveValue by remember(value) { mutableFloatStateOf(value) }
    LaunchedEffect(value) {
        liveValue = value
    }

    val totalHundredths = DualKnobHelper.toTotalHundredths(liveValue)
    val bigVal = DualKnobHelper.getBigValue(totalHundredths)
    val smallMin = DualKnobHelper.getSmallMin(totalHundredths)
    val smallMax = DualKnobHelper.getSmallMax(totalHundredths)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        // Title + Current Value Readout (e.g. "Pitch: 1.15x") and Fine Range (e.g. "Fine: 1.10~1.19")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$title: ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = String.format(Locale.ROOT, "%.2fx", liveValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = String.format(Locale.ROOT, "%s: %.2f~%.2f", stringResource(R.string.fine), smallMin, smallMax),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Coarse Row: "0.1" [Horizontal Edge Knob] "1.1x"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "0.1",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )

            HorizontalEdgeKnob(
                value = bigVal,
                valueRange = 0.5f..2.0f,
                height = rollerHeight,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                enabled = enabled,
                onStep = { steps ->
                    val newVal = DualKnobHelper.stepBig(liveValue, steps)
                    liveValue = newVal
                    onValueChange(newVal)
                },
                onValueChangeFinished = onValueChangeFinished
            )

            Text(
                text = String.format(Locale.ROOT, "%.1fx", bigVal),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Fine Row: "0.01" [Horizontal Edge Knob] "1.15x"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "0.01",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )

            HorizontalEdgeKnob(
                value = liveValue,
                valueRange = smallMin..smallMax,
                height = rollerHeight,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                enabled = enabled,
                onStep = { steps ->
                    val newVal = DualKnobHelper.stepSmall(liveValue, steps)
                    liveValue = newVal
                    onValueChange(newVal)
                },
                onValueChangeFinished = onValueChangeFinished
            )

            Text(
                text = String.format(Locale.ROOT, "%.2fx", liveValue),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Pitch and Speed controls split into TWO SEPARATE ROWS.
 * Pitch is on Row 1, Speed is on Row 2, separated by a horizontal divider.
 * Features enlarged Edge Roller diameter (32.dp) with horizontal scrolling.
 */
@Composable
fun PitchAndSpeedTwoRows(
    pitch: Float,
    onPitchChange: (Float) -> Unit,
    onPitchChangeFinished: () -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onSpeedChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    rollerHeight: Dp = 32.dp
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Row 1: Pitch Control
        SinglePitchOrSpeedEdgeControl(
            title = stringResource(R.string.pitch).trimEnd(':', ' '),
            value = pitch,
            onValueChange = onPitchChange,
            onValueChangeFinished = onPitchChangeFinished,
            enabled = enabled,
            rollerHeight = rollerHeight
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        // Row 2: Speed Control
        SinglePitchOrSpeedEdgeControl(
            title = stringResource(R.string.speed).trimEnd(':', ' '),
            value = speed,
            onValueChange = onSpeedChange,
            onValueChangeFinished = onSpeedChangeFinished,
            enabled = enabled,
            rollerHeight = rollerHeight
        )
    }
}

/**
 * Backward compatibility wrapper for PitchAndSpeedRow.
 * Renders Pitch and Speed as two separate rows with increased Edge Roller diameter.
 */
@Composable
fun PitchAndSpeedRow(
    pitch: Float,
    onPitchChange: (Float) -> Unit,
    onPitchChangeFinished: () -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onSpeedChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    PitchAndSpeedTwoRows(
        pitch = pitch,
        onPitchChange = onPitchChange,
        onPitchChangeFinished = onPitchChangeFinished,
        speed = speed,
        onSpeedChange = onSpeedChange,
        onSpeedChangeFinished = onSpeedChangeFinished,
        modifier = modifier,
        enabled = enabled
    )
}

/**
 * Backwards compatibility wrapper for DualKnobPitchSpeedControl.
 */
@Composable
fun DualKnobPitchSpeedControl(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    SinglePitchOrSpeedEdgeControl(
        title = title,
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier,
        enabled = enabled
    )
}
