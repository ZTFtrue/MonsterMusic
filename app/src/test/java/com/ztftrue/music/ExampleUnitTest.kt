package com.ztftrue.music

import org.junit.Test
import kotlin.math.roundToInt
import com.ztftrue.music.ui.play.DualKnobHelper
import com.ztftrue.music.effects.SoundUtils

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun lyricsCenteringDelta_isCorrect() {
        val viewportHeight = 1000
        val targetCenter = viewportHeight / 2 // 500
        val beforeContentPadding = 500 // halfHeight

        // Case 1: Item is below center on screen
        val itemOffsetBelow = 200
        val itemSize = 60
        val itemScreenCenterBelow = itemOffsetBelow + beforeContentPadding + itemSize / 2 // 200 + 500 + 30 = 730
        val deltaBelow = (itemScreenCenterBelow - targetCenter).toFloat() // +230
        assertEquals(230f, deltaBelow, 0.001f)
        // After scrolling forward by deltaBelow (+230px, items move up):
        val newOffsetBelow = itemOffsetBelow - deltaBelow.toInt() // 200 - 230 = -30
        assertEquals(targetCenter, newOffsetBelow + beforeContentPadding + itemSize / 2) // -30 + 500 + 30 = 500 (centered!)

        // Case 2: Item is above center on screen
        val itemOffsetAbove = -300
        val itemScreenCenterAbove = itemOffsetAbove + beforeContentPadding + itemSize / 2 // -300 + 500 + 30 = 230
        val deltaAbove = (itemScreenCenterAbove - targetCenter).toFloat() // -270
        assertEquals(-270f, deltaAbove, 0.001f)
        // After scrolling backward by deltaAbove (-270px, items move down):
        val newOffsetAbove = itemOffsetAbove - deltaAbove.toInt() // -300 - (-270) = -30
        assertEquals(targetCenter, newOffsetAbove + beforeContentPadding + itemSize / 2) // -30 + 500 + 30 = 500 (centered!)

        // Case 3: Item is already exactly centered (top of item at -30px relative to content start)
        val itemOffsetCentered = -30
        val itemScreenCenter = itemOffsetCentered + beforeContentPadding + itemSize / 2 // -30 + 500 + 30 = 500
        val deltaCentered = (itemScreenCenter - targetCenter).toFloat() // 0
        assertEquals(0f, deltaCentered, 0.001f)

        // Case 4: Item 0 at list top (offset = 0)
        val item0Offset = 0
        val item0ScreenCenter = item0Offset + beforeContentPadding + itemSize / 2 // 0 + 500 + 30 = 530
        val delta0 = (item0ScreenCenter - targetCenter).toFloat() // +30
        assertEquals(30f, delta0, 0.001f)
    }

    @Test
    fun lyricsDragFraction_targetIndexCalculation() {
        val totalCaptions = 50
        val availableHeight = 800f

        // Top edge: fraction 0 -> index 0
        val fractionTop = (0f / availableHeight).coerceIn(0f, 1f)
        val indexTop = (fractionTop * (totalCaptions - 1)).roundToInt().coerceIn(0, totalCaptions - 1)
        assertEquals(0, indexTop)

        // Middle: fraction 0.5 -> 24.5 -> index 25
        val fractionMid = (400f / availableHeight).coerceIn(0f, 1f)
        val indexMid = (fractionMid * (totalCaptions - 1)).roundToInt().coerceIn(0, totalCaptions - 1)
        assertEquals(25, indexMid)

        // Bottom edge: fraction 1 -> index 49
        val fractionBottom = (800f / availableHeight).coerceIn(0f, 1f)
        val indexBottom = (fractionBottom * (totalCaptions - 1)).roundToInt().coerceIn(0, totalCaptions - 1)
        assertEquals(49, indexBottom)

        // Beyond bounds: clamped to valid range
        val fractionOver = (1200f / availableHeight).coerceIn(0f, 1f)
        val indexOver = (fractionOver * (totalCaptions - 1)).roundToInt().coerceIn(0, totalCaptions - 1)
        assertEquals(49, indexOver)

        val fractionUnder = (-200f / availableHeight).coerceIn(0f, 1f)
        val indexUnder = (fractionUnder * (totalCaptions - 1)).roundToInt().coerceIn(0, totalCaptions - 1)
        assertEquals(0, indexUnder)
    }

    @Test
    fun lyricsArrowOffset_pointedItemCalculation() {
        val viewportHeight = 1000f
        val viewportCenterY = viewportHeight / 2f // 500f
        val beforeContentPadding = 500 // halfHeight

        data class MockItem(val index: Int, val offset: Int, val size: Int) {
            val screenCenter: Float get() = offset + beforeContentPadding + size / 2f
        }

        val visibleItems = listOf(
            MockItem(index = 10, offset = -180, size = 60), // screenCenter -180 + 500 + 30 = 350
            MockItem(index = 11, offset = -110, size = 60), // screenCenter -110 + 500 + 30 = 420
            MockItem(index = 12, offset = -30, size = 60),  // screenCenter -30 + 500 + 30 = 500 (middle)
            MockItem(index = 13, offset = 40, size = 60),   // screenCenter 40 + 500 + 30 = 570
            MockItem(index = 14, offset = 110, size = 60),  // screenCenter 110 + 500 + 30 = 640
        )

        // 1. Arrow at resting position (offset = 0):
        val arrowOffsetYAtRest = 0f
        val pointedYAtRest = viewportCenterY + arrowOffsetYAtRest // 500
        val closestAtRest = visibleItems.minByOrNull { kotlin.math.abs(it.screenCenter - pointedYAtRest) }
        assertNotNull(closestAtRest)
        assertEquals(12, closestAtRest!!.index) // points to center item

        // 2. Arrow dragged upwards by 80px (offset = -80):
        val arrowOffsetYUp = -80f
        val pointedYUp = viewportCenterY + arrowOffsetYUp // 420
        val closestUp = visibleItems.minByOrNull { kotlin.math.abs(it.screenCenter - pointedYUp) }
        assertNotNull(closestUp)
        assertEquals(11, closestUp!!.index) // points to item 11

        // 3. Arrow dragged downwards by 135px (offset = +135):
        val arrowOffsetYDown = 135f
        val pointedYDown = viewportCenterY + arrowOffsetYDown // 635 (nearest 640)
        val closestDown = visibleItems.minByOrNull { kotlin.math.abs(it.screenCenter - pointedYDown) }
        assertNotNull(closestDown)
        assertEquals(14, closestDown!!.index) // points to item 14
    }

    @Test
    fun activeLyricIndex_advancementLogic() {
        data class TestCaption(val start: Long, val end: Long = 0L)

        val captions = listOf(
            TestCaption(1000L, 5000L),  // Line 0: 1s - 5s
            TestCaption(6000L, 10000L), // Line 1: 6s - 10s
            TestCaption(12000L, 15000L) // Line 2: 12s - 15s
        )

        fun getLrcIndex(time: Float): Int {
            var cIndex = 0
            for (index in captions.size - 1 downTo 0) {
                if (time >= captions[index].start) {
                    cIndex = index
                    break
                }
            }
            return cIndex
        }

        // Before first lyric (e.g. 500ms): should show line 0 (ready/intro)
        assertEquals(0, getLrcIndex(500f))

        // Right at line 0 start (1000ms)
        assertEquals(0, getLrcIndex(1000f))

        // Middle of line 0 (3000ms)
        assertEquals(0, getLrcIndex(3000f))

        // Advances to line 1 (6000ms)
        assertEquals(1, getLrcIndex(6000f))

        // During line 1 (8000ms)
        assertEquals(1, getLrcIndex(8000f))

        // Between line 1 and line 2 (11000ms): still line 1
        assertEquals(1, getLrcIndex(11000f))

        // Advances to line 2 (12000ms)
        assertEquals(2, getLrcIndex(12000f))

        // Seeking to end of song (50000ms): last line (line 2)
        assertEquals(2, getLrcIndex(50000f))
    }

    @Test
    fun lyricsSelectionDismissal_onTapElsewhere() {
        var isSelected = true
        var isToolbarShown = true
        var showMenu = false
        var selectionEpoch = 0
        var word = "test"
        var selectedTag = "test"

        val dismissAllPopupsAndSelection = {
            isToolbarShown = false
            showMenu = false
            isSelected = false
            word = ""
            selectedTag = ""
            selectionEpoch++
        }

        fun onPointerTap(distance: Float, touchSlop: Float): Boolean {
            val isPopupOpen = isToolbarShown || isSelected || showMenu
            if (!isPopupOpen) return false
            if (distance <= touchSlop) {
                dismissAllPopupsAndSelection()
                return true
            }
            return false
        }

        // 1. Text is selected, user taps elsewhere (distance = 2px, touchSlop = 8px)
        val handled = onPointerTap(distance = 2f, touchSlop = 8f)
        assertTrue(handled)
        assertFalse(isSelected)
        assertFalse(isToolbarShown)
        assertFalse(showMenu)
        assertEquals("", word)
        assertEquals("", selectedTag)
        assertEquals(1, selectionEpoch)

        // 2. Now nothing is selected, user taps again
        val handledWhenEmpty = onPointerTap(distance = 2f, touchSlop = 8f)
        assertFalse(handledWhenEmpty)
        assertEquals(1, selectionEpoch) // No unnecessary resets

        // 3. User selects a word (dictionary popup open)
        showMenu = true
        selectedTag = "word"
        word = "word"
        val handledMenu = onPointerTap(distance = 3f, touchSlop = 8f)
        assertTrue(handledMenu)
        assertFalse(showMenu)
        assertEquals("", selectedTag)
        assertEquals(2, selectionEpoch)

        // 4. User drags/scrolls (distance = 45px > touchSlop = 8px) -> should not be treated as tap dismissal
        isSelected = true
        val handledDrag = onPointerTap(distance = 45f, touchSlop = 8f)
        assertFalse(handledDrag)
        assertTrue(isSelected) // Preserved for drag / scroll handler
    }

    @Test
    fun dualKnob_decompositionAndRanges() {
        // Value 1.15f: Big knob 1.1f, small knob range 1.10f ~ 1.19f, small offset 5
        val h115 = DualKnobHelper.toTotalHundredths(1.15f)
        assertEquals(115, h115)
        assertEquals(1.1f, DualKnobHelper.getBigValue(h115), 0.001f)
        assertEquals(1.10f, DualKnobHelper.getSmallMin(h115), 0.001f)
        assertEquals(1.19f, DualKnobHelper.getSmallMax(h115), 0.001f)
        assertEquals(5, DualKnobHelper.getSmallOffset(h115))

        // Value 1.00f: Big knob 1.0f, small knob range 1.00f ~ 1.09f, small offset 0
        val h100 = DualKnobHelper.toTotalHundredths(1.00f)
        assertEquals(100, h100)
        assertEquals(1.0f, DualKnobHelper.getBigValue(h100), 0.001f)
        assertEquals(1.00f, DualKnobHelper.getSmallMin(h100), 0.001f)
        assertEquals(1.09f, DualKnobHelper.getSmallMax(h100), 0.001f)
        assertEquals(0, DualKnobHelper.getSmallOffset(h100))

        // Value 0.50f: Big knob 0.5f, small knob range 0.50f ~ 0.59f
        val h050 = DualKnobHelper.toTotalHundredths(0.50f)
        assertEquals(50, h050)
        assertEquals(0.5f, DualKnobHelper.getBigValue(h050), 0.001f)
        assertEquals(0.50f, DualKnobHelper.getSmallMin(h050), 0.001f)
        assertEquals(0.59f, DualKnobHelper.getSmallMax(h050), 0.001f)

        // Value 2.00f: Big knob 2.0f, small knob range 2.00f ~ 2.09f
        val h200 = DualKnobHelper.toTotalHundredths(2.00f)
        assertEquals(200, h200)
        assertEquals(2.0f, DualKnobHelper.getBigValue(h200), 0.001f)
        assertEquals(2.00f, DualKnobHelper.getSmallMin(h200), 0.001f)
        assertEquals(2.09f, DualKnobHelper.getSmallMax(h200), 0.001f)
    }

    @Test
    fun dualKnob_bigKnobStepAmount() {
        // "Change the pitch and speed by sliding it to a knob, and the change amount is 0.1 each time."
        val initial = 1.15f
        val steppedUp = DualKnobHelper.stepBig(initial, 1)
        assertEquals(1.25f, steppedUp, 0.001f)
        assertEquals(0.10f, steppedUp - initial, 0.001f)

        val steppedDown = DualKnobHelper.stepBig(initial, -1)
        assertEquals(1.05f, steppedDown, 0.001f)
        assertEquals(-0.10f, steppedDown - initial, 0.001f)

        // Stepping by 2 steps (+0.2)
        val steppedTwo = DualKnobHelper.stepBig(initial, 2)
        assertEquals(1.35f, steppedTwo, 0.001f)
    }

    @Test
    fun dualKnob_smallKnobStepAmountAndRangeCoupling() {
        // "Add a small knob next to it, and the change amount is 0.01 each time.
        // But the range is limited to one decimal place after the big knob.
        // For example, if the big knob is adjusted to 1.1, the adjustment range of the small knob is 1.10~1.19."
        val initial = 1.15f

        // Step up by 0.01
        val stepUp = DualKnobHelper.stepSmall(initial, 1)
        assertEquals(1.16f, stepUp, 0.001f)
        assertEquals(0.01f, stepUp - initial, 0.001f)

        // Step down by 0.01
        val stepDown = DualKnobHelper.stepSmall(initial, -1)
        assertEquals(1.14f, stepDown, 0.001f)
        assertEquals(-0.01f, stepDown - initial, 0.001f)

        // Upper bound restriction (1.19 max when big is 1.1)
        val atMax = 1.19f
        val tryOverMax = DualKnobHelper.stepSmall(atMax, 1)
        assertEquals(1.19f, tryOverMax, 0.001f) // Strictly clamped at 1.19, cannot jump to 1.20

        // Lower bound restriction (1.10 min when big is 1.1)
        val atMin = 1.10f
        val tryUnderMin = DualKnobHelper.stepSmall(atMin, -1)
        assertEquals(1.10f, tryUnderMin, 0.001f) // Strictly clamped at 1.10, cannot drop to 1.09
    }

    @Test
    fun dualKnob_boundaryClamping() {
        // Big knob clamping at MAX (2.0f)
        val atMaxBig = 2.05f
        val overMaxBig = DualKnobHelper.stepBig(atMaxBig, 1)
        assertEquals(2.05f, overMaxBig, 0.001f)

        // Big knob clamping at MIN (0.5f)
        val atMinBig = 0.55f
        val underMinBig = DualKnobHelper.stepBig(atMinBig, -1)
        assertEquals(0.55f, underMinBig, 0.001f)
    }

    @Test
    fun dualKnob_fullRangeContinuousStepping() {
        // Verify stepping from 1.0f all the way up to 2.0f (not stuck at 1.1)
        var v = 1.00f
        val expectedUp = listOf(1.10f, 1.20f, 1.30f, 1.40f, 1.50f, 1.60f, 1.70f, 1.80f, 1.90f, 2.00f)
        for (expected in expectedUp) {
            v = DualKnobHelper.stepBig(v, 1)
            assertEquals(expected, v, 0.001f)
        }
        // Further stepping up stays clamped at 2.00f
        v = DualKnobHelper.stepBig(v, 1)
        assertEquals(2.00f, v, 0.001f)

        // Verify stepping from 2.0f all the way down to 0.5f (not stuck at 0.9)
        val expectedDown = listOf(
            1.90f, 1.80f, 1.70f, 1.60f, 1.50f, 1.40f, 1.30f, 1.20f, 1.10f,
            1.00f, 0.90f, 0.80f, 0.70f, 0.60f, 0.50f
        )
        for (expected in expectedDown) {
            v = DualKnobHelper.stepBig(v, -1)
            assertEquals(expected, v, 0.001f)
        }
        // Further stepping down stays clamped at 0.50f
        v = DualKnobHelper.stepBig(v, -1)
        assertEquals(0.50f, v, 0.001f)
    }

    @Test
    fun fineSlider_stepCalculationsAndResolution() {
        // Coarse slider: range 0.5f..2.0f, steps = 14
        val coarseMin = 0.5f
        val coarseMax = 2.0f
        val coarseSteps = 14
        val coarseInterval = (coarseMax - coarseMin) / (coarseSteps + 1)
        assertEquals(0.1f, coarseInterval, 0.0001f)

        // Fine slider: range smallMin..smallMax (span 0.09f), steps = 8
        val fineSteps = 8
        for (tenths in 5..20) {
            val totalHundredths = tenths * 10
            val smallMin = DualKnobHelper.getSmallMin(totalHundredths)
            val smallMax = DualKnobHelper.getSmallMax(totalHundredths)
            val fineSpan = smallMax - smallMin
            assertEquals(0.09f, fineSpan, 0.0001f)

            val fineInterval = fineSpan / (fineSteps + 1)
            assertEquals(0.01f, fineInterval, 0.0001f)

            // Verify all 10 discrete steps from 0 to 9 hundredths
            for (step in 0..9) {
                val stepVal = (smallMin + step * fineInterval)
                val rounded = (stepVal * 100f).roundToInt() / 100f
                val expected = (tenths * 10 + step) / 100f
                assertEquals(expected, rounded, 0.0001f)
            }
        }
    }

    @Test
    fun fineSlider_rangeCouplingWithCoarseTenths() {
        // For coarse = 1.1x -> range is 1.10 ~ 1.19
        val h11 = DualKnobHelper.toTotalHundredths(1.1f)
        assertEquals(1.10f, DualKnobHelper.getSmallMin(h11), 0.0001f)
        assertEquals(1.19f, DualKnobHelper.getSmallMax(h11), 0.0001f)

        // For coarse = 0.5x -> range is 0.50 ~ 0.59
        val h05 = DualKnobHelper.toTotalHundredths(0.5f)
        assertEquals(0.50f, DualKnobHelper.getSmallMin(h05), 0.0001f)
        assertEquals(0.59f, DualKnobHelper.getSmallMax(h05), 0.0001f)

        // For coarse = 2.0x -> range is 2.00 ~ 2.09
        val h20 = DualKnobHelper.toTotalHundredths(2.0f)
        assertEquals(2.00f, DualKnobHelper.getSmallMin(h20), 0.0001f)
        assertEquals(2.09f, DualKnobHelper.getSmallMax(h20), 0.0001f)
    }

    @Test
    fun fineSlider_coarseOffsetRetention() {
        // When fine-tuning is enabled: coarse slider movement preserves the fine hundredths offset
        val initialPitch = 1.15f
        val pitchHundredths = DualKnobHelper.toTotalHundredths(initialPitch)
        val offset = DualKnobHelper.getSmallOffset(pitchHundredths)
        assertEquals(5, offset)

        // Drag coarse to 1.3:
        val targetCoarse = 1.3f
        val newBigTenths = (targetCoarse * 10f).roundToInt()
        val fineActiveNewPitch = (newBigTenths * 10 + offset) / 100f
        assertEquals(1.35f, fineActiveNewPitch, 0.0001f)

        // When fine-tuning is disabled: coarse slider snaps to tenths
        val fineInactiveNewPitch = newBigTenths / 10f
        assertEquals(1.30f, fineInactiveNewPitch, 0.0001f)
    }

    @Test
    fun soundUtils_downsampleMagnitudes_normalization() {
        // Silence input (all zeros): should produce 0.0f (representing minDb)
        val silence = FloatArray(128) { 0f }
        val downsampledSilence = SoundUtils.downsampleMagnitudes(silence, targetSize = 12, minDb = -60f, needNormalize = true)
        assertEquals(12, downsampledSilence.size)
        for (value in downsampledSilence) {
            assertEquals(0.0f, value, 0.0001f)
        }

        // Peak input (all 1.0f = 0dB relative to refValue 1.0): should produce 1.0f
        val peak = FloatArray(128) { 1.0f }
        val downsampledPeak = SoundUtils.downsampleMagnitudes(peak, targetSize = 12, minDb = -60f, needNormalize = true)
        assertEquals(12, downsampledPeak.size)
        for (value in downsampledPeak) {
            assertEquals(1.0f, value, 0.0001f)
        }

        // -20 dB input (amplitude = 0.1f since 20*log10(0.1) = -20 dB):
        // Normalized = (-20 - (-60)) / 60 = 40 / 60 = 2/3 ≈ 0.6667f
        val minus20Db = FloatArray(128) { 0.1f }
        val downsampled20Db = SoundUtils.downsampleMagnitudes(minus20Db, targetSize = 12, minDb = -60f, needNormalize = true)
        for (value in downsampled20Db) {
            assertEquals(2f / 3f, value, 0.001f)
        }

        // Monotonic test: louder signal strictly produces >= normalized magnitude
        val soft = FloatArray(128) { 0.01f } // -40dB -> 20/60 ≈ 0.333
        val medium = FloatArray(128) { 0.1f } // -20dB -> 40/60 ≈ 0.667
        val loud = FloatArray(128) { 0.5f }   // -6dB  -> 54/60 ≈ 0.900
        val downSoft = SoundUtils.downsampleMagnitudes(soft, targetSize = 12, minDb = -60f, needNormalize = true)
        val downMedium = SoundUtils.downsampleMagnitudes(medium, targetSize = 12, minDb = -60f, needNormalize = true)
        val downLoud = SoundUtils.downsampleMagnitudes(loud, targetSize = 12, minDb = -60f, needNormalize = true)
        for (i in 0 until 12) {
            assertTrue(downSoft[i] < downMedium[i])
            assertTrue(downMedium[i] < downLoud[i])
        }

        // Edge case: Empty input array produces targetSize array of 0f without crashing
        val emptyResult = SoundUtils.downsampleMagnitudes(FloatArray(0), targetSize = 12, needNormalize = true)
        assertEquals(12, emptyResult.size)
        for (v in emptyResult) assertEquals(0f, v, 0.0001f)
    }

    @Test
    fun visualizer_columnAndBandCalculations() {
        // Test column count calculation for various screen widths and column spacings
        val columnSpacing = 20f
        val portraitWidth = 1080f
        val portraitCols = maxOf(1, (portraitWidth / columnSpacing).toInt())
        assertEquals(54, portraitCols)

        val landscapeWidth = 2400f
        val landscapeCols = maxOf(1, (landscapeWidth / columnSpacing).toInt())
        assertEquals(120, landscapeCols)

        // Frequency band mapping for Matrix columns: 32 FFT frequency bands
        val freqBands = IntArray(landscapeCols) { c ->
            ((c.toFloat() / landscapeCols) * 32).toInt().coerceIn(0, 31)
        }
        // First column maps to band 0 (sub-bass)
        assertEquals(0, freqBands[0])
        // Last column maps to band 31 (high treble)
        assertEquals(31, freqBands[landscapeCols - 1])
        // Mid column maps to band 16 (midrange)
        assertEquals(16, freqBands[landscapeCols / 2])
    }

    @Test
    fun visualizer_spectrumPeakHoldPhysics() {
        // Physics verification of peak-hold dot behavior
        var smoothed = 0f
        var peak = 0f
        val smoothingFactor = 0.3f
        val gravity = 0.02f

        // Audio beat hits with high magnitude (0.8f)
        val hitEnergy = 0.8f
        smoothed += (hitEnergy - smoothed) * smoothingFactor
        if (smoothed > peak) peak = smoothed
        assertEquals(0.24f, smoothed, 0.001f)
        assertEquals(0.24f, peak, 0.001f)

        // Next frame, audio drops to silence
        smoothed += (0f - smoothed) * smoothingFactor
        peak = maxOf(smoothed, peak - gravity)
        assertEquals(0.168f, smoothed, 0.001f)
        assertEquals(0.22f, peak, 0.001f) // peak stayed higher than current bar, falling slowly with gravity
        assertTrue(peak > smoothed)
    }

    @Test
    fun soundUtils_downsampleMagnitudes_scaledRefValueAndTilt() {
        // Test with unnormalized native FFT size (512 bins, where full-scale peak is 256.0f)
        val fullScaleSineFft = FloatArray(512) { 0f }
        // Sine wave at bin 10 (~430Hz) with peak amplitude = 256.0f (0 dBFS)
        fullScaleSineFft[10] = 256.0f

        val downsampled = SoundUtils.downsampleMagnitudes(
            fullScaleSineFft,
            targetSize = 32,
            minDb = -60f,
            needNormalize = true,
            needPositive = false,
            refValue = 256.0f,
            tiltFactor = 1.0f
        )
        assertEquals(32, downsampled.size)
        // Active band should reach ~1.0f (loud)
        val maxBand = downsampled.maxOrNull() ?: 0f
        assertTrue("Max band should be near peak (> 0.9f) but was $maxBand", maxBand > 0.9f)
        // Silent bands should stay at 0.0f
        assertEquals(0.0f, downsampled[0], 0.001f)
        assertEquals(0.0f, downsampled[31], 0.001f)
    }

    @Test
    fun matrixRain_speedTracksFrequency() {
        val idleSpeed = 22f
        val peakSpeedBass = 680f
        val peakSpeedTreble = 880f

        fun computeSpeed(energy: Float, freqRatio: Float): Float {
            val peakSpeed = 680f + freqRatio * 200f
            val speedFactor = energy * energy * 0.35f + energy * 0.65f
            return idleSpeed + (peakSpeed - idleSpeed) * speedFactor
        }

        // 1. When silent, speed is idleSpeed
        val silentSpeed = computeSpeed(0.0f, 0.0f)
        assertEquals(22f, silentSpeed, 0.001f)

        // 2. Monotonic speed response: louder frequency band moves strictly faster
        val quietSpeed = computeSpeed(0.2f, 0.5f)
        val mediumSpeed = computeSpeed(0.5f, 0.5f)
        val loudSpeed = computeSpeed(0.8f, 0.5f)
        val peakSpeed = computeSpeed(1.0f, 0.5f)
        assertTrue(quietSpeed > silentSpeed)
        assertTrue(mediumSpeed > quietSpeed)
        assertTrue(loudSpeed > mediumSpeed)
        assertTrue(peakSpeed > loudSpeed)

        // 3. Peak speeds track frequency range:
        val bassPeak = computeSpeed(1.0f, 0.0f)
        val treblePeak = computeSpeed(1.0f, 1.0f)
        assertEquals(peakSpeedBass, bassPeak, 0.001f)
        assertEquals(peakSpeedTreble, treblePeak, 0.001f)
        assertTrue(treblePeak > bassPeak)

        // 4. Trail length (wavelength) tracks frequency inversely:
        // Bass columns have longer trails, treble columns have shorter crisp trails
        fun maxTrailForCol(freqRatio: Float): Int = (28 - freqRatio * 14).toInt()
        val bassTrailLen = maxTrailForCol(0.0f)
        val trebleTrailLen = maxTrailForCol(1.0f)
        assertEquals(28, bassTrailLen)
        assertEquals(14, trebleTrailLen)
        assertTrue(bassTrailLen > trebleTrailLen)
    }

    @Test
    fun exceptionFilter_identifiesHarmlessServiceUnbindException() {
        fun isHarmlessServiceUnbindException(e: Throwable): Boolean {
            var current: Throwable? = e
            while (current != null) {
                val msg = current.message ?: ""
                if (current is IllegalArgumentException && msg.contains("Service not registered")) {
                    return true
                }
                current = current.cause
            }
            return false
        }

        // Exact exception reported by user
        val userException = IllegalArgumentException(
            "Service not registered: androidx.media3.session.MediaControllerImplBase\$SessionServiceConnection@2a2f222"
        )
        assertTrue(isHarmlessServiceUnbindException(userException))

        // Wrapped in RuntimeException
        val wrappedException = RuntimeException("Wrapper", userException)
        assertTrue(isHarmlessServiceUnbindException(wrappedException))

        // Unrelated IllegalArgumentException should NOT be filtered
        val otherArgException = IllegalArgumentException("Invalid slider value: -1")
        assertFalse(isHarmlessServiceUnbindException(otherArgException))

        // Null pointer exception should NOT be filtered
        val npe = NullPointerException("Track item is null")
        assertFalse(isHarmlessServiceUnbindException(npe))
    }
}