package com.ztftrue.music.utils.trackManager

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ztftrue.music.utils.model.AlbumList

object AlbumManager {
    fun getAlbumList(
        context: Context,
        list: LinkedHashMap<Long, AlbumList>,
        sortOrder1: String
    ) {
        val playListProjection = arrayOf(
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ALBUM_ID,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST,
            MediaStore.Audio.Albums.FIRST_YEAR,
            MediaStore.Audio.Albums.LAST_YEAR,
        )
        val sortOrder = sortOrder1.ifBlank { "${MediaStore.Audio.Albums.ALBUM} ASC" }
        val musicResolver = context.contentResolver
        val cursor = musicResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            playListProjection,
            null,
            null,
            sortOrder
        )
        val playList = LinkedHashMap<Long, AlbumList>()
        if (cursor != null && cursor.moveToFirst()) {
            val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)
            val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)
            val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)
            val numberOfSongsColumn =
                cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            val firstYearColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR)
            val lastYearColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)
            do {
                val artist = cursor.getString(artistColumn)
                val albumResult = cursor.getString(albumColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val numberSongs = cursor.getInt(numberOfSongsColumn)
                val firstYear = cursor.getString(firstYearColumn)
                val lastYear = cursor.getString(lastYearColumn)
                val albumList = AlbumList(
                    albumId,
                    albumResult ?: "Unknown Album",
                    artist ?: "Unknown",
                    firstYear ?: "",
                    lastYear ?: "",
                    numberSongs,
                )
                playList[albumId] = albumList
            } while (cursor.moveToNext())
        }
        list.putAll(playList)
        cursor?.close()
    }

    fun searchAlbumByName(context: Context, name: String): ArrayList<AlbumList> {
        val list = ArrayList<AlbumList>()
        val projection = arrayOf(
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ALBUM_ID,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST,
            MediaStore.Audio.Albums.FIRST_YEAR,
            MediaStore.Audio.Albums.LAST_YEAR,
        )

        val selection = "${MediaStore.Audio.Albums.ALBUM} LIKE ?"
        val selectionArgs = arrayOf("%$name%")
        val contentResolver: ContentResolver = context.contentResolver
        contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)
                val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)
                val numberOfSongsColumn =
                    cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
                val firstYearColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR)
                val lastYearColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)
                do {
                    val artist = cursor.getString(artistColumn)
                    val albumResult = cursor.getString(albumColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val numberSongs = cursor.getInt(numberOfSongsColumn)
                    val firstYear = cursor.getString(firstYearColumn)
                    val lastYear = cursor.getString(lastYearColumn)
                    val albumList = AlbumList(
                        albumId,
                        albumResult,
                        artist,
                        firstYear ?: "",
                        lastYear ?: "",
                        numberSongs,
                    )
                    list.add(albumList)
                } while (cursor.moveToNext())
            }
        }

        return list
    }

    fun getAlbumsByGenre(
        context: Context,
        genreId: Uri,
        albumsHashMap: HashMap<Long, AlbumList>,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): ArrayList<AlbumList> {
        // Define the columns to retrieve from the media store for the number of tracks
        val trackProjection = arrayOf(
            MediaStore.Audio.Albums.ALBUM_ID,
        )

        // Create a cursor to query the media store for tracks in the genre
        val trackCursor = context.contentResolver.query(
            genreId,
            trackProjection,
            selection,
            selectionArgs,
            sortOrder
        )

        // Process the cursor and count the number of tracks
        val list = ArrayList<AlbumList>()
        if (trackCursor != null && trackCursor.moveToFirst()) {
            val idColumn = trackCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)
            do {
                val trackId: Long = trackCursor.getLong(idColumn)
                albumsHashMap[trackId]?.let {
                    if (!list.contains(it)) {
                        list.add(it)
                    }
                }
            } while (trackCursor.moveToNext())
        }
        trackCursor?.close()
        return list
    }

    fun getAlbumsByArtist(
        context: Context,
        genreId: Uri,
        albumsHashMap: HashMap<Long, AlbumList>,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): ArrayList<AlbumList> {
        // Define the columns to retrieve from the media store for the number of tracks
        val trackProjection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST
        )

        // Create a cursor to query the media store for tracks in the genre
        val trackCursor = context.contentResolver.query(
            genreId,
            trackProjection,
            selection,
            selectionArgs,
            sortOrder
        )

        // Process the cursor and count the number of tracks
        val list = ArrayList<AlbumList>()
        if (trackCursor != null && trackCursor.moveToFirst()) {
            val idColumn = trackCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            do {
                val trackId: Long = trackCursor.getLong(idColumn)
                albumsHashMap[trackId]?.let {
                    if (!list.contains(it)) {
                        list.add(it)
                    }
                }
            } while (trackCursor.moveToNext())
        }
        trackCursor?.close()
        return list
    }
}