package com.ztftrue.music

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.util.SizeF
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.utils.Utils.getCover


/**
 * Implementation of App Widget functionality.
 */
class PlayMusicWidget : AppWidgetProvider() {

    @OptIn(UnstableApi::class)
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context != null && intent != null && intent.action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            && intent.getStringExtra("source").equals(context.packageName)
        ) {
            val playStatusChange = intent.getBooleanExtra("playStatusChange", false)
            val playingStatus = intent.getBooleanExtra("playingStatus", false)
            val title = intent.getStringExtra("title") ?: ""
            val author = intent.getStringExtra("author") ?: ""
            val path = intent.getStringExtra("path")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    PlayMusicWidget::class.java
                )
            )
            ids.forEach { id ->
                RemoteViews(
                    context.packageName,
                    R.layout.play_music_widget
                ).let {
                    it.setInt(
                        R.id.play_music_widget,
                        "setBackgroundColor",
                        context.resources.getColor(R.color.light_blue_900)
                    )
                    it.setOnClickPendingIntent(
                        R.id.preview, MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        )
                    )
                    it.setOnClickPendingIntent(
                        R.id.pause, MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_PLAY_PAUSE
                        )
                    )
                    it.setOnClickPendingIntent(
                        R.id.next, MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        )
                    )
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    it.setImageViewResource(
                        R.id.pause,
                        if (playingStatus) R.drawable.pause else R.drawable.play
                    )
                    if (!playStatusChange) {

                        if (!path.isNullOrEmpty()) {
                            val cover = getCover(path)
                            if (cover != null) {
                                it.setImageViewBitmap(R.id.cover, cover)
                            } else {
                                it.setImageViewResource(
                                    R.id.cover,
                                    R.drawable.songs_thumbnail_cover
                                )
                            }
                        }
                        it.setTextViewText(R.id.title, title)
                        it.setTextViewText(R.id.author, author)
                    }
                    it.setOnClickPendingIntent(R.id.cover, pendingIntent)
//                    appWidgetManager.updateAppWidget(id, it)
                }
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
        val sizes = newOptions?.getParcelableArrayList<SizeF>(
            AppWidgetManager.OPTION_APPWIDGET_SIZES
        )
        // Check that the list of sizes is provided by the launcher.
        if (sizes.isNullOrEmpty()) {
            return
        }

        // Get the min and max sizes
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        // Map the sizes to the RemoteViews that you want.
//        val remoteViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            RemoteViews(sizes.associateWith(::createRemoteViews))
//        } else {
//
//        }
//        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)

    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        Log.d("PlayMusicWidget", "onAppWidgetOptionsChanged")
    }

    override fun onRestored(context: Context?, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        Log.d("PlayMusicWidget", "onRestored")
    }

    @OptIn(UnstableApi::class)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        //this line replace the original
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onEnabled(context: Context) {
        // TODO send broadcast
        Log.d("PlayMusicWidget", "onEnabled")
        context.getSharedPreferences("Widgets", Context.MODE_PRIVATE).edit().apply {
            putBoolean("enable", true)
            apply()
        }
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(
        context: Context
    ) {
        context.getSharedPreferences("Widgets", Context.MODE_PRIVATE).edit().apply {
            putBoolean("enable", false)
            apply()
        }
        Log.d("PlayMusicWidget", "onDisabled")
    }


    @OptIn(UnstableApi::class)
    internal fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        RemoteViews(
            context.packageName,
            R.layout.play_music_widget
        ).let {
            it.setInt(
                R.id.play_music_widget,
                "setBackgroundColor",
                context.resources.getColor(R.color.light_blue_900)
            )
            it.setOnClickPendingIntent(
                R.id.preview, MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
            )
            it.setOnClickPendingIntent(
                R.id.pause, MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            )
            it.setOnClickPendingIntent(
                R.id.next, MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
            )
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            context.getSharedPreferences("Widgets", Context.MODE_PRIVATE)
                .also { sharedPreferences ->
                    val playingStatus = sharedPreferences.getBoolean("playingStatus", false)
                    val title = sharedPreferences.getString("title", "")
                    val author = sharedPreferences.getString("author", "")
                    val path = sharedPreferences.getString("path", "")
                    it.setImageViewResource(
                        R.id.pause,
                        if (playingStatus) R.drawable.pause else R.drawable.play
                    )
                    if (!path.isNullOrEmpty()) {
                        val cover = getCover(path)
                        if (cover != null) {
                            it.setImageViewBitmap(R.id.cover, cover)
                        } else {
                            it.setImageViewResource(
                                R.id.cover,
                                R.drawable.songs_thumbnail_cover
                            )
                        }
                    }
                    it.setTextViewText(R.id.title, title)
                    it.setTextViewText(R.id.author, author)
                }

            it.setOnClickPendingIntent(R.id.cover, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, it)
        }

    }
}


