package com.ztftrue.music

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
    private val hashMap = mutableMapOf<Int, RemoteViews>()

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
//            val id = intent.getLongExtra("id", 0L)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            hashMap.forEach { (id1, it) ->
                it.setImageViewResource(
                    R.id.pause,
                    if (playingStatus) R.drawable.pause else R.drawable.play
                )
                if (!playStatusChange) {
                    if (!path.isNullOrEmpty()) {
                        val cover: Bitmap = Utils.getCoverBitmap(context, path)
                        it.setImageViewBitmap(R.id.cover, cover)
                    }
                    it.setTextViewText(R.id.title, title)
                    it.setTextViewText(R.id.author, author)
                }
                appWidgetManager.updateAppWidget(id1, it)
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
        if (context != null && newOptions != null && appWidgetManager != null) {
//            val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
//            val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
//            val maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
//            val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
//            val density = context.resources?.displayMetrics?.density ?: 1f
//            val columnCount = (minWidth / (60.dp.toPx(context)))
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }

//        val remoteViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            RemoteViews(sizes.associateWith(::createRemoteViews))
//        } else {
//
//        }
//        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)

    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds?.forEach { hashMap.remove(it) }
//        Log.d("PlayMusicWidget", "onAppWidgetOptionsChanged")
    }

//    override fun onRestored(context: Context?, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
//        super.onRestored(context, oldWidgetIds, newWidgetIds)
////        Log.d("PlayMusicWidget", "onRestored")
//    }

    @OptIn(UnstableApi::class)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
//        val sessionToken = SessionToken(context, ComponentName(context, PlayService::class.java))
//        val controllerFuture: ListenableFuture<MediaController> =
//            MediaController.Builder(context, sessionToken).buildAsync()
//
//        controllerFuture.addListener({
//            try {
//                val controller = controllerFuture.get()
//                // 使用获取到的 controller 更新所有小部件实例
//                appWidgetIds.forEach { appWidgetId ->
//                    updateAppWidget(context, appWidgetManager, appWidgetId, controller)
//                }
//            } catch (e: Exception) {
//                // 处理连接失败的情况
//            } finally {
//                // (重要) 释放 controller future
//                MediaController.releaseFuture(controllerFuture)
//            }
//        }, ContextCompat.getMainExecutor(context))
        //this line replace the original
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onEnabled(context: Context) {
        // TODO send broadcast
//        Log.d("PlayMusicWidget", "onEnabled")
        context.getSharedPreferences("Widgets", Context.MODE_PRIVATE).edit().apply {
            putBoolean("enable", true)
            apply()
        }
//        Toast.makeText(context, "You can set background color in setting", Toast.LENGTH_SHORT).show()
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(
        context: Context
    ) {
        context.getSharedPreferences("Widgets", Context.MODE_PRIVATE).edit().apply {
            putBoolean("enable", false)
            apply()
        }
    }

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
        val pendingIntent = PendingIntent.getService(
            context,
            keyEvent,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent
    }

    @OptIn(UnstableApi::class)
    internal fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // 获取 Widget 的当前配置参数
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

        // 获取最小/最大宽高（单位：dp）
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
//        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
//        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
//        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

        val view = hashMap[appWidgetId]
        if (view != null) {
            updateView(context, view, minWidth)
        } else {
            RemoteViews(
                context.packageName,
                R.layout.play_music_widget
            ).let {
                it.setInt(
                    R.id.play_music_widget,
                    "setBackgroundColor",
                    try {
                        Log.e("Color",SharedPreferencesUtils.getWidgetBackground(context)
                            ?: "#FFFFFF")
                        (SharedPreferencesUtils.getWidgetBackground(context)
                            ?: "#FFFFFF").toColorInt()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Color.WHITE
                    }
                )

                it.setOnClickPendingIntent(
                    R.id.preview,
                    getPendingIntent(
                        context,
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS
                    )
                )

                it.setOnClickPendingIntent(
                    R.id.pause, getPendingIntent(
                        context,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    )
                )

                it.setOnClickPendingIntent(
                    R.id.next, getPendingIntent(
                        context,
                        KeyEvent.KEYCODE_MEDIA_NEXT
                    )
                )

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                it.setOnClickPendingIntent(R.id.play_music_widget, pendingIntent)
                updateView(context, it, minWidth)
                appWidgetManager.updateAppWidget(appWidgetId, it)
                hashMap[appWidgetId] = it
            }
        }
    }

    private fun updateView(context: Context, it: RemoteViews, minWidth: Int) {
        context.getSharedPreferences("Widgets", Context.MODE_PRIVATE)
            .also { sharedPreferences ->
                val playingStatus = sharedPreferences.getBoolean("playingStatus", false)
                val title = sharedPreferences.getString("title", "")
                val author = sharedPreferences.getString("author", "")
                val path = sharedPreferences.getString("path", "")
//                val id = sharedPreferences.getLong("id", 0L)
                if (minWidth <= 180) {
                    it.setViewVisibility(R.id.cover, View.VISIBLE)
                    it.setViewVisibility(R.id.content, View.GONE)
                    it.setInt(
                        R.id.play_music_widget,
                        "setBackgroundColor",
                        Color.TRANSPARENT
                    )

                } else if (minWidth <= 275) {
                    it.setViewVisibility(R.id.cover, View.GONE)
                    it.setViewVisibility(R.id.content, View.VISIBLE)
                    it.setViewVisibility(R.id.small_cover, View.VISIBLE)
                    it.setInt(
                        R.id.play_music_widget,
                        "setBackgroundColor",
                        try {
                            (SharedPreferencesUtils.getWidgetBackground(context)
                                ?: "#FFFFFF").toColorInt()
                        } catch (_: Exception) {
                            Color.WHITE
                        }
                    )
                } else {
                    it.setViewVisibility(R.id.small_cover, View.GONE)
                    it.setViewVisibility(R.id.cover, View.VISIBLE)
                    it.setViewVisibility(R.id.content, View.VISIBLE)
                    it.setInt(
                        R.id.play_music_widget,
                        "setBackgroundColor",
                        try {
                            (SharedPreferencesUtils.getWidgetBackground(context)
                                ?: "#FFFFFF").toColorInt()
                        } catch (_: Exception) {
                            Color.WHITE
                        }
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
                }
                it.setTextViewText(R.id.title, title)
                it.setTextViewText(R.id.author, author)
            }

    }
}


