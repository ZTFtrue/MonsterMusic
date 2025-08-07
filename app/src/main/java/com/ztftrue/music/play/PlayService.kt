package com.ztftrue.music.play

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
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
import com.google.common.reflect.TypeToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.gson.Gson
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
import com.ztftrue.music.utils.BluetoothConnectionReceiver
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


    private   var mediaSession: MediaLibrarySession? = null
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
    private var receiver: BluetoothConnectionReceiver? = null
    private var countDownTimer: CountDownTimer? = null
    override fun onCreate() {
        super.onCreate()
        initExo(this@PlayService)
        initializePlayerData()
        if (SharedPreferencesUtils.getAutoPlayEnable(this)) {
            receiver = BluetoothConnectionReceiver(exoPlayer)
            val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            filter.addAction(Intent.ACTION_HEADSET_PLUG)
            registerReceiver(receiver, filter)
        }
        val contentIntent = Intent(this, MainActivity::class.java)
        val pendingContentIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaLibrarySession.Builder(
            this,
            exoPlayer,
            MySessionCallback(this@PlayService),
        ).build()
        val notification = DefaultMediaNotificationProvider(this)
        setMediaNotificationProvider(notification)
    }

    inner class MySessionCallback(private val context: Context) : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            Log.e("MY_APP_DEBUG", "SERVICE: onConnect called by package: ${controller.packageName}")
            mControllerInfo = controller
            val availableCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                    // 添加所有你定义的 SessionCommand
                    .add(COMMAND_CHANGE_PITCH)
                    .add(COMMAND_CHANGE_Q)
                    .add(COMMAND_ADD_TO_QUEUE)
                    .add(COMMAND_DSP_SET_BAND)
                    .add(COMMAND_DSP_FLATTEN)
                    .add(COMMAND_DSP_SET_BANDS)
                    .add(COMMAND_ECHO_ENABLE)
                    .add(COMMAND_ECHO_SET_DELAY)
                    .add(COMMAND_ECHO_SET_DECAY)
                    .add(COMMAND_ECHO_SET_FEEDBACK)
                    .add(COMMAND_SEARCH)
                    .add(COMMAND_VISUALIZATION_ENABLE)
                    .add(COMMAND_SET_SLEEP_TIMER)
                    .add(COMMAND_VISUALIZATION_CONNECTED)
                    .add(COMMAND_VISUALIZATION_DISCONNECTED)
                    .add(COMMAND_GET_INITIALIZED_DATA)
                    .add(COMMAND_APP_EXIT)
                    .add(COMMAND_TRACKS_UPDATE)
                    .add(COMMAND_REMOVE_FROM_QUEUE)
                    .add(COMMAND_SORT_TRACKS)
                    .add(COMMAND_CHANGE_PLAYLIST)
                    .add(COMMAND_SORT_QUEUE)
                    .add(COMMAND_CLEAR_QUEUE)
                    .add(COMMAND_REFRESH_ALL)
                    .add(COMMAND_TRACK_DELETE)
                    .add(COMMAND_PlAY_LIST_CHANGE)
                    .add(COMMAND_GET_PLAY_LIST_ITEM)
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
            Log.d("PlayService", "onCustomCommand: ${customCommand.customAction}")
            when (customCommand.customAction) {
                // --- DSP & Effects ---
                COMMAND_CHANGE_PITCH.customAction -> {
                    val pitch = args.getFloat("pitch", 1f)
                    val param1 = PlaybackParameters(
                        auxr.speed, pitch
                    )
                    exoPlayer.playbackParameters = param1
                    auxr.pitch = pitch
                    CoroutineScope(Dispatchers.IO).launch {
                        db.AuxDao().update(auxr)
                    }
                }

                COMMAND_CHANGE_Q.customAction -> {
                    val q = args.getFloat("Q", 3.2f)
                    equalizerAudioProcessor.setQ(q)
                    auxr.equalizerQ = q
                    CoroutineScope(Dispatchers.IO).launch {
                        db.AuxDao().update(auxr)
                    }
                }

                COMMAND_DSP_ENABLE.customAction -> {
                    val enable = args.getBoolean("enable")
                    equalizerAudioProcessor.setEqualizerActive(enable)
                    auxr.equalizer = equalizerAudioProcessor.isSetActive()
                    CoroutineScope(Dispatchers.IO).launch {
                        db.AuxDao().update(auxr)
                    }
                }

                COMMAND_SORT_TRACKS.customAction -> {
                    sortAction(args)
                }

                COMMAND_GET_PLAY_LIST_ITEM.customAction -> {
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
                }

                COMMAND_SORT_QUEUE.customAction -> {
                    val method = args.getString("method")
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

                COMMAND_DSP_SET_BAND.customAction -> {
                    val index = args.getInt(KEY_INDEX)
                    val value = args.getInt(KEY_VALUE)
                    equalizerAudioProcessor.setBand(index, value)
                    auxr.equalizerBand[index] = value
                    CoroutineScope(Dispatchers.IO).launch { db.AuxDao().update(auxr) }
                }

                COMMAND_DSP_FLATTEN.customAction -> {
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

                COMMAND_DSP_SET_BANDS.customAction -> {
                    args.getIntArray(KEY_INT_ARRAY_VALUE)?.forEachIndexed { index, v ->
                        equalizerAudioProcessor.setBand(index, v)
                    }
                }

                COMMAND_ECHO_ENABLE.customAction -> {
                    val enable = args.getBoolean(KEY_ENABLE)
                    equalizerAudioProcessor.setEchoActive(enable)
                    auxr.echo = enable
                    CoroutineScope(Dispatchers.IO).launch { db.AuxDao().update(auxr) }
                }

                COMMAND_ECHO_SET_DELAY.customAction -> {
                    val delay = args.getFloat(KEY_DELAY)
                    equalizerAudioProcessor.setDaleyTime(delay)
                    auxr.echoDelay = delay
                    CoroutineScope(Dispatchers.IO).launch { db.AuxDao().update(auxr) }
                }

                COMMAND_ECHO_SET_DECAY.customAction -> {
                    val decay = args.getFloat(KEY_DECAY)
                    equalizerAudioProcessor.setDecay(decay)
                    auxr.echoDecay = decay
                    CoroutineScope(Dispatchers.IO).launch { db.AuxDao().update(auxr) }
                }

                COMMAND_ECHO_SET_FEEDBACK.customAction -> {
                    val enable = args.getBoolean(KEY_ENABLE)
                    equalizerAudioProcessor.setFeedBack(enable)
                    auxr.echoRevert = enable
                    CoroutineScope(Dispatchers.IO).launch { db.AuxDao().update(auxr) }
                }

                // --- Visualization ---
                COMMAND_VISUALIZATION_ENABLE.customAction -> {
                    val enable = args.getBoolean(KEY_ENABLE)
                    musicVisualizationEnable = enable
                    equalizerAudioProcessor.setVisualizationAudioActive(enable)
                    SharedPreferencesUtils.saveEnableMusicVisualization(this@PlayService, enable)
                }

                COMMAND_VISUALIZATION_CONNECTED.customAction -> {
                    Log.d("PlayService", "Visualization component connected. Enabling processing.")
                    if (musicVisualizationEnable) { // musicVisualizationEnable 是 Service 的一个状态变量
                        equalizerAudioProcessor.setVisualizationAudioActive(true)
                    }
                }

                COMMAND_GET_INITIALIZED_DATA.customAction -> {
                    val future = SettableFuture.create<SessionResult>()
                    serviceScope.async {
                        isInitialized.await()
                        val bundle = Bundle()
                        setData(bundle, exoPlayer.currentPosition)
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, bundle))
                    }
                    return future;
                }

                COMMAND_VISUALIZATION_DISCONNECTED.customAction -> {
                    Log.d(
                        "PlayService",
                        "Visualization component disconnected. Disabling processing."
                    )
                    equalizerAudioProcessor.setVisualizationAudioActive(false)
                }

                // --- Sleep Timer ---
                COMMAND_SET_SLEEP_TIMER.customAction -> {
                    // 注意：旧的 timeSet 方法需要改造
                    // 它不能再直接调用 result.sendResult()
                    // 而是应该返回一个结果，或者我们在这里处理
                    // 假设我们直接在这里处理
                    timeSet(args) // 改造 timeSet，让它不再需要 Result 参数
                }

                COMMAND_APP_EXIT.customAction -> {
                    exoPlayer.pause()
                    exoPlayer.clearMediaItems()
                    // musicQueue.clear()
                    session.release()
                }

                COMMAND_CHANGE_PLAYLIST.customAction -> {
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
                        }
//                                db.QueueDao().deleteAllQueue()
//                                db.QueueDao().insertAll(musicQueue)
                    }
                }

                COMMAND_SEARCH.customAction -> {
                    val future = SettableFuture.create<SessionResult>()
                    val query = args.getString(KEY_SEARCH_QUERY, "")
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

                COMMAND_PlAY_LIST_CHANGE.customAction -> {
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

                COMMAND_REFRESH_ALL.customAction -> {
                    val future = SettableFuture.create<SessionResult>()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            refreshAction()
                            val resultData = Bundle().apply {
                                putParcelableArrayList(
                                    "songsList",
                                    ArrayList(allTracksLinkedHashMap.values)
                                )
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

                COMMAND_TRACK_DELETE.customAction -> {
                    val idsToDelete = args.getLong(KEY_TRACK_ID)
                    val c = PlayUtils.trackDelete(
                        idsToDelete,
                        musicQueue,
                        exoPlayer,
                        db,
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
                        putParcelableArrayList("songsList", ArrayList(tracksLinkedHashMap.values))
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

                COMMAND_TRACKS_UPDATE.customAction -> {
                    val future = SettableFuture.create<SessionResult>()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val id = args.getLong(KEY_TRACK_ID)
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
                COMMAND_ADD_TO_QUEUE.customAction -> {

                }


                COMMAND_CLEAR_QUEUE.customAction -> {
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


        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return super.onSetMediaItems(
                mediaSession,
                controller,
                mediaItems,
                startIndex,
                startPositionMs
            )
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return super.onPlaybackResumption(mediaSession, controller)
        }

        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent
        ): Boolean {
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            Log.d("PlayService", "onGetLibraryRoot")
            val clientPackageName = browser.packageName
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            // 为 Android Auto 提供一个简化的根节点
//            if (clientPackageName == "com.google.android.projection.gearhead") {
//                Log.d("PlayService", "Android Auto is connecting.")
//                // 返回一个为驾驶场景优化的根节点
//                val autoRootItem = MediaItemUtils.createFullFeaturedRoot()
//                return Futures.immediateFuture(LibraryResult.ofItem(autoRootItem, null))
//            }
//            // 为你自己的 App 提供一个功能完整的根节点
//            else
            if (clientPackageName == context.packageName) {
                Log.d("PlayService", "My own app is connecting.")
                val fullFeaturedRootItem = MediaItemUtils.createFullFeaturedRoot()
                return Futures.immediateFuture(LibraryResult.ofItem(fullFeaturedRootItem, null))
            } else {
                // 拒绝其他未知应用的连接
                Log.w(
                    "PlayService",
                    "Rejecting connection from unknown package: $clientPackageName"
                )
                // 在新版 Media3 中，应该返回一个拒绝的 Future
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
            Log.e("MY_APP_DEBUG", "SERVICE: onGetChildren called with parentId: '$parentId'")
            Log.d("PlayService", "onGetChildren: $parentId")
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
                            getSongsAll(context, future, params)
                        }

                        parentId == "albums_root" -> {
                            getAlbums(context, future, params)
                        }

                        parentId == "artists_root" -> {
                            getArtists(context, future, params)
                        }

                        parentId == "playlists_root" -> {
                            getPlayList(context, future, params)
                        }

                        parentId == "genres_root" -> {
                            getGenres(context, future, params)
                        }

                        parentId == "folders_root" -> {
                            getFolders(context, future, params)
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
                                getAlbumListTracks(context, id, future)
                            }
                        }

                        parentId.startsWith(PlayListType.Artists.name + "_track_") -> {
                            handlePrefixedId(
                                parentId,
                                PlayListType.Artists.name + "_track_",
                                future
                            ) { id ->
                                getArtistListTracks(context, id, future)
                            }
                        }

                        parentId.startsWith(PlayListType.Genres.name + "_track_") -> {
                            handlePrefixedId(
                                parentId,
                                PlayListType.Genres.name + "_track_",
                                future
                            ) { id ->
                                getGenreListTracks(context, id, future)
                            }
                        }

                        parentId.startsWith(PlayListType.PlayLists.name + "_track_") -> {
                            handlePrefixedId(
                                parentId,
                                PlayListType.PlayLists.name + "_track_",
                                future
                            ) { id ->
                                getPlayListTracks(context, id, future)
                            }
                        }

                        parentId.startsWith(PlayListType.Folders.name + "_track_") -> {
                            handlePrefixedId(
                                parentId,
                                PlayListType.Folders.name + "_track_",
                                future
                            ) { id ->
                                getFolderListTracks(context, id, future)
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
//                    mediaSession?.connectedControllers?.find { it.packageName == this@PlayService.packageName }
                    mControllerInfo?.let {
                        mediaSession?.sendCustomCommand(
                            it,
                            COMMAND_SLEEP_STATE_UPDATE,
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
                    COMMAND_SLEEP_STATE_UPDATE,
                    bundle
                )
            }
        }
    }

//    private fun playShuffleMusic(extras: Bundle, result: Result<Bundle>) {
//        if (notify == null) {
//            notify = CreateNotification(this@PlayService, mediaSession)
//        }
////        val switchQueue = extras.getBoolean("switch_queue", false)
////        val enableShuffle = extras.getBoolean("enable_shuffle", false)
//        val index = 0
////        val musicItem = extras.getParcelable<MusicItem>("musicItem")
//        val playList = extras.getParcelable<AnyListBase>("playList")
//        var musicItems = extras.getParcelableArrayList<MusicItem>("musicItems")
//        if (playList != null && musicItems != null) {
//            val gson = Gson()
//            val musicItemListType =
//                object : TypeToken<ArrayList<MusicItem?>?>() {}.type
//            musicItems =
//                gson.fromJson(gson.toJson(musicItems), musicItemListType)
//            musicItems?.let {
//                musicItems.forEachIndexed { i, item ->
//                    item.tableId = i + 1L
//                }
//                val dbArrayList = ArrayList<MusicItem>()
//                dbArrayList.addAll(musicItems)
//                musicItems.shuffle()
//                playListCurrent = playList
//                musicQueue.clear()
//                musicQueue.addAll(musicItems)
//                val t1 = ArrayList<MediaItem>()
//                if (musicQueue.isNotEmpty()) {
//                    musicQueue.forEachIndexed { i, musicItem ->
//                        musicItem.priority = i + 1
//                        t1.add(MediaItemUtils.musicItemToMediaItem(it))
//                    }
//                    val bundle = Bundle()
//                    bundle.putParcelableArrayList("list", musicQueue)
//                    result.sendResult(bundle)
//                    CoroutineScope(Dispatchers.IO).launch {
//                        var currentList = db.CurrentListDao().findCurrentList()
//                        if (currentList == null) {
//                            currentList = CurrentList(null, playList.id, playList.type.name)
//                            db.CurrentListDao().insert(currentList)
//                        } else {
//                            currentList.listID = playList.id
//                            currentList.type = playList.type.name
//                            db.CurrentListDao().update(currentList)
//                        }
//                        db.QueueDao().deleteAllQueue()
//                        db.QueueDao().insertAll(dbArrayList)
//                        SharedPreferencesUtils.saveSelectMusicId(
//                            this@PlayService,
//                            musicItems[index].id
//                        )
//                        SharedPreferencesUtils.enableShuffle(this@PlayService, true)
//                    }
//                    CoroutineScope(Dispatchers.Main).launch {
//                        exoPlayer.clearMediaItems()
//                        exoPlayer.setMediaItems(t1)
//                        exoPlayer.seekToDefaultPosition(index)
//                        exoPlayer.seekTo(0)
//                        exoPlayer.playWhenReady = true
//                        exoPlayer.prepare()
//                    }
//                }
//
//            }
//        } else {
//            result.sendResult(null)
//        }
//    }

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
                COMMAND_SLEEP_STATE_UPDATE,
                bundle
            )
        }
    }

    private val serviceJob = SupervisorJob()

    // 使用主线程作为默认调度器，因为很多操作最终需要和 Player 交互
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)


    private var config: PlayConfig? = null
    private var musicVisualizationEnable = false

    private fun initializePlayerData() {
        // 启动一个非阻塞的协程来执行所有初始化工作
        serviceScope.launch {
            Log.d("PlayService", "Starting player data initialization...")
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


    private fun setData(bundle: Bundle, position: Long?) {
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
        bundle.putLong("position", position ?: exoPlayer.currentPosition)
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
//           cancelNotification()
            mediaSession?.release()
            exoPlayer.stop()
            exoPlayer.release()
            serviceJob.cancel() // 在 Service 销毁时取消所有协程
        } catch (_: Exception) {
        }
        if (receiver != null) {
            unregisterReceiver(receiver)
        }
        super.onDestroy()
    }


    private fun playMusicSwitchQueue(
        playList: AnyListBase,
        index: Int,
        musicItems: ArrayList<MusicItem>,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            playListCurrent = playList
            musicQueue.clear()
            musicQueue.addAll(musicItems)
            if (musicQueue.isNotEmpty()) {
                val t1 = ArrayList<MediaItem>()
                musicQueue.forEachIndexed { index, it ->
                    it.tableId = index + 1L
                    t1.add(MediaItemUtils.musicItemToMediaItem(it))
                }
                var needPlay = true
                val currentPosition = if (currentPlayTrack?.id == musicQueue[index].id) {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        needPlay = false
                    }
                    exoPlayer.currentPosition
                } else {
                    0
                }
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItems(t1)
                CoroutineScope(Dispatchers.IO).launch {
                    var currentList = db.CurrentListDao().findCurrentList()
                    if (currentList == null) {
                        currentList = CurrentList(null, playList.id, playList.type.name)
                        db.CurrentListDao().insert(currentList)
                    } else {
                        currentList.listID = playList.id
                        currentList.type = playList.type.name
                        db.CurrentListDao().update(currentList)
                    }
                    db.QueueDao().deleteAllQueue()
                    db.QueueDao().insertAll(musicQueue)
                    SharedPreferencesUtils.saveSelectMusicId(
                        this@PlayService,
                        musicQueue[index].id
                    )
                }
                exoPlayer.seekToDefaultPosition(index)
                exoPlayer.seekTo(currentPosition)
                exoPlayer.playWhenReady = needPlay
                exoPlayer.prepare()
//                if(needPlay){
//                    mediaController?.transportControls?.play()
//                }else{
//                    mediaController?.transportControls?.pause()
//                }
            }
        }
    }

    private fun initExo(context: Context) {
        val renderersFactory: DefaultRenderersFactory =
            object : DefaultRenderersFactory(context) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean,
                ): AudioSink {
//                    sonicAudioProcessor.setPitch(0.9f)
                    val au = DefaultAudioSink.Builder(context)
                        .setEnableFloatOutput(enableFloatOutput)
                        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                        .setAudioProcessors(
                            arrayOf(
//                                sonicAudioProcessor,
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
//        exoPlayer.shuffleModeEnabled=true;
//        exoPlayer.setShuffleOrder()
        exoPlayer.playWhenReady = true
        exoPlayer.repeatMode = config?.repeatModel ?: Player.REPEAT_MODE_ALL
        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
        playerAddListener()
    }


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
                    val qList: Collection<MusicItem> =
                        (newQueue.map { MediaItemUtils.mediaItemToMusicItem(it) }).filterNotNull()
                    val qIndex = exoPlayer.currentMediaItemIndex
                    musicQueue.clear()
                    musicQueue.addAll(qList)
                    if (exoPlayer.shuffleModeEnabled && SharedPreferencesUtils.getAutoToTopRandom(
                            this@PlayService
                        )
                    ) {
                        if (qIndex == 0) return
                        val music = musicQueue[qIndex]
                        musicQueue.removeAt(qIndex)
                        musicQueue.add(0, music)
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        db.QueueDao().deleteAllQueue()
                        db.QueueDao().insertAll(musicQueue)
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
                            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                            intent.putExtra("source", this@PlayService.packageName)
                            val ids = AppWidgetManager.getInstance(
                                application
                            ).getAppWidgetIds(
                                ComponentName(
                                    application,
                                    PlayMusicWidget::class.java
                                )
                            )
//                            intent.putExtra("playStatusChange", true)
                            intent.putExtra("playingStatus", isPlaying)
//                            intent.putExtra("playingStatus", exoPlayer.isPlaying)
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
                updateNotify()
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
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
                    SharedPreferencesUtils.saveCurrentDuration(this@PlayService, 0)
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
                                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
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
                // wait currentPlayTrack changed
//                updateNotify()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)
                val bitArray = exoPlayer.mediaMetadata.artworkData
//                val metadataBuilder = MediaMetadataCompat.Builder()
//                metadataBuilder.putLong(
//                    MediaMetadataCompat.METADATA_KEY_DURATION,
//                    exoPlayer.duration
//                )
//                metadataBuilder.putText(
//                    MediaMetadataCompat.METADATA_KEY_TITLE,
//                    currentPlayTrack?.name
//                )
//                metadataBuilder.putText(
//                    MediaMetadataCompat.METADATA_KEY_ARTIST,
//                    currentPlayTrack?.artist
//                )
//                if (bitArray != null) {
//                    val bit = BitmapFactory.decodeByteArray(bitArray, 0, bitArray.size)
//                    metadataBuilder.putBitmap(
//                        MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON,
//                        bit
//                    )
//                }
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

    fun updateNotify(position: Long? = null, duration: Long? = null) {
//        notify?.updateNotification(
//            this@PlayService,
//            currentPlayTrack?.name ?: "",
//            currentPlayTrack?.artist ?: "",
//            exoPlayer.isPlaying,
//            exoPlayer.playbackParameters.speed,
//            position ?: exoPlayer.currentPosition,
//            duration ?: exoPlayer.duration
//        )
//        val metadataBuilder = MediaMetadataCompat.Builder()
//        metadataBuilder.putLong(
//            MediaMetadataCompat.METADATA_KEY_DURATION,
//            exoPlayer.duration
//        )
//        metadataBuilder.putText(
//            MediaMetadataCompat.METADATA_KEY_TITLE,
//            currentPlayTrack?.name
//        )
//        metadataBuilder.putText(
//            MediaMetadataCompat.METADATA_KEY_ARTIST,
//            currentPlayTrack?.artist
//        )
//        val bitArray = exoPlayer.mediaMetadata.artworkData
//        if (bitArray != null) {
//            val bit = BitmapFactory.decodeByteArray(bitArray, 0, bitArray.size)
//            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bit)
//        }
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

    private fun refreshAction() {
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
        clearCacheData()
        CoroutineScope(Dispatchers.IO).launch {
            val sortData =
                db.SortFiledDao().findSortByType(PlayListType.Songs.name)
            TracksManager.getFolderList(
                this@PlayService,
                foldersLinkedHashMap,
                tracksLinkedHashMap,
                "${sortData?.filed ?: ""} ${sortData?.method ?: ""}",
                true
            )
            val sortData1 = db.SortFiledDao().findSortAll()
            showIndicatorList.clear()
            sortData1.forEach {
                if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                    showIndicatorList.add(it)
                }
            }
        }
    }

    //
    private fun sortAction(extras: Bundle) {
        val typeString = extras.getString("type")
        if (!typeString.isNullOrEmpty()) {
            when (typeString) {
                PlayListType.PlayLists.name -> {
                    playListLinkedHashMap.clear()
                }

                PlayListType.Albums.name -> {
                    albumsLinkedHashMap.clear()
                }

                PlayListType.Artists.name -> {
                    artistsLinkedHashMap.clear()
                }

                PlayListType.Genres.name -> {
                    genresLinkedHashMap.clear()
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
                            "${sortData?.filed ?: ""} ${sortData?.method ?: ""}",
                            true
                        )
                        val sortData1 = db.SortFiledDao().findSortAll()
                        showIndicatorList.clear()
                        sortData1.forEach {
                            if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                                showIndicatorList.add(it)
                            }
                        }
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
                    }
                }

            }
        }
    }


    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    companion object {
        // DSP & Effects
        val COMMAND_CHANGE_PITCH = SessionCommand("dsp.CHANGE_PITCH", Bundle.EMPTY)
        val COMMAND_CHANGE_Q = SessionCommand("dsp.CHANGE_Q", Bundle.EMPTY)
        val COMMAND_DSP_ENABLE = SessionCommand("dsp.ENABLE", Bundle.EMPTY)

        // DSP
        val COMMAND_DSP_SET_BAND = SessionCommand("dsp.SET_BAND", Bundle.EMPTY)
        val COMMAND_DSP_FLATTEN = SessionCommand("dsp.FLATTEN", Bundle.EMPTY)
        val COMMAND_DSP_SET_BANDS = SessionCommand("dsp.SET_BANDS", Bundle.EMPTY)

        // Echo
        val COMMAND_ECHO_ENABLE = SessionCommand("echo.ENABLE", Bundle.EMPTY)
        val COMMAND_ECHO_SET_DELAY = SessionCommand("echo.SET_DELAY", Bundle.EMPTY)
        val COMMAND_ECHO_SET_DECAY = SessionCommand("echo.SET_DECAY", Bundle.EMPTY)
        val COMMAND_ECHO_SET_FEEDBACK = SessionCommand("echo.SET_FEEDBACK", Bundle.EMPTY)

        val COMMAND_SEARCH = SessionCommand("app.SEARCH", Bundle.EMPTY)

        // Visualization
        val COMMAND_VISUALIZATION_ENABLE = SessionCommand("vis.ENABLE", Bundle.EMPTY)
        val COMMAND_VISUALIZATION_DATA = SessionCommand("vis.VISUALIZATION_DATA", Bundle.EMPTY)

        // Sleep Timer
        val COMMAND_SET_SLEEP_TIMER = SessionCommand("timer.SET_SLEEP", Bundle.EMPTY)
        val COMMAND_SLEEP_STATE_UPDATE = SessionCommand("timer.SLEEP_STATE", Bundle.EMPTY)

        val COMMAND_VISUALIZATION_CONNECTED = SessionCommand("vis.CONNECTED", Bundle.EMPTY)
        val COMMAND_GET_INITIALIZED_DATA = SessionCommand("vis.GET_INITIALIZED_DATA", Bundle.EMPTY)

        // 命令：通知 Service 可视化组件将要断开/不可见
        val COMMAND_VISUALIZATION_DISCONNECTED =
            SessionCommand("vis.DISCONNECTED", Bundle.EMPTY)

        val COMMAND_APP_EXIT = SessionCommand("app.EXIT", Bundle.EMPTY)
        val COMMAND_TRACKS_UPDATE = SessionCommand("tracks.UPDATE", Bundle.EMPTY)
        val COMMAND_TRACK_DELETE = SessionCommand("app.TRACK_DELETE", Bundle.EMPTY)
        val COMMAND_PlAY_LIST_CHANGE = SessionCommand("app.PlAY_LIST_CHANGE", Bundle.EMPTY)
        val COMMAND_GET_PLAY_LIST_ITEM = SessionCommand("app.GET_PLAY_LIST_ITEM", Bundle.EMPTY)

        val COMMAND_SORT_QUEUE = SessionCommand("queue.SORT_QUEUE", Bundle.EMPTY)
        val COMMAND_SORT_TRACKS = SessionCommand("queue.SORT_TRACKS", Bundle.EMPTY)

        val COMMAND_CHANGE_PLAYLIST = SessionCommand("queue.CHANGE_PLAYLIST", Bundle.EMPTY)


        // Key：用于指定排序方式的参数
        const val KEY_SORT_BY = "key_sort_by"
        const val VALUE_SORT_BY_TITLE_ASC = "sort_title_asc"
        const val VALUE_SORT_BY_ARTIST_ASC = "sort_artist_asc"
        const val VALUE_SORT_BY_CUSTOM = "sort_custom" // 例如，用户手动拖拽后的顺序

        // --- 定义所有 Bundle Key ---
        const val KEY_INDEX = "index"
        const val KEY_VALUE = "value"
        const val KEY_INT_ARRAY_VALUE = "int_array_value"
        const val KEY_TRACK_ID = "long_id_value"
        const val KEY_ENABLE = "enable"
        const val KEY_DELAY = "delay"
        const val KEY_DECAY = "decay"
        const val KEY_SEARCH_QUERY = "search_query"
        const val KEY_SEARCH_RESULT_JSON = "search_result_json"

        // Queue Management
        val COMMAND_ADD_TO_QUEUE = SessionCommand("queue.ADD", Bundle.EMPTY)
        val COMMAND_REMOVE_FROM_QUEUE = SessionCommand("queue.REMOVE", Bundle.EMPTY)
        val COMMAND_CLEAR_QUEUE = SessionCommand("queue.CLEAR", Bundle.EMPTY)
        // ... 其他队列操作 ...

        // Playback
        // 注意：播放特定歌曲不再是自定义命令，而是通过 player.setMediaItems() 和 player.seekTo() 实现

        // Other
        val COMMAND_REFRESH_ALL = SessionCommand("app.REFRESH_ALL", Bundle.EMPTY)

    }

    fun getPlayList(
        context: Context,
        future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>, params: LibraryParams?
    ) {
        if (playListLinkedHashMap.isNotEmpty()) {
            val mediaItems = ArrayList(playListLinkedHashMap.values).map { playlist ->
                MediaItemUtils.playlistToMediaItem(playlist)
            }
            Log.d("", "set f getPlayList: $mediaItems")
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
            Log.d("", "set f getPlayList: $mediaItems")
            future.set(LibraryResult.ofItemList(mediaItems, null))
        }
    }

    fun getPlayListTracks(
        context: Context,
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
        future.set(LibraryResult.ofItemList(emptyList(), null))
    }

    fun getGenreListTracks(
        context: Context,
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
        context: Context,
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
        context: Context,
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
        context: Context,
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
        context: Context,
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
                "${sortData?.filed ?: ""} ${sortData?.method ?: ""}",
                true
            )
            val mediaItems = tracksLinkedHashMap.values.map { playlist ->
                MediaItemUtils.musicItemToMediaItem(playlist)
            }
            future.set(LibraryResult.ofItemList(mediaItems, null))
        }
    }

    fun getGenres(
        context: Context,
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
        context: Context,
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
        context: Context,
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
        context: Context,
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
                "${sortData?.filed ?: ""} ${sortData?.method ?: ""}",
                true
            )
            val mediaItems = foldersLinkedHashMap.values.map { playlist ->
                MediaItemUtils.folderToMediaItem(playlist)
            }
            future.set(LibraryResult.ofItemList(mediaItems, null))
        }
    }

    private fun addPlayQueue(extras: Bundle) {
        var musicItem = extras.getParcelable<MusicItem>("musicItem")
        var musicItems = extras.getParcelableArrayList<MusicItem>("musicItems")
        val index = extras.getInt("index", musicQueue.size)
        val gson = Gson()
        val musicItemListType = object : TypeToken<ArrayList<MusicItem?>?>() {}.type
        musicItems = if (musicItems != null) {
            gson.fromJson(gson.toJson(musicItems), musicItemListType)
        } else null
        if (musicItem != null) {
            musicItem = gson.fromJson(gson.toJson(musicItem), MusicItem::class.java)
        }
        if (musicItems == null && musicItem != null) {
            musicItems = ArrayList<MusicItem>()
            musicItems.add(musicItem)
        }
        val enableShuffle = SharedPreferencesUtils.getEnableShuffle(this@PlayService)
        // check this queue had shuffle
        val histShuffle = if (musicQueue.isEmpty()) {
            false
        } else {
            musicQueue[0].priority > 0
        }
        if (musicItems != null) {
            val list = ArrayList<MediaItem>()
            musicItems.forEachIndexed { index1, it ->
                it.tableId = index1.toLong() + index + 1L
                if (histShuffle) {
                    it.priority = index1 + index + 1
                }
                list.add(MediaItemUtils.musicItemToMediaItem(it))
            }
//                musicQueue.forEachIndexed() { index1, it ->
//                    if (it.tableId!! >= index.toLong()) {
//                        it.tableId =  it.tableId!! + 1L + index
//                    }
//                    if(it.priority >= index){
//                        it.priority = it.priority  + index + 1
//                    }
//                }

            playListCurrent = null
            CoroutineScope(Dispatchers.IO).launch {
                db.CurrentListDao().delete()
            }
            if (index == musicQueue.size) {
                musicQueue.addAll(musicItems)
                CoroutineScope(Dispatchers.IO).launch {
                    db.QueueDao().insertAll(musicItems)
                }
            } else {
                if (enableShuffle) {
                    val pList = ArrayList<MusicItem>(musicQueue)
                    pList.addAll(index, musicItems)
                    for (i in index until pList.size) {
                        pList[i].priority = i + 1
                    }
                    val tList = ArrayList<MusicItem>(musicQueue)
                    tList.sortBy { it.tableId }
                    tList.addAll(index, musicItems)
                    for (i in index until pList.size) {
                        tList[i].tableId = i + 1L
                    }
                    musicQueue.clear()
                    musicQueue.addAll(pList)
                    CoroutineScope(Dispatchers.IO).launch {
                        db.QueueDao().deleteAllQueue()
                        db.QueueDao().insertAll(tList)
                    }
                } else {
                    musicQueue.addAll(index, musicItems)
                    for (i in index until musicQueue.size) {
                        musicQueue[i].tableId = i + 1L
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        db.QueueDao().deleteAllQueue()
                        db.QueueDao().insertAll(musicQueue)
                    }
                }
            }
            if (!exoPlayer.isPlaying) {
                exoPlayer.playWhenReady = false
            }
            exoPlayer.addMediaItems(index, list)
        }
    }

    // 这个函数在 PlayService 内部定义，在主线程上调用
    private fun applyLoadedDataToPlayer() {
        volumeValue = SharedPreferencesUtils.getVolume(this@PlayService)
        exoPlayer.volume = volumeValue / 100f
        exoPlayer.repeatMode = config?.repeatModel ?: Player.REPEAT_MODE_ALL
        val p = PlaybackParameters(
            auxr.speed,
            auxr.pitch
        )
        var position = 0L
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
            exoPlayer.setMediaItems(t1)
            if (currentPlayTrack != null) {
                position = SharedPreferencesUtils.getCurrentPosition(this@PlayService)
                if (position >= (currentPlayTrack?.duration ?: 0)) {
                    position = 0
                }
                exoPlayer.seekTo(currentIndex, position)
                updateNotify(position, currentPlayTrack?.duration)
            }
            exoPlayer.playWhenReady = false
            exoPlayer.prepare()
        }
    }


    private suspend fun loadAllTracksAndFolders() {
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
            false,
            allTracksLinkedHashMap
        )
    }

    private suspend fun loadAuxSettings() {
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

    private suspend fun loadPlayConfig() {
        config = db.PlayConfigDao().findConfig()
        if (config == null) {
            config = PlayConfig(0, Player.REPEAT_MODE_ALL)
            db.PlayConfigDao().insert(config!!)
        }
    }

    private suspend fun loadMainTabs() {
        val list =
            db.MainTabDao().findAllIsShowMainTabSortByPriority()
        if (list.isEmpty()) {
            PlayUtils.addDefaultMainTab(mainTab)
            db.MainTabDao().insertAll(mainTab)
        } else {
            mainTab.addAll(list)
        }
    }

    private suspend fun loadShowIndicatorList() {
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

    private suspend fun loadVisualizationSettings() {
        // TODO move to   val auxTemp = db.AuxDao().findFirstAux()
        musicVisualizationEnable =
            SharedPreferencesUtils.getEnableMusicVisualization(this@PlayService)
//                                visualizationAudioProcessor.isActive = musicVisualizationEnable
        equalizerAudioProcessor.setVisualizationAudioActive(
            musicVisualizationEnable
        )
    }

    private suspend fun loadLastQueue() {
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
        Log.d("PlayService", "onTaskRemoved")
    }
}
