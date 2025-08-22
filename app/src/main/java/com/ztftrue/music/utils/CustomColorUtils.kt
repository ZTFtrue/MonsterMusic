package com.ztftrue.music.utils

import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor
object CustomColorUtils {
    fun isColorDark(color: Int): Boolean {
        // 计算相对亮度
        // Color.luminance = (0.299 * R + 0.587 * G + 0.114 * B) / 255
        val darkness: Double =
            1 - (0.299 * AndroidColor.red(color) + 0.587 * AndroidColor.green(color) + 0.114 * AndroidColor.blue(color)) / 255
        // 根据阈值判断是深色还是浅色
        return darkness >= 0.5
    }
    fun hexToColor(hex: String): Color {
        val sanitizedHex = hex.removePrefix("#")
        val colorLong = ("FF$sanitizedHex").toLong(16)
        return Color(colorLong)
    }
}