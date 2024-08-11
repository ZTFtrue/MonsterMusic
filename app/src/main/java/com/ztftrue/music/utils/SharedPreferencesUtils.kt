package com.ztftrue.music.utils

import android.annotation.SuppressLint
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
        return context.getSharedPreferences("display", Context.MODE_PRIVATE)
            .getBoolean("AutoScroll", true)
    }

    fun saveAutoHighLight(context: Context, autoHighLight: Boolean) {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit()
            .putBoolean("AutoHighLight", autoHighLight).apply()
    }

    fun getAutoHighLight(context: Context): Boolean {
        return context.getSharedPreferences("display", Context.MODE_PRIVATE)
            .getBoolean("AutoHighLight", true)
    }

    fun enableShuffle(context: Context, enable: Boolean) {
        context.getSharedPreferences("queue", Context.MODE_PRIVATE).edit()
            .putBoolean("EnableShuffle", enable).apply()
    }

    fun getEnableShuffle(context: Context): Boolean {
        return context.getSharedPreferences("queue", Context.MODE_PRIVATE)
            .getBoolean("EnableShuffle", false)
    }

    fun setCoverImage(context: Context, path: String) {
        context.getSharedPreferences("cover", Context.MODE_PRIVATE).edit().putString("image", path)
            .apply()
    }
    fun getCoverImage(context: Context): String {
        return context.getSharedPreferences("cover", Context.MODE_PRIVATE).getString("image", "")
            ?: ""
    }
    fun setAlwaysShowCover(context: Context, always: Boolean) {
        context.getSharedPreferences("cover", Context.MODE_PRIVATE).edit().putBoolean("always", always)
            .apply()
    }
    fun getAlwaysShowCover(context: Context ):Boolean {
        return context.getSharedPreferences("cover", Context.MODE_PRIVATE).getBoolean("always", false)
    }
      fun saveSelectMusicId(context: Context,id: Long) {
        context.getSharedPreferences(
            "SelectedPlayTrack",
            Context.MODE_PRIVATE
        ).edit().putLong("SelectedPlayTrack", id).apply()
    }

      fun getCurrentPlayId(context: Context): Long {
        return context.getSharedPreferences(
            "SelectedPlayTrack",
            Context.MODE_PRIVATE
        ).getLong("SelectedPlayTrack", -1)
    }

    @SuppressLint("ApplySharedPref")
      fun saveCurrentDuration(context: Context,duration: Long) {
        context.getSharedPreferences(
            "SelectedPlayTrack",
            Context.MODE_PRIVATE
        ).edit().putLong("CurrentPosition", duration).commit()
    }

    @SuppressLint("ApplySharedPref")
      fun saveVolume(context: Context,volume: Int) {
        context.getSharedPreferences(
            "volume",
            Context.MODE_PRIVATE
        ).edit().putInt("volume", volume).commit()
    }

      fun getVolume(context: Context): Int {
        return context.getSharedPreferences(
            "volume",
            Context.MODE_PRIVATE
        ).getInt("volume", 100)
    }

      fun getCurrentPosition(context: Context): Long {
        return context.getSharedPreferences(
            "SelectedPlayTrack",
            Context.MODE_PRIVATE
        ).getLong("CurrentPosition", 0)
    }
}