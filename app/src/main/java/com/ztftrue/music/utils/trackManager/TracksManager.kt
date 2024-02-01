package com.ztftrue.music.utils.trackManager

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.OptIn
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MainActivity
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.FolderList
import com.ztftrue.music.utils.OperateTypeInActivity


object TracksManager {

    fun getFolderList(
        context: Context,
        list: LinkedHashMap<Long, FolderList>,
        result: MediaBrowserServiceCompat.Result<Bundle>?,
        tracksHashMap: LinkedHashMap<Long, MusicItem>,
        map: HashMap<Long, LinkedHashMap<Long, MusicItem>>,
        needTrack: Boolean = false
    ) {


        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        val selection = "title != ''"

        val musicResolver = context.contentResolver
        val cursor = musicResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            trackMediaProjection, selection, null, sortOrder
        )
        val mapFolder = LinkedHashMap<Long, FolderList>()
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
            val genreColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
            } else {
                null
            }
            val genreIdColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.Audio.Media.GENRE_ID)
            } else {
                null
            }
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

                if (isMusic) {
                    tracksHashMap[musicID] = musicItem
                }
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
        list.clear()
        list.putAll(mapFolder)
        cursor?.close()
        if(result == null) return
        val bundle = Bundle()
        if (needTrack) {
            bundle.putParcelableArrayList("list", ArrayList(tracksHashMap.values))
        } else {
            bundle.putParcelableArrayList("list", ArrayList(list.values))
        }
        result.sendResult(bundle)
    }


    fun getTracksById(
        context: Context,
        genreId: Uri,
        tracksHashMap: LinkedHashMap<Long, MusicItem>,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): ArrayList<MusicItem> {
        // Define the columns to retrieve from the media store for the number of tracks
        val trackProjection = arrayOf(
            MediaStore.Audio.Media._ID,
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
        } else {
            val uri =
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId)
            contentResolver.delete(uri, null, null)
            return true
        }
    }

    private val trackMediaProjection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
    } else {
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
            MediaStore.Audio.Media.DISPLAY_NAME,
//            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.BUCKET_ID,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Audio.Media.IS_MUSIC
        )
    }

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

                val genreColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else {
                    null
                }
                val genreIdColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE_ID)
                } else {
                    null
                }

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

    // Define content provider URI
    private val TRACKS_CONTENT_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    @OptIn(UnstableApi::class)
    fun saveTrackInfo(
        context: Context,
        trackId: Long,
        title: String?,
        artist: String?,
        album: String?,
        genre: String?,
        year: String?,
    ): Boolean {

        val contentResolver = context.contentResolver
        val values = ContentValues()
// https://stackoverflow.com/questions/57804074/how-to-update-metadata-of-audio-file-in-android-q-media-store
        // Define the track URI using the track ID
        val trackUri = ContentUris.withAppendedId(TRACKS_CONTENT_URI, trackId)
        // Update the track information using the content resolver
//        contentResolver.update(trackUri, values, null, null)
        // Get the current list of track IDs for the playlist
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (context.checkUriPermission(
                    trackUri,
                    Process.myPid(),
                    Process.myUid(),
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val uri =
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)
                values.put(MediaStore.Audio.Media.IS_PENDING, 1)
                contentResolver.update(uri, values, null, null)
                values.clear()
                values.put(MediaStore.Audio.Media.TITLE, title)
                values.put(MediaStore.Audio.Media.ARTIST, artist)
                values.put(MediaStore.Audio.Media.ALBUM, album)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    values.put(MediaStore.Audio.Media.GENRE, genre)
                }
                if (!year.isNullOrEmpty()) {
                    values.put(MediaStore.Audio.Media.YEAR, year.toInt())
                }
                contentResolver.update(uri, values, null, null)
            } else {
                if (context is MainActivity) {
                    val bundle = context.bundle
                    bundle.putString(
                        "action",
                        OperateTypeInActivity.EditTrackInfo.name
                    )
                    val uri = MediaStore.Audio.Media.getContentUri("external", trackId)
                    bundle.putParcelable("uri", uri)
                    values.put(MediaStore.Audio.Media.TITLE, title)
                    values.put(MediaStore.Audio.Media.ARTIST, artist)
                    values.put(MediaStore.Audio.Media.ALBUM, album)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        values.put(MediaStore.Audio.Media.GENRE, genre)
                    }
                    if (!year.isNullOrEmpty()) {
                        values.put(MediaStore.Audio.Media.YEAR, year.toInt())
                    }
                    bundle.putParcelable("value", values)
                    bundle.putLong("id", trackId)
                    val pendingIntent =
                        MediaStore.createWriteRequest(contentResolver, setOf(trackUri))
                    val intentSenderRequest: IntentSenderRequest =
                        IntentSenderRequest.Builder(pendingIntent.intentSender)
                            .setFlags(
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            .build()
                    context.modifyMediaLauncher.launch(intentSenderRequest)
                }
                return false
            }
        } else {
            contentResolver.update(trackUri, values, null, null)
        }
        return true
    }
}
