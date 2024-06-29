package com.ztftrue.music.utils.trackManager

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MainActivity
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.OperateTypeInActivity
import com.ztftrue.music.utils.model.FolderList


object TracksManager {

    fun getFolderList(
        context: Context,
        folderListLinkedHashMap: LinkedHashMap<Long, FolderList>,
        result: MediaBrowserServiceCompat.Result<Bundle>?,
        tracksHashMap: LinkedHashMap<Long, MusicItem>,
        sortOrder1: String,
        needTracks: Boolean = false,
        allTracksHashMap: LinkedHashMap<Long, MusicItem>?=null,
    ) {
        tracksHashMap.clear()
        folderListLinkedHashMap.clear()
        allTracksHashMap?.clear()
        val sharedPreferences = context.getSharedPreferences("scan_config", Context.MODE_PRIVATE)
        // -1 don't ignore any,0 ignore duration less than or equal 0s,
        val ignoreDuration = sharedPreferences.getLong("ignore_duration", 0)
        val ignoreFolders = sharedPreferences.getString("ignore_folders", "")
        val ignoreFoldersMap: List<Long> =
            if (ignoreFolders.isNullOrEmpty()) emptyList() else ignoreFolders.split(",")
                .map { it.toLong() }
        val sortOrder = sortOrder1.ifBlank {
            "${MediaStore.Audio.Media.TITLE} ASC"
        }
        // Build the selection clause to exclude the folders by their IDs
        val selectionBuilder = StringBuilder()
        val selectionArgs = mutableListOf<String>()
        selectionBuilder.append("  title != ''")
        if (ignoreDuration >= 0) {
            if (selectionBuilder.isNotEmpty()) {
                selectionBuilder.append(" AND ")
            }
            selectionBuilder.append("${MediaStore.Audio.Media.DURATION} > ?")
            selectionArgs.add(ignoreDuration.toString())
        }
        if (ignoreFoldersMap.isNotEmpty()) {
            ignoreFoldersMap.forEach { folderId ->
                if (selectionBuilder.isNotEmpty()) {
                    selectionBuilder.append(" AND ")
                }
                selectionBuilder.append("${MediaStore.Audio.Media.BUCKET_ID} != ?")
                selectionArgs.add(folderId.toString())
            }
        }

        val selection = selectionBuilder.toString()
        val musicResolver = context.contentResolver
        val cursor = musicResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            trackMediaProjection, selection, selectionArgs.toTypedArray(), sortOrder
        )
        val mapFolder = LinkedHashMap<Long, FolderList>()
        val map: HashMap<Long, LinkedHashMap<Long, MusicItem>> = HashMap()
        if (cursor != null && cursor.moveToFirst()) {
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID)
            val bucketNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
            val iDColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val displayNameColumn =
                cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val isMusicColumn = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)
            val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val artistIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
            val genreColumn =
                cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
            val genreIdColumn =
                cursor.getColumnIndex(MediaStore.Audio.Media.GENRE_ID)
            val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
