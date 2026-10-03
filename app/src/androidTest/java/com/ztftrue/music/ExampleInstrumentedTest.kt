package com.ztftrue.music

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import com.ztftrue.music.utils.SortUtils

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(appContext.packageName.startsWith("com.ztftrue.music"))
    }

    @Test
    fun testSortUtilsOnDevice() {
        assertEquals("A", SortUtils.getSectionLabel("Apple"))
        assertEquals("啊", SortUtils.getSectionLabel("啊"))
        assertEquals("八", SortUtils.getSectionLabel("八方"))
        assertEquals("陈", SortUtils.getSectionLabel("陈奕迅"))
        assertEquals("周", SortUtils.getSectionLabel("周杰伦"))
        assertEquals("あ", SortUtils.getSectionLabel("あさ"))
        assertEquals("#", SortUtils.getSectionLabel("123"))
        assertEquals("#", SortUtils.getSectionLabel("!Special"))

        // Western before Chinese
        assertTrue(SortUtils.compareStrings("Zebra", "啊") < 0)
        // Chinese in Pinyin order
        assertTrue(SortUtils.compareStrings("Apple", "Banana") < 0)
        assertTrue(SortUtils.compareStrings("啊", "把") < 0)
        assertTrue(SortUtils.compareStrings("把", "陈奕迅") < 0)
        assertTrue(SortUtils.compareStrings("陈奕迅", "周杰伦") < 0)
        // Chinese before Japanese
        assertTrue(SortUtils.compareStrings("周杰伦", "あさ") < 0)
    }
}