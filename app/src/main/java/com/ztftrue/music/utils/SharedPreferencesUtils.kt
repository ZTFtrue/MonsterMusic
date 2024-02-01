package com.ztftrue.music.utils

import android.content.Context
import androidx.compose.ui.text.style.TextAlign

object SharedPreferencesUtils {
    fun saveFontSize(context: Context, fontSize: Int) {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit()
            .putInt("fontSize", fontSize).apply()
    }

    fun getFontSize(context: Context): Int {
        return context.getSharedPreferences("display", Context.MODE_PRIVATE).getInt("fontSize", 16)
    }

    fun saveDisplayAlign(context: Context, textAlign: TextAlign) {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit()
            .putString("displayAlign", textAlign.toString()).apply()
    }

    fun getDisplayAlign(context: Context): TextAlign {
        val t = context.getSharedPreferences("display", Context.MODE_PRIVATE)
            .getString("displayAlign", TextAlign.Center.toString())
        return TextAlign.values().firstOrNull { it.toString() == t.toString() }
            ?: TextAlign.Center
    }
    fun saveAutoScroll(context: Context, autoScroll: Boolean) {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit()
            .putBoolean("AutoScroll", autoScroll).apply()
    }

    fun getAutoScroll(context: Context): Boolean {
        return context.getSharedPreferences("display", Context.MODE_PRIVATE).getBoolean("AutoScroll", true)
    }
    fun saveAutoHighLight(context: Context, autoHighLight: Boolean) {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit()
            .putBoolean("AutoHighLight", autoHighLight).apply()
    }

    fun getAutoHighLight(context: Context): Boolean {
        return context.getSharedPreferences("display", Context.MODE_PRIVATE).getBoolean("AutoHighLight", true)
    }
}