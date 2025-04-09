package com.ztftrue.music.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.edit
import com.ztftrue.music.sqlData.model.MusicItem

object SharedPreferencesUtils {
    fun saveFontSize(context: Context, fontSize: Int) {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit {
            putInt("fontSize", fontSize)
        }
    }

    fun getFontSize(context: Context): Int {
        return context.getSharedPreferences("display", Context.MODE_PRIVATE).getInt("fontSize", 16)
    }

    fun saveDisplayAlign(context: Context, textAlign: TextAlign) {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit {
            putString("displayAlign", textAlign.toString())
        }
    }

    fun getDisplayAlign(context: Context): TextAlign {
        val t = context.getSharedPreferences("display", Context.MODE_PRIVATE)
            .getString("displayAlign", TextAlign.Center.toString())
        return TextAlign.values().firstOrNull { it.toString() == t.toString() }
            ?: TextAlign.Center
    }

    fun saveAutoScroll(context: Context, autoScroll: Boolean) {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit {
            putBoolean("AutoScroll", autoScroll)
        }
    }

    fun getAutoScroll(context: Context): Boolean {
        return context.getSharedPreferences("display", Context.MODE_PRIVATE)
            .getBoolean("AutoScroll", true)
    }

    fun saveAutoHighLight(context: Context, autoHighLight: Boolean) {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit {
            putBoolean("AutoHighLight", autoHighLight)
        }
    }

    fun getAutoHighLight(context: Context): Boolean {
        return context.getSharedPreferences("display", Context.MODE_PRIVATE)
            .getBoolean("AutoHighLight", true)
    }

    fun enableShuffle(context: Context, enable: Boolean) {
        context.getSharedPreferences("queue", Context.MODE_PRIVATE).edit {
            putBoolean("EnableShuffle", enable)
        }
    }

    fun getEnableShuffle(context: Context): Boolean {
        return context.getSharedPreferences("queue", Context.MODE_PRIVATE)
            .getBoolean("EnableShuffle", false)
    }

    fun getEnableMusicVisualization(context: Context): Boolean {
        return context.getSharedPreferences("Visualization", Context.MODE_PRIVATE)
            .getBoolean("Enable", false)
    }

    fun saveEnableMusicVisualization(context: Context, enable: Boolean) {
        context.getSharedPreferences("Visualization", Context.MODE_PRIVATE).edit {
            putBoolean("Enable", enable)
        }
    }

    fun getShowMusicCover(context: Context): Boolean {
        return context.getSharedPreferences("Visualization", Context.MODE_PRIVATE)
            .getBoolean("Cover", true)
    }

    fun saveShowMusicCover(context: Context, enable: Boolean) {
        context.getSharedPreferences("Visualization", Context.MODE_PRIVATE).edit {
            putBoolean("Cover", enable)
        }
    }

    fun saveSelectMusicId(context: Context, id: Long) {
        context.getSharedPreferences(
            "SelectedPlayTrack",
            Context.MODE_PRIVATE
        ).edit { putLong("SelectedPlayTrack", id) }
    }

    fun getCurrentPlayId(context: Context): Long {
        return context.getSharedPreferences(
            "SelectedPlayTrack",
            Context.MODE_PRIVATE
        ).getLong("SelectedPlayTrack", -1)
    }

    @SuppressLint("ApplySharedPref")
    fun saveCurrentDuration(context: Context, duration: Long) {
        context.getSharedPreferences(
            "SelectedPlayTrack",
            Context.MODE_PRIVATE
        ).edit(commit = true) { putLong("CurrentPosition", duration) }
    }

    @SuppressLint("ApplySharedPref")
    fun saveVolume(context: Context, volume: Int) {
        context.getSharedPreferences(
            "volume",
            Context.MODE_PRIVATE
        ).edit(commit = true) { putInt("volume", volume) }
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

    fun setWidgetData(context: Context, isPlaying: Boolean, currentPlayTrack: MusicItem?) {
        context.getSharedPreferences("Widgets", Context.MODE_PRIVATE).edit {
            putBoolean("playingStatus", isPlaying)
                .putString("title", currentPlayTrack?.name ?: "")
                .putLong("id", currentPlayTrack?.id ?: 0L)
                .putString("author", currentPlayTrack?.artist ?: "")
                .putString("path", currentPlayTrack?.path ?: "")
        }
    }

    fun getAutoPlayWaitTime(context: Context): Long {
        return context.getSharedPreferences("AutoPlay", Context.MODE_PRIVATE)
            .getLong("waitTime", 1000)
    }

    fun getAutoPlayEnable(context: Context): Boolean {
        return context.getSharedPreferences("AutoPlay", Context.MODE_PRIVATE)
            .getBoolean("enable", false)
    }

    fun setAutoPlayWaitTime(context: Context, waitTime: Long) {
        context.getSharedPreferences("AutoPlay", Context.MODE_PRIVATE).edit {
            putLong("waitTime", waitTime)
        }
    }

    fun setAutoPlayEnable(context: Context, enable: Boolean) {
        context.getSharedPreferences("AutoPlay", Context.MODE_PRIVATE).edit {
            putBoolean("enable", enable)
        }
    }

    fun getShowSlideIndicators(context: Context): Boolean {
        return context.getSharedPreferences("display", Context.MODE_PRIVATE)
            .getBoolean("lyrics_slider_indicators", false)
    }

    fun setShowSlideIndicators(context: Context, enable: Boolean) {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit {
            putBoolean("lyrics_slider_indicators", enable)
        }
    }

    fun getAutoToTopRandom(context: Context): Boolean {
        return context.getSharedPreferences("config", Context.MODE_PRIVATE)
            .getBoolean("auto_to_top_when_random", false)
    }

    fun setAutoToTopRandom(context: Context, enable: Boolean) {
        context.getSharedPreferences("config", Context.MODE_PRIVATE).edit {
            putBoolean("auto_to_top_when_random", enable)
        }
    }
    fun getCurrentLanguage(context: Context): String? {
        return context.getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("language", null)
    }

    fun setCurrentLanguage(context: Context, value: String) {
        context.getSharedPreferences("config", Context.MODE_PRIVATE).edit {
            putString("language", value)
        }
    }
}