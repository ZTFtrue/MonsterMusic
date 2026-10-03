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
        assertEquals("A", SortUtils.getSectionLabel("啊"))
        assertEquals("B", SortUtils.getSectionLabel("八方"))
        assertEquals("C", SortUtils.getSectionLabel("陈奕迅"))
        assertEquals("Z", SortUtils.getSectionLabel("周杰伦"))
        assertEquals("あ", SortUtils.getSectionLabel("あさ"))
        assertEquals("#", SortUtils.getSectionLabel("123"))
        assertEquals("#", SortUtils.getSectionLabel("!Special"))

        assertTrue(SortUtils.compareStrings("Apple", "Banana") < 0)
        assertTrue(SortUtils.compareStrings("啊", "把") < 0)
        assertTrue(SortUtils.compareStrings("陈奕迅", "周杰伦") < 0)
    }
}