package com.ztftrue.music.utils.trackManager

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import com.ztftrue.music.utils.model.ArtistList

object ArtistManager {
    fun getArtistIdByName(context: Context, artistName: String): Long? {
        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST
        )
        val sortOrder = "${MediaStore.Audio.Artists.ARTIST} ASC"
        val selection = "${MediaStore.Audio.Artists.ARTIST} = ?"
        val selectionArgs = arrayOf(artistName)
        val contentResolver: ContentResolver = context.contentResolver
        contentResolver.query(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val artistIdIndex = cursor.getColumnIndex(MediaStore.Audio.Artists._ID)
                return cursor.getLong(artistIdIndex)
            }
        }
        return null
    }

    fun getArtistByName(context: Context, artistName: String): ArrayList<ArtistList> {
        val list = ArrayList<ArtistList>()
        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
        )

        val selection = "${MediaStore.Audio.Artists.ARTIST} LIKE ?"
        val selectionArgs = arrayOf("%$artistName%")
        val contentResolver: ContentResolver = context.contentResolver
        contentResolver.query(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val iDColumn = cursor.getColumnIndex(MediaStore.Audio.Artists._ID)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)
                val numberOfAlbumsColumn =
                    cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
                val numberOfTracksColumn =
                    cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
                do {
                    val id = cursor.getLong(iDColumn)
                    val artist = cursor.getString(artistColumn)
                    val numberOfTracks = cursor.getInt(numberOfTracksColumn)
                    val numberOfAlbums = cursor.getInt(numberOfAlbumsColumn)
                    val artistList = ArtistList(
                        id,
                        artist,
                        numberOfTracks,
                        numberOfAlbums
                    )
                    list.add(artistList)
                } while (cursor.moveToNext())
            }
        }
        return list
    }

    fun getArtistList(
        context: Context,
        list: LinkedHashMap<Long, ArtistList>,
        sortOrder1: String
    ) {
        val playListProjection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
        )
        val musicResolver = context.contentResolver
        val cursor = musicResolver.query(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            playListProjection,
            null,
            null,
            sortOrder1.ifBlank { null }
        )
        val playList = LinkedHashMap<Long, ArtistList>()
        if (cursor != null && cursor.moveToFirst()) {
            val iDColumn = cursor.getColumnIndex(MediaStore.Audio.Artists._ID)
            val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)
            val numberOfAlbumsColumn =
                cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
            val numberOfTracksColumn =
                cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
            do {
                val id = cursor.getLong(iDColumn)
                val artist = cursor.getString(artistColumn)
                val numberOfTracks = cursor.getInt(numberOfTracksColumn)
                val numberOfAlbums = cursor.getInt(numberOfAlbumsColumn)
                val artistList = ArtistList(
                    id,
                    artist ?: "Unknown Artist",
                    numberOfTracks,
                    numberOfAlbums
                )
                playList[id] = artistList
            } while (cursor.moveToNext())
        }
        list.putAll(playList)
        cursor?.close()
    }
}