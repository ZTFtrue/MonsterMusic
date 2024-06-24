package com.ztftrue.music.play

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.BuildConfig
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.ztftrue.music.MainActivity
import com.ztftrue.music.R
import com.ztftrue.music.effects.EchoAudioProcessor
import com.ztftrue.music.effects.EqualizerAudioProcessor
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.model.Auxr
import com.ztftrue.music.sqlData.model.CurrentList
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.sqlData.model.PlayConfig
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.model.AlbumList
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.model.ArtistList
import com.ztftrue.music.utils.model.FolderList
import com.ztftrue.music.utils.model.GenresList
import com.ztftrue.music.utils.model.MusicPlayList
import com.ztftrue.music.utils.stringToEnumForPlayListType
import com.ztftrue.music.utils.trackManager.AlbumManager
import com.ztftrue.music.utils.trackManager.ArtistManager
import com.ztftrue.music.utils.trackManager.GenreManager
import com.ztftrue.music.utils.trackManager.PlaylistManager
import com.ztftrue.music.utils.trackManager.TracksManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.locks.ReentrantLock

/**
 * playList
 * albumsList
 * artistsList
 * genresList
 * songsList
 * foldersList
 */
const val ACTION_GET_TRACKS = "ACTION_GET_TRACKS"
const val ACTION_PLAY_MUSIC = "PLAY_MUSIC"
const val ACTION_SEEK_TO = "SeekTo"
const val ACTION_CHANGE_PITCH = "ChangePitch"
const val ACTION_SEARCH = "ACTION_SEARCH"

//const val ACTION_CHANGE_SPEED = "ACTION_CHANGE_SPEED"  use MediaSessionCompat instead
const val ACTION_DSP_ENABLE = "DSP_ENABLE"
const val ACTION_DSP_BAND = "ACTION_DSP_BAND"
const val ACTION_DSP_BAND_FLATTEN = "ACTION_DSP_BAND_FLATTEN"
const val ACTION_DSP_BANDS_SET = "ACTION_DSP_BANDS_SET"
const val ACTION_ECHO_ENABLE = "ACTION_ECHO_ENABLE"
const val ACTION_ECHO_DELAY = "ACTION_ECHO_DELAY"
const val ACTION_ECHO_DECAY = "ACTION_ECHO_DECAY"
const val ACTION_ECHO_FEEDBACK = "ACTION_ECHO_FEEDBACK"
const val ACTION_SET_SLEEP_TIME = "set_sleep_time"
const val ACTION_AddPlayQueue = "AddPlayQueue"
const val ACTION_RemoveFromQueue = "ACTION_RemoveFromQueue"
const val ACTION_PlayLIST_CHANGE = "ACTION_PlayLIST_CHANGE"
const val ACTION_TRACKS_DELETE = "ACTION_TRACKS_DELETE"
const val ACTION_TRACKS_UPDATE = "ACTION_TRACKS_UPDATE"
const val ACTION_GET_TRACK_BY_ID = "ACTION_GET_TRACK_BY_ID"
const val ACTION_CLEAR_QUEUE = "ACTION_CLEAR_QUEUE"

const val EVENT_MEDIA_ITEM_Change = 3
const val EVENT_SLEEP_TIME_Change = 5
const val EVENT_DATA_READY = 6
const val ACTION_GET_ALBUM_BY_ID = "GET_ARTIST_FROM_ALBUM"
const val MY_MEDIA_ROOT_ID = "MY_MEDIA_ROOT_ID"

//@Suppress("deprecation")
@UnstableApi
class PlayService : MediaBrowserServiceCompat() {

    private var mediaSession: MediaSessionCompat? = null


    val equalizerAudioProcessor: EqualizerAudioProcessor = EqualizerAudioProcessor()
    val echoAudioProcessor: EchoAudioProcessor = EchoAudioProcessor()
    val sonicAudioProcessor = SonicAudioProcessor()
    lateinit var exoPlayer: ExoPlayer


    private var playListCurrent: AnyListBase? = null
    var musicQueue = java.util.ArrayList<MusicItem>()
    var currentPlayTrack: MusicItem? = null


    private val playListTracksHashMap = java.util.HashMap<Long, java.util.ArrayList<MusicItem>>()

    // album tracks
    private val albumsListTracksHashMap = java.util.HashMap<Long, java.util.ArrayList<MusicItem>>()
    private val artistsListTracksHashMap = java.util.HashMap<Long, java.util.ArrayList<MusicItem>>()
    private val genresListTracksHashMap = java.util.HashMap<Long, java.util.ArrayList<MusicItem>>()
    private val foldersListTracksHashMap =
        java.util.HashMap<Long, java.util.LinkedHashMap<Long, MusicItem>>()

    private val tracksLinkedHashMap: LinkedHashMap<Long, MusicItem> = LinkedHashMap()

    private val albumsLinkedHashMap: LinkedHashMap<Long, AlbumList> = LinkedHashMap()
    private val playListLinkedHashMap: LinkedHashMap<Long, MusicPlayList> = LinkedHashMap()
    private val artistsLinkedHashMap: LinkedHashMap<Long, ArtistList> = LinkedHashMap()
    private val genresLinkedHashMap: LinkedHashMap<Long, GenresList> = LinkedHashMap()
    private val foldersLinkedHashMap: LinkedHashMap<Long, FolderList> = LinkedHashMap()

