package com.ztftrue.music.play

import android.provider.MediaStore
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.PlayListType

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
    @Suppress("DEPRECATION")
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


    fun trackDelete(
        id: Long,
        musicQueue: ArrayList<MusicItem>,
        tracksLinkedHashMap: LinkedHashMap<Long, MusicItem>
    ): Long {
        tracksLinkedHashMap.remove(id)
        val i = musicQueue.indexOfFirst { it.id == id }
//        if (i > -1) {
//            val musicItem = musicQueue.removeAt(i)
//            changePriorityTableId(musicQueue, musicItem, db)
//        }
        return i.toLong()
    }


    fun areQueuesContentAndOrderEqual(list1: List<MusicItem>, list2: List<MusicItem>): Boolean {
        if (list1.size != list2.size) {
            return false // 数量不同，肯定不一致
        }
        val l2 = list2.sortedBy {
            it.tableId
        }

        // 注意：我们只比较那些代表歌曲本身的字段，忽略 tableId 和 priority
        for (i in list1.indices) {
            val item1 = list1[i]
            val item2 = l2[i]

            // 比较歌曲的核心唯一标识符（例如，id）
            if (item1.id != item2.id) return false

            // (可选) 比较其他关键字段，以确保它是同一首歌的完全相同版本
            if (item1.priority != item2.priority) return false
//            if (item1.path != item2.path) return false
        }
        return true // 所有歌曲都相同且顺序一致
    }
}