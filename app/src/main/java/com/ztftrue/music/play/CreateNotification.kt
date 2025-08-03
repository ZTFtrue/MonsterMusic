package com.ztftrue.music.play

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.ztftrue.music.R

private const val CHANNEL_ID = "PlayService"
private const val NOTIFICATION_ID = 1

@UnstableApi
class CreateNotification(service: Service, private val mediaSession: MediaSession?):
    DefaultMediaNotificationProvider(service) {

    private val context: Context = service
    override fun getMediaButtons(
        session: MediaSession,
        playerCommands: Player.Commands,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        showPauseButton: Boolean
    ): ImmutableList<CommandButton> {
        return super.getMediaButtons(
            session,
            playerCommands,
            mediaButtonPreferences,
            showPauseButton
        )
    }

    //    override fun createNotification(
//        mediaSession: MediaSession,
//        mediaButtonPreferences: ImmutableList<CommandButton>,
//        actionFactory: MediaNotification.ActionFactory,
//        onNotificationChangedCallback: MediaNotification.Provider.Callback
//    ): MediaNotification {
//        createNotificationChannel(context)
//        // 1. 创建一个基础的 NotificationCompat.Builder
//        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
//            .setSmallIcon(R.mipmap.ic_launcher_foreground) // 你的通知小图标
//            .setStyle(
//                // 2. (关键) 使用 Media3 提供的 MediaStyle
//                // 它会自动处理封面、标题、艺术家和标准控制按钮（上一首/播放/下一首）
//                androidx.media3.ui.PlayerNotificationManager.MediaStyle()
//                    .setMediaSessionToken(mediaSession.sessionToken)
//            )
////            .setStyle(
////                androidx.media.app.NotificationCompat.MediaStyle()
////                    .setMediaSession(mediaSession?.sessionToken)
////            )
////            .setContentIntent(pendingContentIntent)
////            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//        // 3. 返回一个 MediaNotification 对象，它包装了你的通知和 ID
//        //    NOTIFICATION_ID 是你在 Service 中定义的常量，例如 101
//        return MediaNotification(NOTIFICATION_ID, builder.build())
//    }
//
//    /**
//     * 当通知被移除时（例如，用户划掉了通知），这个回调会被调用。
//     */
//    override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean {
//        // 如果用户通过滑动清除了通知，且音乐没有在播放，我们应该停止服务
//        if (action == "com.google.android.exoplayer.dismiss" && !session.player.playWhenReady) {
//            // 这是优雅退出的方式
//            session.player.stop()
//            session.player.clearMediaItems()
//            session.release() // 释放会话，Service 会在之后被销毁
//        }
//        return true
//    }

//    init {
//        createNotificationChannel(service)
//        val contentIntent = Intent(service, MainActivity::class.java)
//        val pendingContentIntent = PendingIntent.getActivity(
//            service, 0, contentIntent,
//            PendingIntent.FLAG_IMMUTABLE
//        )
//        val builder = NotificationCompat.Builder(service, CHANNEL_ID)
//            .setSmallIcon(R.mipmap.ic_launcher_foreground)
//            .setStyle(
//                androidx.media.app.NotificationCompat.MediaStyle()
//                    .setMediaSession(mediaSession?.sessionToken)
//            )
//            .setContentIntent(pendingContentIntent)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//        service.startForeground(NOTIFICATION_ID, builder.build())
//    }
//
//
//    fun cancelNotification() {
////        with(NotificationManagerCompat.from(service)) {
////            cancel(NOTIFICATION_ID)
////            cancelAll()
////            service.stopForeground(STOP_FOREGROUND_REMOVE)
////        }
//    }
//
//    fun stop(service: Service) {
//        service.stopForeground(STOP_FOREGROUND_REMOVE)
//    }
//
//    //   exoPlayer: ExoPlayer,
//    fun updateNotification(
//        service: Service,
//        title: String,
//        subTitle: String,
//        isPlaying: Boolean,
//        playSpeed: Float = 1f,
//        position: Long = 0L,
//        duration: Long = 0L
//    ) {
//        val metadataBuilder = MediaMetadataCompat.Builder()
//        metadataBuilder.putLong(
//            MediaMetadataCompat.METADATA_KEY_DURATION,
//            duration
//        )
//        metadataBuilder.putText(MediaMetadataCompat.METADATA_KEY_TITLE, title)
//        metadataBuilder.putText(MediaMetadataCompat.METADATA_KEY_ARTIST, subTitle)
//        mediaSession?.setMetadata(metadataBuilder.build())
////        mediaSession?.setQueueTitle(exoPlayer.currentMediaItem?.mediaMetadata?.title)
//        mediaSession?.isActive = true
//        mediaSession?.setPlaybackState(
//            PlaybackStateCompat.Builder()
//                .setState(
//                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
//                    position,
//                    playSpeed
//                )
//                .setActions(
//                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
//                            or PlaybackStateCompat.ACTION_SEEK_TO or
//                            PlaybackStateCompat.ACTION_STOP
//                )
//                .build()
//        )
//        val builder = NotificationCompat.Builder(service, CHANNEL_ID)
//            .setStyle(
//                androidx.media.app.NotificationCompat.MediaStyle()
//                    .setMediaSession(mediaSession?.sessionToken)
//                    .setShowActionsInCompactView(0, 1, 2)
//                    .setShowCancelButton(true)
//                    .setCancelButtonIntent(
//                        MediaButtonReceiver.buildMediaButtonPendingIntent(
//                            service,
//                            PlaybackStateCompat.ACTION_STOP
//                        )
//                    )
//            ).setContentTitle(title)
//            .setContentText(subTitle)
//            .setSmallIcon(R.mipmap.ic_launcher_foreground)
//            .setLargeIcon(mediaSession?.controller?.metadata?.description?.iconBitmap)
//            .setContentIntent(mediaSession?.controller?.sessionActivity)
////            .setContentIntent(pendingContentIntent)
//            // Stop the service when the notification is swiped away
//            .setDeleteIntent(
//                MediaButtonReceiver.buildMediaButtonPendingIntent(
//                    service,
//                    PlaybackStateCompat.ACTION_STOP
//                )
//            )
//            .setProgress(
//                (duration / 1000).toInt(),
//                (position / 1000).toInt(),
//                false
//            )
//            .setAutoCancel(false)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//        builder.addAction(
//            R.drawable.play_previous_song, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(
//                service,
//                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
//            )
//        )
//        builder.addAction(
//            if (isPlaying) {
//                R.drawable.pause
//            } else {
//                R.drawable.play
//            },
//            if (isPlaying) "Pause" else "Play",
//            MediaButtonReceiver.buildMediaButtonPendingIntent(
//                service,
//                PlaybackStateCompat.ACTION_PLAY_PAUSE
//            )
//        )
//        builder.addAction(
//            R.drawable.play_next_song, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(
//                service,
//                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
//            )
//        )
//
//        builder.addAction(
//            R.drawable.ic_close, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(
//                service,
//                PlaybackStateCompat.ACTION_STOP
//            )
//        )
//        service.startForeground(NOTIFICATION_ID, builder.build())
//    }
//
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