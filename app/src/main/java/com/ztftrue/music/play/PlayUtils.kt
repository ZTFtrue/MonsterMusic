package com.ztftrue.music.play

import android.provider.MediaStore
import com.ztftrue.music.sqlData.model.MainTab
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
        val PlayListsTracks = "PlayLists@Tracks"
        val AlbumsTracks = "Albums@Tracks"
        val ArtistsTracks = "Artists@Tracks"
        val GenresTracks = "Genres@Tracks"
        val FoldersTracks = "Folders@Tracks"
    }

    // for tracks in album or artist etc. child
    val trackSortFiledMap = mapOf(
        ListTypeTracks.PlayListsTracks to tracksFiled,
        ListTypeTracks.AlbumsTracks to tracksFiled,
        ListTypeTracks.ArtistsTracks to tracksFiled,
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

}