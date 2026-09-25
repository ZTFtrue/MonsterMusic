package com.ztftrue.music.utils

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.ztftrue.music.R
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

    fun saveShowFolderPath(context: Context, value: Boolean) {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit {
            putBoolean("show_folder_path", value)
        }
    }

    fun getShowFolderPath(context: Context): Boolean {
        return context.getSharedPreferences("display", Context.MODE_PRIVATE)
            .getBoolean("show_folder_path", false)
    }

    fun saveShowFolderTree(context: Context, value: Boolean) {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit {
            putBoolean("show_folder_tree", value)
        }
    }

    fun getShowFolderTree(context: Context): Boolean {
        return context.getSharedPreferences("display", Context.MODE_PRIVATE)
            .getBoolean("show_folder_tree", false)
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

    fun getEqualizerType(context: Context): Int {
        return context.getSharedPreferences("Equalizer", Context.MODE_PRIVATE)
            .getInt("EqualizerType", 0)
    }

    fun saveEqualizerType(context: Context, type: Int) {
        context.getSharedPreferences("Equalizer", Context.MODE_PRIVATE).edit {
            putInt("EqualizerType", type)
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

    fun saveCurrentPlayMusicId(context: Context, id: Long) {
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

    fun setWidgetEnable(context: Context, enable: Boolean) {
        context.getSharedPreferences("Widgets", Context.MODE_PRIVATE).edit(commit = true) {
            putBoolean(
                "enable",
                enable
            )
        }
    }

    fun getWidgetEnable(context: Context): Boolean {
        return context.getSharedPreferences("Widgets", Context.MODE_PRIVATE).getBoolean(
            "enable",
            false
        )
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

    fun getShowRightIndicator(context: Context): Boolean {
        return context.getSharedPreferences("display", Context.MODE_PRIVATE)
            .getBoolean("lyrics_right_indicator", true)
    }

    fun setShowRightIndicator(context: Context, enable: Boolean) {
        context.getSharedPreferences("display", Context.MODE_PRIVATE).edit {
            putBoolean("lyrics_right_indicator", enable)
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

    fun getMergeAlbum(context: Context): Boolean {
        return context.getSharedPreferences("config", Context.MODE_PRIVATE)
            .getBoolean("merge_album", false)
    }

    fun setMergeAlbum(context: Context, enable: Boolean) {
        context.getSharedPreferences("config", Context.MODE_PRIVATE).edit {
            putBoolean("merge_album", enable)
        }
    }

    fun getIgnoreDuration(context: Context): Long {
        return context.getSharedPreferences("scan_config", Context.MODE_PRIVATE)
            .getLong("ignore_duration", 0L)
    }

    fun setIgnoreDuration(context: Context, durationValue: Long) {
        context.getSharedPreferences("scan_config", Context.MODE_PRIVATE).edit {
            putLong("ignore_duration", durationValue)
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

    fun setAutoHandleAudioFocus(context: Context, enable: Boolean) {
        context.getSharedPreferences("config", Context.MODE_PRIVATE).edit(commit = true) {
            putBoolean("auto_handle_audio_focus", enable)
        }
    }

    fun getAutoHandleAudioFocus(context: Context): Boolean {
        return context.getSharedPreferences("config", Context.MODE_PRIVATE)
            .getBoolean("auto_handle_audio_focus", true)
    }


    fun setTrackCoverData(context: Context, coverPath: String) {
        context.getSharedPreferences("Cover", Context.MODE_PRIVATE).edit {
            putString("path", coverPath)
        }
    }

    fun getTrackCoverData(context: Context): String? {
        return context.getSharedPreferences("Cover", Context.MODE_PRIVATE).getString("path", "")
    }

    fun setWidgetBackground(context: Context, color: String, appWidgetId: Int? = null) {
        context.getSharedPreferences("config", Context.MODE_PRIVATE).edit(commit = true) {
            putString("widget_background", color)
            if (appWidgetId != null && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                putString("widget_background_$appWidgetId", color)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun getWidgetBackground(context: Context, appWidgetId: Int? = null): String {
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        val defaultColor = "#" + ContextCompat.getColor(context, R.color.light_blue_900).toHexString()
        if (appWidgetId != null && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val specific = prefs.getString("widget_background_$appWidgetId", null)
            if (specific != null) return specific
        }
        return prefs.getString("widget_background", defaultColor) ?: defaultColor
    }

    fun setWidgetTextContrast(context: Context, mode: String, appWidgetId: Int? = null) {
        context.getSharedPreferences("config", Context.MODE_PRIVATE).edit(commit = true) {
            putString("widget_text_contrast", mode)
            if (appWidgetId != null && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                putString("widget_text_contrast_$appWidgetId", mode)
            }
        }
    }

    fun getWidgetTextContrast(context: Context, appWidgetId: Int? = null): String {
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        if (appWidgetId != null && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val specific = prefs.getString("widget_text_contrast_$appWidgetId", null)
            if (specific != null) return specific
        }
        return prefs.getString("widget_text_contrast", "auto") ?: "auto"
    }

    fun removeWidgetConfig(context: Context, appWidgetId: Int) {
        context.getSharedPreferences("config", Context.MODE_PRIVATE).edit(commit = true) {
            remove("widget_background_$appWidgetId")
            remove("widget_text_contrast_$appWidgetId")
        }
    }

    fun setAutoDismissDicPop(context: Context, auto: Boolean) {
        context.getSharedPreferences("config", Context.MODE_PRIVATE).edit {
            putBoolean("auto_dismiss_dic_pop", auto)
        }
    }

    fun getAutoDismissDicPop(context: Context): Boolean {
        return context.getSharedPreferences("config", Context.MODE_PRIVATE)
            .getBoolean("auto_dismiss_dic_pop", false)
    }

    // ==========================================
    // Audio Effects Settings
    // ==========================================
    private const val PREFS_AUDIO_EFFECTS = "audio_effects_prefs"

    // Reverb
    fun saveReverbEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putBoolean("reverb_enabled", enabled)
        }
    }
    fun getReverbEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getBoolean("reverb_enabled", false)
    }
    fun saveReverbRoomSize(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("reverb_room_size", value)
        }
    }
    fun getReverbRoomSize(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("reverb_room_size", 0.5f)
    }
    fun saveReverbDamping(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("reverb_damping", value)
        }
    }
    fun getReverbDamping(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("reverb_damping", 0.5f)
    }
    fun saveReverbMix(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("reverb_mix", value)
        }
    }
    fun getReverbMix(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("reverb_mix", 0.3f)
    }

    // Chorus
    fun saveChorusEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putBoolean("chorus_enabled", enabled)
        }
    }
    fun getChorusEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getBoolean("chorus_enabled", false)
    }
    fun saveChorusRate(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("chorus_rate", value)
        }
    }
    fun getChorusRate(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("chorus_rate", 1.5f)
    }
    fun saveChorusDepth(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("chorus_depth", value)
        }
    }
    fun getChorusDepth(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("chorus_depth", 0.5f)
    }
    fun saveChorusMix(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("chorus_mix", value)
        }
    }
    fun getChorusMix(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("chorus_mix", 0.5f)
    }

    // Flanger
    fun saveFlangerEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putBoolean("flanger_enabled", enabled)
        }
    }
    fun getFlangerEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getBoolean("flanger_enabled", false)
    }
    fun saveFlangerRate(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("flanger_rate", value)
        }
    }
    fun getFlangerRate(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("flanger_rate", 0.5f)
    }
    fun saveFlangerDepth(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("flanger_depth", value)
        }
    }
    fun getFlangerDepth(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("flanger_depth", 0.7f)
    }
    fun saveFlangerFeedback(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("flanger_feedback", value)
        }
    }
    fun getFlangerFeedback(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("flanger_feedback", 0.5f)
    }
    fun saveFlangerMix(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("flanger_mix", value)
        }
    }
    fun getFlangerMix(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("flanger_mix", 0.5f)
    }

    // Polyphony
    fun savePolyphonyEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putBoolean("polyphony_enabled", enabled)
        }
    }
    fun getPolyphonyEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getBoolean("polyphony_enabled", false)
    }
    fun savePolyphonySemitones(context: Context, value: Int) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putInt("polyphony_semitones", value)
        }
    }
    fun getPolyphonySemitones(context: Context): Int {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getInt("polyphony_semitones", 0)
    }
    fun savePolyphonyDetune(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("polyphony_detune", value)
        }
    }
    fun getPolyphonyDetune(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("polyphony_detune", 0.0f)
    }
    fun savePolyphonyMix(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("polyphony_mix", value)
        }
    }
    fun getPolyphonyMix(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("polyphony_mix", 0.5f)
    }

    // Delay Effect
    fun saveDelayEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putBoolean("delay_enabled", enabled)
        }
    }
    fun getDelayEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getBoolean("delay_enabled", false)
    }
    fun saveDelayTime(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("delay_time", value)
        }
    }
    fun getDelayTime(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("delay_time", 0.35f)
    }
    fun saveDelayFeedback(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("delay_feedback", value)
        }
    }
    fun getDelayFeedback(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("delay_feedback", 0.4f)
    }
    fun saveDelayMix(context: Context, value: Float) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putFloat("delay_mix", value)
        }
    }
    fun getDelayMix(context: Context): Float {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getFloat("delay_mix", 0.4f)
    }

    // Pitch & Speed Fine-tuning Slider Display
    fun saveShowPitchFine(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putBoolean("show_pitch_fine", value)
        }
    }

    fun getShowPitchFine(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getBoolean("show_pitch_fine", false)
    }

    fun saveShowSpeedFine(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE).edit {
            putBoolean("show_speed_fine", value)
        }
    }

    fun getShowSpeedFine(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_AUDIO_EFFECTS, Context.MODE_PRIVATE)
            .getBoolean("show_speed_fine", false)
    }
}