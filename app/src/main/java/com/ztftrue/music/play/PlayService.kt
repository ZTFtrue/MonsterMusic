package com.ztftrue.music.play

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED
import androidx.media3.common.Timeline
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.ztftrue.music.BuildConfig
import com.ztftrue.music.MainActivity
import com.ztftrue.music.PlayMusicWidget
import com.ztftrue.music.R
import com.ztftrue.music.effects.EqualizerAudioProcessor
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.model.Auxr
import com.ztftrue.music.sqlData.model.CurrentList
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.sqlData.model.PlayConfig
import com.ztftrue.music.sqlData.model.SortFiledData
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.SharedPreferencesUtils
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.model.AlbumList
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.model.ArtistList
import com.ztftrue.music.utils.model.FolderList
import com.ztftrue.music.utils.model.GenresList
import com.ztftrue.music.utils.model.MusicPlayList
import com.ztftrue.music.utils.trackManager.AlbumManager
import com.ztftrue.music.utils.trackManager.ArtistManager
import com.ztftrue.music.utils.trackManager.GenreManager
import com.ztftrue.music.utils.trackManager.PlaylistManager
import com.ztftrue.music.utils.trackManager.TracksManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * playList
 * albumsList
 * artistsList
 * genresList
 * songsList
 * foldersList
 */


@Suppress("deprecation")
@UnstableApi
class PlayService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null
    private var mControllerInfo: MediaSession.ControllerInfo? = null
    val equalizerAudioProcessor: EqualizerAudioProcessor = EqualizerAudioProcessor()
//    val sonicAudioProcessor = SonicAudioProcessor()

    lateinit var exoPlayer: ExoPlayer

    private var playListCurrent: AnyListBase? = null
    var musicQueue = java.util.ArrayList<MusicItem>()
    var currentPlayTrack: MusicItem? = null
    private var showIndicatorList = ArrayList<SortFiledData>()
    private var volumeValue: Int = 0
    private val playListTracksHashMap = java.util.HashMap<Long, java.util.ArrayList<MusicItem>>()

    // album tracks
    private val albumsListTracksHashMap = java.util.HashMap<Long, java.util.ArrayList<MusicItem>>()
    private val artistsListTracksHashMap = java.util.HashMap<Long, java.util.ArrayList<MusicItem>>()
    private val genresListTracksHashMap = java.util.HashMap<Long, java.util.ArrayList<MusicItem>>()
    private val foldersListTracksHashMap =
        java.util.HashMap<Long, java.util.LinkedHashMap<Long, MusicItem>>()

    private val tracksLinkedHashMap: LinkedHashMap<Long, MusicItem> = LinkedHashMap()
    private val allTracksLinkedHashMap: LinkedHashMap<Long, MusicItem> = LinkedHashMap()

    private val albumsLinkedHashMap: LinkedHashMap<Long, AlbumList> = LinkedHashMap()
    private val playListLinkedHashMap: LinkedHashMap<Long, MusicPlayList> = LinkedHashMap()
    private val artistsLinkedHashMap: LinkedHashMap<Long, ArtistList> = LinkedHashMap()
    private val genresLinkedHashMap: LinkedHashMap<Long, GenresList> = LinkedHashMap()
    private val foldersLinkedHashMap: LinkedHashMap<Long, FolderList> = LinkedHashMap()

    private val artistHasAlbumMap: HashMap<Long, ArrayList<AlbumList>> = HashMap()
    private val genreHasAlbumMap: HashMap<Long, ArrayList<AlbumList>> = HashMap()

    private val isInitialized = CompletableDeferred<Unit>()

    private val mainTab = ArrayList<MainTab>(7)
    private lateinit var db: MusicDatabase
    private var bandsValue =
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    private var auxr = Auxr(
        0, 1f, 1f, false, 0.2f, 0.5f,
        echoRevert = true,
        equalizer = false,
        equalizerBand = bandsValue,
        equalizerQ = Utils.Q
    )

    var remainingTime = 0L
    var playCompleted = false
    var needPlayPause = false
    var sleepTime = 0L
    private var countDownTimer: CountDownTimer? = null
    private var headsetCallback: HeadsetConnectionCallback? = null
    private lateinit var audioManager: AudioManager

    override fun onCreate() {
        super.onCreate()
        initExo(this@PlayService)
        initializePlayerData()
        val contentIntent = Intent(this, MainActivity::class.java)
        val pendingContentIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaLibrarySession.Builder(
            this,
            exoPlayer,
            MySessionCallback(this@PlayService),
        ).setSessionActivity(pendingContentIntent)
            .build()
        if (SharedPreferencesUtils.getAutoPlayEnable(this)) {
            headsetCallback = HeadsetConnectionCallback(mediaSession?.player, this@PlayService)
            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.registerAudioDeviceCallback(
                headsetCallback,
                Handler(Looper.getMainLooper())
            )
        }
        val notification = DefaultMediaNotificationProvider(this)
        setMediaNotificationProvider(notification)
    }

    inner class MySessionCallback(private val context: Context) : MediaLibrarySession.Callback {


        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            mControllerInfo = controller
            val availableCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                    // 添加所有你定义的 SessionCommand
                    .add(MediaCommands.COMMAND_CHANGE_PITCH)
                    .add(MediaCommands.COMMAND_CHANGE_Q)
                    .add(MediaCommands.COMMAND_DSP_ENABLE)
                    .add(MediaCommands.COMMAND_ADD_TO_QUEUE)
                    .add(MediaCommands.COMMAND_DSP_SET_BAND)
                    .add(MediaCommands.COMMAND_DSP_FLATTEN)
                    .add(MediaCommands.COMMAND_DSP_SET_BANDS)
                    .add(MediaCommands.COMMAND_ECHO_ENABLE)
                    .add(MediaCommands.COMMAND_ECHO_SET_DELAY)
                    .add(MediaCommands.COMMAND_ECHO_SET_DECAY)
                    .add(MediaCommands.COMMAND_ECHO_SET_FEEDBACK)
                    .add(MediaCommands.COMMAND_SEARCH)
                    .add(MediaCommands.COMMAND_VISUALIZATION_ENABLE)
                    .add(MediaCommands.COMMAND_SET_SLEEP_TIMER)
                    .add(MediaCommands.COMMAND_VISUALIZATION_CONNECTED)
                    .add(MediaCommands.COMMAND_VISUALIZATION_DISCONNECTED)
                    .add(MediaCommands.COMMAND_GET_INITIALIZED_DATA)
                    .add(MediaCommands.COMMAND_APP_EXIT)
                    .add(MediaCommands.COMMAND_TRACKS_UPDATE)
                    .add(MediaCommands.COMMAND_REMOVE_FROM_QUEUE)
                    .add(MediaCommands.COMMAND_SORT_TRACKS)
                    .add(MediaCommands.COMMAND_CHANGE_PLAYLIST)
                    .add(MediaCommands.COMMAND_GET_CURRENT_PLAYLIST)
                    .add(MediaCommands.COMMAND_SMART_SHUFFLE)
                    .add(MediaCommands.COMMAND_SORT_QUEUE)
                    .add(MediaCommands.COMMAND_CLEAR_QUEUE)
                    .add(MediaCommands.COMMAND_GET_PLAY_LIST_ITEM)
                    .add(MediaCommands.COMMAND_REFRESH_ALL)
                    .add(MediaCommands.COMMAND_TRACK_DELETE)
                    .add(MediaCommands.COMMAND_PlAY_LIST_CHANGE)
                    .add(MediaCommands.COMMAND_SLEEP_STATE_UPDATE)
                    .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableCommands)
