package com.ztftrue.music.utils

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange

object CustomColorUtils {
    fun isColorDark(color: Int): Boolean {
        // 计算相对亮度
        // Color.luminance = (0.299 * R + 0.587 * G + 0.114 * B) / 255
        val darkness: Double =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        // 根据阈值判断是深色还是浅色
        return darkness >= 0.5
    }

    fun getContrastingColor(color1: Int, color2: Int): Int {
        val averageColor = averageColor(color1, color2)
        return averageColor
    }

    fun averageColor(color1: Int, color2: Int): Int {
        val red = ((Color.red(color1) + Color.red(color2)) / 2).toInt()
        val green = (Color.green(color1) + Color.green(color2)) /  2.3
        val blue = (Color.blue(color1) + Color.blue(color2)) / 2
        return Color.rgb(red, green.toInt(), blue.toInt())
    }

    fun getContrastingColor(color: Int): Int {
        val luminance =
            (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255

        return if (luminance > 0.5) {
            blendARGB(color, Color.BLACK, 0.7f)
        } else {
            blendARGB(color, Color.WHITE, 0.7f)
        }
    }

    @ColorInt
    fun blendARGB(
        @ColorInt color1: Int, @ColorInt color2: Int,
        @FloatRange(from = 0.0, to = 1.0) ratio: Float
    ): Int {
        val inverseRatio = 1 - ratio
        val a = Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio
        val r = Color.red(color1) * inverseRatio + Color.red(color2) * ratio
        val g = Color.green(color1) * inverseRatio + Color.green(color2) * ratio
        val b = Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio
        return Color.argb(a.toInt(), r.toInt(), g.toInt(), b.toInt())
    }

    fun alpha(color: Int): Int {
        return color ushr 24
    }

    /**
     * Return the red component of a color int. This is the same as saying
     * (color >> 16) & 0xFF
     */
    fun red(color: Int): Int {
        return color shr 16 and 0xFF
    }

    /**
     * Return the green component of a color int. This is the same as saying
     * (color >> 8) & 0xFF
     */
    fun green(color: Int): Int {
        return color shr 8 and 0xFF
    }

    /**
     * Return the blue component of a color int. This is the same as saying
     * color & 0xFF
     */
    fun blue(color: Int): Int {
        return color and 0xFF
    }

    fun Int.toAndroidColor(): Int {
        return Color.argb(
            Color.alpha(this),
            Color.red(this),
            Color.green(this),
            Color.blue(this)
        )
    }
}