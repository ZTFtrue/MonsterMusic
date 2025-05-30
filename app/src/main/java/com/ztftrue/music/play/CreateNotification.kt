package com.ztftrue.music.play

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.Service.STOP_FOREGROUND_REMOVE
import android.content.Context
import android.content.Intent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MainActivity
import com.ztftrue.music.R

private const val CHANNEL_ID = "PlayService"
private const val NOTIFICATION_ID = 1

@UnstableApi
class CreateNotification(service: Service, private val mediaSession: MediaSessionCompat?) {

    init {
        createNotificationChannel(service)
        val contentIntent = Intent(service, MainActivity::class.java)
        val pendingContentIntent = PendingIntent.getActivity(
            service, 0, contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(service, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
            )
            .setContentIntent(pendingContentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        service.startForeground(NOTIFICATION_ID, builder.build())
    }


    fun cancelNotification() {
//        with(NotificationManagerCompat.from(service)) {
//            cancel(NOTIFICATION_ID)
//            cancelAll()
//            service.stopForeground(STOP_FOREGROUND_REMOVE)
//        }
    }

    fun stop(service: Service) {
        service.stopForeground(STOP_FOREGROUND_REMOVE)
    }

    //   exoPlayer: ExoPlayer,
    fun updateNotification(
        service: Service,
        title: String,
        subTitle: String,
        isPlaying: Boolean,
        playSpeed: Float = 1f,
        position: Long = 0L,
        duration: Long = 0L
    ) {
        val metadataBuilder = MediaMetadataCompat.Builder()
        metadataBuilder.putLong(
            MediaMetadataCompat.METADATA_KEY_DURATION,
            duration
        )
        metadataBuilder.putText(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        metadataBuilder.putText(MediaMetadataCompat.METADATA_KEY_ARTIST, subTitle)
        mediaSession?.setMetadata(metadataBuilder.build())
//        mediaSession?.setQueueTitle(exoPlayer.currentMediaItem?.mediaMetadata?.title)
        mediaSession?.isActive = true
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    position,
                    playSpeed
                )
                .setActions(
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                            or PlaybackStateCompat.ACTION_SEEK_TO or
                            PlaybackStateCompat.ACTION_STOP
                )
                .build()
        )
        val builder = NotificationCompat.Builder(service, CHANNEL_ID)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            service,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
            ).setContentTitle(title)
            .setContentText(subTitle)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setLargeIcon(mediaSession?.controller?.metadata?.description?.iconBitmap)
            .setContentIntent(mediaSession?.controller?.sessionActivity)
//            .setContentIntent(pendingContentIntent)
            // Stop the service when the notification is swiped away
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    service,
                    PlaybackStateCompat.ACTION_STOP
                )
            )
            .setProgress(
                (duration / 1000).toInt(),
                (position / 1000).toInt(),
                false
            )
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        builder.addAction(
            R.drawable.play_previous_song, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(
                service,
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
        )
        builder.addAction(
            if (isPlaying) {
                R.drawable.pause
            } else {
                R.drawable.play
            },
            if (isPlaying) "Pause" else "Play",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                service,
                PlaybackStateCompat.ACTION_PLAY_PAUSE
            )
        )
        builder.addAction(
            R.drawable.play_next_song, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(
                service,
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            )
        )

        builder.addAction(
            R.drawable.ic_close, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(
                service,
                PlaybackStateCompat.ACTION_STOP
            )
        )
        service.startForeground(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val name = context.getString(R.string.app_name)
        val descriptionText = context.getString(R.string.play_notify)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

}