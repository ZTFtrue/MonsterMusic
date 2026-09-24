package com.ztftrue.music

import org.junit.Test
import kotlin.math.roundToInt

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
}