//            val discNumberColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISC_NUMBER)
            val songNumberColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
            do {
                val musicID = cursor.getLong(iDColumn)
                val folderId = cursor.getLong(bucketIdColumn)
                val folderName = cursor.getString(bucketNameColumn)
                val path = cursor.getString(dataColumn)
                val displayName = cursor.getString(displayNameColumn)
                val thisTitle = cursor.getString(titleColumn)
                val thisArtist = cursor.getString(artistColumn)
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val artistId = cursor.getLong(artistIdColumn)
                val album = cursor.getString(albumColumn)
                val isMusic = cursor.getInt(isMusicColumn) != 0

                val genre = if (genreColumn != null) {
                    cursor.getString(genreColumn) ?: ""
                } else {
                    ""
                }
                val genreId = if (genreIdColumn != null) cursor.getLong(genreIdColumn) else -1
                val year = cursor.getInt(yearColumn)
//                val discNumber = cursor.getInt(discNumberColumn)
                val songNumber = cursor.getInt(songNumberColumn)
                val musicItem = MusicItem(
                    null,
                    musicID,
                    thisTitle,
                    path,
                    duration,
                    displayName,
                    album,
                    albumId,
                    thisArtist,
                    artistId,
                    genre,
                    genreId,
                    year,
//                    discNumber,
                    songNumber
                )
                // For songs
                if (isMusic) {
                    tracksHashMap[musicID] = musicItem
                }
                // For not songs, example RingTones
                allTracksHashMap?.set(musicID, musicItem)
                map.getOrPut(folderId) {
                    LinkedHashMap()
                }[musicID] = musicItem
                mapFolder.putIfAbsent(
                    folderId, FolderList(
                        folderName ?: "/",
                        folderId,
                        map[folderId]?.size ?: 0,
                    )
                )
            } while (cursor.moveToNext())
        }

        mapFolder.forEach { it.value.trackNumber = map[it.key]?.size ?: 0 }
        folderListLinkedHashMap.clear()
        folderListLinkedHashMap.putAll(mapFolder)
        cursor?.close()
        if (result == null) return
        val bundle = Bundle()
        if (needTracks) {
            bundle.putParcelableArrayList("songsList", ArrayList(tracksHashMap.values))
        } else {
            bundle.putParcelableArrayList("songsList", ArrayList(folderListLinkedHashMap.values))
        }
        result.sendResult(bundle)
    }


    fun getTracksById(
        context: Context,
        uri: Uri,
        tracksHashMap: LinkedHashMap<Long, MusicItem>,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder1: String?,
    ): ArrayList<MusicItem> {
        // Define the columns to retrieve from the media store for the number of tracks
        val trackProjection = arrayOf(
            MediaStore.Audio.Media._ID,
        )
        val sortOrder=sortOrder1?.ifBlank {  "${MediaStore.Audio.Media.TITLE} ASC" }
        // Create a cursor to query the media store for tracks in the genre
        val trackCursor = context.contentResolver.query(
            uri,
            trackProjection,
            selection,
            selectionArgs,
            sortOrder
        )
        // Process the cursor and count the number of tracks
        val list = ArrayList<MusicItem>()
        if (trackCursor != null && trackCursor.moveToFirst()) {
            val idColumn = trackCursor.getColumnIndex(MediaStore.Audio.Media._ID)
            do {
                val trackId: Long = trackCursor.getLong(idColumn)
                tracksHashMap[trackId]?.let { list.add(it) }
            } while (trackCursor.moveToNext())
        }
        trackCursor?.close()
        return list
    }

    fun searchTracks(
        context: Context,
        tracksHashMap: LinkedHashMap<Long, MusicItem>,
        searchName: String?,
    ): ArrayList<MusicItem> {
        // Define the columns to retrieve from the media store for the number of tracks
        val trackProjection = arrayOf(
            MediaStore.Audio.Media._ID,
        )
        val selection = "${MediaStore.Audio.Media.TITLE} LIKE ?"
        val selectionArgs = arrayOf("%$searchName%")
        // Create a cursor to query the media store for tracks in the genre
        val trackCursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            trackProjection,
            selection,
            selectionArgs,
            null
        )
        // Process the cursor and count the number of tracks
        val list = ArrayList<MusicItem>()
        if (trackCursor != null && trackCursor.moveToFirst()) {
            val idColumn = trackCursor.getColumnIndex(MediaStore.Audio.Media._ID)
            do {
                val trackId: Long = trackCursor.getLong(idColumn)
                tracksHashMap[trackId]?.let { list.add(it) }
            } while (trackCursor.moveToNext())
        }
        trackCursor?.close()
        return list
    }

    @UnstableApi
    fun removeMusicById(context: Context, musicId: Long): Boolean {
        val contentResolver = context.contentResolver
        var uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        uri = ContentUris.withAppendedId(uri, musicId)
        if (context.checkUriPermission(
                uri,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            contentResolver.delete(uri, null, null)
            return true
        } else {
            if (context is MainActivity) {
                val bundle = context.bundle
                bundle.putString("action", OperateTypeInActivity.RemoveTrackFromStorage.name)
                bundle.putParcelable("uri", uri)
                bundle.putLong("musicId", musicId)
                val pendingIntent =
                    MediaStore.createWriteRequest(contentResolver, setOf(uri))
                val intentSenderRequest: IntentSenderRequest =
                    IntentSenderRequest.Builder(pendingIntent.intentSender)
                        .setFlags(
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        .build()
                context.modifyMediaLauncher.launch(intentSenderRequest)
            }
        }
        return false
    }

    private val trackMediaProjection =
        arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.GENRE_ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
//            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.BUCKET_ID,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Audio.Media.IS_MUSIC
        )

    @UnstableApi
    fun getMusicById(context: Context, musicId: Long): MusicItem? {
        val contentResolver = context.contentResolver

        var musicItem: MusicItem? = null
        val selectionArgs = arrayOf(musicId.toString())
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            trackMediaProjection,
            MediaStore.Audio.Media._ID + " =?",
            selectionArgs,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {
                val iDColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val artistIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
                val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val displayNameColumn =
                    cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
//                val discNumberColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISC_NUMBER)
                val songNumberColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)

                val genreColumn =
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                val genreIdColumn =
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE_ID)

                val musicID = cursor.getLong(iDColumn)
                val path = cursor.getString(dataColumn)
                val displayName = cursor.getString(displayNameColumn)
                val thisTitle = cursor.getString(titleColumn)
                val thisArtist = cursor.getString(artistColumn)
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val artistId = cursor.getLong(artistIdColumn)
                val album = cursor.getString(albumColumn)

                val genre = if (genreColumn != null) cursor.getString(genreColumn) ?: "" else ""
                val genreId = if (genreIdColumn != null) cursor.getLong(genreIdColumn) else -1
                val year = cursor.getInt(yearColumn)
//                val discNumber = cursor.getInt(discNumberColumn)
                val songNumber = cursor.getInt(songNumberColumn)

                musicItem = MusicItem(
                    null,
                    musicID,
                    thisTitle,
                    path,
                    duration,
                    displayName,
                    album,
                    albumId,
                    thisArtist,
                    artistId,
                    genre,
                    genreId,
                    year,
//                    discNumber,
                    songNumber
                )
            }

        }

        return musicItem
    }

}
