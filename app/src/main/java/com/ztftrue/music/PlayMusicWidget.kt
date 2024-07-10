package com.ztftrue.music

import android.Manifest
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.play.EVENT_MEDIA_ITEM_Change
import com.ztftrue.music.play.PlayService

/**
 * Implementation of App Widget functionality.
 */
class PlayMusicWidget : AppWidgetProvider() {
    var context: Context? = null

    @OptIn(UnstableApi::class)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        this.context = context
        arrayList.clear()
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        this.context = context
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
              mediaBrowser = MediaBrowserCompat(
                context,
                ComponentName(context, PlayService::class.java),
                connectionCallbacks,
                null // optional Bundle
            )
            mediaBrowser?.connect()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onEnabled(context: Context) {
        Log.d("TAG", "onEnabled")
        this.context = context
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
              mediaBrowser = MediaBrowserCompat(
                context,
                ComponentName(context, PlayService::class.java),
                connectionCallbacks,
                null // optional Bundle
            )
            mediaBrowser?.connect()
        }
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(
        context: Context
    ) {
        this.context = context
        mediaController = null
        mediaBrowser?.disconnect()
        Log.d("TAG", "onDisabled")
        // Enter relevant functionality for when the last widget is disabled
    }

    var mediaBrowser: MediaBrowserCompat? = null
    var arrayList = ArrayList<RemoteViews>()
//    var views:RemoteViews=
    @OptIn(UnstableApi::class)
    internal fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Get the layout for the widget and attach an onClick listener to
        // the button.
        val views: RemoteViews = RemoteViews(
            context.packageName,
            R.layout.play_music_widget
        ).also {
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
            arrayList.add(it)
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    var mediaController: MediaControllerCompat? = null
    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            // Get the token for the MediaSession
            mediaBrowser?.sessionToken.also { token ->
                // Create a MediaControllerCompat
                mediaController = token?.let {
                    MediaControllerCompat(
                        context, // Context
                        it
                    )
                }
                val extras = mediaBrowser?.extras
                if (extras != null) {
//                    getInitData(extras)
                }
//                MediaControllerCompat.setMediaController(context, mediaController)
                mediaController?.registerCallback(callback)
            }
        }

        override fun onConnectionSuspended() {
        }

        override fun onConnectionFailed() {
            // The Service has refused our connection
        }

    }
    val callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            if (state != null) {
                arrayList.forEach() {
//                    it.setImageViewBitmap()
                    it.setImageViewResource(R.id.pause, if
                            (state.state == PlaybackStateCompat.STATE_PLAYING) R.drawable.pause else R.drawable.play)
                }
//                playStatus.value = state.state == PlaybackStateCompat.STATE_PLAYING
//                getSeek()
            }
        }

        override fun onExtrasChanged(extras: Bundle?) {
            super.onExtrasChanged(extras)
            extras?.let {
                if (it.getInt("type") == EVENT_MEDIA_ITEM_Change) {
                    // before switch to another music, must clear lyrics
//                    val index = it.getInt("index")
//                    if (index >= 0 && musicViewModel.musicQueue.size > index && index != musicViewModel.currentPlayQueueIndex.intValue) {
//                        musicViewModel.currentCaptionList.clear()
//                        musicViewModel.currentMusicCover.value = null
//                        musicViewModel.currentPlay.value =
//                            musicViewModel.musicQueue[index]
//                        musicViewModel.currentPlayQueueIndex.intValue = index
//                        musicViewModel.sliderPosition.floatValue = 0f
//                        musicViewModel.currentDuration.longValue =
//                            musicViewModel.currentPlay.value?.duration ?: 0
//                        musicViewModel.dealLyrics(
//                            this@MainActivity,
//                            musicViewModel.musicQueue[index]
//                        )
//                    }
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
        }

        override fun onQueueTitleChanged(title: CharSequence?) {
            super.onQueueTitleChanged(title)
        }

        override fun onAudioInfoChanged(info: MediaControllerCompat.PlaybackInfo?) {
            super.onAudioInfoChanged(info)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            when (repeatMode) {
//                PlaybackStateCompat.REPEAT_MODE_NONE -> {
//                    musicViewModel.repeatModel.intValue = Player.REPEAT_MODE_OFF
//                }
//
//                PlaybackStateCompat.REPEAT_MODE_ALL -> {
//                    musicViewModel.repeatModel.intValue = Player.REPEAT_MODE_ALL
//                }
//
//                PlaybackStateCompat.REPEAT_MODE_ONE -> {
//                    musicViewModel.repeatModel.intValue = Player.REPEAT_MODE_ONE
//                }
//
//                PlaybackStateCompat.REPEAT_MODE_GROUP -> {
//                }
//
//                PlaybackStateCompat.REPEAT_MODE_INVALID -> {
//                }
            }
        }
    }
}


