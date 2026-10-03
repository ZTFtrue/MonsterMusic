package com.ztftrue.music.utils.trackManager

import android.content.Context
import android.provider.MediaStore
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.SortUtils
import com.ztftrue.music.utils.model.GenresList

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
        val sortOrder = sortOrder1.ifBlank { "${MediaStore.Audio.Genres.NAME} ASC" }

        val playList = LinkedHashMap<Long, GenresList>()
        try {
            musicResolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                playListProjection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val iDColumn = cursor.getColumnIndex(MediaStore.Audio.Genres._ID)
                    val nameColumn = cursor.getColumnIndex(MediaStore.Audio.Genres.NAME)
                    do {
                        val id = cursor.getLong(iDColumn)
                        val name = cursor.getString(nameColumn)
                        val trackUri = MediaStore.Audio.Genres.Members.getContentUri("external", id)
                        val listT: ArrayList<MusicItem> =
                            TracksManager.getTracksById(context, trackUri, tracksHashMap, null, null, null)
                        if (listT.isEmpty()) continue
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
            }
        } catch (_: Exception) {
        }

        // Fallback: If MediaStore returned 0 genres (e.g. MediaStore.Audio.Genres.Members deprecated on Android 10+),
        // group in-memory tracks by track.genre
        if (playList.isEmpty() && tracksHashMap.isNotEmpty()) {
            val genreGroups = tracksHashMap.values
                .filter { it.genre.isNotBlank() && it.genre != "Unknown genre" }
                .groupBy { it.genre }
            var pseudoId = 1L
            for ((genreName, items) in genreGroups) {
                val existingGenreId = items.firstOrNull { it.genreId > 0 }?.genreId ?: pseudoId++
                playList[existingGenreId] = GenresList(
                    existingGenreId,
                    genreName,
                    items.size,
                    0,
                    PlayListType.Genres
                )
            }
        }

        SortUtils.sortGenres(playList, sortOrder1)
        list.putAll(playList)
    }

}