//                .setAvailablePlayerCommands(availableCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                // --- DSP & Effects ---
                MediaCommands.COMMAND_CHANGE_PITCH.customAction -> {
                    val pitch = args.getFloat("pitch", 1f)
                    exoPlayer.playbackParameters = PlaybackParameters(
                        auxr.speed, pitch
                    )
                    auxr.pitch = pitch
                    CoroutineScope(Dispatchers.IO).launch {
                        db.AuxDao().update(auxr)
                    }
                }

                MediaCommands.COMMAND_CHANGE_Q.customAction -> {
                    val q = args.getFloat("Q", 3.2f)
                    equalizerAudioProcessor.setQ(q)
                    auxr.equalizerQ = q
                    CoroutineScope(Dispatchers.IO).launch {
                        db.AuxDao().update(auxr)
                    }
                }

                MediaCommands.COMMAND_SMART_SHUFFLE.customAction -> {
                    val enable = args.getBoolean("enable")
                    if (enable) {
                        val isQueue = args.getBoolean("queue")
                        val autoPlay = args.getBoolean("autoPlay")
                        val newMusicItems: ArrayList<MusicItem>? =
                            if (isQueue) {
                                musicQueue
                            } else {
                                val playListType = args.getString("playListType", "")
                                val playListId = args.getLong("playListId", 0L)
                                var result: ArrayList<MusicItem>? = null
                                if (playListType.isNullOrEmpty() || playListId == 0L) {
                                    return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                                }
                                if (playListType == PlayListType.Songs.name) {
                                    result = ArrayList(tracksLinkedHashMap.values)
                                } else if (playListType == PlayListType.PlayLists.name) {
                                    val playList = playListTracksHashMap[playListId]
                                    if (playList != null) {
                                        result = ArrayList(playList)
                                    } else {
                                        null
                                    }
                                } else if (playListType == PlayListType.Albums.name) {
                                    val playList = albumsListTracksHashMap[playListId]
                                    if (playList != null) {
                                        result = ArrayList(playList)
                                    } else {
                                        null
                                    }
                                } else if (playListType == PlayListType.Artists.name) {
                                    val playList = artistsListTracksHashMap[playListId]
                                    if (playList != null) {
                                        result = ArrayList(playList)
                                    } else {
                                        null
                                    }
                                } else if (playListType == PlayListType.Genres.name) {
                                    val playList = genresListTracksHashMap[playListId]
                                    if (playList != null) {
                                        result = ArrayList(playList)
                                    } else {
                                        null
                                    }
                                } else if (playListType == PlayListType.Folders.name) {
                                    val playList = foldersListTracksHashMap[playListId]?.values
                                    if (playList != null) {
                                        result = ArrayList(playList)
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }
                                result?.forEachIndexed { index, musicItem ->
                                    musicItem.tableId = index.toLong() + 1
                                }
                                result
                            }
                        val startMediaId: Long? = args.getLong(MediaCommands.KEY_START_MEDIA_ID)
                        if (newMusicItems.isNullOrEmpty()) {
                            return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                        }
                        SharedPreferencesUtils.enableShuffle(this@PlayService, true)
                        ContextCompat.getMainExecutor(this@PlayService).execute {
                            val currentPlayer = this@PlayService.exoPlayer
                            val position = if (isQueue) {
                                currentPlayer.currentPosition
                            } else {
                                0
                            }
                            val needPlay = currentPlayer.isPlaying || autoPlay
                            val autoToTopEnabled =
                                SharedPreferencesUtils.getAutoToTopRandom(this@PlayService)
                            val currentMusicItem: MusicItem? =
                                newMusicItems.find { it.id == startMediaId }

                            var finalShuffledQueue: List<MusicItem>
                            if (autoToTopEnabled && currentMusicItem != null) {
                                val otherItems =
                                    newMusicItems.filter { it.id != currentMusicItem.id }
                                val shuffledOtherItems = otherItems.shuffled()
                                finalShuffledQueue = mutableListOf<MusicItem>().apply {
                                    add(currentMusicItem) // 置顶
                                    addAll(shuffledOtherItems)
                                }
                            } else {
                                finalShuffledQueue = newMusicItems.shuffled()
                            }
//                            finalShuffledQueue.forEachIndexed { index, item ->
//                                item.priority = index + 1
//                            }
                            val newMediaItems =
                                finalShuffledQueue.map { MediaItemUtils.musicItemToMediaItem(it) }
                            val newStartIndex =
                                if (currentMusicItem != null) {
                                    finalShuffledQueue.indexOfFirst { it.id == currentMusicItem.id }
                                        .let { if (it == -1) 0 else it }
                                } else {
                                    0
                                }
                            currentPlayer.shuffleModeEnabled = true
                            currentPlayer.setMediaItems(
                                newMediaItems,
                                newStartIndex,
                                position
                            )
                            currentPlayer.playWhenReady = needPlay
                            currentPlayer.prepare()
                            if (needPlay) {
                                currentPlayer.play()
                            }
                        }
                    } else {
                        SharedPreferencesUtils.enableShuffle(this@PlayService, false)
                        ContextCompat.getMainExecutor(this@PlayService).execute {
                            val currentPlayer = this@PlayService.exoPlayer
                            val needPlay = currentPlayer.isPlaying
                            val position = currentPlayer.currentPosition
                            currentPlayer.shuffleModeEnabled = false
                            musicQueue.sortBy { it.tableId }
                            val newStartIndex =
                                musicQueue.indexOfFirst { it.id == currentPlayer.currentMediaItem?.mediaId?.toLong() }
                            val newMediaItems =
                                musicQueue.map { MediaItemUtils.musicItemToMediaItem(it) }
                            currentPlayer.setMediaItems(
                                newMediaItems,
                                if (newStartIndex < 0) {
                                    0
                                } else {
                                    newStartIndex
                                },
                                position
                            )
                            currentPlayer.playWhenReady = needPlay
                            currentPlayer.prepare()
                            if (needPlay) {
                                currentPlayer.play()
                            }
                        }
                    }


                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                MediaCommands.COMMAND_DSP_ENABLE.customAction -> {
                    val enable = args.getBoolean("enable")
                    equalizerAudioProcessor.setEqualizerActive(enable)
                    auxr.equalizer = equalizerAudioProcessor.isSetActive()
                    CoroutineScope(Dispatchers.IO).launch {
                        db.AuxDao().update(auxr)
                    }
                }

                MediaCommands.COMMAND_SORT_TRACKS.customAction -> {
                    val future = SettableFuture.create<SessionResult>()
                    sortAction(args, future)
                    return future
                }

                MediaCommands.COMMAND_GET_PLAY_LIST_ITEM.customAction -> {
                    val future = SettableFuture.create<SessionResult>()
                    val type = args.getString("type")
                    val id = args.getLong("id")
                    val bundle = Bundle()
                    if (PlayListType.Genres.name == type) {
                        val album = genresLinkedHashMap[id]
                        if (album == null) {
                            future.set(SessionResult(SessionError.ERROR_BAD_VALUE))
                            return future
                        }
                        bundle.putParcelable("data", album)
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, bundle))
                        return future
                    } else if (PlayListType.Artists.name == type) {
                        val album = artistsLinkedHashMap[id]
                        if (album == null) {
                            future.set(SessionResult(SessionError.ERROR_BAD_VALUE))
                            return future
                        }
                        bundle.putParcelable("data", album)
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, bundle))
                        return future
                    } else if (PlayListType.Albums.name == type) {
                        val album = albumsLinkedHashMap[id]
                        if (album == null) {
                            future.set(SessionResult(SessionError.ERROR_BAD_VALUE))
                            return future
                        }
                        bundle.putParcelable("data", album)
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, bundle))
                        return future
                    } else if (PlayListType.Folders.name == type) {
                        val album = foldersLinkedHashMap[id]
                        if (album == null) {
                            future.set(SessionResult(SessionError.ERROR_BAD_VALUE))
                            return future
                        }
                        bundle.putParcelable("data", album)
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, bundle))
                        return future
                    } else if (PlayListType.PlayLists.name == type) {
                        val album = playListLinkedHashMap[id]
                        if (album == null) {
                            future.set(SessionResult(SessionError.ERROR_BAD_VALUE))
                            return future
                        }
                        bundle.putParcelable("data", album)
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, bundle))
                        return future
                    }
                    return future
                }

                MediaCommands.COMMAND_SORT_QUEUE.customAction -> {
                    val index = args.getInt("index", 0)
                    val targetIndex = args.getInt("targetIndex", 0)
                    if (index == targetIndex) {
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    if (exoPlayer.shuffleModeEnabled) {
                        val startMediaId = musicQueue[exoPlayer.currentMediaItemIndex].id
                        val position = exoPlayer.currentPosition
                        val needPlay = exoPlayer.isPlaying
                        val tempQueue = ArrayList(musicQueue)
                        val music = tempQueue.removeAt(index)
                        tempQueue.add(targetIndex, music)
//                        tempQueue.forEachIndexed { index, musicItem ->
//                            musicItem.priority = index + 1
//                        }
                        val newStartIndex = tempQueue.indexOfFirst { it.id == startMediaId }
                            .let { if (it == -1) 0 else it }
                        val newMediaItems =
                            tempQueue.map { MediaItemUtils.musicItemToMediaItem(it) }
                        exoPlayer.setMediaItems(
                            newMediaItems,
                            newStartIndex,
                            position
                        )
                        exoPlayer.playWhenReady = needPlay
                        exoPlayer.prepare()
                        if (needPlay) {
                            exoPlayer.play()
                        }
                    } else {
                        val startMediaId = musicQueue[exoPlayer.currentMediaItemIndex].id
                        val position = exoPlayer.currentPosition
                        val needPlay = exoPlayer.isPlaying
                        val tempQueue = ArrayList(musicQueue)
                        val music = tempQueue.removeAt(index)
                        tempQueue.add(targetIndex, music)
//                        tempQueue.forEachIndexed { index, musicItem ->
//                            musicItem.priority = index + 1
//                        }
                        val newStartIndex = tempQueue.indexOfFirst { it.id == startMediaId }
                            .let { if (it == -1) 0 else it }

                        val newMediaItems =
                            tempQueue.map { MediaItemUtils.musicItemToMediaItem(it) }
                        exoPlayer.setMediaItems(
                            newMediaItems,
                            newStartIndex,
                            position
                        )
                        exoPlayer.playWhenReady = needPlay
                        exoPlayer.prepare()
                        if (needPlay) {
                            exoPlayer.play()
                        }
                    }

//                    val bundle = Bundle()
//                    bundle.putString(
//                        "method",
//                        PlayUtils.methodMap[methodSelected] ?: ""
//                    )
//                    bundle.putString(
//                        "filed", sortFiledOptions[filedSelected]
//                            ?: ""
//                    )
//                    bundle.putString(
//                        "type",
//                        musicViewModel.mainTabList[pagerState.currentPage].type.name
//                    )
                }

                MediaCommands.COMMAND_DSP_SET_BAND.customAction -> {
                    val index = args.getInt(MediaCommands.KEY_INDEX)
                    val value = args.getInt(MediaCommands.KEY_VALUE)
                    equalizerAudioProcessor.setBand(index, value)
                    auxr.equalizerBand[index] = value
                    CoroutineScope(Dispatchers.IO).launch { db.AuxDao().update(auxr) }
                }

                MediaCommands.COMMAND_DSP_FLATTEN.customAction -> {
                    if (equalizerAudioProcessor.flatBand()) {
                        repeat(auxr.equalizerBand.size) {
                            auxr.equalizerBand[it] = 0
                        }
                        CoroutineScope(Dispatchers.IO).launch { db.AuxDao().update(auxr) }
                        // 如果需要返回结果，可以这样做
                        val resultData = Bundle().apply { putBoolean("result", true) }
                        return Futures.immediateFuture(
                            SessionResult(
                                SessionResult.RESULT_SUCCESS,
                                resultData
                            )
                        )
                    }
                }

                MediaCommands.COMMAND_DSP_SET_BANDS.customAction -> {
                    args.getIntArray("value")?.forEachIndexed { index, v ->
                        equalizerAudioProcessor.setBand(index, v)
                    }
                }

                MediaCommands.COMMAND_ECHO_ENABLE.customAction -> {
                    val enable = args.getBoolean(MediaCommands.KEY_ENABLE)
                    equalizerAudioProcessor.setEchoActive(enable)
                    auxr.echo = enable
                    CoroutineScope(Dispatchers.IO).launch { db.AuxDao().update(auxr) }
                }

                MediaCommands.COMMAND_ECHO_SET_DELAY.customAction -> {
                    val delay = args.getFloat(MediaCommands.KEY_DELAY)
                    equalizerAudioProcessor.setDaleyTime(delay)
                    auxr.echoDelay = delay
                    CoroutineScope(Dispatchers.IO).launch { db.AuxDao().update(auxr) }
                }

                MediaCommands.COMMAND_ECHO_SET_DECAY.customAction -> {
                    val decay = args.getFloat(MediaCommands.KEY_DECAY)
                    equalizerAudioProcessor.setDecay(decay)
                    auxr.echoDecay = decay
                    CoroutineScope(Dispatchers.IO).launch { db.AuxDao().update(auxr) }
                }

                MediaCommands.COMMAND_ECHO_SET_FEEDBACK.customAction -> {
                    val enable = args.getBoolean(MediaCommands.KEY_ENABLE)
                    equalizerAudioProcessor.setFeedBack(enable)
                    auxr.echoRevert = enable
                    CoroutineScope(Dispatchers.IO).launch { db.AuxDao().update(auxr) }
                }

                // --- Visualization ---
                MediaCommands.COMMAND_VISUALIZATION_ENABLE.customAction -> {
                    val enable = args.getBoolean(MediaCommands.KEY_ENABLE)
                    musicVisualizationEnable = enable
                    equalizerAudioProcessor.setVisualizationAudioActive(enable)
                    SharedPreferencesUtils.saveEnableMusicVisualization(this@PlayService, enable)
                }

                MediaCommands.COMMAND_VISUALIZATION_CONNECTED.customAction -> {
                    if (musicVisualizationEnable) { // musicVisualizationEnable 是 Service 的一个状态变量
                        equalizerAudioProcessor.setVisualizationAudioActive(true)
                    }
                }

                MediaCommands.COMMAND_GET_INITIALIZED_DATA.customAction -> {
                    val future = SettableFuture.create<SessionResult>()
                    serviceScope.async {
                        isInitialized.await()
                        val bundle = Bundle()
                        setData(bundle)
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, bundle))
                    }
                    return future
                }

                MediaCommands.COMMAND_VISUALIZATION_DISCONNECTED.customAction -> {
                    equalizerAudioProcessor.setVisualizationAudioActive(false)
                }

                // --- Sleep Timer ---
                MediaCommands.COMMAND_SET_SLEEP_TIMER.customAction -> {
                    // 注意：旧的 timeSet 方法需要改造
                    // 它不能再直接调用 result.sendResult()
                    // 而是应该返回一个结果，或者我们在这里处理
                    // 假设我们直接在这里处理
                    timeSet(args) // 改造 timeSet，让它不再需要 Result 参数
                }

                MediaCommands.COMMAND_APP_EXIT.customAction -> {
                    stopForeground(true)
                    stopSelf()
                }

                MediaCommands.COMMAND_GET_CURRENT_PLAYLIST.customAction -> {
                    val future = SettableFuture.create<SessionResult>()
                    future.set(SessionResult(SessionResult.RESULT_SUCCESS, Bundle().apply {
                        putParcelable("playList", playListCurrent)
                    }))
                    return future
                }

                MediaCommands.COMMAND_CHANGE_PLAYLIST.customAction -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        val playList = args.getParcelable<AnyListBase>("playList")
                        if (playList != null) {
                            var currentList = db.CurrentListDao().findCurrentList()
                            if (currentList == null) {
                                currentList = CurrentList(null, playList.id, playList.type.name)
                                db.CurrentListDao().insert(currentList)
                            } else {
                                currentList.listID = playList.id
                                currentList.type = playList.type.name
                                db.CurrentListDao().update(currentList)
                            }
                            playListCurrent = playList
                        }
