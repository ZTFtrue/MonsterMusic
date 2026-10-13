package com.ztftrue.music.utils.trackManager

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ztftrue.music.utils.SortUtils
import com.ztftrue.music.utils.model.AlbumList

object AlbumManager {
    fun getAlbumList(
        context: Context,
        list: LinkedHashMap<Long, AlbumList>,
        sortOrder1: String,
        needMerge: Boolean = false,
        mergedIdsMap: HashMap<Long, HashSet<Long>>? = null
    ) {
        val playListProjection = arrayOf(
            MediaStore.Audio.Albums._ID,
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
                val idColumn = cursor.getColumnIndex(MediaStore.Audio.Albums._ID)
                val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)
                val actualIdCol = if (idColumn != -1) idColumn else albumIdColumn
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)
                val numberOfSongsColumn =
                    cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
                val firstYearColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR)
                val lastYearColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)
                do {
                    val artist = cursor.getString(artistColumn)
                    val albumResult = cursor.getString(albumColumn)
                    val albumId = if (actualIdCol != -1) cursor.getLong(actualIdCol) else 0L
                    val numberSongs = cursor.getInt(numberOfSongsColumn)
                    val firstYear = cursor.getString(firstYearColumn)
                    val lastYear = cursor.getString(lastYearColumn)
                    if (needMerge) {
                        val albumName = albumResult?.trim() ?: "Unknown Album"
                        val artistName = artist?.trim() ?: "Unknown"
                        val uniqueKey = "${albumName.lowercase()}|${artistName.lowercase()}"
                        if (mergedMap.containsKey(uniqueKey)) {
                            val existingAlbum = mergedMap[uniqueKey]!!
                            existingAlbum.trackNumber += numberSongs
                            mergedIdsMap?.getOrPut(existingAlbum.id) { hashSetOf(existingAlbum.id) }?.add(albumId)
                            if (existingAlbum.firstYear.isBlank() || (!firstYear.isNullOrBlank() && firstYear < existingAlbum.firstYear)) {
                                existingAlbum.firstYear = firstYear ?: existingAlbum.firstYear
                            }
                            if (existingAlbum.lastYear.isBlank() || (!lastYear.isNullOrBlank() && lastYear > existingAlbum.lastYear)) {
                                existingAlbum.lastYear = lastYear ?: existingAlbum.lastYear
                            }
                        } else {
                            val albumList = AlbumList(
                                albumId, // We use the ID of the first part found (for Album Art)
                                albumName,
                                artistName,
                                firstYear ?: "",
                                lastYear ?: "",
                                numberSongs
                            )
                            mergedMap[uniqueKey] = albumList
                            mergedIdsMap?.getOrPut(albumId) { hashSetOf(albumId) }?.add(albumId)
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
        SortUtils.sortAlbums(list, sortOrder1)
    }

    fun searchAlbumByName(
        context: Context,
        name: String,
        needMerge: Boolean = false
    ): ArrayList<AlbumList> {
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
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
                val idColumn = cursor.getColumnIndex(MediaStore.Audio.Albums._ID)
                val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)
                val actualIdCol = if (idColumn != -1) idColumn else albumIdColumn
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)
                val numberOfSongsColumn =
                    cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
                val firstYearColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR)
                val lastYearColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)
                do {
                    val artist = cursor.getString(artistColumn)
                    val albumResult = cursor.getString(albumColumn)
                    val albumId = if (actualIdCol != -1) cursor.getLong(actualIdCol) else 0L
                    val numberSongs = cursor.getInt(numberOfSongsColumn)
                    val firstYear = cursor.getString(firstYearColumn)
                    val lastYear = cursor.getString(lastYearColumn)

                    if (needMerge) {
                        val albumName = albumResult?.trim() ?: "Unknown Album"
                        val artistName = artist?.trim() ?: "Unknown"
                        val uniqueKey = "${albumName.lowercase()}|${artistName.lowercase()}"
                        if (mergedMap.containsKey(uniqueKey)) {
                            val existingAlbum = mergedMap[uniqueKey]!!
                            existingAlbum.trackNumber += numberSongs
                            if (existingAlbum.firstYear.isBlank() || (!firstYear.isNullOrBlank() && firstYear < existingAlbum.firstYear)) {
                                existingAlbum.firstYear = firstYear ?: existingAlbum.firstYear
                            }
                            if (existingAlbum.lastYear.isBlank() || (!lastYear.isNullOrBlank() && lastYear > existingAlbum.lastYear)) {
                                existingAlbum.lastYear = lastYear ?: existingAlbum.lastYear
                            }
                        } else {
                            val albumList = AlbumList(
                                albumId, // We use the ID of the first part found (for Album Art)
                                albumName,
                                artistName,
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
        val res = if (needMerge) {
            ArrayList(mergedMap.values)
        } else {
            albumArrayList
        }
        SortUtils.sortAlbumsList(res)
        return res
    }

    fun getAlbumsByGenre(
        context: Context,
        genreId: Uri,
        albumsHashMap: HashMap<Long, AlbumList>,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
        mergedIdsMap: HashMap<Long, HashSet<Long>>? = null
    ): ArrayList<AlbumList> {
        // Define the columns to retrieve from the media store for the number of tracks
        val trackProjection = arrayOf(
            MediaStore.Audio.Albums.ALBUM_ID,
        )

        // Create a cursor to query the media store for tracks in the genre
        val list = ArrayList<AlbumList>()
        context.contentResolver.query(
            genreId,
            trackProjection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { trackCursor ->
            if (trackCursor.moveToFirst()) {
                val idColumn = trackCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)
                do {
                    val trackId: Long = trackCursor.getLong(idColumn)
                    val resolvedAlbum = albumsHashMap[trackId] ?: if (mergedIdsMap != null) {
                        albumsHashMap.values.firstOrNull { mergedIdsMap[it.id]?.contains(trackId) == true }
                    } else null
                    resolvedAlbum?.let {
                        if (!list.contains(it)) {
                            list.add(it)
                        }
                    }
                } while (trackCursor.moveToNext())
            }
        }
        return list
    }

    fun getAlbumsByArtist(
        context: Context,
        genreId: Uri,
        albumsHashMap: HashMap<Long, AlbumList>,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
        mergedIdsMap: HashMap<Long, HashSet<Long>>? = null
    ): ArrayList<AlbumList> {
        // Define the columns to retrieve from the media store for the number of tracks
        val trackProjection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST
        )

        // Create a cursor to query the media store for tracks in the genre
        val list = ArrayList<AlbumList>()
        context.contentResolver.query(
            genreId,
            trackProjection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { trackCursor ->
            if (trackCursor.moveToFirst()) {
                val idColumn = trackCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                do {
                    val trackId: Long = trackCursor.getLong(idColumn)
                    val resolvedAlbum = albumsHashMap[trackId] ?: if (mergedIdsMap != null) {
                        albumsHashMap.values.firstOrNull { mergedIdsMap[it.id]?.contains(trackId) == true }
                    } else null
                    resolvedAlbum?.let {
                        if (!list.contains(it)) {
                            list.add(it)
                        }
                    }
                } while (trackCursor.moveToNext())
            }
        }
        return list
    }


    fun getAlbumById(
        context: Context,
        albumId: Long,
    ): AlbumList? {
        val infoCursor = context.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM_ID,
                MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                MediaStore.Audio.Albums.FIRST_YEAR,
                MediaStore.Audio.Albums.LAST_YEAR
            ),
            "${MediaStore.Audio.Albums._ID} = ?",
            arrayOf(albumId.toString()),
            null
        )
        var albumList: AlbumList? = null
        infoCursor?.use {
            if (it.moveToFirst()) {
                val idColumn = it.getColumnIndex(MediaStore.Audio.Albums._ID)
                val albumIdColumn = it.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)
                val actualIdCol = if (idColumn != -1) idColumn else albumIdColumn
                val albumColumn = it.getColumnIndex(MediaStore.Audio.Albums.ALBUM)
                val artistColumn = it.getColumnIndex(MediaStore.Audio.Albums.ARTIST)
                val numberOfSongsColumn =
                    it.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
                val firstYearColumn = it.getColumnIndex(MediaStore.Audio.Albums.FIRST_YEAR)
                val lastYearColumn = it.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)
                val artist = it.getString(artistColumn)
                val albumResult = it.getString(albumColumn)
                val id = if (actualIdCol != -1) it.getLong(actualIdCol) else albumId
                val numberSongs = if (numberOfSongsColumn != -1) it.getInt(numberOfSongsColumn) else 0
                val firstYear = if (firstYearColumn != -1) it.getString(firstYearColumn) else ""
                val lastYear = if (lastYearColumn != -1) it.getString(lastYearColumn) else ""
                albumList = AlbumList(
                    id,
                    albumResult ?: "Unknown Album",
                    artist ?: "Unknown",
                    firstYear ?: "",
                    lastYear ?: "",
                    numberSongs
                )
            }
        }
        return albumList
    }
}