    private val artistHasAlbumMap: HashMap<Long, ArrayList<AlbumList>> = HashMap()
    private val genreHasAlbumMap: HashMap<Long, ArrayList<AlbumList>> = HashMap()

    var sleepTime = 0L

    private var mediaController: MediaControllerCompat? = null
    private val mainTab = ArrayList<MainTab>(7)
    private lateinit var db: MusicDatabase
    private var bandsValue =
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    private var auxr = Auxr(
        0, 1f, 1f, false, 0.2f, 0.5f,
        echoRevert = true,
        equalizer = false,
        equalizerBand = bandsValue
    )

    override fun onCreate() {
        super.onCreate()
        initExo(this@PlayService)
        val contentIntent = Intent(this, MainActivity::class.java)
        val pendingContentIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaSessionCompat(baseContext, PlayService::class.java.simpleName).apply {
            val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_SEEK_TO
                            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                            or PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT

                )
            setPlaybackState(stateBuilder.build())
            setSessionToken(sessionToken)
            setSessionActivity(pendingContentIntent)
            setRepeatMode(
                when (exoPlayer.repeatMode) {
                    Player.REPEAT_MODE_ALL -> {
                        PlaybackStateCompat.REPEAT_MODE_ALL
                    }

                    Player.REPEAT_MODE_ONE -> {
                        PlaybackStateCompat.REPEAT_MODE_ONE
                    }

                    else -> {
                        PlaybackStateCompat.REPEAT_MODE_NONE
                    }
                }
            )

            mediaController = controller

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    pauseOrPlayMusic()
                }

                override fun onPause() {
                    pauseOrPlayMusic()
                }

                override fun onStop() {
//                    exoPlayer.stop()
                    notify?.stop(this@PlayService)
                } // Implement other media control callbacks as needed

