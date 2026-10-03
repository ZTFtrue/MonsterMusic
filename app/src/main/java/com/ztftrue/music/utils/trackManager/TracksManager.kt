package com.ztftrue.music.utils.trackManager

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.OptIn
import androidx.core.text.isDigitsOnly
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MainActivity
import com.ztftrue.music.R
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.OperateTypeInActivity
import com.ztftrue.music.utils.SharedPreferencesUtils
import com.ztftrue.music.utils.SortUtils
import com.ztftrue.music.utils.model.FolderList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


object TracksManager {

    fun getFolderList(
        context: Context,
        folderListLinkedHashMap: LinkedHashMap<Long, FolderList>,
        tracksHashMap: LinkedHashMap<Long, MusicItem>,
        sortOrder1: String,
        allTracksHashMap: LinkedHashMap<Long, MusicItem>? = null,
        foldersListTracksHashMap: HashMap<Long, LinkedHashMap<Long, MusicItem>>? = null,
    ) {
        tracksHashMap.clear()
        folderListLinkedHashMap.clear()
        allTracksHashMap?.clear()
        val sharedPreferences = context.getSharedPreferences("scan_config", Context.MODE_PRIVATE)
        // -1 don't ignore any,0 ignore duration less than or equal 0s,
        val ignoreDuration = SharedPreferencesUtils.getIgnoreDuration(context)
        val ignoreFolders = sharedPreferences.getString("ignore_folders", "")
        val ignoreFoldersMap: List<Long> =
            if (ignoreFolders.isNullOrEmpty()) emptyList() else ignoreFolders.split(",")
                .map { it.toLong() }
        val sortOrder = sortOrder1.ifBlank {
            "${MediaStore.Audio.Media.TITLE} ASC"
        }
        val scanMode = SharedPreferencesUtils.getScanMode(context)
        val whitelistFolders = SharedPreferencesUtils.getWhitelistFolders(context)
        val blacklistFolders = SharedPreferencesUtils.getBlacklistFolders(context)
        val isWhitelistMode = scanMode == SharedPreferencesUtils.SCAN_MODE_WHITELIST
        val normalizedWhitelistPaths = whitelistFolders.map { it.path.trimEnd('/') }.filter { it.isNotEmpty() }
        val normalizedBlacklistPaths = blacklistFolders.map { it.path.trimEnd('/') }.filter { it.isNotEmpty() }

        // Build the selection clause to exclude or include the folders
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
        if (isWhitelistMode) {
            if (normalizedWhitelistPaths.isEmpty()) {
                // In whitelist mode with no folders added, return empty list immediately
                return
            }
            if (selectionBuilder.isNotEmpty()) {
                selectionBuilder.append(" AND ")
            }
            selectionBuilder.append("(")
            normalizedWhitelistPaths.forEachIndexed { index, path ->
                if (index > 0) selectionBuilder.append(" OR ")
                selectionBuilder.append("${MediaStore.Audio.Media.DATA} LIKE ?")
                selectionArgs.add("$path/%")
            }
            selectionBuilder.append(")")
        } else {
            // Blacklist mode
            if (normalizedBlacklistPaths.isNotEmpty()) {
                normalizedBlacklistPaths.forEach { path ->
                    if (selectionBuilder.isNotEmpty()) {
                        selectionBuilder.append(" AND ")
                    }
                    selectionBuilder.append("${MediaStore.Audio.Media.DATA} NOT LIKE ?")
                    selectionArgs.add("$path/%")
                }
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
        }

        val selection = selectionBuilder.toString()
        val musicResolver = context.contentResolver
        val mapFolder = LinkedHashMap<Long, FolderList>()
        val map: HashMap<Long, LinkedHashMap<Long, MusicItem>> = HashMap()
        musicResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            trackMediaProjection, selection, selectionArgs.toTypedArray(), sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
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
                    val path = cursor.getString(dataColumn) ?: ""
                    if (isWhitelistMode) {
                        val inWhitelist = normalizedWhitelistPaths.any { folderPath ->
                            path.startsWith("$folderPath/", ignoreCase = true) ||
                            path.substringBeforeLast('/', "").equals(folderPath, ignoreCase = true)
                        }
                        if (!inWhitelist) {
                            continue
                        }
                    } else {
                        val inBlacklist = normalizedBlacklistPaths.any { folderPath ->
                            path.startsWith("$folderPath/", ignoreCase = true) ||
                            path.substringBeforeLast('/', "").equals(folderPath, ignoreCase = true)
                        }
                        if (inBlacklist || ignoreFoldersMap.contains(folderId)) {
                            continue
                        }
                    }
                    val displayName = cursor.getString(displayNameColumn) ?: "Unknown"
                    val thisTitle = cursor.getString(titleColumn) ?: "Unknown Title"
                    val thisArtist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val duration = cursor.getLong(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val artistId = cursor.getLong(artistIdColumn)
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val isMusic = cursor.getInt(isMusicColumn) != 0
                    val genre = cursor.getString(genreColumn) ?: "Unknown genre"
                    val genreId = cursor.getLong(genreIdColumn)
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
                            path = path.substringBeforeLast('/', ""),
                            name = folderName ?: "/",
                            id = folderId,
                            trackNumber = map[folderId]?.size ?: 0,
                        )
                    )
                } while (cursor.moveToNext())
            }
        }

        val videoSupportEnable = SharedPreferencesUtils.getVideoSupportEnable(context)
        if (videoSupportEnable) {
            val hasVideoPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_MEDIA_VIDEO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            if (hasVideoPermission) {
                val videoSelectionBuilder = StringBuilder()
                val videoSelectionArgs = mutableListOf<String>()
                videoSelectionBuilder.append("  title != ''")
                if (ignoreDuration >= 0) {
                    videoSelectionBuilder.append(" AND ${MediaStore.Video.Media.DURATION} > ?")
                    videoSelectionArgs.add(ignoreDuration.toString())
                }
                if (isWhitelistMode) {
                    if (normalizedWhitelistPaths.isNotEmpty()) {
                        videoSelectionBuilder.append(" AND (")
                        normalizedWhitelistPaths.forEachIndexed { index, path ->
                            if (index > 0) videoSelectionBuilder.append(" OR ")
                            videoSelectionBuilder.append("${MediaStore.Video.Media.DATA} LIKE ?")
                            videoSelectionArgs.add("$path/%")
                        }
                        videoSelectionBuilder.append(")")
                    }
                } else {
                    if (normalizedBlacklistPaths.isNotEmpty()) {
                        normalizedBlacklistPaths.forEach { path ->
                            videoSelectionBuilder.append(" AND ${MediaStore.Video.Media.DATA} NOT LIKE ?")
                            videoSelectionArgs.add("$path/%")
                        }
                    }
                    if (ignoreFoldersMap.isNotEmpty()) {
                        ignoreFoldersMap.forEach { folderId ->
                            videoSelectionBuilder.append(" AND ${MediaStore.Video.Media.BUCKET_ID} != ?")
                            videoSelectionArgs.add(folderId.toString())
                        }
                    }
                }

                val videoProjection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.TITLE,
                    MediaStore.Video.Media.ARTIST,
                    MediaStore.Video.Media.ALBUM,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.BUCKET_ID,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Video.Media.DATE_ADDED
                )

                try {
                    musicResolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        videoProjection,
                        videoSelectionBuilder.toString(),
                        videoSelectionArgs.toTypedArray(),
                        "${MediaStore.Video.Media.TITLE} ASC"
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
                            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                            val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                            val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                            val artistCol = cursor.getColumnIndex(MediaStore.Video.Media.ARTIST)
                            val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                            val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                            val displayNameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                            val albumCol = cursor.getColumnIndex(MediaStore.Video.Media.ALBUM)

                            do {
                                val rawVideoID = cursor.getLong(idCol)
                                val folderId = cursor.getLong(bucketIdCol)
                                val folderName = cursor.getString(bucketNameCol)
                                val path = cursor.getString(dataCol) ?: ""

                                if (isWhitelistMode) {
                                    val inWhitelist = normalizedWhitelistPaths.any { folderPath ->
                                        path.startsWith("$folderPath/", ignoreCase = true) ||
                                        path.substringBeforeLast('/', "").equals(folderPath, ignoreCase = true)
                                    }
                                    if (!inWhitelist) continue
                                } else {
                                    val inBlacklist = normalizedBlacklistPaths.any { folderPath ->
                                        path.startsWith("$folderPath/", ignoreCase = true) ||
                                        path.substringBeforeLast('/', "").equals(folderPath, ignoreCase = true)
                                    }
                                    if (inBlacklist || ignoreFoldersMap.contains(folderId)) continue
                                }

                                val displayName = cursor.getString(displayNameCol) ?: "Unknown"
                                val thisTitle = cursor.getString(titleCol) ?: displayName
                                val thisArtist = cursor.getString(artistCol) ?: "<video>"
                                val duration = cursor.getLong(durationCol)
                                val album = cursor.getString(albumCol) ?: "<video>"

                                // Bijective negative ID mapping to guarantee zero ID collision with audio
                                val musicID = -(rawVideoID + 1L)

                                val musicItem = MusicItem(
                                    tableId = null,
                                    id = musicID,
                                    name = thisTitle,
                                    path = path,
                                    duration = duration,
                                    displayName = displayName,
                                    album = album,
                                    albumId = 0L,
                                    artist = thisArtist,
                                    artistId = 0L,
                                    genre = "Video",
                                    genreId = 0L,
                                    year = 0,
                                    songNumber = 0
                                )

                                tracksHashMap[musicID] = musicItem
                                allTracksHashMap?.set(musicID, musicItem)
                                map.getOrPut(folderId) {
                                    LinkedHashMap()
                                }[musicID] = musicItem
                                mapFolder.putIfAbsent(
                                    folderId, FolderList(
                                        path = path.substringBeforeLast('/', ""),
                                        name = folderName ?: "/",
                                        id = folderId,
                                        trackNumber = map[folderId]?.size ?: 0,
                                    )
                                )
                            } while (cursor.moveToNext())
                        }
                    }
                } catch (e: Exception) {
                    Log.w("TracksManager", "Error querying MediaStore.Video.Media", e)
                }
            }
        }

        mapFolder.forEach { it.value.trackNumber = map[it.key]?.size ?: 0 }
        SortUtils.sortTracksMap(tracksHashMap, sortOrder1)
        SortUtils.sortFolders(mapFolder, ascending = true)
        folderListLinkedHashMap.clear()
        folderListLinkedHashMap.putAll(mapFolder)
        foldersListTracksHashMap?.clear()
        map.forEach { (fId, fTracks) ->
            SortUtils.sortTracksMap(fTracks, sortOrder1)
            foldersListTracksHashMap?.put(fId, fTracks)
        }
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
        val sortOrder = sortOrder1?.ifBlank { "${MediaStore.Audio.Media.TITLE} ASC" }
        val list = ArrayList<MusicItem>()
        context.contentResolver.query(
            uri,
            trackProjection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { trackCursor ->
            if (trackCursor.moveToFirst()) {
                val idColumn = trackCursor.getColumnIndex(MediaStore.Audio.Media._ID)
                do {
                    val trackId: Long = trackCursor.getLong(idColumn)
                    tracksHashMap[trackId]?.let { list.add(it) }
                } while (trackCursor.moveToNext())
            }
        }
        SortUtils.sortTracks(list, sortOrder1)
        return list
    }

    fun searchTracks(
        context: Context,
        tracksHashMap: LinkedHashMap<Long, MusicItem>,
        searchName: String?,
    ): ArrayList<MusicItem> {
        if (searchName.isNullOrBlank()) {
            return ArrayList()
        }
        val list = ArrayList<MusicItem>()
        val seenIds = HashSet<Long>()
        // Define the columns to retrieve from the media store for the number of tracks
        val trackProjection = arrayOf(
            MediaStore.Audio.Media._ID,
        )
        val selection = "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?"
        val selectionArgs = arrayOf("%$searchName%", "%$searchName%")
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            // Create a cursor to query the media store for tracks matching title or artist
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                trackProjection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { trackCursor ->
                if (trackCursor.moveToFirst()) {
                    val idColumn = trackCursor.getColumnIndex(MediaStore.Audio.Media._ID)
                    do {
                        val trackId: Long = trackCursor.getLong(idColumn)
                        tracksHashMap[trackId]?.let { item ->
                            if (seenIds.add(trackId)) {
                                list.add(item)
                            }
                        }
                    } while (trackCursor.moveToNext())
                }
            }
        } catch (_: Exception) {
        }

        if (SharedPreferencesUtils.getVideoSupportEnable(context)) {
            try {
                val videoSelection = "${MediaStore.Video.Media.TITLE} LIKE ? OR ${MediaStore.Video.Media.ARTIST} LIKE ?"
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Video.Media._ID),
                    videoSelection,
                    selectionArgs,
                    "${MediaStore.Video.Media.TITLE} ASC"
                )?.use { trackCursor ->
                    if (trackCursor.moveToFirst()) {
                        val idColumn = trackCursor.getColumnIndex(MediaStore.Video.Media._ID)
                        do {
                            val rawVideoId: Long = trackCursor.getLong(idColumn)
                            val videoMusicId = -(rawVideoId + 1L)
                            tracksHashMap[videoMusicId]?.let { item ->
                                if (seenIds.add(videoMusicId)) {
                                    list.add(item)
                                }
                            }
                        } while (trackCursor.moveToNext())
                    }
                }
            } catch (_: Exception) {
            }
        }

        // In-memory fallback/supplement to ensure non-ASCII / Unicode case-insensitivity
        // or tracks that might have been missed by MediaStore LIKE query
        val trimmed = searchName.trim()
        for (item in tracksHashMap.values) {
            if (!seenIds.contains(item.id)) {
                if (item.name.contains(trimmed, ignoreCase = true) ||
                    item.artist.contains(trimmed, ignoreCase = true)
                ) {
                    seenIds.add(item.id)
                    list.add(item)
                }
            }
        }
        SortUtils.sortTracks(list, null)
        return list
    }


    @UnstableApi
    fun removeMusicById(context: Context, musicId: Long): Boolean {
        val contentResolver = context.contentResolver
        val uri = if (musicId < 0) {
            val rawVideoId = (-musicId) - 1L
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, rawVideoId)
        } else {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId)
        }
        if (context.checkUriPermission(
                uri,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return try {
                val deleted = contentResolver.delete(uri, null, null) > 0
                if (!deleted) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, R.string.file_not_found_please_rescan, Toast.LENGTH_SHORT).show()
                    }
                }
                deleted
            } catch (e: Exception) {
                Log.e("TracksManager", "Failed to delete track $musicId", e)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, R.string.file_not_found_please_rescan, Toast.LENGTH_SHORT).show()
                }
                false
            }
        } else {
            if (context is MainActivity) {
                val bundle = context.bundle
                bundle.putString("action", OperateTypeInActivity.RemoveTrackFromStorage.name)
                bundle.putParcelable("uri", uri)
                bundle.putLong("musicId", musicId)
                try {
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
                } catch (e: Exception) {
                    Log.e("TracksManager", "Failed to create write request for track $musicId", e)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, R.string.file_not_found_please_rescan, Toast.LENGTH_SHORT).show()
                    }
                }
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

        if (musicId < 0) {
            val rawVideoId = (-musicId) - 1L
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.ARTIST,
                MediaStore.Video.Media.ALBUM,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
            )
            try {
                contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    videoProjection,
                    "${MediaStore.Video.Media._ID} = ?",
                    arrayOf(rawVideoId.toString()),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                        val artistCol = cursor.getColumnIndex(MediaStore.Video.Media.ARTIST)
                        val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                        val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                        val displayNameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                        val albumCol = cursor.getColumnIndex(MediaStore.Video.Media.ALBUM)

                        val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                        val displayName = if (displayNameCol != -1) cursor.getString(displayNameCol) ?: "Unknown" else "Unknown"
                        val thisTitle = if (titleCol != -1) cursor.getString(titleCol) ?: displayName else displayName
                        val thisArtist = if (artistCol != -1) cursor.getString(artistCol) ?: "<video>" else "<video>"
                        val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                        val album = if (albumCol != -1) cursor.getString(albumCol) ?: "<video>" else "<video>"

                        return MusicItem(
                            tableId = null,
                            id = musicId,
                            name = thisTitle,
                            path = path,
                            duration = duration,
                            displayName = displayName,
                            album = album,
                            albumId = 0L,
                            artist = thisArtist,
                            artistId = 0L,
                            genre = "Video",
                            genreId = 0L,
                            year = 0,
                            songNumber = 0
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("TracksManager", "Failed to get video by ID $musicId", e)
            }
            return null
        }

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
                val displayName = cursor.getString(displayNameColumn) ?: "Unknown"
                val thisTitle = cursor.getString(titleColumn) ?: "Unknown Title"
                val thisArtist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val artistId = cursor.getLong(artistIdColumn)
                val album = cursor.getString(albumColumn) ?: "Unknown Album"

                val genre = cursor.getString(genreColumn) ?: "Unknown genre"
                val genreId = cursor.getLong(genreIdColumn)
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

    @OptIn(UnstableApi::class)
    fun saveTrackInfo(
        context: Context,
        musicId: Long,
        musicPath: String,
        title: String?,
        album: String?,
        artist: String?,
        genre: String?,
        year: String?,
        bitmap: Bitmap?,
        lyrics: String?
    ): Boolean {
        val contentResolver = context.contentResolver
        val uri = if (musicId < 0) {
            val rawVideoId = (-musicId) - 1L
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, rawVideoId)
        } else {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId)
        }
        if (context.checkUriPermission(
                uri,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            editTrackInfo(
                context,
                musicId,
                musicPath,
                title,
                album,
                artist,
                genre,
                year,
                bitmap,
                lyrics
            )
            return true
        } else {
            if (context is MainActivity) {
                val bundle = context.bundle
                bundle.putString("action", OperateTypeInActivity.EditTrackInfo.name)
                bundle.putParcelable("uri", uri)
                bundle.putLong("musicId", musicId)
                try {
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
                } catch (e: Exception) {
                    Log.e("TracksManager", "Failed to create write request for track $musicId", e)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, R.string.file_not_found_please_rescan, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        return false
    }

    @OptIn(UnstableApi::class)
    fun requestEditPermission(
        context: Context,
        musicId: Long,
    ): Boolean {
        val contentResolver = context.contentResolver
        val uri = if (musicId < 0) {
            val rawVideoId = (-musicId) - 1L
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, rawVideoId)
        } else {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId)
        }
        if (context.checkUriPermission(
                uri,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        } else {
            if (context is MainActivity) {
                val bundle = context.bundle
                bundle.putString("action", OperateTypeInActivity.EditTrackInfo.name)
                bundle.putParcelable("uri", uri)
                bundle.putLong("musicId", musicId)
                try {
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
                } catch (e: Exception) {
                    Log.e("TracksManager", "Failed to create write request for track $musicId", e)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, R.string.file_not_found_please_rescan, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        return false
    }

    private fun editTrackInfo(
        context: Context,
        musicId: Long,
        musicPath: String,
        title: String?,
        album: String?,
        artist: String?,
        genre: String?,
        year: String?,
        bitmap: Bitmap?,
        lyrics: String?
    ) {
        runBlocking {
            awaitAll(
                async(Dispatchers.IO) {
                    saveEmbeddedCover(
                        musicId, musicPath, context, bitmap, lyrics, title,
                        album,
                        artist,
                        genre,
                        year
                    )
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(musicPath),
                        arrayOf("*/*"),
                        object : MediaScannerConnectionClient {
                            override fun onMediaScannerConnected() {}
                            override fun onScanCompleted(path: String, uri: Uri) {

                            }
                        })
                })
        }

    }

    private fun saveEmbeddedCover(
        musicId: Long,
        path: String,
        context: Context,
        bitmap: Bitmap? = null,
        lyrics: String? = null,
        title: String?,
        album: String?,
        artist: String?,
        genre: String?,
        year: String?,
    ) {
        var pfd: ParcelFileDescriptor? = null
        var inputStream: FileInputStream? = null
        var outputStream: FileOutputStream? = null
        var pfdWt: ParcelFileDescriptor? = null
        var cacheOut: FileInputStream? = null
        var fileOutputStream: FileOutputStream? = null
        var cacheFile: File? = null

        try {
            val uri: Uri = if (musicId < 0) {
                val rawVideoId = (-musicId) - 1L
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, rawVideoId)
            } else {
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId)
            }

            // 1. Read the existing file into a temporary cache
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd == null) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, R.string.file_not_found_please_rescan, Toast.LENGTH_SHORT).show()
                }
                return
            }
            inputStream = ParcelFileDescriptor.AutoCloseInputStream(pfd) as FileInputStream

            // Create a unique cache file name
            val fileName = path.substringAfterLast("/") // More robust way to get file name
            cacheFile = File(context.externalCacheDir, "temp_$fileName")
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            cacheFile.createNewFile()

            outputStream = FileOutputStream(cacheFile)
            inputStream.copyTo(outputStream)

            // 2. Modify the tags using Jaudiotagger
            if (!AudioFileIO.isSupported(cacheFile)) {
                return
            }
            val f = AudioFileIO.read(cacheFile)
            val tag: Tag = f.tagOrCreateAndSetDefault
            bitmap?.let {
                val byteArrayOutputStream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val imageData: ByteArray = byteArrayOutputStream.toByteArray()
                val artwork = ArtworkFactory.getNew()
                artwork.binaryData = imageData
                artwork.mimeType = "image/jpeg"
                tag.deleteArtworkField()
                tag.setField(artwork)
            }

            artist?.takeIf { it.isNotEmpty() }?.let { tag.setField(FieldKey.ARTIST, it) }
            lyrics?.let { tag.setField(FieldKey.LYRICS, it) }
            title?.let { tag.setField(FieldKey.TITLE, it) }
            album?.let { tag.setField(FieldKey.ALBUM, it) }
            genre?.let { tag.setField(FieldKey.GENRE, it) }
            year?.takeIf { it.isDigitsOnly() && it.toIntOrNull() != null }
                ?.let { tag.setField(FieldKey.YEAR, it) }

            f.commit() // Commit changes to the cache file

            // 3. Write the modified cache file back to the original location
            pfdWt = context.contentResolver.openFileDescriptor(uri, "wt")
            if (pfdWt == null) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, R.string.file_not_found_please_rescan, Toast.LENGTH_SHORT).show()
                }
                return
            }
            cacheOut = FileInputStream(cacheFile)
            fileOutputStream = FileOutputStream(pfdWt.fileDescriptor)

            // Using a buffer to copy for better memory management than reading all bytes at once
            val buffer = ByteArray(4096) // 4KB buffer
            var bytesRead: Int
            while (cacheOut.read(buffer).also { bytesRead = it } != -1) {
                fileOutputStream.write(buffer, 0, bytesRead)
            }
            fileOutputStream.flush()

        } catch (e: Exception) {
            e.printStackTrace()
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, R.string.file_not_found_please_rescan, Toast.LENGTH_SHORT).show()
            }
        } finally {
            // Close all streams and file descriptors in a finally block to ensure they are released
            try {
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                outputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                pfd?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                cacheOut?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                fileOutputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                pfdWt?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Delete the temporary cache file
            cacheFile?.delete()
        }
    }
}
