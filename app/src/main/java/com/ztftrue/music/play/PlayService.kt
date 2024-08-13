package com.ztftrue.music.play

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ztftrue.music.MainActivity
import com.ztftrue.music.PlayMusicWidget
import com.ztftrue.music.R
import com.ztftrue.music.effects.EchoAudioProcessor
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.math3.util.FastMath
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
const val ACTION_SWITCH_SHUFFLE = "ACTION_SWITCH_SHUFFLE"

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
const val ACTION_Sort_Queue = "ACTION_Sort_Queue"
const val ACTION_PlayLIST_CHANGE = "ACTION_PlayLIST_CHANGE"
const val ACTION_Volume_CHANGE = "ACTION_Volume_CHANGE"
const val ACTION_TRACKS_DELETE = "ACTION_TRACKS_DELETE"
const val ACTION_TRACKS_UPDATE = "ACTION_TRACKS_UPDATE"
const val ACTION_GET_TRACK_BY_ID = "ACTION_GET_TRACK_BY_ID"
const val ACTION_CLEAR_QUEUE = "ACTION_CLEAR_QUEUE"
const val ACTION_SORT = "ACTION_SORT"
const val ACTION_SHUFFLE_PLAY_QUEUE = "ACTION_SHUFFLE_PLAY_QUEUE"
const val ACTION_GET_ALBUM_BY_ID = "GET_ARTIST_FROM_ALBUM"

const val MY_MEDIA_ROOT_ID = "MY_MEDIA_ROOT_ID"