//                                db.QueueDao().deleteAllQueue()
//                                db.QueueDao().insertAll(musicQueue)
                    }
                }

                MediaCommands.COMMAND_SEARCH.customAction -> {
                    val future = SettableFuture.create<SessionResult>()
                    val query = args.getString(MediaCommands.KEY_SEARCH_QUERY, "")
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val searchResult = search(query)
                            val resultData = Bundle().apply {
                                putParcelableArrayList("tracks", searchResult.tracks)
                                putParcelableArrayList("albums", searchResult.albums)
                                putParcelableArrayList("artist", searchResult.artists)
                            }
                            future.set(SessionResult(SessionResult.RESULT_SUCCESS, resultData))
                        } catch (e: Exception) {
                            future.setException(e)
                        }
                    }

                    return future
                }

                MediaCommands.COMMAND_PlAY_LIST_CHANGE.customAction -> {
                    val future = SettableFuture.create<SessionResult>()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            playListLinkedHashMap.clear()
                            playListTracksHashMap.clear()
                            val sortData =
                                db.SortFiledDao().findSortByType(PlayListType.PlayLists.name)
                            val newPlaylists: List<MusicPlayList> = PlaylistManager.getPlaylists(
                                this@PlayService,
                                playListLinkedHashMap,
                                tracksLinkedHashMap,
                                sortData?.filed,
                                sortData?.method
                            )
                            newPlaylists.forEach { playListLinkedHashMap[it.id] = it }
                            val resultData = Bundle().apply {
                                putInt("new_playlist_count", newPlaylists.size)
                            }
                            future.set(SessionResult(SessionResult.RESULT_SUCCESS, resultData))
                        } catch (e: Exception) {
                            // 如果刷新过程中发生错误，将异常设置到 future
                            Log.e("PlayService", "Error refreshing playlists", e)
                            future.setException(e)
                        }
                    }
                    return future
                }

                MediaCommands.COMMAND_REFRESH_ALL.customAction -> {
                    val future = SettableFuture.create<SessionResult>()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            refreshAction(future)
                        } catch (e: Exception) {
                            // 如果刷新过程中发生错误，将异常设置到 future
                            Log.e("PlayService", "Error refreshing playlists", e)
                            future.setException(e)
                        }
                    }
                    return future
                }

                MediaCommands.COMMAND_TRACK_DELETE.customAction -> {
                    val idsToDelete = args.getLong("id")
                    val c = PlayUtils.trackDelete(
                        idsToDelete,
                        musicQueue,
                        tracksLinkedHashMap
                    )
                    clearCacheData()
                    if (c > -1) {
                        CoroutineScope(Dispatchers.Main).launch {
                            exoPlayer.removeMediaItem(c.toInt())
                            if (exoPlayer.currentMediaItem?.mediaId == c.toInt().toString()) {
                                playListCurrent = null
                            }
                        }
                    }
                    // 返回一个包含操作结果的 Bundle
                    val resultData = Bundle().apply {
                        putBoolean("success", true)
                        putBoolean("wasInQueue", c > -1)
                        putInt("playIndex", exoPlayer.currentMediaItemIndex)
                        putLong("id", c)
                        putParcelableArrayList("queue", musicQueue)
                    }
                    return Futures.immediateFuture(
                        SessionResult(
                            SessionResult.RESULT_SUCCESS,
                            resultData
                        )
                    )
                }

                MediaCommands.COMMAND_TRACKS_UPDATE.customAction -> {
                    val future = SettableFuture.create<SessionResult>()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val id = args.getLong(MediaCommands.KEY_TRACK_ID)
                            val musicTrack = TracksManager.getMusicById(this@PlayService, id)
                            if (musicTrack != null) {
                                tracksLinkedHashMap[id] = musicTrack
                            } else {
                                tracksLinkedHashMap.remove(id)
                            }
                            clearCacheData()
                            val updatedItemInQueue: MusicItem? = musicQueue.find { it.id == id }
                            if (updatedItemInQueue != null && musicTrack != null) {
                                updatedItemInQueue.apply {
                                    name = musicTrack.name
                                    path = musicTrack.path
                                    duration = musicTrack.duration
                                    displayName = musicTrack.displayName
                                    album = musicTrack.album
                                    albumId = musicTrack.albumId
                                    artist = musicTrack.artist
                                    artistId = musicTrack.artistId
                                    genre = musicTrack.genre
                                    genreId = musicTrack.genreId
                                    year = musicTrack.year
                                    songNumber = musicTrack.songNumber
                                }
                                db.QueueDao().update(updatedItemInQueue)
                            }

                            // --- 准备返回值 ---
                            val resultData = Bundle().apply {
                                // 返回更新后的整个歌曲列表
                                putParcelableArrayList(
                                    "list",
                                    ArrayList(tracksLinkedHashMap.values)
                                )
                                // 返回更新后的那个 item
                                putParcelable("item", musicTrack)
                            }
                            future.set(SessionResult(SessionResult.RESULT_SUCCESS, resultData))
                        } catch (e: Exception) {
                            future.setException(e)
                        }
                    }
                    return future
                }
                // --- Queue Management ---
                MediaCommands.COMMAND_ADD_TO_QUEUE.customAction -> {

                }


                MediaCommands.COMMAND_CLEAR_QUEUE.customAction -> {
                    ContextCompat.getMainExecutor(this@PlayService).execute {
                        exoPlayer.pause()
                        exoPlayer.clearMediaItems()
                        playListCurrent = null
                    }
                    SharedPreferencesUtils.saveSelectMusicId(this@PlayService, -1)
                    playListCurrent = null
                    musicQueue.clear()
                    CoroutineScope(Dispatchers.IO).launch {
                        db.QueueDao().deleteAllQueue()
                        val c = db.CurrentListDao().findCurrentList()
                        if (c != null) {
                            db.CurrentListDao().delete()
                        }
                        SharedPreferencesUtils.saveSelectMusicId(this@PlayService, -1)
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                else -> {
                    // 如果不是我们认识的命令，交由父类处理
                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val clientPackageName = browser.packageName
            // 为 Android Auto 提供一个简化的根节点
//            if (clientPackageName == "com.google.android.projection.gearhead") {
//                // 返回一个为驾驶场景优化的根节点
//                val autoRootItem = MediaItemUtils.createFullFeaturedRoot()
//                return Futures.immediateFuture(LibraryResult.ofItem(autoRootItem, null))
//            }
//            // 为你自己的 App 提供一个功能完整的根节点
//            else
            if (clientPackageName == context.packageName) {
                val fullFeaturedRootItem = MediaItemUtils.createFullFeaturedRoot()
                return Futures.immediateFuture(LibraryResult.ofItem(fullFeaturedRootItem, null))
            } else {
                return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_SESSION_DISCONNECTED))
            }
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    isInitialized.await()
                    when {
                        parentId == "root" -> {
                            val topLevelItems = createTopLevelCategories()
                            future.set(LibraryResult.ofItemList(topLevelItems, null))
                        }

                        parentId == "songs_root" -> {
                            getSongsAll(future, params)
                        }

                        parentId == "albums_root" -> {
                            getAlbums(future, params)
                        }

                        parentId == "artists_root" -> {
                            getArtists(future, params)
                        }

                        parentId == "playlists_root" -> {
                            getPlayList(context, future, params)
                        }

                        parentId == "genres_root" -> {
                            getGenres(future, params)
                        }

                        parentId == "folders_root" -> {
                            getFolders(future, params)
                        }
                        // genre includes albums
                        parentId.startsWith(PlayListType.Genres.name + "_album_") -> {
                            handlePrefixedId(
                                parentId,
                                PlayListType.Genres.name + "_album_",
                                future
                            ) { id ->
                                val list = genreHasAlbumMap[id]
                                val mediaItems: List<MediaItem> = list?.map { playlist ->
                                    MediaItemUtils.albumToMediaItem(playlist)
                                } ?: emptyList()
                                future.set(LibraryResult.ofItemList(mediaItems, null))
                            }
                        }
                        // artist includes albums
                        parentId.startsWith(PlayListType.Artists.name + "_album_") -> {
                            handlePrefixedId(
                                parentId,
                                PlayListType.Artists.name + "_album_",
                                future
                            ) { id ->
                                val list = artistHasAlbumMap[id]
                                val mediaItems: List<MediaItem> = list?.map { playlist ->
                                    MediaItemUtils.albumToMediaItem(playlist)
                                } ?: emptyList()
                                future.set(LibraryResult.ofItemList(mediaItems, null))
                            }
                        }
                        // --- 二级子列表（具体实体下的内容） ---
                        parentId.startsWith(PlayListType.Albums.name + "_track_") -> {
                            handlePrefixedId(
                                parentId,
                                PlayListType.Albums.name + "_track_",
                                future
                            ) { id ->
                                getAlbumListTracks(id, future)
                            }
                        }

                        parentId.startsWith(PlayListType.Artists.name + "_track_") -> {
                            handlePrefixedId(
                                parentId,
                                PlayListType.Artists.name + "_track_",
                                future
                            ) { id ->
                                getArtistListTracks(id, future)
                            }
                        }

                        parentId.startsWith(PlayListType.Genres.name + "_track_") -> {
                            handlePrefixedId(
                                parentId,
                                PlayListType.Genres.name + "_track_",
                                future
                            ) { id ->
                                getGenreListTracks(id, future)
                            }
                        }

                        parentId.startsWith(PlayListType.PlayLists.name + "_track_") -> {
                            handlePrefixedId(
                                parentId,
                                PlayListType.PlayLists.name + "_track_",
                                future
                            ) { id ->
                                getPlayListTracks(id, future)
                            }
                        }

                        parentId.startsWith(PlayListType.Folders.name + "_track_") -> {
                            handlePrefixedId(
                                parentId,
                                PlayListType.Folders.name + "_track_",
                                future
                            ) { id ->
                                getFolderListTracks(id, future)
                            }
                        }

                        else -> {
                            Log.w("PlayService", "onGetChildren: Unknown parentId '$parentId'")
                            future.set(LibraryResult.ofItemList(listOf(), null)) // 返回空列表
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PlayService", "Error in onGetChildren for parentId: $parentId", e)
                    future.setException(e)
                }
            }
            return future
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val future = SettableFuture.create<LibraryResult<MediaItem>>()
            mediaId.split("@").let {
                if (it.size == 2) {
                    val type = it[0]
                    val id = it[1].toLong()
                    if (PlayListType.Genres.name == type) {
                        val album = genresLinkedHashMap[id]
                        if (album == null) {
                            future.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                            return future
                        }
                        val mediaItems = MediaItemUtils.genreToMediaItem(album)
                        future.set(LibraryResult.ofItem(mediaItems, null))
                        return future
                    } else if (PlayListType.Artists.name == type) {
                        val album = artistsLinkedHashMap[id]
                        if (album == null) {
                            future.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                            return future
                        }
                        val mediaItems = MediaItemUtils.artistToMediaItem(album)
                        future.set(LibraryResult.ofItem(mediaItems, null))
                        return future
                    } else if (PlayListType.Albums.name == type) {
                        val album = albumsLinkedHashMap[id]
                        if (album == null) {
                            future.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                            return future
                        }
                        val mediaItems = MediaItemUtils.albumToMediaItem(album)
                        future.set(LibraryResult.ofItem(mediaItems, null))
                        return future
                    } else if (PlayListType.Folders.name == type) {
                        val album = foldersLinkedHashMap[id]
                        if (album == null) {
                            future.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                            return future
                        }
                        val mediaItems = MediaItemUtils.folderToMediaItem(album)
                        future.set(LibraryResult.ofItem(mediaItems, null))
                        return future
                    } else if (PlayListType.PlayLists.name == type) {
                        val album = playListLinkedHashMap[id]
                        if (album == null) {
                            future.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                            return future
                        }
                        val mediaItems = MediaItemUtils.playlistToMediaItem(album)
                        future.set(LibraryResult.ofItem(mediaItems, null))
                        return future
                    }
                }
                return super.onGetItem(session, browser, mediaId)
            }

        }

        private suspend fun handlePrefixedId(
            parentId: String,
            prefix: String,
            future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>,
            action: suspend (id: Long) -> Unit // 使用 suspend lambda 作为参数
        ) {
            val id = parentId.removePrefix(prefix).toLongOrNull()
            if (id != null) {
                action(id) // 调用传入的业务逻辑
            } else {
                // 使用 LibraryResult 的标准错误码
                future.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }
        }

