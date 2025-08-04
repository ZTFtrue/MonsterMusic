package com.ztftrue.music.play

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat.Result
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.SharedPreferencesUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object PlayUtils {

    val tracksFiled = linkedMapOf(
        "Alphabetical" to MediaStore.Audio.Media.TITLE,
        "Album" to MediaStore.Audio.Media.ALBUM,
        "Artist" to MediaStore.Audio.Media.ARTIST,
        "Duration" to MediaStore.Audio.Media.DURATION,
        "Year" to MediaStore.Audio.Media.YEAR
    )
    val albumsFiled = linkedMapOf(
        "Alphabetical" to MediaStore.Audio.Albums.ALBUM,
        "Artist" to MediaStore.Audio.Albums.ARTIST,
        "First year" to MediaStore.Audio.Albums.FIRST_YEAR,
        "Last year" to MediaStore.Audio.Albums.LAST_YEAR,
        "Number of songs" to MediaStore.Audio.Albums.NUMBER_OF_SONGS
    )
    val playListFiled = linkedMapOf(
        "Alphabetical" to MediaStore.Audio.Playlists.NAME,
    )

    val artistFiled = linkedMapOf(
        "Alphabetical" to MediaStore.Audio.Artists.ARTIST,
        "Number of albums" to MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
        "Number of tracks" to MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
    )
    val genresFiled = linkedMapOf(
        "Alphabetical" to MediaStore.Audio.Genres.NAME,
    )

    // for main tab parent
    val sortFiledMap = mapOf(
        PlayListType.Songs.name to tracksFiled,
        PlayListType.PlayLists.name to playListFiled,
        PlayListType.Albums.name to albumsFiled,
        PlayListType.Artists.name to artistFiled,
        PlayListType.Genres.name to genresFiled,
    )

    object ListTypeTracks {
        const val PlayListsTracks = "PlayLists@Tracks"
        const val AlbumsTracks = "Albums@Tracks"
        const val ArtistsTracks = "Artists@Tracks"
        const val GenresTracks = "Genres@Tracks"
        const val FoldersTracks = "Folders@Tracks"
    }

    // for tracks in album or artist etc. child
    val trackSortFiledMap = mapOf(
        ListTypeTracks.PlayListsTracks to tracksFiled,
        ListTypeTracks.AlbumsTracks to linkedMapOf(
            "Alphabetical" to MediaStore.Audio.Media.TITLE,
            "Artist" to MediaStore.Audio.Media.ARTIST,
            "Duration" to MediaStore.Audio.Media.DURATION,
            "Year" to MediaStore.Audio.Media.YEAR,
            "Track number" to MediaStore.Audio.Media.TRACK
        ),
        ListTypeTracks.ArtistsTracks to linkedMapOf(
            "Alphabetical" to MediaStore.Audio.Media.TITLE,
            "Album" to MediaStore.Audio.Media.ALBUM,
            "Duration" to MediaStore.Audio.Media.DURATION,
            "Year" to MediaStore.Audio.Media.YEAR
        ),
        ListTypeTracks.GenresTracks to tracksFiled,
        ListTypeTracks.FoldersTracks to tracksFiled,
    )
    val methodMap = mapOf(
        "Ascending" to "ASC",
        "Descending" to "DESC"
    )

    fun addDefaultMainTab(mainTab: ArrayList<MainTab>) {
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
    }

    fun shuffleModelSwitch(
        extras: Bundle, result: Result<Bundle>,
        musicQueue: ArrayList<MusicItem>,
        currentPlayTrack: MusicItem?,
        exoPlayer: ExoPlayer,
        context: Context,
        db: MusicDatabase
    ) {

        var index = 0
        val enable = extras.getBoolean("enable", false)
        if (enable) {
            if (musicQueue.isNotEmpty()) {
                if (musicQueue[0].priority == 0) {
                    val dbArrayList = ArrayList<MusicItem>()
                    dbArrayList.addAll(musicQueue)
                    musicQueue.shuffle()
                    val t1 = ArrayList<MediaItem>()
                    if (musicQueue.isNotEmpty()) {
                        musicQueue.forEachIndexed { i, it ->
                            it.priority = i + 1
                            if (it.id == currentPlayTrack?.id) {
                                index = i
                            }
                            t1.add(MediaItem.fromUri(File(it.path).toUri()))
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            db.QueueDao().updateList(dbArrayList)
                        }
                        CoroutineScope(Dispatchers.Main).launch {
                            exoPlayer.pause()
                            val cp = exoPlayer.currentPosition
                            exoPlayer.clearMediaItems()
                            exoPlayer.setMediaItems(t1)
                            exoPlayer.seekToDefaultPosition(index)
                            exoPlayer.seekTo(cp)
                            exoPlayer.playWhenReady = true
                            exoPlayer.prepare()
                        }
                    }
                } else {
                    musicQueue.sortBy { it.priority }
                    val t1 = ArrayList<MediaItem>()
                    if (musicQueue.isNotEmpty()) {
                        musicQueue.forEachIndexed { i, it ->
                            if (it.id == currentPlayTrack?.id) {
                                index = i
                            }
                            t1.add(MediaItem.fromUri(File(it.path).toUri()))
                        }
                        CoroutineScope(Dispatchers.Main).launch {
                            exoPlayer.pause()
                            val cp = exoPlayer.currentPosition
                            exoPlayer.clearMediaItems()
                            exoPlayer.setMediaItems(t1)
                            exoPlayer.seekToDefaultPosition(index)
                            exoPlayer.seekTo(cp)
                            exoPlayer.playWhenReady = true
                            exoPlayer.prepare()
                        }
                    }
                }
            }
            SharedPreferencesUtils.enableShuffle(context, true)
        } else {
            musicQueue.sortBy { it.tableId }
            val t1 = ArrayList<MediaItem>()
            if (musicQueue.isNotEmpty()) {
                musicQueue.forEachIndexed { i, it ->
                    if (it.id == currentPlayTrack?.id) {
                        index = i
                    }
                    t1.add(MediaItem.fromUri(File(it.path).toUri()))
                }
                CoroutineScope(Dispatchers.Main).launch {
                    exoPlayer.pause()
                    val cp = exoPlayer.currentPosition
                    exoPlayer.clearMediaItems()
                    exoPlayer.setMediaItems(t1)
                    exoPlayer.seekToDefaultPosition(index)
                    exoPlayer.seekTo(cp)
                    exoPlayer.playWhenReady = true
                    exoPlayer.prepare()
                }
            }
        }
        SharedPreferencesUtils.enableShuffle(context, enable)
        val bundle = Bundle()
        bundle.putParcelableArrayList("list", musicQueue)
        bundle.putInt("index", index)
        result.sendResult(bundle)
    }

    fun trackDelete(
        id: Long,
        musicQueue: ArrayList<MusicItem>,
        exoPlayer: ExoPlayer,
        db: MusicDatabase,
        tracksLinkedHashMap: LinkedHashMap<Long, MusicItem>
    ): Long {
        tracksLinkedHashMap.remove(id)
        val i = musicQueue.indexOfFirst { it.id == id }
        if (i > -1) {
            val musicItem = musicQueue.removeAt(i)
            changePriorityTableId(musicQueue, musicItem, db)
        }
        return i.toLong()
    }

    fun removePlayQueue(
        extras: Bundle?, result: Result<Bundle>, musicQueue: ArrayList<MusicItem>,
        exoPlayer: ExoPlayer, db: MusicDatabase, context: Context
    ) {
        if (extras != null) {
            val index = extras.getInt("index")
            if (index < musicQueue.size) {
                val musicItem = musicQueue[index]
                musicQueue.removeAt(index)
                changePriorityTableId(musicQueue, musicItem, db)
                exoPlayer.removeMediaItem(index)
                if (musicQueue.isNotEmpty()) {
                    val currentIndex = exoPlayer.currentMediaItemIndex
                    CoroutineScope(Dispatchers.IO).launch {
                        SharedPreferencesUtils.saveSelectMusicId(
                            context,
                            musicQueue[currentIndex].id
                        )
                    }
                    result.sendResult(null)
                    return
                }
            }
        }
        result.sendResult(null)
    }

    private fun changePriorityTableId(
        musicQueue: ArrayList<MusicItem>,
        musicItem: MusicItem,
        db: MusicDatabase
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val tId = musicItem.tableId!!
            val priority = musicItem.priority
            if (musicQueue.isNotEmpty())
                musicQueue.forEach {
                    if (it.tableId!! >= tId) {
                        it.tableId = it.tableId!! - 1
                    }
                    if (it.priority >= priority && it.priority > 0) {
                        it.priority -= 1
                    }
                }
            db.QueueDao().deleteAllQueue()
            db.QueueDao().insertAll(musicQueue)
            db.CurrentListDao().delete()
        }
    }

}