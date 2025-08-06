package com.ztftrue.music.utils.trackManager

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.model.GenresList
import com.ztftrue.music.utils.PlayListType

object GenreManager {
    fun getGenresList(
        context: Context,
        list: LinkedHashMap<Long, GenresList>,
        tracksHashMap: LinkedHashMap<Long, MusicItem>,
        sortOrder1: String
    ) {
        val playListProjection = arrayOf(
            MediaStore.Audio.Genres.Members._ID,
            MediaStore.Audio.Genres.NAME,
        )
        val musicResolver = context.contentResolver
        val sortOrder =sortOrder1.ifBlank { "${MediaStore.Audio.Genres.NAME} ASC" }

        val cursor = musicResolver.query(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            playListProjection,
            null,
            null,
            sortOrder
        )
        val playList = LinkedHashMap<Long, GenresList>()
        if (cursor != null && cursor.moveToFirst()) {
            val iDColumn = cursor.getColumnIndex(MediaStore.Audio.Genres._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.Audio.Genres.NAME)
            do {
                val id = cursor.getLong(iDColumn)
                val name = cursor.getString(nameColumn)
                val trackUri = MediaStore.Audio.Genres.Members.getContentUri("external", id)
                val listT: ArrayList<MusicItem> =
                    TracksManager.getTracksById(context, trackUri, tracksHashMap, null, null, null)
                if (listT.size == 0) continue
                val albumList = GenresList(
                    id,
                    name ?: "unknown",
                    listT.size,
                    0,
                    PlayListType.Genres,
                )
                playList[id] = albumList
            } while (cursor.moveToNext())
        }
        list.putAll(playList)
        cursor?.close()
    }

}