//        override fun onAddMediaItems(
//            mediaSession: MediaSession,
//            controller: MediaSession.ControllerInfo,
//            mediaItems: List<MediaItem>
//        ): ListenableFuture<List<MediaItem>> {
//            val future = SettableFuture.create<List<MediaItem>>()
//            CoroutineScope(Dispatchers.IO).launch {
//                try {
//                    // 1. 将传入的 MediaItem 转换为我们自己的 MusicItem
//                    //    这一步假设 mediaId 就是我们的 song_id
//                    val newMusicItems = mediaItems.mapNotNull { mediaItem ->
//                        val songId = mediaItem.mediaId.toLongOrNull()
//                        // 从数据库或缓存中获取完整的 MusicItem
//                        if (songId != null) TracksManager.getMusicById(
//                            this@PlayService,
//                            songId
//                        ) else null
//                    }
//
//                    if (newMusicItems.isEmpty()) {
//                        future.set(listOf()) // 如果没有有效的歌曲，返回空列表
//                        return@launch
//                    }
//
//                    // --- 开始迁移你的核心业务逻辑 ---
//
//                    // 2. 确定插入位置。Media3 的 Player 会自动处理 index，
//                    //    我们在这里主要是为了更新我们自己的 musicQueue 和数据库。
//                    //    真实的插入位置由 player.addMediaItems(index, ...) 决定。
//                    //    我们假设需要添加到队尾。
//                    val playableMediaItems = newMusicItems.map { musicItem ->
//                        MediaItem.Builder()
//                            .setMediaId(musicItem.id.toString())
//                            .setUri(musicItem.path.toUri())
//                            .setMediaMetadata(MediaItemUtils.musicItemToMediaMetadata(musicItem)) // 假设有这个转换函数
//                            .build()
//                    }
//                    future.set(playableMediaItems)
//                } catch (e: Exception) {
//                    future.setException(e)
//                }
//            }
//
//            return future
//        }


    }


    private fun timeSet(extras: Bundle) {
        val t = extras.getLong("time")
        val v = extras.getBoolean("play_completed", false)
        sleepTime = t
        playCompleted = v
        if (countDownTimer != null) {
            remainingTime = 0
            countDownTimer?.cancel()
        }
        if (sleepTime > 0) {
            countDownTimer = object : CountDownTimer(sleepTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingTime = millisUntilFinished
                    val bundle = Bundle()
                    bundle.putLong("remaining", remainingTime)
                    mControllerInfo?.let {
                        mediaSession?.sendCustomCommand(
                            it,
                            MediaCommands.COMMAND_SLEEP_STATE_UPDATE,
                            bundle
                        )
                    }
                }

                override fun onFinish() {
                    if (playCompleted) {
                        needPlayPause = true
                    } else {
                        timeFinish()
                    }
                }
            }
            countDownTimer?.start()
        } else {
            remainingTime = 0
            countDownTimer?.cancel()
            val bundle = Bundle()
            bundle.putLong("remaining", remainingTime)
            mControllerInfo?.let {
                mediaSession?.sendCustomCommand(
                    it,
                    MediaCommands.COMMAND_SLEEP_STATE_UPDATE,
                    bundle
                )
            }
        }
    }


    fun timeFinish() {
        remainingTime = 0
        sleepTime = 0
        exoPlayer.pause()
        val bundle = Bundle()
        bundle.putLong("remaining", remainingTime)
        bundle.putLong("sleepTime", 0L)
        mControllerInfo?.let {
            mediaSession?.sendCustomCommand(
                it,
                MediaCommands.COMMAND_SLEEP_STATE_UPDATE,
                bundle
            )
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var config: PlayConfig? = null
    private var musicVisualizationEnable = false

    private fun initializePlayerData() {
        serviceScope.launch {
            val loadingJob = launch(Dispatchers.IO) {
                db = MusicDatabase.getDatabase(this@PlayService)
                loadAllTracksAndFolders()
                awaitAll(
                    async { loadAuxSettings() },
                    async { loadPlayConfig() },
                    async { loadMainTabs() },
                    async { loadShowIndicatorList() },
                    async { loadVisualizationSettings() },
                    async { loadLastQueue() }
                )
            }
            loadingJob.join()
            applyLoadedDataToPlayer()
            isInitialized.complete(Unit)
        }
    }


    private fun setData(bundle: Bundle) {
        bundle.putInt("volume", volumeValue)
        bundle.putLong("playListID", playListCurrent?.id ?: -1)
        bundle.putString("playListType", playListCurrent?.type?.name)
        bundle.putParcelableArrayList("songsList", ArrayList(tracksLinkedHashMap.values))
        bundle.putParcelableArrayList("musicQueue", musicQueue)
        bundle.putSerializable("playListCurrent", playListCurrent)
        bundle.putBoolean("isPlaying", exoPlayer.isPlaying)
        bundle.putBoolean("play_completed", playCompleted)
        bundle.putInt("index", exoPlayer.currentMediaItemIndex)
        bundle.putFloat("pitch", auxr.pitch)
        bundle.putFloat("speed", auxr.speed)
        bundle.putFloat("Q", auxr.equalizerQ)
        bundle.putBoolean("equalizerEnable", equalizerAudioProcessor.isSetActive())
        bundle.putIntArray("equalizerValue", equalizerAudioProcessor.getBandLevels())
        bundle.putParcelable("musicItem", currentPlayTrack)
        bundle.putLong("sleepTime", sleepTime)
        bundle.putLong("remaining", remainingTime)
        bundle.putFloat("delayTime", auxr.echoDelay)
        bundle.putFloat("decay", auxr.echoDecay)
        bundle.putBoolean("echoActive", auxr.echo)
        bundle.putBoolean("echoFeedBack", auxr.echoRevert)
        bundle.putInt("repeat", exoPlayer.repeatMode)
        bundle.putParcelableArrayList("mainTabList", mainTab)
        bundle.putParcelableArrayList("showIndicatorList", showIndicatorList)
    }


    private fun stopTimer() {
        remainingTime = 0
        countDownTimer?.cancel()
    }

    override fun onDestroy() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopTimer()
            SharedPreferencesUtils.saveCurrentDuration(
                this@PlayService,
                exoPlayer.currentPosition
            )
            mediaSession?.release()
            exoPlayer.stop()
            exoPlayer.release()
            serviceJob.cancel() // 在 Service 销毁时取消所有协程
        } catch (_: Exception) {
        }
        if (headsetCallback != null) {
            audioManager.unregisterAudioDeviceCallback(headsetCallback)
            headsetCallback = null
        }
        super.onDestroy()
    }

    private fun initExo(context: Context) {
        val renderersFactory: DefaultRenderersFactory =
            object : DefaultRenderersFactory(context) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean,
                ): AudioSink {
                    val au = DefaultAudioSink.Builder(context)
                        .setEnableFloatOutput(enableFloatOutput)
                        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                        .setAudioProcessors(
                            arrayOf(
                                equalizerAudioProcessor,
                            )
                        )
                        .build()
                    return object : ForwardingAudioSink(au) {
                        override fun configure(
                            inputFormat: Format,
                            specifiedBufferSize: Int,
                            outputChannels: IntArray?
                        ) {
                            val bytesPerSec = Util.getPcmFrameSize(
                                inputFormat.pcmEncoding,
                                inputFormat.channelCount
                            ) * inputFormat.sampleRate * 1 // 这里计算出的就是 1s音频的缓冲长度

                            CoroutineScope(Dispatchers.Main).launch {
                                if (needPlayPause) {
                                    needPlayPause = false
                                    timeFinish()
                                }
                                SharedPreferencesUtils.saveCurrentDuration(
                                    this@PlayService,
                                    exoPlayer.duration
                                )
                            }
                            super.configure(inputFormat, bytesPerSec, outputChannels)
                        }

                    }
                }
            }
        val trackSelectionFactory = AdaptiveTrackSelection.Factory()
        val trackSelectorParameters = DefaultTrackSelector.Parameters.Builder().build()
        val trackSelector = DefaultTrackSelector(context, trackSelectionFactory)
        trackSelector.parameters = trackSelectorParameters
        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer.shuffleOrder = NoShuffleOrder(0)
        exoPlayer.playWhenReady = true
        exoPlayer.repeatMode = config?.repeatModel ?: Player.REPEAT_MODE_ALL
        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        playerAddListener()
    }

    private val dbQueueWriteMutex = Mutex()
    var errorCount = 0
    private fun playerAddListener() {
        exoPlayer.addListener(@UnstableApi object : Player.Listener {
            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                CoroutineScope(Dispatchers.IO).launch {
                    val config = db.PlayConfigDao().findConfig()
                    if (config != null) {
                        config.repeatModel = repeatMode
                        db.PlayConfigDao().update(config)
                    }
                }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (!timeline.isEmpty && TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED == reason) {
                    val newQueue = mutableListOf<MediaItem>()
                    for (i in 0 until timeline.windowCount) {
                        // a. 获取指定索引的窗口信息，为了效率，我们重用一个 Window 对象
                        val window = timeline.getWindow(i, Timeline.Window())
                        val mediaItem = window.mediaItem
                        newQueue.add(mediaItem)
                    }
                    val qList: ArrayList<MusicItem> =
                        ArrayList((newQueue.map { MediaItemUtils.mediaItemToMusicItem(it) }).filterNotNull())
                    if (exoPlayer.shuffleModeEnabled) {
                        var maxTableId = qList.maxOfOrNull { it.tableId ?: 0 } ?: 0
                        qList.forEachIndexed { index, musicItem ->
                            musicItem.priority = index + 1
                            if (musicItem.tableId == null) {
                                maxTableId = maxTableId + 1
                                musicItem.tableId = maxTableId
                            }
                        }
                    } else {
                        var maxPriority = qList.maxOfOrNull { it.priority } ?: 0
                        qList.forEachIndexed { index, musicItem ->
                            musicItem.tableId = index.toLong() + 1
                            if (musicItem.priority == 0) {
                                maxPriority = maxPriority + 1
                                musicItem.priority = maxPriority
                            }
                        }
                    }
                    val l1 = musicQueue.sortedBy {
                        it.tableId
                    }
                    if (!PlayUtils.areQueuesContentAndOrderEqual(l1, qList)) {
                        musicQueue.clear()
                        musicQueue.addAll(qList)
                        serviceScope.launch {
                            withContext(Dispatchers.IO) {
                                dbQueueWriteMutex.withLock {
                                    db.QueueDao().deleteAllQueue()
                                    if (qList.isNotEmpty()) {
                                        db.QueueDao().insertAll(musicQueue)
                                    }
                                }
                            }
                        }
                    }
                }
                super.onTimelineChanged(timeline, reason)
            }

            override fun onVolumeChanged(volume: Float) {
                val volumeInt = (volume * 100).toInt()
                volumeValue = volumeInt
                SharedPreferencesUtils.saveVolume(this@PlayService, volumeValue)
            }


            @SuppressLint("ApplySharedPref")
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                currentPlayTrack =
                    if (musicQueue.isNotEmpty() && exoPlayer.currentMediaItemIndex < musicQueue.size) {
                        musicQueue[exoPlayer.currentMediaItemIndex]
                    } else {
                        null
                    }
                getSharedPreferences("Widgets", MODE_PRIVATE).getBoolean(
                    "enable",
                    false
                )
                    .let {
                        if (it) {
                            val intent = Intent(this@PlayService, PlayMusicWidget::class.java)
                            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            intent.putExtra("source", this@PlayService.packageName)
                            val ids = AppWidgetManager.getInstance(
                                application
                            ).getAppWidgetIds(
                                ComponentName(
                                    application,
                                    PlayMusicWidget::class.java
                                )
                            )
                            intent.putExtra("playingStatus", isPlaying)
                            intent.putExtra("title", currentPlayTrack?.name ?: "")
                            intent.putExtra("author", currentPlayTrack?.artist ?: "")
                            intent.putExtra("path", currentPlayTrack?.path ?: "")
                            intent.putExtra("id", currentPlayTrack?.id ?: 0L)
                            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                            SharedPreferencesUtils.setWidgetData(
                                this@PlayService,
                                isPlaying,
                                currentPlayTrack
                            )
                            sendBroadcast(intent)
                        }
                    }
                SharedPreferencesUtils.saveCurrentDuration(
                    this@PlayService,
                    exoPlayer.currentPosition
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                Log.e("ERROR", "", error)
                if (BuildConfig.DEBUG) {
                    error.printStackTrace()
                }
                if (errorCount > 3) {
                    Toast.makeText(
                        this@PlayService,
                        getString(R.string.mutiple_error_tip),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@PlayService,
                        getString(R.string.play_error_play_next),
                        Toast.LENGTH_SHORT
                    ).show()
                    exoPlayer.seekToNextMediaItem()
                    exoPlayer.prepare()
                    errorCount++
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (musicQueue.isEmpty() || exoPlayer.currentMediaItemIndex >= musicQueue.size) return
                if (currentPlayTrack?.id != musicQueue[exoPlayer.currentMediaItemIndex].id) {
                    SharedPreferencesUtils.saveSelectMusicId(
                        this@PlayService,
                        musicQueue[exoPlayer.currentMediaItemIndex].id
                    )
                    if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                        SharedPreferencesUtils.saveCurrentDuration(this@PlayService, 0)
                    }
                    currentPlayTrack =
                        musicQueue[exoPlayer.currentMediaItemIndex]
                    getSharedPreferences("Widgets", MODE_PRIVATE).getBoolean(
                        "enable",
                        false
                    )
                        .let {
                            if (it) {
                                val intent =
                                    Intent(this@PlayService, PlayMusicWidget::class.java)
                                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                intent.putExtra("source", this@PlayService.packageName)
                                val ids = AppWidgetManager.getInstance(
                                    application
                                ).getAppWidgetIds(
                                    ComponentName(
                                        application,
                                        PlayMusicWidget::class.java
                                    )
                                )
                                intent.putExtra("playingStatus", exoPlayer.isPlaying)
                                intent.putExtra("title", currentPlayTrack?.name ?: "")
                                intent.putExtra("author", currentPlayTrack?.artist ?: "")
                                intent.putExtra("path", currentPlayTrack?.path ?: "")
                                intent.putExtra("id", currentPlayTrack?.id ?: 0L)
                                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                                SharedPreferencesUtils.setWidgetData(
                                    this@PlayService,
                                    exoPlayer.isPlaying,
                                    currentPlayTrack
                                )
                                sendBroadcast(intent)
                            }
                        }

                    if (needPlayPause) {
                        needPlayPause = false
                        timeFinish()
                    }
                }
            }


            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                val newSpeed = playbackParameters.speed
                val newPitch = playbackParameters.pitch
                auxr.speed = newSpeed
                auxr.pitch = newPitch
                CoroutineScope(Dispatchers.IO).launch {
                    db.AuxDao().update(auxr)
                }
            }
        })
    }


    private fun clearCacheData() {
        albumsListTracksHashMap.clear()
        artistsListTracksHashMap.clear()
        genresListTracksHashMap.clear()
        foldersListTracksHashMap.clear()
        albumsLinkedHashMap.clear()
        playListLinkedHashMap.clear()
        artistsLinkedHashMap.clear()
        genresLinkedHashMap.clear()
        foldersLinkedHashMap.clear()
        artistHasAlbumMap.clear()
        genreHasAlbumMap.clear()
    }


    data class SearchResult(
        val tracks: ArrayList<MusicItem>,
        val albums: ArrayList<AlbumList>,
        val artists: ArrayList<ArtistList>
    )

    private suspend fun search(query: String): SearchResult {
        if (query.isEmpty() || query.length <= 1) {
            return SearchResult(ArrayList(), ArrayList(), ArrayList())
        }
        return coroutineScope {
            val tracksDeferred = async(Dispatchers.IO) {
                TracksManager.searchTracks(this@PlayService, tracksLinkedHashMap, query)
            }
            val albumsDeferred = async(Dispatchers.IO) {
                AlbumManager.getAlbumByName(this@PlayService, query)
            }
            val artistsDeferred = async(Dispatchers.IO) {
                ArtistManager.getArtistByName(this@PlayService, query)
            }
            SearchResult(
                tracks = tracksDeferred.await(),
                albums = albumsDeferred.await(),
                artists = artistsDeferred.await()
            )
        }
    }

    private fun sortAction(extras: Bundle, future: SettableFuture<SessionResult>) {
        val typeString = extras.getString("type")
        if (!typeString.isNullOrEmpty()) {
            when (typeString) {
                PlayListType.PlayLists.name -> {
                    playListLinkedHashMap.clear()
                    future.set(SessionResult(SessionResult.RESULT_SUCCESS, Bundle()))
                }

                PlayListType.Albums.name -> {
                    albumsLinkedHashMap.clear()
                    future.set(SessionResult(SessionResult.RESULT_SUCCESS, Bundle()))
                }

                PlayListType.Artists.name -> {
                    artistsLinkedHashMap.clear()
                    future.set(SessionResult(SessionResult.RESULT_SUCCESS, Bundle()))
                }

                PlayListType.Genres.name -> {
                    genresLinkedHashMap.clear()
                    future.set(SessionResult(SessionResult.RESULT_SUCCESS, Bundle()))
                }

                PlayListType.Songs.name -> {
                    tracksLinkedHashMap.clear()
                    CoroutineScope(Dispatchers.IO).launch {
                        val sortData =
                            db.SortFiledDao().findSortByType(PlayListType.Songs.name)
                        TracksManager.getFolderList(
                            this@PlayService,
                            foldersLinkedHashMap,
                            tracksLinkedHashMap,
                            "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
                        )
                        val sortData1 = db.SortFiledDao().findSortAll()
                        showIndicatorList.clear()
                        sortData1.forEach {
                            if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                                showIndicatorList.add(it)
                            }
                        }
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, Bundle()))
                    }
                }

                PlayUtils.ListTypeTracks.PlayListsTracks -> {
                    playListTracksHashMap.clear()
                    CoroutineScope(Dispatchers.IO).launch {
                        val sortData1 = db.SortFiledDao().findSortAll()
                        showIndicatorList.clear()
                        sortData1.forEach {
                            if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                                showIndicatorList.add(it)
                            }
                        }
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, Bundle()))
                    }
                }

                PlayUtils.ListTypeTracks.AlbumsTracks -> {
                    albumsListTracksHashMap.clear()
                    CoroutineScope(Dispatchers.IO).launch {
                        val sortData1 = db.SortFiledDao().findSortAll()
                        showIndicatorList.clear()
                        sortData1.forEach {
                            if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                                showIndicatorList.add(it)
                            }
                        }
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, Bundle()))


                    }
                }

                PlayUtils.ListTypeTracks.ArtistsTracks -> {
                    artistsListTracksHashMap.clear()
                    CoroutineScope(Dispatchers.IO).launch {
                        val sortData1 = db.SortFiledDao().findSortAll()
                        showIndicatorList.clear()
                        sortData1.forEach {
                            if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                                showIndicatorList.add(it)
                            }
                        }
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, Bundle()))

                    }
                }

                PlayUtils.ListTypeTracks.GenresTracks -> {
                    genresListTracksHashMap.clear()
                    CoroutineScope(Dispatchers.IO).launch {
                        val sortData1 = db.SortFiledDao().findSortAll()
                        showIndicatorList.clear()
                        sortData1.forEach {
                            if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                                showIndicatorList.add(it)
                            }
                        }
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, Bundle()))
                    }
                }

                PlayUtils.ListTypeTracks.FoldersTracks -> {
                    foldersListTracksHashMap.clear()
                    CoroutineScope(Dispatchers.IO).launch {
                        val sortData1 = db.SortFiledDao().findSortAll()
                        showIndicatorList.clear()
                        sortData1.forEach {
                            if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                                showIndicatorList.add(it)
                            }
                        }
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, Bundle()))
                    }
                }

            }
        }
    }

    private fun refreshAction(future: SettableFuture<SessionResult>) {
        playListLinkedHashMap.clear()
        albumsLinkedHashMap.clear()
        artistsLinkedHashMap.clear()
        genresLinkedHashMap.clear()
        tracksLinkedHashMap.clear()
        playListTracksHashMap.clear()
        albumsListTracksHashMap.clear()
        artistsListTracksHashMap.clear()
        genresListTracksHashMap.clear()
        foldersListTracksHashMap.clear()
        allTracksLinkedHashMap.clear()
        clearCacheData()
        CoroutineScope(Dispatchers.IO).launch {
            val sortData =
                db.SortFiledDao().findSortByType(PlayListType.Songs.name)
            TracksManager.getFolderList(
                this@PlayService,
                foldersLinkedHashMap,
                tracksLinkedHashMap,
                "${sortData?.filed ?: ""} ${sortData?.method ?: ""}",
                allTracksLinkedHashMap
            )
            val sortData1 = db.SortFiledDao().findSortAll()
            showIndicatorList.clear()
            sortData1.forEach {
                if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                    showIndicatorList.add(it)
                }
            }
            val resultData = Bundle().apply {
                putParcelableArrayList(
                    "songsList",
                    ArrayList(allTracksLinkedHashMap.values)
                )
            }
            future.set(SessionResult(SessionResult.RESULT_SUCCESS, resultData))
        }
    }


    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    fun getPlayList(
        context: Context,
        future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>, params: LibraryParams?
    ) {
        if (playListLinkedHashMap.isNotEmpty()) {
            val mediaItems = ArrayList(playListLinkedHashMap.values).map { playlist ->
                MediaItemUtils.playlistToMediaItem(playlist)
            }
            future.set(LibraryResult.ofItemList(mediaItems, null))
        } else {
            val sortData =
                db.SortFiledDao().findSortByType(PlayListType.PlayLists.name)
            val result = PlaylistManager.getPlaylists(
                context,
                playListLinkedHashMap,
                tracksLinkedHashMap,
                sortData?.filed,
                sortData?.method
            )
            val mediaItems = result.map { playlist ->
                MediaItemUtils.playlistToMediaItem(playlist)
            }
            future.set(LibraryResult.ofItemList(mediaItems, null))
        }
    }

    fun getPlayListTracks(
        id: Long,
        future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>
    ) {
        val list = playListTracksHashMap[id]
        if (list != null) {
            val mediaItems = list.map { playlist ->
                MediaItemUtils.musicItemToMediaItem(playlist)
            }
            // playListLinkedHashMap[id]
            //   bundle.putParcelable("message", playListLinkedHashMap[id])
            future.set(LibraryResult.ofItemList(mediaItems, null))
            return
        } else {
            val sortData =
                db.SortFiledDao()
                    .findSortByType(PlayUtils.ListTypeTracks.PlayListsTracks)
            val playList = playListLinkedHashMap[id]
            if (playList != null) {
                val file = File(playList.path)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Files.getContentUri("external"), id
                )
                val tracks =
                    PlaylistManager.getTracksByPlayListId(
                        this@PlayService,
                        contentUri,
                        file.parent!!,
                        allTracksLinkedHashMap,
                        sortData?.filed, sortData?.method
                    )
                playListTracksHashMap[id] = tracks
                val mediaItems = tracks.map { playlist ->
                    MediaItemUtils.musicItemToMediaItem(playlist)
                }
                // bundle.putParcelable("message", playListLinkedHashMap[id])
                future.set(LibraryResult.ofItemList(mediaItems, null))
                return
            } else {
                future.set(LibraryResult.ofItemList(emptyList(), null))
                return
            }
        }
    }

    fun getGenreListTracks(
        id: Long,
        future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>
    ) {
        val list = genresListTracksHashMap[id]
        //   bundle.putParcelableArrayList("albums", genreHasAlbumMap[id])
        if (list != null) {
            val mediaItems = list.map { playlist ->
                MediaItemUtils.musicItemToMediaItem(playlist)
            }
            // playListLinkedHashMap[id]
            //   bundle.putParcelable("message", playListLinkedHashMap[id])
            future.set(LibraryResult.ofItemList(mediaItems, null))
        } else {
            val playList = genresLinkedHashMap[id]
            if (playList != null) {
                val sortData =
                    db.SortFiledDao()
                        .findSortByType(PlayUtils.ListTypeTracks.GenresTracks)
                val uri =
                    MediaStore.Audio.Genres.Members.getContentUri("external", id)
                runBlocking {
                    awaitAll(
                        async(Dispatchers.IO) {
                            val sortDataP =
                                db.SortFiledDao()
                                    .findSortByType(PlayListType.Albums.name)
                            AlbumManager.getAlbumList(
                                this@PlayService,
                                albumsLinkedHashMap,
                                "${sortDataP?.filed ?: ""} ${sortDataP?.method ?: ""}"
                            )
                        }
                    )
                }
                val albums = AlbumManager.getAlbumsByGenre(
                    this@PlayService,
                    uri,
                    albumsLinkedHashMap,
                    null,
                    null,
                    null
                )
                val listT: ArrayList<MusicItem> =
                    TracksManager.getTracksById(
                        this@PlayService,
                        uri,
                        allTracksLinkedHashMap,
                        null,
                        null,
                        "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
                    )
                genresListTracksHashMap[id] = listT
                genreHasAlbumMap[id] = albums
                val mediaItems = listT.map { playlist ->
                    MediaItemUtils.musicItemToMediaItem(playlist)
                }
                // bundle.putParcelable("message", playListLinkedHashMap[id])
                future.set(LibraryResult.ofItemList(mediaItems, null))
            } else {
                future.set(LibraryResult.ofItemList(emptyList(), null))
            }
        }
    }

    fun getAlbumListTracks(
        id: Long,
        future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>
    ) {
        val list = albumsListTracksHashMap[id]
        if (list != null) {
            val mediaItems = list.map { playlist ->
                MediaItemUtils.musicItemToMediaItem(playlist)
            }
            // playListLinkedHashMap[id]
            //   bundle.putParcelable("message", playListLinkedHashMap[id])
            future.set(LibraryResult.ofItemList(mediaItems, null))
        } else {
            val sortData =
                db.SortFiledDao()
                    .findSortByType(PlayUtils.ListTypeTracks.AlbumsTracks)
            val trackUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val listT: ArrayList<MusicItem> = TracksManager.getTracksById(
                this@PlayService,
                trackUri,
                allTracksLinkedHashMap,
                MediaStore.Audio.Media.ALBUM_ID + "=?",
                arrayOf(id.toString()),
                "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
            )
            albumsListTracksHashMap[id] = listT
            val mediaItems = listT.map { playlist ->
                MediaItemUtils.musicItemToMediaItem(playlist)
            }
            // bundle.putParcelable("message", playListLinkedHashMap[id])
            future.set(LibraryResult.ofItemList(mediaItems, null))
        }
    }

    fun getArtistListTracks(
        id: Long,
        future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>
    ) {
        val list = artistsListTracksHashMap[id]
        if (list != null) {
            val mediaItems = list.map { playlist ->
                MediaItemUtils.musicItemToMediaItem(playlist)
            }
//            bundle.putParcelableArrayList("albums", artistHasAlbumMap[id])
            // playListLinkedHashMap[id]
            //   bundle.putParcelable("message", playListLinkedHashMap[id])
            future.set(LibraryResult.ofItemList(mediaItems, null))
        } else {
            val playList = artistsLinkedHashMap[id]
            if (playList != null) {
                val sortData =
                    db.SortFiledDao()
                        .findSortByType(PlayUtils.ListTypeTracks.ArtistsTracks)
                val trackUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                runBlocking {
                    awaitAll(
                        async(Dispatchers.IO) {
                            val sortDataP =
                                db.SortFiledDao()
                                    .findSortByType(PlayListType.Albums.name)
                            AlbumManager.getAlbumList(
                                this@PlayService,
                                albumsLinkedHashMap,
                                "${sortDataP?.filed ?: ""} ${sortDataP?.method ?: ""}"
                            )
                        }
                    )
                }

                val albumsList = AlbumManager.getAlbumsByArtist(
                    this@PlayService,
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    albumsLinkedHashMap,
                    MediaStore.Audio.Media.ARTIST_ID + "=?",
                    arrayOf(id.toString()),
                    null
                )
                val listT: ArrayList<MusicItem> = TracksManager.getTracksById(
                    this@PlayService,
                    trackUri,
                    allTracksLinkedHashMap,
                    MediaStore.Audio.Media.ARTIST_ID + "=?",
                    arrayOf(id.toString()),
                    "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
                )
                artistHasAlbumMap[id] = albumsList
                artistsListTracksHashMap[id] = listT
                val mediaItems = listT.map { playlist ->
                    MediaItemUtils.musicItemToMediaItem(playlist)
                }
                // bundle.putParcelable("message", playListLinkedHashMap[id])
                future.set(LibraryResult.ofItemList(mediaItems, null))
            } else {
                future.set(LibraryResult.ofItemList(emptyList(), null))
            }
        }
    }

    fun getFolderListTracks(
        id: Long,
        future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>
    ) {
        val list = foldersListTracksHashMap[id]
        if (list != null) {
            val mediaItems = list.values.map { playlist ->
                MediaItemUtils.musicItemToMediaItem(playlist)
            }
            // playListLinkedHashMap[id]
            //   bundle.putParcelable("message", playListLinkedHashMap[id])
            future.set(LibraryResult.ofItemList(mediaItems, null))
        } else {
            val sortData =
                db.SortFiledDao()
                    .findSortByType(PlayUtils.ListTypeTracks.FoldersTracks)
            val uri =
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val selection = MediaStore.Audio.Media.BUCKET_ID + "=?"
            val listT: ArrayList<MusicItem> =
                TracksManager.getTracksById(
                    this@PlayService,
                    uri,
                    allTracksLinkedHashMap,
                    selection,
                    arrayOf(id.toString()),
                    "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
                )
            val tracks = LinkedHashMap<Long, MusicItem>()
            listT.forEach { itM ->
                tracks[itM.id] = itM
            }
            foldersListTracksHashMap[id] = tracks
            val mediaItems = listT.map { playlist ->
                MediaItemUtils.musicItemToMediaItem(playlist)
            }
            // bundle.putParcelable("message", playListLinkedHashMap[id])
            future.set(LibraryResult.ofItemList(mediaItems, null))
        }
    }

    fun getSongsAll(
        future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>, params: LibraryParams?
    ) {
        if (tracksLinkedHashMap.isNotEmpty()) {
            val mediaItems = tracksLinkedHashMap.values.map { playlist ->
                MediaItemUtils.musicItemToMediaItem(playlist)
            }
            future.set(LibraryResult.ofItemList(mediaItems, null))
            return
        } else {
            val sortData =
                db.SortFiledDao().findSortByType(PlayListType.Songs.name)
            TracksManager.getFolderList(
                this@PlayService,
                foldersLinkedHashMap,
                tracksLinkedHashMap,
                "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
            )
            val mediaItems = tracksLinkedHashMap.values.map { playlist ->
                MediaItemUtils.musicItemToMediaItem(playlist)
            }
            future.set(LibraryResult.ofItemList(mediaItems, null))
        }
    }

    fun getGenres(
        future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>, params: LibraryParams?
    ) {
        if (genresLinkedHashMap.isNotEmpty()) {
            val mediaItems = genresLinkedHashMap.values.map { playlist ->
                MediaItemUtils.genreToMediaItem(playlist)
            }
            future.set(LibraryResult.ofItemList(mediaItems, null))
            return
        } else {
            val sortDataP = db.SortFiledDao().findSortByType(PlayListType.Genres.name)
            GenreManager.getGenresList(
                this@PlayService,
                genresLinkedHashMap,
                tracksLinkedHashMap,
                "${sortDataP?.filed ?: ""} ${sortDataP?.method ?: ""}"
            )
            val mediaItems = genresLinkedHashMap.values.map { playlist ->
                MediaItemUtils.genreToMediaItem(playlist)
            }
            future.set(LibraryResult.ofItemList(mediaItems, null))
        }
    }

    fun getAlbums(
        future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>, params: LibraryParams?
    ) {
        if (albumsLinkedHashMap.isNotEmpty()) {
            val mediaItems = albumsLinkedHashMap.values.map { playlist ->
                MediaItemUtils.albumToMediaItem(playlist)
            }
            future.set(LibraryResult.ofItemList(mediaItems, null))
        } else {
            val sortDataP =
                db.SortFiledDao().findSortByType(PlayListType.Albums.name)
            AlbumManager.getAlbumList(
                this@PlayService,
                albumsLinkedHashMap,
                "${sortDataP?.filed ?: ""} ${sortDataP?.method ?: ""}"
            )
            val mediaItems = albumsLinkedHashMap.values.map { playlist ->
                MediaItemUtils.albumToMediaItem(playlist)
            }
            future.set(LibraryResult.ofItemList(mediaItems, null))
        }
    }

    fun getArtists(
        future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>, params: LibraryParams?
    ) {
        if (artistsLinkedHashMap.isNotEmpty()) {
            val mediaItems = artistsLinkedHashMap.values.map { playlist ->
                MediaItemUtils.artistToMediaItem(playlist)
            }
            future.set(LibraryResult.ofItemList(mediaItems, null))
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val sortDataP =
                    db.SortFiledDao().findSortByType(PlayListType.Artists.name)
                ArtistManager.getArtistList(
                    this@PlayService,
                    artistsLinkedHashMap,
                    "${sortDataP?.filed ?: ""} ${sortDataP?.method ?: ""}"
                )
                val mediaItems = artistsLinkedHashMap.values.map { playlist ->
                    MediaItemUtils.artistToMediaItem(playlist)
                }
                future.set(LibraryResult.ofItemList(mediaItems, null))
            }
        }
    }

    fun getFolders(
        future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>, params: LibraryParams?
    ) {
        if (foldersLinkedHashMap.isNotEmpty()) {
            val mediaItems = foldersLinkedHashMap.values.map { playlist ->
                MediaItemUtils.folderToMediaItem(playlist)
            }
            future.set(LibraryResult.ofItemList(mediaItems, null))
        } else {
            val sortData =
                db.SortFiledDao().findSortByType(PlayListType.Songs.name)
            TracksManager.getFolderList(
                this@PlayService,
                foldersLinkedHashMap,
                tracksLinkedHashMap,
                "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
            )
            val mediaItems = foldersLinkedHashMap.values.map { playlist ->
                MediaItemUtils.folderToMediaItem(playlist)
            }
            future.set(LibraryResult.ofItemList(mediaItems, null))
        }
    }

    private fun applyLoadedDataToPlayer() {
        volumeValue = SharedPreferencesUtils.getVolume(this@PlayService)
        exoPlayer.volume = volumeValue / 100f
        exoPlayer.repeatMode = config?.repeatModel ?: Player.REPEAT_MODE_ALL
        val p = PlaybackParameters(
            auxr.speed,
            auxr.pitch
        )
        var position: Long
        exoPlayer.playbackParameters = p
        if (musicQueue.isNotEmpty()) {
            val t1 = ArrayList<MediaItem>()
            var currentIndex = 0
            musicQueue.forEachIndexed { index, it ->
                t1.add(MediaItemUtils.musicItemToMediaItem(it))
                if (it.id == currentPlayTrack?.id) {
                    currentIndex = index
                }
            }
            exoPlayer.shuffleModeEnabled = SharedPreferencesUtils.getEnableShuffle(this@PlayService)
            exoPlayer.setMediaItems(t1)
            if (currentPlayTrack != null) {
                position = SharedPreferencesUtils.getCurrentPosition(this@PlayService)
                if (position >= (currentPlayTrack?.duration ?: 0)) {
                    position = 0
                }
                exoPlayer.seekTo(currentIndex, position)
            }
            exoPlayer.playWhenReady = false
            exoPlayer.prepare()
        }
    }


    private fun loadAllTracksAndFolders() {
        db = MusicDatabase.getDatabase(this@PlayService)
        val sortDataDao =
            db.SortFiledDao()
        val sortData =
            sortDataDao.findSortByType(PlayListType.Songs.name)
        TracksManager.getFolderList(
            this@PlayService,
            foldersLinkedHashMap,
            tracksLinkedHashMap,
            "${sortData?.filed ?: ""} ${sortData?.method ?: ""}",
            allTracksLinkedHashMap
        )
    }

    private fun loadAuxSettings() {
        val auxTemp = db.AuxDao().findFirstAux()
        if (auxTemp == null) {
            db.AuxDao().insert(auxr)
        } else {
            auxr = auxTemp
        }
        equalizerAudioProcessor.setDaleyTime(auxr.echoDelay)
        equalizerAudioProcessor.setDecay(auxr.echoDecay)
        equalizerAudioProcessor.setFeedBack(auxr.echoRevert)
        equalizerAudioProcessor.setEchoActive(auxr.echo)
        equalizerAudioProcessor.setEqualizerActive(auxr.equalizer)
        equalizerAudioProcessor.setQ(auxr.equalizerQ, false)
        val selectedPreset = this@PlayService.getSharedPreferences(
            "SelectedPreset",
            MODE_PRIVATE
        ).getString("SelectedPreset", Utils.custom)
        if (selectedPreset == Utils.custom) {
            for (i in 0 until 10) {
                equalizerAudioProcessor.setBand(
                    i,
                    auxr.equalizerBand[i]
                )
            }
        } else {
            Utils.eqPreset[selectedPreset]?.forEachIndexed { index, i ->
                equalizerAudioProcessor.setBand(
                    index,
                    i
                )
            }
        }
    }

    private fun loadPlayConfig() {
        config = db.PlayConfigDao().findConfig()
        if (config == null) {
            config = PlayConfig(0, Player.REPEAT_MODE_ALL)
            db.PlayConfigDao().insert(config!!)
        }
    }

    private fun loadMainTabs() {
        val list =
            db.MainTabDao().findAllIsShowMainTabSortByPriority()
        if (list.isEmpty()) {
            PlayUtils.addDefaultMainTab(mainTab)
            db.MainTabDao().insertAll(mainTab)
        } else {
            mainTab.addAll(list)
        }
    }

    private fun loadShowIndicatorList() {
        val sortData1 = db.SortFiledDao().findSortAll()
        showIndicatorList.clear()
        sortData1.forEach {
            if (it.type == PlayListType.Songs.name || it.type.endsWith(
                    "@Tracks"
                )
            ) {
                showIndicatorList.add(it)
            }
        }
    }

    private fun loadVisualizationSettings() {
        // TODO move to   val auxTemp = db.AuxDao().findFirstAux()
        musicVisualizationEnable =
            SharedPreferencesUtils.getEnableMusicVisualization(this@PlayService)
//                                visualizationAudioProcessor.isActive = musicVisualizationEnable
        equalizerAudioProcessor.setVisualizationAudioActive(
            musicVisualizationEnable
        )
    }

    private fun loadLastQueue() {
        val queue =
            if (SharedPreferencesUtils.getEnableShuffle(this@PlayService)) {
                // don't need to check shuffle, should not no shuffle, on first time
                db.QueueDao().findQueueShuffle()
            } else {
                db.QueueDao().findQueue()
            }
        musicQueue.clear()
        if (queue.isNotEmpty()) {
            val idCurrent =
                SharedPreferencesUtils.getCurrentPlayId(this@PlayService)
            var id = -1L
            queue.forEach {
                // check has this tracks, avoid user remove it in storage
                if (allTracksLinkedHashMap[it.id] != null) {
                    musicQueue.add(it)
                    if (it.id == idCurrent) {
                        currentPlayTrack = allTracksLinkedHashMap[it.id]
                        id = idCurrent
                    }
                }
            }
            if (musicQueue.isNotEmpty() && id >= 0) {
                val plaC = db.CurrentListDao().findCurrentList()
                if (plaC != null) {
                    playListCurrent =
                        AnyListBase(
                            plaC.listID,
                            enumValueOf(plaC.type)
                        )
                }
                if (currentPlayTrack == null) {
                    playListCurrent = null
                    SharedPreferencesUtils.saveSelectMusicId(
                        this@PlayService,
                        -1
                    )
                    SharedPreferencesUtils.saveCurrentDuration(
                        this@PlayService,
                        0
                    )
                }
            }
        }
    }

    fun createTopLevelCategories(): List<MediaItem> {
        return listOf(
            createCategoryMediaItem("songs_root", "歌曲"),
            createCategoryMediaItem("albums_root", "专辑"),
            createCategoryMediaItem("artists_root", "艺术家"),
            createCategoryMediaItem("playlists_root", "播放列表"),
            createCategoryMediaItem("genres_root", "流派"),
            createCategoryMediaItem("folders_root", "文件夹")
        )
    }

    private fun createCategoryMediaItem(id: String, title: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(true) // 关键！必须是可浏览的
            .setIsPlayable(false)
            .build()
        return MediaItem.Builder().setMediaId(id).setMediaMetadata(metadata).build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }
}