const val EVENT_MEDIA_ITEM_Change = 3
const val EVENT_SLEEP_TIME_Change = 5
const val EVENT_DATA_READY = 6


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
                CoroutineScope(Dispatchers.IO).launch {
                    val sortData =
                        db.SortFiledDao().findSortByType(PlayListType.PlayLists.name)
                    PlaylistManager.getPlaylists(
                        this@PlayService,
                        playListLinkedHashMap,
                        tracksLinkedHashMap,
                        result,
                        "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
                    )
                }
            }
        } else if (PlayListType.Songs.name == action) {
            if (tracksLinkedHashMap.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putParcelableArrayList("songsList", ArrayList(tracksLinkedHashMap.values))
                result.sendResult(bundle)
                return
            } else {
                result.detach()
                CoroutineScope(Dispatchers.IO).launch {
                    val sortData =
                        db.SortFiledDao().findSortByType(PlayListType.Songs.name)
                    TracksManager.getFolderList(
                        this@PlayService,
                        foldersLinkedHashMap,
                        result,
                        tracksLinkedHashMap,
                        "${sortData?.filed ?: ""} ${sortData?.method ?: ""}",
                        true
                    )
                }
            }
        } else if (PlayListType.Genres.name == action) {
            if (genresLinkedHashMap.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putParcelableArrayList("list", ArrayList(genresLinkedHashMap.values))
                result.sendResult(bundle)
                return
            } else {
                result.detach()
                CoroutineScope(Dispatchers.IO).launch {
                    val sortDataP =
                        db.SortFiledDao().findSortByType(PlayListType.Genres.name)
                    GenreManager.getGenresList(
                        this@PlayService,
                        genresLinkedHashMap,
                        tracksLinkedHashMap,
                        result,
                        "${sortDataP?.filed ?: ""} ${sortDataP?.method ?: ""}"
                    )
                }
            }
        } else if (PlayListType.Albums.name == action) {
            if (albumsLinkedHashMap.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putParcelableArrayList("list", ArrayList(albumsLinkedHashMap.values))
                result.sendResult(bundle)
                return
            } else {
                result.detach()
                CoroutineScope(Dispatchers.IO).launch {
                    val sortDataP =
                        db.SortFiledDao().findSortByType(PlayListType.Albums.name)
                    AlbumManager.getAlbumList(
                        this@PlayService,
                        albumsLinkedHashMap,
                        result,
                        "${sortDataP?.filed ?: ""} ${sortDataP?.method ?: ""}"
                    )
                }
            }
        } else if (PlayListType.Artists.name == action) {
            if (artistsLinkedHashMap.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putParcelableArrayList("list", ArrayList(artistsLinkedHashMap.values))
                result.sendResult(bundle)
                return
            } else {
                result.detach()
                CoroutineScope(Dispatchers.IO).launch {
                    val sortDataP =
                        db.SortFiledDao().findSortByType(PlayListType.Artists.name)
                    ArtistManager.getArtistList(
                        this@PlayService,
                        artistsLinkedHashMap,
                        result,
                        "${sortDataP?.filed ?: ""} ${sortDataP?.method ?: ""}"
                    )
                }
            }
        } else if (PlayListType.Folders.name == action) {
            if (foldersLinkedHashMap.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putParcelableArrayList("list", ArrayList(foldersLinkedHashMap.values))
                result.sendResult(bundle)
                return
            } else {
                result.detach()
                CoroutineScope(Dispatchers.IO).launch {
                    val sortData =
                        db.SortFiledDao().findSortByType(PlayListType.Songs.name)
                    TracksManager.getFolderList(
                        this@PlayService,
                        foldersLinkedHashMap,
                        result,
                        tracksLinkedHashMap,
                        "${sortData?.filed ?: ""} ${sortData?.method ?: ""}", false
                    )
                }
            }
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
            // get tracks from album/playList/Artist
            getTracksByType(extras, result)
        } else if (ACTION_AddPlayQueue == action) {
            // add tracks to queue
            addPlayQueue(extras, result)
        } else if (ACTION_RemoveFromQueue == action) {
            // remove tracks from queue
            playListCurrent = null
            PlayUtils.removePlayQueue(
                extras, result, musicQueue,
                exoPlayer,
                db, this@PlayService
            )
        } else if (ACTION_Sort_Queue == action) {
            sortPlayQueue(extras, result)
        } else if (ACTION_SWITCH_SHUFFLE == action) {
            if (extras != null) {
                PlayUtils.shuffleModelSwitch(
                    extras,
                    result,
                    musicQueue,
                    currentPlayTrack,
                    exoPlayer,
                    this@PlayService,
                    db
                )
            } else {
                result.sendResult(null)
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
        } else if (ACTION_SHUFFLE_PLAY_QUEUE == action) {
            if (extras != null) {
                playShuffleMusic(extras, result)
            } else {
                result.sendResult(null)
            }
        } else if (ACTION_PlayLIST_CHANGE == action) {
            playListLinkedHashMap.clear()
            playListTracksHashMap.clear()
            CoroutineScope(Dispatchers.IO).launch {
                val sortData =
                    db.SortFiledDao().findSortByType(PlayListType.PlayLists.name)
                PlaylistManager.getPlaylists(
                    this@PlayService,
                    playListLinkedHashMap,
                    tracksLinkedHashMap,
                    null,
                    "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
                )
            }
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
                val c = PlayUtils.trackDelete(
                    extras,
                    result,
                    musicQueue,
                    exoPlayer,
                    db,
                    tracksLinkedHashMap
                )
                clearCacheData()
                if (c) {
                    playListCurrent = null
                }
            } else {
                result.sendResult(null)
            }

        } else if (ACTION_TRACKS_UPDATE == action) {
            result.detach()
            if (extras != null) {
                val id = extras.getLong("id")
                val musicTrack = TracksManager.getMusicById(this@PlayService, id)
                if (musicTrack != null) {
                    tracksLinkedHashMap[id] = musicTrack
                } else {
                    tracksLinkedHashMap.remove(id)
                }
                clearCacheData()
                val bundle = Bundle()
                bundle.putParcelableArrayList("list", ArrayList(tracksLinkedHashMap.values))
                if (musicTrack != null) {
                    for (it in musicQueue) {
                        if (it.id == id) {
                            it.name = musicTrack.name
                            it.path = musicTrack.path
                            it.duration = musicTrack.duration
                            it.displayName = musicTrack.displayName
                            it.album = musicTrack.album
                            it.albumId = musicTrack.albumId
                            it.artist = musicTrack.artist
                            it.artistId = musicTrack.artistId
                            it.genre = musicTrack.genre
                            it.genreId = musicTrack.genreId
                            it.year = musicTrack.year
                            it.songNumber = musicTrack.songNumber
                            bundle.putParcelable("item", musicTrack)
                            CoroutineScope(Dispatchers.IO).launch {
                                db.QueueDao().update(it)
                            }
                            break
                        }
                    }
                }
                result.sendResult(bundle)
                return
            } else {
                result.sendResult(null)
            }
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
                SharedPreferencesUtils.saveSelectMusicId(this@PlayService, -1)
            }
            result.sendResult(null)
        } else if (action == ACTION_SORT) {
            sortAction(extras, result)
        } else if (action == ACTION_Volume_CHANGE) {
            if (extras != null) {
                volumeValue = extras.getInt("volume", 100)
                SharedPreferencesUtils.saveVolume(this@PlayService, volumeValue)
//                equalizerAudioProcessor.setVolume(volumeValue)
                exoPlayer.volume = volumeValue / 100f
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

    private fun playShuffleMusic(extras: Bundle, result: Result<Bundle>) {
        if (notify == null) {
            notify = CreateNotification(this@PlayService, mediaSession)
        }
//        val switchQueue = extras.getBoolean("switch_queue", false)
//        val enableShuffle = extras.getBoolean("enable_shuffle", false)
        val index = 0
//        val musicItem = extras.getParcelable<MusicItem>("musicItem")
        val playList = extras.getParcelable<AnyListBase>("playList")
        val musicItems = extras.getParcelableArrayList<MusicItem>("musicItems")
        if (playList != null && musicItems != null) {
            musicItems.forEachIndexed { i, item ->
                item.tableId = i + 1L
            }
            val dbArrayList = ArrayList<MusicItem>()
            dbArrayList.addAll(musicItems)
            musicItems.shuffle()
            playListCurrent = playList
            musicQueue.clear()
            musicQueue.addAll(musicItems)
            val t1 = ArrayList<MediaItem>()
            if (musicQueue.isNotEmpty()) {
                musicQueue.forEachIndexed { i, it ->
                    it.priority = i + 1
                    t1.add(MediaItem.fromUri(File(it.path).toUri()))
                }
                val bundle = Bundle()
                bundle.putParcelableArrayList("list", musicQueue)
                result.sendResult(bundle)
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
                    db.QueueDao().insertAll(dbArrayList)
                    SharedPreferencesUtils.saveSelectMusicId(this@PlayService, musicItems[index].id)
                    SharedPreferencesUtils.enableShuffle(this@PlayService, true)
                }
                CoroutineScope(Dispatchers.Main).launch {
                    exoPlayer.clearMediaItems()
                    exoPlayer.setMediaItems(t1)
                    exoPlayer.seekToDefaultPosition(index)
                    exoPlayer.seekTo(0)
                    exoPlayer.playWhenReady = true
                    exoPlayer.prepare()
                }
            }
        } else {
            result.sendResult(null)
        }
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
                    val sortDataDao =
                        db.SortFiledDao()
                    val sortData =
                        sortDataDao.findSortByType(PlayListType.Songs.name)
                    TracksManager.getFolderList(
                        this@PlayService,
                        foldersLinkedHashMap,
                        null,
                        tracksLinkedHashMap,
                        "${sortData?.filed ?: ""} ${sortData?.method ?: ""}",
                        false,
                        allTracksLinkedHashMap
                    )
                    runBlocking {
                        awaitAll(
                            async {
                                val sortData1 = db.SortFiledDao().findSortAll()
                                if (sortData1 != null) {
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
                            },
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
                                val queue =
                                    if (SharedPreferencesUtils.getEnableShuffle(this@PlayService)) {
                                        // don't need to check shuffle, should not no shuffle, on first time
                                        db.QueueDao().findQueueShuffle()
                                    } else {
                                        db.QueueDao().findQueue()
                                    }
                                musicQueue.clear()
                                if (!queue.isNullOrEmpty()) {
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
                            },
                            async {
                                config = db.PlayConfigDao().findConfig()
                                if (config == null) {
                                    config = PlayConfig(0, Player.REPEAT_MODE_ALL)
                                    db.PlayConfigDao().insert(config!!)
                                }
                            },
                            async {
                                val list =
                                    db.MainTabDao().findAllIsShowMainTabSortByPriority()
                                if (list.isNullOrEmpty()) {
                                    PlayUtils.addDefaultMainTab(mainTab)
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

        volumeValue = SharedPreferencesUtils.getVolume(this@PlayService)
//             equalizerAudioProcessor.setVolume(volumeValue)
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
                t1.add(MediaItem.fromUri(File(it.path).toUri()))
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
        bundle.putParcelableArrayList("showIndicatorList", showIndicatorList)
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
            SharedPreferencesUtils.saveCurrentDuration(this@PlayService, exoPlayer.currentPosition)
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
                SharedPreferencesUtils.saveSelectMusicId(this@PlayService, musicQueue[index].id)
                SharedPreferencesUtils.saveCurrentDuration(this@PlayService, 0L)
                exoPlayer.seekToDefaultPosition(index)
                exoPlayer.playWhenReady = true
                exoPlayer.prepare()
            }
        }
    }

    private fun playMusicSwitchQueue(
        playList: AnyListBase,
        index: Int,
        musicItems: ArrayList<MusicItem>,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            SharedPreferencesUtils.enableShuffle(this@PlayService, false)
            playListCurrent = playList
            musicQueue.clear()
            musicQueue.addAll(musicItems)
            if (musicQueue.isNotEmpty()) {
                val t1 = ArrayList<MediaItem>()
                musicQueue.forEachIndexed { index, it ->
                    it.tableId = index + 1L
                    t1.add(MediaItem.fromUri(File(it.path).toUri()))
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
                    SharedPreferencesUtils.saveSelectMusicId(this@PlayService, musicQueue[index].id)
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
                                SharedPreferencesUtils.saveCurrentDuration(
                                    this@PlayService,
                                    exoPlayer.duration
                                )
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
//        exoPlayer.shuffleModeEnabled=true;
//        exoPlayer.setShuffleOrder()
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
        SharedPreferencesUtils.saveCurrentDuration(this@PlayService, 0)
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    private fun playNext() {

        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
        SharedPreferencesUtils.saveCurrentDuration(this@PlayService, 0)
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
            @SuppressLint("ApplySharedPref")
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (musicQueue.isNotEmpty()) {
                    currentPlayTrack =
                        musicQueue[exoPlayer.currentMediaItemIndex]
                }
                getSharedPreferences("Widgets", Context.MODE_PRIVATE).getBoolean(
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

                            getSharedPreferences("Widgets", Context.MODE_PRIVATE).edit()
                                .putBoolean("playingStatus", isPlaying)
                                .putString("title", currentPlayTrack?.name ?: "")
                                .putLong("title", currentPlayTrack?.id ?: 0L)
                                .putString("author", currentPlayTrack?.artist ?: "")
                                .putString("path", currentPlayTrack?.path ?: "").commit()

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

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                if (musicQueue.isEmpty()) return

                if (oldPosition.mediaItemIndex != newPosition.mediaItemIndex || reason >= 4 || currentPlayTrack?.id != musicQueue[newPosition.mediaItemIndex].id) {
                    SharedPreferencesUtils.saveSelectMusicId(
                        this@PlayService,
                        musicQueue[newPosition.mediaItemIndex].id
                    )
                    currentPlayTrack =
                        musicQueue[newPosition.mediaItemIndex]
                    val bundle = Bundle()
                    bundle.putParcelable("current", currentPlayTrack)
                    bundle.putInt("type", EVENT_MEDIA_ITEM_Change)
                    bundle.putInt("index", exoPlayer.currentMediaItemIndex)
                    getSharedPreferences("Widgets", Context.MODE_PRIVATE).getBoolean(
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
                                Log.d("TAG", currentPlayTrack?.name ?: "")
                                intent.putExtra("playingStatus", exoPlayer.isPlaying)
                                intent.putExtra("title", currentPlayTrack?.name ?: "")
                                intent.putExtra("author", currentPlayTrack?.artist ?: "")
                                intent.putExtra("path", currentPlayTrack?.path ?: "")
                                intent.putExtra("id", currentPlayTrack?.id ?: 0L)
                                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                                getSharedPreferences("Widgets", Context.MODE_PRIVATE).edit()
                                    .putBoolean("playingStatus", exoPlayer.isPlaying)
                                    .putString("title", currentPlayTrack?.name ?: "")
                                    .putLong("id", currentPlayTrack?.id ?: 0L)
                                    .putString("author", currentPlayTrack?.artist ?: "")
                                    .putString("path", currentPlayTrack?.path ?: "").commit()
                                sendBroadcast(intent)
                            }
                        }
                    mediaSession?.setExtras(bundle)
                    if (needPlayPause) {
                        needPlayPause = false
                        timeFinish()
                    }
                }
                // wait currentPlayTrack changed
                updateNotify()
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
                    metadataBuilder.putBitmap(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON,
                        bit
                    )
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
                    list.add(MediaItem.fromUri(File(it.path).toUri()))
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
        result.sendResult(null)
    }

    private fun sortPlayQueue(extras: Bundle?, result: Result<Bundle>) {
        result.detach()
        if (extras != null) {
            val index = extras.getInt("index")
            val targetIndex = extras.getInt("targetIndex")
            val m = musicQueue.removeAt(index)
            musicQueue.add(targetIndex, m)
            val im = exoPlayer.getMediaItemAt(index)
            exoPlayer.removeMediaItem(index)
            exoPlayer.addMediaItem(targetIndex, im)
            val start = FastMath.min(index, targetIndex)
            val end = FastMath.max(index, targetIndex)
            if (SharedPreferencesUtils.getEnableShuffle(this@PlayService)) {
                val changedQueueArray = ArrayList<MusicItem>(end - start + 1)
                for (i in start..end) {
                    musicQueue[i].priority = i + 1
                    changedQueueArray.add(musicQueue[i])
                }
                CoroutineScope(Dispatchers.IO).launch {
                    db.QueueDao().updateList(changedQueueArray)
                }
            } else {
                val changedQueueArray = ArrayList<MusicItem>(end - start + 1)
                for (i in start..end) {
                    musicQueue[i].tableId = i.toLong() + 1
                    changedQueueArray.add(musicQueue[i])
                }
                CoroutineScope(Dispatchers.IO).launch {
                    db.QueueDao().updateList(changedQueueArray)
                }
            }
        }
        result.sendResult(null)
    }


    private fun sortAction(extras: Bundle?, result: Result<Bundle>) {
        val bundle = Bundle()
        if (extras != null) {
            // method
            //filed
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
                        result.detach()
                        CoroutineScope(Dispatchers.IO).launch {
                            val sortData =
                                db.SortFiledDao().findSortByType(PlayListType.Songs.name)
                            TracksManager.getFolderList(
                                this@PlayService,
                                foldersLinkedHashMap,
                                result,
                                tracksLinkedHashMap,
                                "${sortData?.filed ?: ""} ${sortData?.method ?: ""}",
                                true
                            )
                            val sortData1 = db.SortFiledDao().findSortAll()
                            if (sortData1 != null) {
                                showIndicatorList.clear()
                                sortData1.forEach {
                                    if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                                        showIndicatorList.add(it)
                                    }
                                }
                            }
                        }
                        return
                    }

                    PlayUtils.ListTypeTracks.PlayListsTracks -> {
                        playListTracksHashMap.clear()
                        CoroutineScope(Dispatchers.IO).launch {
                            val sortData1 = db.SortFiledDao().findSortAll()
                            if (sortData1 != null) {
                                showIndicatorList.clear()
                                sortData1.forEach {
                                    if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                                        showIndicatorList.add(it)
                                    }
                                }
                            }
                        }
                    }

                    PlayUtils.ListTypeTracks.AlbumsTracks -> {
                        albumsListTracksHashMap.clear()
                        CoroutineScope(Dispatchers.IO).launch {
                            val sortData1 = db.SortFiledDao().findSortAll()
                            if (sortData1 != null) {
                                showIndicatorList.clear()
                                sortData1.forEach {
                                    if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                                        showIndicatorList.add(it)
                                    }
                                }
                            }
                        }
                    }

                    PlayUtils.ListTypeTracks.ArtistsTracks -> {
                        artistsListTracksHashMap.clear()
                        CoroutineScope(Dispatchers.IO).launch {
                            val sortData1 = db.SortFiledDao().findSortAll()
                            if (sortData1 != null) {
                                showIndicatorList.clear()
                                sortData1.forEach {
                                    if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                                        showIndicatorList.add(it)
                                    }
                                }
                            }
                        }
                    }

                    PlayUtils.ListTypeTracks.GenresTracks -> {
                        genresListTracksHashMap.clear()
                        CoroutineScope(Dispatchers.IO).launch {
                            val sortData1 = db.SortFiledDao().findSortAll()
                            if (sortData1 != null) {
                                showIndicatorList.clear()
                                sortData1.forEach {
                                    if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                                        showIndicatorList.add(it)
                                    }
                                }
                            }
                        }
                    }

                    PlayUtils.ListTypeTracks.FoldersTracks -> {
                        foldersListTracksHashMap.clear()
                        CoroutineScope(Dispatchers.IO).launch {
                            val sortData1 = db.SortFiledDao().findSortAll()
                            if (sortData1 != null) {
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
        }
        result.sendResult(bundle)
    }

    private fun getTracksByType(extras: Bundle?, result: Result<Bundle>) {
        val bundle = Bundle()
        if (extras != null) {
            val type = extras.getString(
                "type",
                PlayListType.PlayLists.name
            )
            val id = extras.getLong("id")
            when (type) {
                PlayListType.PlayLists.name -> {
                    if (playListTracksHashMap[id] != null) {
                        bundle.putParcelableArrayList("list", playListTracksHashMap[id])
                        bundle.putParcelable("message", playListLinkedHashMap[id])
                    } else {
                        result.detach()
                        CoroutineScope(Dispatchers.IO).launch {
                            val sortData =
                                db.SortFiledDao()
                                    .findSortByType(PlayUtils.ListTypeTracks.PlayListsTracks)
                            val tracksUri = MediaStore.Audio.Playlists.Members.getContentUri(
                                "external", id
                            )
                            val tracks =
                                PlaylistManager.getTracksByPlayListId(
                                    this@PlayService,
                                    tracksUri,
                                    allTracksLinkedHashMap,
                                    null,
                                    null,
                                    "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
                                )
                            playListTracksHashMap[id] = tracks
                            bundle.putParcelableArrayList("list", playListTracksHashMap[id])
                            bundle.putParcelable("message", playListLinkedHashMap[id])
                            result.sendResult(bundle)
                        }

                        return
                    }


                }

                PlayListType.Albums.name -> {
                    if (albumsListTracksHashMap[id] != null) {
                        bundle.putParcelableArrayList("list", albumsListTracksHashMap[id])
                        bundle.putParcelable("message", albumsLinkedHashMap[id])
                    } else {
                        result.detach()
                        CoroutineScope(Dispatchers.IO).launch {
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
                            bundle.putParcelableArrayList("list", albumsListTracksHashMap[id])
                            bundle.putParcelable("message", albumsLinkedHashMap[id])
                            result.sendResult(bundle)
                        }
                        return
                    }
                }

                PlayListType.Artists.name -> {
                    if (artistHasAlbumMap[id] != null && artistsListTracksHashMap[id] != null) {
                        bundle.putParcelableArrayList("list", artistsListTracksHashMap[id])
                        bundle.putParcelableArrayList("albums", artistHasAlbumMap[id])
                        bundle.putParcelable("message", artistsLinkedHashMap[id])
                    } else {
                        result.detach()
                        CoroutineScope(Dispatchers.IO).launch {
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
                                            null,
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
                            artistHasAlbumMap[id] = albumsList
                            val listT: ArrayList<MusicItem> = TracksManager.getTracksById(
                                this@PlayService,
                                trackUri,
                                allTracksLinkedHashMap,
                                MediaStore.Audio.Media.ARTIST_ID + "=?",
                                arrayOf(id.toString()),
                                "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
                            )
                            artistsListTracksHashMap[id] = listT
                            bundle.putParcelableArrayList("list", artistsListTracksHashMap[id])
                            bundle.putParcelableArrayList("albums", artistHasAlbumMap[id])
                            bundle.putParcelable("message", artistsLinkedHashMap[id])
                            result.sendResult(bundle)
                        }
                        return
                    }
                }

                PlayListType.Genres.name -> {
                    if (genreHasAlbumMap[id] != null && genresListTracksHashMap[id] != null) {
                        bundle.putParcelableArrayList("list", genresListTracksHashMap[id])
                        bundle.putParcelableArrayList("albums", genreHasAlbumMap[id])
                        bundle.putParcelable("message", genresLinkedHashMap[id])
                    } else {
                        result.detach()
                        CoroutineScope(Dispatchers.IO).launch {
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
                                            null,
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
                            bundle.putParcelableArrayList("list", genresListTracksHashMap[id])
                            bundle.putParcelableArrayList("albums", genreHasAlbumMap[id])
                            bundle.putParcelable("message", genresLinkedHashMap[id])
                            result.sendResult(bundle)
                        }
                        return
                    }
                }

                PlayListType.Folders.name -> {
                    if (foldersListTracksHashMap[id] != null && foldersLinkedHashMap[id] != null) {
                        bundle.putParcelableArrayList(
                            "list",
                            foldersListTracksHashMap[id]?.values?.let { ArrayList(it) })
                        bundle.putParcelable("message", foldersLinkedHashMap[id])
                    } else {
                        result.detach()
                        CoroutineScope(Dispatchers.IO).launch {
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
                            val m = LinkedHashMap<Long, MusicItem>()
                            listT.forEach { itM ->
                                m[itM.id] = itM
                            }
                            foldersListTracksHashMap[id] = m
                            bundle.putParcelableArrayList("list", listT)
                            bundle.putParcelable("message", foldersLinkedHashMap[id])
                            result.sendResult(bundle)
                        }
                        return
                    }
                }

                else -> {
                    bundle.putParcelableArrayList(
                        "list",
                        ArrayList(allTracksLinkedHashMap.values)
                    )
                }
            }
        }
        result.sendResult(bundle)
    }
}
