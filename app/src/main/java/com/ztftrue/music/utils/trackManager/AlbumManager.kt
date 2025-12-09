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
        sortOrder1: String,
        needMerge: Boolean = false
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
        val mergedMap = LinkedHashMap<String, AlbumList>()
        val longMap = LinkedHashMap<Long, AlbumList>()
        cursor?.use {
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
                    if (needMerge) {
                        val uniqueKey = "${albumResult.trim()}|${artist.trim()}"
                        if (mergedMap.containsKey(uniqueKey)) {
                            val existingAlbum = mergedMap[uniqueKey]!!
                            existingAlbum.trackNumber += numberSongs
                        } else {
                            val albumList = AlbumList(
                                albumId, // We use the ID of the first part found (for Album Art)
                                albumResult ?: "Unknown Album",
                                artist ?: "Unknown",
                                firstYear ?: "",
                                lastYear ?: "",
                                numberSongs
                            )
                            mergedMap[uniqueKey] = albumList
                        }
                    } else {
                        val albumList = AlbumList(
                            albumId, // We use the ID of the first part found (for Album Art)
                            albumResult ?: "Unknown Album",
                            artist ?: "Unknown",
                            firstYear ?: "",
                            lastYear ?: "",
                            numberSongs
                        )
                        longMap[albumId] = albumList
                    }
                } while (cursor.moveToNext())
            }
        }
        if (needMerge) {
            val finalPlayList: Map<Long, AlbumList> = mergedMap.values.associateBy { it.id }
            list.putAll(finalPlayList)
        } else {
            list.putAll(longMap)
        }
    }

    fun searchAlbumByName(
        context: Context,
        name: String,
        needMerge: Boolean = false
    ): ArrayList<AlbumList> {
        val projection = arrayOf(
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ALBUM_ID,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST,
            MediaStore.Audio.Albums.FIRST_YEAR,
            MediaStore.Audio.Albums.LAST_YEAR,
        )
        val albumArrayList = ArrayList<AlbumList>()
        val mergedMap = LinkedHashMap<String, AlbumList>()
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

                    val uniqueKey = "${albumResult.trim()}|${artist.trim()}"
                    if (needMerge) {
                        if (mergedMap.containsKey(uniqueKey)) {
                            val existingAlbum = mergedMap[uniqueKey]!!
                            existingAlbum.trackNumber += numberSongs
                        } else {
                            val albumList = AlbumList(
                                albumId, // We use the ID of the first part found (for Album Art)
                                albumResult ?: "Unknown Album",
                                artist ?: "Unknown",
                                firstYear ?: "",
                                lastYear ?: "",
                                numberSongs
                            )
                            mergedMap[uniqueKey] = albumList
                        }
                    } else {
                        albumArrayList.add(
                            AlbumList(
                                albumId, // We use the ID of the first part found (for Album Art)
                                albumResult ?: "Unknown Album",
                                artist ?: "Unknown",
                                firstYear ?: "",
                                lastYear ?: "",
                                numberSongs
                            )
                        )
                    }

                } while (cursor.moveToNext())
            }
        }
        if (needMerge) {

            return ArrayList(mergedMap.values)
        } else {
            return albumArrayList
        }
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


    fun getAlbumById(
        context: Context,
        albumId: Long,
    ): AlbumList? {
        val infoCursor = context.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM_ID
            ),
            "${MediaStore.Audio.Albums._ID} = ?",
            arrayOf(albumId.toString()),
            null
        )
        var albumList: AlbumList? = null
        infoCursor?.use {
            if (it.moveToFirst()) {
                val albumColumn = it.getColumnIndex(MediaStore.Audio.Albums.ALBUM)
                val albumIdColumn = it.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)
                val artistColumn = it.getColumnIndex(MediaStore.Audio.Albums.ARTIST)
//                val numberOfSongsColumn =
//                    it.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
//                val firstYearColumn = it.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR)
//                val lastYearColumn = it.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)
                val artist = it.getString(artistColumn)
                val albumResult = it.getString(albumColumn)
                val albumId = it.getLong(albumIdColumn)
//                val numberSongs = it.getInt(numberOfSongsColumn)
//                val firstYear = it.getString(firstYearColumn)
//                val lastYear = it.getString(lastYearColumn)
                albumList = AlbumList(
                    albumId, // We use the ID of the first part found (for Album Art)
                    albumResult ?: "Unknown Album",
                    artist ?: "Unknown",
                    /*  firstYear ?:*/ "",
                    /*   lastYear ?:*/ "",
                    0
                )
            }
        }
        return albumList
    }
}