                override fun onSkipToNext() {
                    super.onSkipToNext()
                    playNext()
                }

                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    playPreview()
                }

                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    exoPlayer.seekTo(pos)
                }

                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }


                override fun onSetPlaybackSpeed(speed: Float) {
                    super.onSetPlaybackSpeed(speed)
                    val param1 = PlaybackParameters(
                        speed, auxr.pitch
                    )
                    auxr.speed = speed
                    CoroutineScope(Dispatchers.IO).launch {
                        db.AuxDao().update(auxr)
                    }
                    exoPlayer.playbackParameters = param1
                    notify?.updateNotification(
                        this@PlayService,
                        currentPlayTrack?.name ?: "",
                        currentPlayTrack?.artist ?: "",
                        exoPlayer.isPlaying,
                        exoPlayer.playbackParameters.speed,
                        exoPlayer.currentPosition,
                        exoPlayer.duration
                    )
                }


                override fun onSetRepeatMode(repeatMode: Int) {
                    super.onSetRepeatMode(repeatMode)

                    when (repeatMode) {
                        PlaybackStateCompat.REPEAT_MODE_NONE -> {
                            switchRepeatModel(Player.REPEAT_MODE_OFF)
                        }

                        PlaybackStateCompat.REPEAT_MODE_ALL -> {
                            switchRepeatModel(Player.REPEAT_MODE_ALL)
                        }

                        PlaybackStateCompat.REPEAT_MODE_ONE -> {
                            switchRepeatModel(Player.REPEAT_MODE_ONE)
                        }

                        PlaybackStateCompat.REPEAT_MODE_GROUP -> {
                        }

                        PlaybackStateCompat.REPEAT_MODE_INVALID -> {
                        }
                    }
                }
            })
        }
    }


    override fun onCustomAction(action: String, extras: Bundle?, result: Result<Bundle>) {
        if (PlayListType.PlayLists.name == action) {
            if (playListLinkedHashMap.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putParcelableArrayList("list", ArrayList(playListLinkedHashMap.values))
                result.sendResult(bundle)
                return
            } else {
                result.detach()
                PlaylistManager.getPlaylists(
                    this@PlayService,
                    playListLinkedHashMap,
                    playListTracksHashMap,
                    tracksLinkedHashMap,
                    result
                )
            }
        } else if (PlayListType.Songs.name == action) {
            if (tracksLinkedHashMap.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putParcelableArrayList("list", ArrayList(tracksLinkedHashMap.values))
                result.sendResult(bundle)
                return
            } else {
                result.detach()
                TracksManager.getFolderList(
                    this@PlayService,
                    foldersLinkedHashMap,
                    result,
                    tracksLinkedHashMap,
                    foldersListTracksHashMap,
                    true
                )
            }
        } else if (PlayListType.Genres.name == action) {
            if (genresLinkedHashMap.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putParcelableArrayList("list", ArrayList(genresLinkedHashMap.values))
                result.sendResult(bundle)
                return
            } else {
                result.detach()
                GenreManager.getGenresList(
                    this@PlayService,
                    genresLinkedHashMap,
                    genresListTracksHashMap,
                    tracksLinkedHashMap,
                    result
                )
            }
        } else if (PlayListType.Albums.name == action) {
            if (albumsLinkedHashMap.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putParcelableArrayList("list", ArrayList(albumsLinkedHashMap.values))
                result.sendResult(bundle)
                return
            } else {
                result.detach()
                AlbumManager.getAlbumList(
                    this@PlayService,
                    albumsLinkedHashMap,
                    result
                )
            }
        } else if (PlayListType.Artists.name == action) {
            if (artistsLinkedHashMap.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putParcelableArrayList("list", ArrayList(artistsLinkedHashMap.values))
                result.sendResult(bundle)
                return
            } else {
                result.detach()
                ArtistManager.getArtistList(
                    this@PlayService,
                    artistsLinkedHashMap,
                    result
                )
            }
        } else if (PlayListType.Folders.name == action) {
            if (foldersLinkedHashMap.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putParcelableArrayList("list", ArrayList(foldersLinkedHashMap.values))
                result.sendResult(bundle)
                return
            } else {
                result.detach()
                TracksManager.getFolderList(
                    this@PlayService,
                    foldersLinkedHashMap,
                    result,
                    tracksLinkedHashMap,
                    foldersListTracksHashMap
                )
            }
        } else if (ACTION_PLAY_MUSIC == action) {
            if (extras != null) {
                if (notify == null) {
                    notify = CreateNotification(this@PlayService, mediaSession)
                }
                val musicItem = extras.getParcelable<MusicItem>("musicItem")
                val switchQueue = extras.getBoolean("switch_queue", false)
                val index = extras.getInt("index")
                if (musicItem != null) {
                    if (switchQueue) {
                        val playList = extras.getParcelable<AnyListBase>("playList")
                        val musicItems = extras.getParcelableArrayList<MusicItem>("musicItems")
                        if (playList != null && musicItems != null) {
                            playMusicSwitchQueue(playList, index, musicItems)
                        }
                    } else {
                        playMusicCurrentQueue(musicItem, index)
                    }
                }
            }
            result.sendResult(null)
        } else if (ACTION_SEEK_TO == action) {
            if (extras != null) {
                exoPlayer.seekTo(extras.getLong("position"))
            }
            result.sendResult(null)
        } else if (ACTION_CHANGE_PITCH == action) {
            if (extras != null) {
                val pitch = extras.getFloat("pitch", 1f)
                val param1 = PlaybackParameters(
                    auxr.speed, pitch
                )
                exoPlayer.playbackParameters = param1
                auxr.pitch = pitch
                CoroutineScope(Dispatchers.IO).launch {
                    db.AuxDao().update(auxr)
                }
            }
            result.sendResult(null)
        } else if (ACTION_DSP_ENABLE == action) {
            if (extras != null) {
                equalizerAudioProcessor.isActive = extras.getBoolean("enable")
                auxr.equalizer = equalizerAudioProcessor.isSetActive()
                CoroutineScope(Dispatchers.IO).launch {
                    db.AuxDao().update(auxr)
                }
            }
            result.sendResult(null)
        } else if (ACTION_DSP_BAND == action) {
            if (extras != null) {
                val i = extras.getInt("index")
                val v = extras.getInt("value")
                equalizerAudioProcessor.setBand(i, v)
                auxr.equalizerBand[i] = v
                CoroutineScope(Dispatchers.IO).launch {
                    db.AuxDao().update(auxr)
                }
            }
            result.sendResult(null)
        } else if (ACTION_DSP_BAND_FLATTEN == action) {
            if (extras != null) {
                if (equalizerAudioProcessor.flatBand()) {
                    result.sendResult(Bundle().apply {
                        putBoolean("result", true)
                    })
                    repeat(auxr.equalizerBand.size) {
                        auxr.equalizerBand[it] = 0
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        db.AuxDao().update(auxr)
                    }
                    return
                }
            }
            result.sendResult(null)
        } else if (ACTION_DSP_BANDS_SET == action) {
            if (extras != null) {
                val value = extras.getIntArray("value")
                value?.forEachIndexed { index, v ->
                    equalizerAudioProcessor.setBand(index, v)
                }
            }
            result.sendResult(null)
        } else if (ACTION_ECHO_ENABLE == action) {
            if (extras != null) {
                val echoActive = extras.getBoolean("enable")
                echoAudioProcessor.isActive = echoActive
                auxr.echo = echoActive
                CoroutineScope(Dispatchers.IO).launch {
                    db.AuxDao().update(auxr)
                }
            }
            result.sendResult(null)
        } else if (ACTION_ECHO_DELAY == action) {
            if (extras != null) {
                val delayTime = extras.getFloat("delay")
                echoAudioProcessor.setDaleyTime(delayTime)
                auxr.echoDelay = delayTime
                CoroutineScope(Dispatchers.IO).launch {
                    db.AuxDao().update(auxr)
                }
            }
            result.sendResult(null)
        } else if (ACTION_ECHO_DECAY == action) {
            if (extras != null) {
                val decay = extras.getFloat("decay")
                echoAudioProcessor.setDecay(decay)
                auxr.echoDecay = decay
                CoroutineScope(Dispatchers.IO).launch {
                    db.AuxDao().update(auxr)
                }
            }
            result.sendResult(null)
        } else if (ACTION_ECHO_FEEDBACK == action) {
            if (extras != null) {
                val echoFeedBack = extras.getBoolean("enable")
                echoAudioProcessor.setFeedBack(echoFeedBack)
                auxr.echoRevert = echoFeedBack
                CoroutineScope(Dispatchers.IO).launch {
                    db.AuxDao().update(auxr)
                }
            }
            result.sendResult(null)
        } else if (ACTION_SET_SLEEP_TIME == action) {
            if (extras != null) {
                timeSet(extras, result)
            }
        } else if (ACTION_GET_TRACKS == action) {
            getTracksByType(extras, result)
        } else if (ACTION_AddPlayQueue == action) {
            addPlayQueue(extras, result)
        } else if (ACTION_RemoveFromQueue == action) {
            removePlayQueue(extras, result)
        } else if (ACTION_PlayLIST_CHANGE == action) {
            playListLinkedHashMap.clear()
            playListTracksHashMap.clear()
            PlaylistManager.getPlaylists(
                this@PlayService,
                playListLinkedHashMap,
                playListTracksHashMap,
                tracksLinkedHashMap,
                null
            )
            result.sendResult(null)
        } else if (ACTION_SEARCH == action) {
            search(extras, result)
        } else if (ACTION_GET_ALBUM_BY_ID == action) {
            if (extras != null) {
                val albumId = extras.getLong("albumId")
                albumsLinkedHashMap[albumId]?.let {
                    result.sendResult(Bundle().apply {
                        putParcelable("album", it)
                    })
                }
                return
            }
            result.sendResult(null)
        } else if (ACTION_TRACKS_DELETE == action) {
            if (extras != null) {
                val id = extras.getLong("id")
                tracksLinkedHashMap.remove(id)
                clearCacheData()
                return
            }
            result.sendResult(null)
        } else if (ACTION_TRACKS_UPDATE == action) {
            if (extras != null) {
                val id = extras.getLong("id")
                val musicTrack = TracksManager.getMusicById(this@PlayService, id)
                if (musicTrack != null) {
                    tracksLinkedHashMap[id] = musicTrack
                } else {
                    tracksLinkedHashMap.remove(id)
                }
                clearCacheData()
                result.sendResult(null)
                return
            }
            result.sendResult(null)
        } else if ("com.ztftrue.music.ACTION_EXIT" == action) {
            //initially, i thought app need to notify system this service can stop.
            // then app can be stop. but i found this cant work, sometimes, it make a bug.
            // so i add this code, let user exit app.
            // of course, user can remove notify,make system stop service.
            exoPlayer.pause()
            notify?.cancelNotification()
            stopSelf()
            result.sendResult(null)
        } else if (ACTION_GET_TRACK_BY_ID == action) {
            //initially, i thought app need to notify system this service can stop.
            // then app can be stop. but i found this cant work, sometimes, it make a bug.
            // so i add this code, let user exit app.
            // of course, user can remove notify,make system stop service.
            val bundle = Bundle()
            bundle.putParcelable("track", tracksLinkedHashMap[extras?.getLong("id")])
            result.sendResult(bundle)
        } else if (ACTION_CLEAR_QUEUE == action) {
            playListCurrent = null
            currentPlayTrack = null
            exoPlayer.pause()
            musicQueue.clear()
            exoPlayer.setMediaItems(ArrayList())

            CoroutineScope(Dispatchers.IO).launch {
                db.QueueDao().deleteAllQueue()
                val c = db.CurrentListDao().findCurrentList()
                if (c != null) {
                    db.CurrentListDao().delete()
                }
                saveSelectMusicId(-1)
            }
            result.sendResult(null)
        }
    }

    var remainingTime = 0L
    var playCompleted = false
    var needPlayPause = false
    private var countDownTimer: CountDownTimer? = null
    private fun timeSet(extras: Bundle, result: Result<Bundle>) {
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
                    bundle.putInt("type", EVENT_SLEEP_TIME_Change)
                    bundle.putLong("remaining", remainingTime)
                    mediaSession?.setExtras(bundle)
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
            bundle.putInt("type", EVENT_SLEEP_TIME_Change)
            bundle.putLong("remaining", remainingTime)
            mediaSession?.setExtras(bundle)
        }
        result.sendResult(extras)
    }

    fun timeFinish() {
        remainingTime = 0
        sleepTime = 0
        exoPlayer.pause()
        val bundle = Bundle()
        bundle.putInt("type", EVENT_SLEEP_TIME_Change)
        bundle.putLong("remaining", remainingTime)
        bundle.putLong("sleepTime", 0L)
        mediaSession?.setExtras(bundle)
        notify?.cancelNotification()
    }


    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return if (clientPackageName == "com.ztftrue.music") {
            lock.lock()
            var bundle = getData(Bundle())
            if (bundle == null) {
                bundle = Bundle()
                initSqlData(bundle)
            }
            lock.unlock()
            BrowserRoot(MY_MEDIA_ROOT_ID, bundle)
        } else {
            null
        }
    }

    private var sqlDataInitialized = false
    private var config: PlayConfig? = null
    private val lock = ReentrantLock()
    private fun initSqlData(bundle: Bundle) {
        runBlocking {
            awaitAll(
                async(Dispatchers.IO) {
                    db = MusicDatabase.getDatabase(this@PlayService)
                    // Read musics , times 300ms
                    TracksManager.getFolderList(
                        this@PlayService,
                        foldersLinkedHashMap,
                        null,
                        tracksLinkedHashMap,
                        foldersListTracksHashMap
                    )
                    runBlocking {
                        awaitAll(
                            async {
                                val auxTemp = db.AuxDao().findFirstAux()
                                if (auxTemp == null) {
                                    db.AuxDao().insert(auxr)
                                } else {
                                    auxr = auxTemp
                                }
                                echoAudioProcessor.setDaleyTime(auxr.echoDelay)
                                echoAudioProcessor.setDecay(auxr.echoDecay)
                                echoAudioProcessor.setFeedBack(auxr.echoRevert)
                                echoAudioProcessor.isActive = auxr.echo
                                equalizerAudioProcessor.isActive = auxr.equalizer
                                val selectedPreset = this@PlayService.getSharedPreferences(
                                    "SelectedPreset",
                                    Context.MODE_PRIVATE
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
                            },
                            async {
                                val queue = db.QueueDao().findQueue()
                                musicQueue.clear()
                                if (!queue.isNullOrEmpty()) {
                                    val idCurrent = getCurrentPlayId()
                                    var id = -1L
                                    queue.forEach {
                                        // check has this tracks, avoid user remove it in storage
                                        if (tracksLinkedHashMap[it.id] != null) {
                                            musicQueue.add(it)
                                            if (it.id == idCurrent) {
                                                currentPlayTrack = tracksLinkedHashMap[it.id]
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
                                            saveSelectMusicId(-1)
                                            saveCurrentDuration(0)
                                        }
                                    }
                                }
                            },
                            async {
                                config = db.PlayConfigDao().findConfig()
                                if (config == null) {
                                    config = PlayConfig(0, Player.REPEAT_MODE_ALL)
                                    db.PlayConfigDao().insert(config!!)
                                }
                            },
                            async {
                                AlbumManager.getAlbumList(
                                    this@PlayService,
                                    albumsLinkedHashMap,
                                    null
                                )
                            },
                            async {
                                PlaylistManager.getPlaylists(
                                    this@PlayService,
                                    playListLinkedHashMap,
                                    playListTracksHashMap,
                                    tracksLinkedHashMap,
                                    null
                                )
                            },
                            async {
                                GenreManager.getGenresList(
                                    this@PlayService,
                                    genresLinkedHashMap,
                                    genresListTracksHashMap,
                                    tracksLinkedHashMap,
                                    null
                                )
                            },
                            async {
                                ArtistManager.getArtistList(
                                    this@PlayService,
                                    artistsLinkedHashMap,
                                    null
                                )
                            },
                            async {
                                val list =
                                    db.MainTabDao().findAllIsShowMainTabSortByPriority()
                                if (list.isNullOrEmpty()) {
                                    mainTab.add(
                                        MainTab(
                                            null,
                                            "Songs",
                                            PlayListType.Songs,
                                            1,
                                            true
                                        )
                                    )
                                    mainTab.add(
                                        MainTab(
                                            null,
                                            "PlayLists",
                                            PlayListType.PlayLists,
                                            2,
                                            true
                                        )
                                    )
                                    mainTab.add(
                                        MainTab(
                                            null,
                                            "Queue",
                                            PlayListType.Queue,
                                            3,
                                            true
                                        )
                                    )
                                    mainTab.add(
                                        MainTab(
                                            null,
                                            "Albums",
                                            PlayListType.Albums,
                                            4,
                                            true
                                        )
                                    )
                                    mainTab.add(
                                        MainTab(
                                            null,
                                            "Artists",
                                            PlayListType.Artists,
                                            5,
                                            true
                                        )
                                    )
                                    mainTab.add(
                                        MainTab(
                                            null,
                                            "Genres",
                                            PlayListType.Genres,
                                            6,
                                            true
                                        )
                                    )
                                    mainTab.add(
                                        MainTab(
                                            null,
                                            "Folders",
                                            PlayListType.Folders,
                                            7,
                                            true
                                        )
                                    )
                                    db.MainTabDao().insertAll(mainTab)
                                } else {
                                    mainTab.addAll(list)
                                }
                            }
                        )
                    }
                    sqlDataInitialized = true
                }
            )
        }

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
                t1.add(MediaItem.fromUri(File(it.path).toUri()))
                if (it.id == currentPlayTrack?.id) {
                    currentIndex = index
                }
            }
            exoPlayer.setMediaItems(t1)
            if (currentPlayTrack != null) {
                position = getCurrentPosition()
                exoPlayer.seekTo(currentIndex, position)
                if (notify == null) {
                    notify = CreateNotification(this@PlayService, mediaSession)
                }
                updateNotify(position, currentPlayTrack?.duration)
            }
        }
        setData(bundle, position.toFloat())
    }

    private fun getData(bundle: Bundle): Bundle? {
        return if (sqlDataInitialized) {
            setData(bundle, null)
            bundle
        } else {
            null
        }
    }

    private fun setData(bundle: Bundle, position: Float?) {
        bundle.putInt("type", EVENT_DATA_READY)
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
        bundle.putFloat("position", position ?: exoPlayer.currentPosition.toFloat())
        bundle.putParcelableArrayList("mainTabList", mainTab)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId != MY_MEDIA_ROOT_ID) {
            val mediaItems: MutableList<MediaBrowserCompat.MediaItem> = mutableListOf()
            result.sendResult(mediaItems)
        }
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>,
        options: Bundle
    ) {
        super.onLoadChildren(parentId, result, options)
    }

    private fun stopTimer() {
        remainingTime = 0
        countDownTimer?.cancel()
    }

    override fun onDestroy() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopTimer()
            saveCurrentDuration(exoPlayer.currentPosition)
            notify?.cancelNotification()
            mediaSession?.release()
            exoPlayer.stop()
            exoPlayer.release()
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    private var notify: CreateNotification? = null

    private fun playMusicCurrentQueue(musicItem: MusicItem, index: Int) {
        CoroutineScope(Job() + Dispatchers.Main).launch {
            if (currentPlayTrack?.id == musicItem.id) {
                val pbState = mediaController?.playbackState?.state
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    mediaController?.transportControls?.pause()
                } else {
                    mediaController?.transportControls?.play()
                }
            } else {
                saveSelectMusicId(musicQueue[index].id)
                saveCurrentDuration(0L)
                exoPlayer.seekToDefaultPosition(index)
                exoPlayer.playWhenReady = true
                exoPlayer.prepare()
            }
        }
    }

    private fun playMusicSwitchQueue(
        playList: AnyListBase, index: Int, musicItems: ArrayList<MusicItem>
    ) {
        CoroutineScope(Job() + Dispatchers.Main).launch {
            playListCurrent = playList
            musicQueue.clear()
            musicQueue.addAll(musicItems)
            if (musicQueue.isNotEmpty()) {
                val t1 = ArrayList<MediaItem>()
                musicQueue.forEach {
                    t1.add(MediaItem.fromUri(File(it.path).toUri()))
                }
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
                    saveSelectMusicId(musicQueue[index].id)
                }
                exoPlayer.seekToDefaultPosition(index)
                exoPlayer.playWhenReady = true
                exoPlayer.prepare()
                mediaController?.transportControls?.play()
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
                    val au = DefaultAudioSink.Builder(context)
                        .setEnableFloatOutput(enableFloatOutput)
                        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                        .setAudioProcessors(
                            arrayOf(
                                sonicAudioProcessor,
                                echoAudioProcessor,
                                equalizerAudioProcessor
                            )
                        )
                        .build()
                    return object : ForwardingAudioSink(au) {
                        override fun configure(
                            inputFormat: Format,
                            specifiedBufferSize: Int,
                            outputChannels: IntArray?
                        ) {
//                            val bytesPerSec = Util.getPcmFrameSize(
//                                inputFormat.pcmEncoding,
//                                inputFormat.channelCount
//                            ) * inputFormat.sampleRate * 1 // 这里计算出的就是1s音频的缓冲长度

                            CoroutineScope(Dispatchers.Main).launch {
                                if (needPlayPause) {
                                    needPlayPause = false
                                    timeFinish()
                                }
                                saveCurrentDuration(exoPlayer.duration)
                            }

                            super.configure(inputFormat, specifiedBufferSize, outputChannels)
                        }

                    }
                }
            }
        val trackSelectionFactory = AdaptiveTrackSelection.Factory()
        val trackSelectorParameters = DefaultTrackSelector.Parameters.Builder(context).build()
        val trackSelector = DefaultTrackSelector(context, trackSelectionFactory)
        trackSelector.parameters = trackSelectorParameters
        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer.playWhenReady = true
        exoPlayer.repeatMode = config?.repeatModel ?: Player.REPEAT_MODE_ALL
        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true
        )
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
        playerAddListener()
    }


    private fun playPreview() {
        saveCurrentDuration(0)
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    private fun playNext() {

        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
        saveCurrentDuration(0)
    }

    private fun pauseOrPlayMusic() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else if (exoPlayer.mediaItemCount > 0) {
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    var errorCount = 0
    private fun playerAddListener() {
        exoPlayer.addListener(@UnstableApi object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (musicQueue.isNotEmpty()) {
                    currentPlayTrack =
                        musicQueue[exoPlayer.currentMediaItemIndex]
                }
                if (isPlaying) {
                    notify?.updateNotification(
                        this@PlayService,
                        currentPlayTrack?.name ?: "",
                        currentPlayTrack?.artist ?: "",
                        true,
                        exoPlayer.playbackParameters.speed,
                        exoPlayer.currentPosition,
                        exoPlayer.duration
                    )
                    saveCurrentDuration(exoPlayer.currentPosition)
                } else {
                    notify?.updateNotification(
                        this@PlayService,
                        currentPlayTrack?.name ?: "",
                        currentPlayTrack?.artist ?: "",
                        false,
                        exoPlayer.playbackParameters.speed,
                        exoPlayer.currentPosition,
                        exoPlayer.duration
                    )
                }
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

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                if (musicQueue.isEmpty()) return
                updateNotify()
                if(oldPosition.mediaItemIndex!=newPosition.mediaItemIndex){
                    saveSelectMusicId(
                        musicQueue[newPosition.mediaItemIndex].id
                    )
                    currentPlayTrack =
                        musicQueue[newPosition.mediaItemIndex]
                    val bundle = Bundle()
                    bundle.putParcelable("current", currentPlayTrack)
                    bundle.putInt("type", EVENT_MEDIA_ITEM_Change)
                    bundle.putInt("index", exoPlayer.currentMediaItemIndex)
                    mediaSession?.setExtras(bundle)
                    if (needPlayPause) {
                        needPlayPause = false
                        timeFinish()
                    }
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)
                val bitArray = exoPlayer.mediaMetadata.artworkData
                val metadataBuilder = MediaMetadataCompat.Builder()
                metadataBuilder.putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    exoPlayer.duration
                )
                metadataBuilder.putText(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    currentPlayTrack?.name
                )
                metadataBuilder.putText(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    currentPlayTrack?.artist
                )
                if (bitArray != null) {
                    val bit = BitmapFactory.decodeByteArray(bitArray, 0, bitArray.size)
                    metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bit)
                }
            }
        })
    }

    fun updateNotify(position: Long? = null, duration: Long? = null) {
        notify?.updateNotification(
            this@PlayService,
            currentPlayTrack?.name ?: "",
            currentPlayTrack?.artist ?: "",
            exoPlayer.isPlaying,
            exoPlayer.playbackParameters.speed,
            position ?: exoPlayer.currentPosition,
            duration ?: exoPlayer.duration
        )
        val metadataBuilder = MediaMetadataCompat.Builder()
        metadataBuilder.putLong(
            MediaMetadataCompat.METADATA_KEY_DURATION,
            exoPlayer.duration
        )
        metadataBuilder.putText(
            MediaMetadataCompat.METADATA_KEY_TITLE,
            currentPlayTrack?.name
        )
        metadataBuilder.putText(
            MediaMetadataCompat.METADATA_KEY_ARTIST,
            currentPlayTrack?.artist
        )
        val bitArray = exoPlayer.mediaMetadata.artworkData
        if (bitArray != null) {
            val bit = BitmapFactory.decodeByteArray(bitArray, 0, bitArray.size)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bit)
        }
        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun saveSelectMusicId(id: Long) {
        this@PlayService.getSharedPreferences(
            "SelectedPlayTrack",
            Context.MODE_PRIVATE
        ).edit().putLong("SelectedPlayTrack", id).apply()
    }

    private fun getCurrentPlayId(): Long {
        return this@PlayService.getSharedPreferences(
            "SelectedPlayTrack",
            Context.MODE_PRIVATE
        ).getLong("SelectedPlayTrack", -1)
    }

    @SuppressLint("ApplySharedPref")
    private fun saveCurrentDuration(duration: Long) {
        this@PlayService.getSharedPreferences(
            "SelectedPlayTrack",
            Context.MODE_PRIVATE
        ).edit().putLong("CurrentPosition", duration).commit()
    }

    private fun getCurrentPosition(): Long {
        return this@PlayService.getSharedPreferences(
            "SelectedPlayTrack",
            Context.MODE_PRIVATE
        ).getLong("CurrentPosition", 0)
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

    fun switchRepeatModel(repeatModel: Int) {
        exoPlayer.repeatMode = repeatModel
        CoroutineScope(Dispatchers.IO).launch {
            val config = db.PlayConfigDao().findConfig()
            if (config != null) {
                config.repeatModel = repeatModel
                db.PlayConfigDao().update(config)
            }
        }
    }

    private fun search(extras: Bundle?, result: Result<Bundle>) {
        if (extras != null) {
            val keywords = extras.getString("keyword") ?: ""
            if (keywords.isEmpty() || keywords.length <= 1) {
                result.sendResult(null)
                return
            }
            result.detach()
            CoroutineScope(Dispatchers.IO).launch {
                var tracksList = ArrayList<MusicItem>()
                var albumsList = ArrayList<AlbumList>()
                var artistList = ArrayList<ArtistList>()
                runBlocking {
                    awaitAll(
                        async {
                            tracksList = TracksManager.searchTracks(
                                this@PlayService,
                                tracksLinkedHashMap,
                                keywords
                            )
                        },
                        async {
                            albumsList = AlbumManager.getAlbumByName(this@PlayService, keywords)
                        },
                        async {
                            artistList =
                                ArtistManager.getArtistByName(this@PlayService, keywords)
                        }
                    )
                }
                val bundle = Bundle()
                bundle.putParcelableArrayList("tracks", tracksList)
                bundle.putParcelableArrayList("albums", albumsList)
                bundle.putParcelableArrayList("artist", artistList)
                result.sendResult(bundle)
            }
        } else {
            result.sendResult(null)
        }
    }

    private fun addPlayQueue(extras: Bundle?, result: Result<Bundle>) {
        if (extras != null) {
            val musicItem = extras.getParcelable<MusicItem>("musicItem")
            var musicItems = extras.getParcelableArrayList<MusicItem>("musicItems")
            val index = extras.getInt("index", musicQueue.size)
            if (musicItems == null && musicItem != null) {
                musicItems = ArrayList<MusicItem>()
                musicItems.add(musicItem)
            }
            if (musicItems != null) {
                val list = ArrayList<MediaItem>()
                musicItems.forEach {
                    list.add(MediaItem.fromUri(File(it.path).toUri()))
                }
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
                    musicQueue.addAll(index, musicItems)
                    musicQueue.forEach {
                        it.tableId = null
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        db.QueueDao().deleteAllQueue()
                        db.QueueDao().insertAll(musicQueue)
                    }
                }
                if (!exoPlayer.isPlaying) {
                    exoPlayer.playWhenReady = false
                }
                exoPlayer.addMediaItems(index, list)
            }
        }
        result.sendResult(null)
    }

    private fun removePlayQueue(extras: Bundle?, result: Result<Bundle>) {
        if (extras != null) {
            val index = extras.getInt("index")
            if (index < musicQueue.size) {
                exoPlayer.removeMediaItem(index)
                musicQueue.removeAt(index)
                musicQueue.forEach {
                    it.tableId = null
                }
                CoroutineScope(Dispatchers.IO).launch {
                    db.QueueDao().deleteAllQueue()
                    if (musicQueue.isNotEmpty())
                        db.QueueDao().insertAll(musicQueue)
                }
                playListCurrent = if (playListCurrent?.type == PlayListType.None) {
                    // Search page id = id-1, avoid new's id equal this
                    AnyListBase(playListCurrent!!.id + 1, PlayListType.None)
                } else {
                    // Search page default id is -1
                    AnyListBase(3, PlayListType.None)
                }
                CoroutineScope(Dispatchers.IO).launch {
                    var c = db.CurrentListDao().findCurrentList()
                    if (c == null) {
                        c = CurrentList(null, playListCurrent?.id ?: -1, PlayListType.None.name)
                        db.CurrentListDao().insert(c)
                    } else {
                        c.listID = playListCurrent?.id ?: -1
                        c.type = PlayListType.None.name
                        db.CurrentListDao().update(c)
                    }
                }
                if (musicQueue.isNotEmpty()) {
                    val currentIndex = exoPlayer.currentMediaItemIndex
                    CoroutineScope(Dispatchers.IO).launch {
                        saveSelectMusicId(musicQueue[currentIndex].id)
                    }
                    result.sendResult(null)
                    return
                }
            }
        }
        result.sendResult(null)
    }

    private fun getTracksByType(extras: Bundle?, result: Result<Bundle>) {
        val bundle = Bundle()
        if (extras != null) {
            val type = stringToEnumForPlayListType(
                extras.getString(
                    "type",
                    PlayListType.PlayLists.name
                )
            )
            val id = extras.getLong("id")
            when (type) {
                PlayListType.PlayLists -> {
                    if (playListTracksHashMap[id] != null) {
                        bundle.putParcelableArrayList("list", playListTracksHashMap[id])
                        bundle.putParcelable("message", playListLinkedHashMap[id])
                    } else {
                        result.detach()
                        val tracksUri = MediaStore.Audio.Playlists.Members.getContentUri(
                            "external", id
                        )
                        val tracks =
                            PlaylistManager.getTracksByPlayListId(
                                this@PlayService,
                                tracksUri,
                                tracksLinkedHashMap
                            )
                        playListTracksHashMap[id] = tracks
                        bundle.putParcelableArrayList("list", playListTracksHashMap[id])
                        bundle.putParcelable("message", playListLinkedHashMap[id])
                        result.sendResult(bundle)
                        return
                    }


                }

                PlayListType.Albums -> {
                    if (albumsListTracksHashMap[id] != null) {
                        bundle.putParcelableArrayList("list", albumsListTracksHashMap[id])
                        bundle.putParcelable("message", albumsLinkedHashMap[id])
                    } else {
                        result.detach()
                        val trackUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        val listT: ArrayList<MusicItem> = TracksManager.getTracksById(
                            this@PlayService,
                            trackUri,
                            tracksLinkedHashMap,
                            MediaStore.Audio.Media.ALBUM_ID + "=?",
                            arrayOf(id.toString()),
                            null
                        )
                        albumsListTracksHashMap[id] = listT
                        bundle.putParcelableArrayList("list", albumsListTracksHashMap[id])
                        bundle.putParcelable("message", albumsLinkedHashMap[id])
                        result.sendResult(bundle)
                        return
                    }
                }

                PlayListType.Artists -> {
                    if (artistHasAlbumMap[id] != null && artistsListTracksHashMap[id] != null) {
                        bundle.putParcelableArrayList("list", artistsListTracksHashMap[id])
                        bundle.putParcelableArrayList("albums", artistHasAlbumMap[id])
                        bundle.putParcelable("message", artistsLinkedHashMap[id])
                    } else {
                        result.detach()
                        val trackUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        val albumsList = AlbumManager.getAlbumsByArtist(
                            this@PlayService,
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            albumsLinkedHashMap,
                            MediaStore.Audio.Media.ARTIST_ID + "=?",
                            arrayOf(id.toString()),
                            null
                        )
                        artistHasAlbumMap[id] = albumsList
                        val listT: ArrayList<MusicItem> = TracksManager.getTracksById(
                            this@PlayService,
                            trackUri,
                            tracksLinkedHashMap,
                            MediaStore.Audio.Media.ARTIST_ID + "=?",
                            arrayOf(id.toString()),
                            null
                        )
                        artistsListTracksHashMap[id] = listT
                        bundle.putParcelableArrayList("list", artistsListTracksHashMap[id])
                        bundle.putParcelableArrayList("albums", artistHasAlbumMap[id])
                        bundle.putParcelable("message", artistsLinkedHashMap[id])
                        result.sendResult(bundle)
                        return
                    }
                }

                PlayListType.Genres -> {
                    if (genreHasAlbumMap[id] != null && genresListTracksHashMap[id] != null) {
                        bundle.putParcelableArrayList("list", genresListTracksHashMap[id])
                        bundle.putParcelableArrayList("albums", genreHasAlbumMap[id])
                        bundle.putParcelable("message", genresLinkedHashMap[id])
                    } else {
                        result.detach()
                        val uri =
                            MediaStore.Audio.Genres.Members.getContentUri("external", id)
                        val albums = AlbumManager.getAlbumsByGenre(
                            this@PlayService,
                            uri,
                            albumsLinkedHashMap,
                            null,
                            null,
                            null
                        )
                        genreHasAlbumMap[id] = albums
                        bundle.putParcelableArrayList("list", genresListTracksHashMap[id])
                        bundle.putParcelableArrayList("albums", genreHasAlbumMap[id])
                        bundle.putParcelable("message", genresLinkedHashMap[id])
                        result.sendResult(bundle)
                        return
                    }
                }

                PlayListType.Folders -> {
                    bundle.putParcelableArrayList("list",
                        foldersListTracksHashMap[id]?.values?.let { ArrayList(it) })
                    bundle.putParcelable("message", foldersLinkedHashMap[id])
                }

                PlayListType.Songs -> {
                    bundle.putParcelableArrayList(
                        "list",
                        ArrayList(tracksLinkedHashMap.values)
                    )
                }

                else -> {
                    bundle.putParcelableArrayList("list", musicQueue)
                }
            }
        }
        result.sendResult(bundle)
    }
}
