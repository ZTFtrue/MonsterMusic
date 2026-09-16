package com.ztftrue.music

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.graphics.toColorInt
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.play.PlayService
import com.ztftrue.music.utils.SharedPreferencesUtils
import com.ztftrue.music.utils.Utils

/**
 * Implementation of App Widget functionality.
 */
class PlayMusicWidget : AppWidgetProvider() {

    @OptIn(UnstableApi::class)
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context != null && intent != null && intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: appWidgetManager.getAppWidgetIds(ComponentName(context, PlayMusicWidget::class.java))
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        if (context != null && appWidgetManager != null) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        if (context != null && appWidgetIds != null) {
            for (id in appWidgetIds) {
                SharedPreferencesUtils.removeWidgetConfig(context, id)
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onEnabled(context: Context) {
        SharedPreferencesUtils.setWidgetEnable(context, true)
    }

    override fun onDisabled(context: Context) {
        SharedPreferencesUtils.setWidgetEnable(context, false)
    }

    companion object {
        @UnstableApi
        fun getPendingIntent(
            context: Context,
            keyEvent: Int
        ): PendingIntent {
            val intent = Intent(context, PlayService::class.java).apply {
                action = Intent.ACTION_MEDIA_BUTTON
                putExtra(
                    Intent.EXTRA_KEY_EVENT,
                    KeyEvent(KeyEvent.ACTION_DOWN, keyEvent)
                )
            }
            return PendingIntent.getService(
                context,
                keyEvent,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun isColorDark(color: Int): Boolean {
            val alpha = Color.alpha(color)
            if (alpha < 64) {
                // Highly transparent background: standard Android launcher assumption is white text with shadow
                return true
            }
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val darkness = 1.0 - (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            return darkness >= 0.45
        }

        @OptIn(UnstableApi::class)
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0

            val bgColorStr = SharedPreferencesUtils.getWidgetBackground(context, appWidgetId)
            val bgColor = try {
                bgColorStr.toColorInt()
            } catch (_: Exception) {
                Color.WHITE
            }

            val contrastMode = SharedPreferencesUtils.getWidgetTextContrast(context, appWidgetId)
            val useLightText = when (contrastMode) {
                "light" -> true
                "dark" -> false
                else -> isColorDark(bgColor)
            }

            val titleColor = if (useLightText) Color.WHITE else Color.parseColor("#1C1B1F")
            val authorColor = if (useLightText) Color.parseColor("#B3FFFFFF") else Color.parseColor("#79747E")
            val iconColor = if (useLightText) Color.WHITE else Color.parseColor("#1C1B1F")

            val views = RemoteViews(
                context.packageName,
                R.layout.play_music_widget
            ).apply {
                setOnClickPendingIntent(
                    R.id.preview,
                    getPendingIntent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                )

                setOnClickPendingIntent(
                    R.id.pause,
                    getPendingIntent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                )

                setOnClickPendingIntent(
                    R.id.next,
                    getPendingIntent(context, KeyEvent.KEYCODE_MEDIA_NEXT)
                )

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.play_music_widget, pendingIntent)

                // Apply dynamic contrast colors
                setTextColor(R.id.title, titleColor)
                setTextColor(R.id.author, authorColor)
                setInt(R.id.preview, "setColorFilter", iconColor)
                setInt(R.id.pause, "setColorFilter", iconColor)
                setInt(R.id.next, "setColorFilter", iconColor)
            }

            updateView(context, views, minWidth, bgColor)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun updateView(
            context: Context,
            it: RemoteViews,
            minWidth: Int,
            bgColor: Int
        ) {
            val sharedPreferences = context.getSharedPreferences("Widgets", Context.MODE_PRIVATE)
            val playingStatus = sharedPreferences.getBoolean("playingStatus", false)
            val title = sharedPreferences.getString("title", "")
            val author = sharedPreferences.getString("author", "")
            val path = sharedPreferences.getString("path", "")

            // Only collapse to cover-only if width is specifically measured between 1 and 180dp.
            // When minWidth == 0 (uninitialized or default on many launchers), default to full mode!
            if (minWidth in 1..180) {
                it.setViewVisibility(R.id.cover, View.VISIBLE)
                it.setViewVisibility(R.id.content, View.GONE)
                it.setInt(
                    R.id.play_music_widget,
                    "setBackgroundColor",
                    Color.TRANSPARENT
                )
            } else if (minWidth in 181..275) {
                it.setViewVisibility(R.id.cover, View.GONE)
                it.setViewVisibility(R.id.content, View.VISIBLE)
                it.setViewVisibility(R.id.small_cover, View.VISIBLE)
                it.setInt(
                    R.id.play_music_widget,
                    "setBackgroundColor",
                    bgColor
                )
            } else {
                it.setViewVisibility(R.id.small_cover, View.GONE)
                it.setViewVisibility(R.id.cover, View.VISIBLE)
                it.setViewVisibility(R.id.content, View.VISIBLE)
                it.setInt(
                    R.id.play_music_widget,
                    "setBackgroundColor",
                    bgColor
                )
            }

            it.setImageViewResource(
                R.id.pause,
                if (playingStatus) R.drawable.pause else R.drawable.play
            )

            if (!path.isNullOrEmpty()) {
                val cover = Utils.getCoverBitmap(context, path)
                it.setImageViewBitmap(R.id.cover, cover)
                it.setImageViewBitmap(R.id.small_cover, cover)
            } else {
                it.setImageViewResource(R.id.cover, R.drawable.songs_thumbnail_cover)
                it.setImageViewResource(R.id.small_cover, R.drawable.songs_thumbnail_cover)
            }

            val displayTitle = if (title.isNullOrEmpty()) {
                context.getString(R.string.app_name)
            } else {
                title
            }
            val displayAuthor = if (author.isNullOrEmpty()) {
                context.getString(R.string.widget_no_track_playing)
            } else {
                author
            }

            it.setTextViewText(R.id.title, displayTitle)
            it.setTextViewText(R.id.author, displayAuthor)
        }
    }
}
