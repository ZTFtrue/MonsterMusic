package com.ztftrue.music.utils

import android.graphics.Color

object CustomColorUtils {
    fun isColorDark(color: Int): Boolean {
        // 计算相对亮度
        // Color.luminance = (0.299 * R + 0.587 * G + 0.114 * B) / 255
        val darkness: Double =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        // 根据阈值判断是深色还是浅色
        return darkness >= 0.5
    }

}