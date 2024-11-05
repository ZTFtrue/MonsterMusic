package com.ztftrue.music.utils

import androidx.annotation.IntRange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderDefaults.colors
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

//val thumbRadius: Dp = 12.dp
//val trackHeight: Dp = 5.dp
//@Composable
//fun MySlider(
//    value: Float,
//    onValueChange: (Float) -> Unit,
//    modifier: Modifier = Modifier,
//    enabled: Boolean = true,
//    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
//    @IntRange(from = 0) steps: Int = 0,
//    onValueChangeFinished: (() -> Unit)? = null,
//    colors: SliderColors = SliderDefaults.colors(),
//    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
//) {
//
//    val activeTrackColorEnable = MaterialTheme.colorScheme.primary
//    val activeTrackColorDisable = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
//    val inactiveTrackColorEnable = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
//    val inactiveTrackColorDisable = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
//
//    val sliderWidth = remember { mutableIntStateOf(0) }
//    val sliderPosition = remember { mutableFloatStateOf(value) }
//    val allValue = remember { mutableFloatStateOf(valueRange.endInclusive - valueRange.start) }
//    val perStepWidthView = remember { mutableIntStateOf(sliderWidth.intValue / steps) }
//    val perStepWidthValue = remember { mutableFloatStateOf(allValue.floatValue / steps) }
//    val currentStepIndex =
//        remember { mutableFloatStateOf((sliderPosition.floatValue / perStepWidthValue.floatValue)) }
//    LaunchedEffect(key1 = valueRange.endInclusive, key2 = valueRange.start, key3 = steps) {
//        allValue.floatValue = valueRange.endInclusive - valueRange.start
//        perStepWidthValue.floatValue = allValue.floatValue / steps
//        perStepWidthView.intValue = sliderWidth.intValue / steps
//    }
//    LaunchedEffect(key1 = sliderWidth) {
//        allValue.floatValue = valueRange.endInclusive - valueRange.start
//        perStepWidthView.intValue = sliderWidth.intValue / steps
//        perStepWidthValue.floatValue = allValue.floatValue / steps
//    }
//    LaunchedEffect(key1 = value) {
//        sliderPosition.floatValue = value
//    }
//    LaunchedEffect(key1 = sliderPosition.floatValue) {
//        currentStepIndex.floatValue = (sliderPosition.floatValue / perStepWidthValue.floatValue)
//    }
//
//    Box(
//        modifier = modifier
//            .fillMaxWidth()
//            .height(thumbRadius * 4)
//            .pointerInput(Unit) {
//                if (!enabled) {
//                    return@pointerInput
//                }
//                detectHorizontalDragGestures(onDragStart = { offset ->
//                    val pointX = if (offset.x < 0) 0f else offset.x - thumbRadius.toPx()
//                    // current x , thumb radius
//                    var x = perStepWidthView.intValue *
//                            currentStepIndex.floatValue - thumbRadius.toPx()
//                    x = if (x < 0f) 0f else x
//                    if (abs(x - 2 * thumbRadius.toPx() - (pointX)) > 60.dp.toPx()) {
//                        sliderPosition.floatValue =
//                            ((pointX / sliderWidth.intValue) * allValue.floatValue).coerceIn(
//                                valueRange.start,
//                                valueRange.endInclusive
//                            )
//                    }
//                }, onDragEnd = {
//                    onValueChangeFinished?.invoke()
//                }) { change, dragAmount ->
//                    change.consume()
//                    sliderPosition.floatValue =
//                        (sliderPosition.floatValue + (dragAmount / sliderWidth.intValue) * allValue.floatValue).coerceIn(
//                            valueRange.start,
//                            valueRange.endInclusive
//                        )
//                    Log.d("Slider", sliderPosition.floatValue.toString())
//                    onValueChange(sliderPosition.floatValue)
//                }
//            }
//    ) {
//        Box(
//            modifier = modifier
//                .onSizeChanged { size ->
//                    sliderWidth.intValue = size.width  // Get the slider's width in pixels
//                }
//                .padding(horizontal = thumbRadius)
//                .fillMaxWidth()
//        ) {
//            // Track
//            Canvas(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(trackHeight)
//            ) {
//                val trackWidth = size.width
//                drawLine(
//                    color = if (enabled) inactiveTrackColorEnable else inactiveTrackColorDisable,
//                    start = Offset(
//                        perStepWidthView.intValue *
//                                currentStepIndex.floatValue, thumbRadius.toPx()
//                    ),
//                    end = Offset(trackWidth, thumbRadius.toPx()),
//                    strokeWidth = trackHeight.toPx()
//                )
//                // Draw active track (left side of the thumb)
//                drawLine(
//                    color = if (enabled) activeTrackColorEnable else activeTrackColorDisable,
//                    start = Offset(0f, thumbRadius.toPx()),
//                    end = Offset(
//                        perStepWidthView.intValue *
//                                currentStepIndex.floatValue, thumbRadius.toPx()
//                    ),
//                    strokeWidth = trackHeight.toPx()
//                )
//            }
//
//            // Thumb
//            Canvas(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(thumbRadius * 2)
//            ) {
//                val x = perStepWidthView.intValue *
//                        currentStepIndex.floatValue - thumbRadius.toPx()
//                drawCircle(
//                    color = if (enabled) activeTrackColorEnable else activeTrackColorDisable,
//                    radius = thumbRadius.toPx(),
//                    center = Offset(
//                        if (x < 0f) 0f else x, center.y
//                    )
//                )
//            }
//        }
//    }
//}

@Composable
@ExperimentalMaterial3Api
fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    @IntRange(from = 0) steps: Int = 0,
    thumb: @Composable (SliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = interactionSource,
            colors = colors,
            thumbSize = DpSize(20.dp, 20.dp),
            enabled = enabled
        )
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        SliderDefaults.Track(
            modifier = Modifier.height(4.dp),
            colors = colors(),
            sliderState = sliderState,
            thumbTrackGapSize = 0.dp,
            enabled = enabled,
            drawStopIndicator = {
//                drawStopIndicator(
//                    drawScope = this,
//                    offset = it,
//                    color = colors.trackColor(enabled, active = true),
//                    size = TrackStopIndicatorSize
//                )
            },
            drawTick = { offset, color ->
//                drawStopIndicator(drawScope = this, offset = offset, color = color, size = TickSize)
            },
        )
    },
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    Slider(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = remember { MutableInteractionSource() },
        thumb = thumb,
        track = track,
        enabled = enabled
    )
}
