package com.ztftrue.music.utils.trackManager

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Process
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.OptIn
import androidx.core.text.isDigitsOnly
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MainActivity
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.OperateTypeInActivity
import com.ztftrue.music.utils.SharedPreferencesUtils
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
                val path = cursor.getString(dataColumn) ?: ""
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
                        path = path.take(path.lastIndexOf("/")),
                        name = folderName ?: "/",
                        id = folderId,
                        trackNumber = map[folderId]?.size ?: 0,
                    )
                )
            } while (cursor.moveToNext())
        }

        mapFolder.forEach { it.value.trackNumber = map[it.key]?.size ?: 0 }
        folderListLinkedHashMap.clear()
        folderListLinkedHashMap.putAll(mapFolder)
        cursor?.close()
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
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        // Create a cursor to query the media store for tracks in the genre
        val trackCursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
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
        var uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        uri = ContentUris.withAppendedId(uri, musicId)
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

    @OptIn(UnstableApi::class)
    fun requestEditPermission(
        context: Context,
        musicId: Long,
    ): Boolean {
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
            return true
        } else {
            if (context is MainActivity) {
                val bundle = context.bundle
                bundle.putString("action", OperateTypeInActivity.EditTrackInfo.name)
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
        try {
            var uri: Uri =
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            uri = ContentUris.withAppendedId(uri, musicId)
            val pfd: ParcelFileDescriptor =
                context.contentResolver.openFileDescriptor(uri, "r") ?: return
            val inputStream = ParcelFileDescriptor.AutoCloseInputStream(pfd)
            val cacheFile = File(
                context.externalCacheDir,
                "temp_" + path.substring(path.lastIndexOf("/") + 1, path.length)
            )
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            cacheFile.createNewFile()
            // Create an output stream to the cache file
            val outputStream = FileOutputStream(cacheFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            pfd.close()
            val f = AudioFileIO.read(cacheFile)
            val tag: Tag = f.tag
            if (bitmap != null) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val imageData: ByteArray = byteArrayOutputStream.toByteArray()
                val artwork = ArtworkFactory.getNew()
                artwork.binaryData = imageData
                artwork.mimeType = "image/jpeg"
                tag.deleteArtworkField()
                tag.setField(artwork)
            }
            if (!artist.isNullOrEmpty()) {
                tag.setField(FieldKey.ARTIST, artist)
            }
            if (lyrics != null) {
                tag.setField(FieldKey.LYRICS, lyrics)
            }
            if (title != null) {
                tag.setField(FieldKey.TITLE, title)
            }
            if (album != null) {
                tag.setField(FieldKey.ALBUM, album)
            }
            if (genre != null) {
                tag.setField(FieldKey.GENRE, genre)
            }
            if (year != null && year != "" && year.isDigitsOnly() && (year.toIntOrNull() != null)) {
                tag.setField(FieldKey.YEAR, year)
            }
            f.commit()
            val pfdWt: ParcelFileDescriptor =
                context.contentResolver.openFileDescriptor(uri, "wt") ?: return
            val cacheOut = FileInputStream(
                File(
                    context.externalCacheDir,
                    "temp_" + path.substring(path.lastIndexOf("/") + 1, path.length)
                )
            )
            val fileOutputStream = FileOutputStream(pfdWt.fileDescriptor)
            val b = cacheOut.readBytes()
            fileOutputStream.write(b)
            fileOutputStream.flush()
            fileOutputStream.close()
            cacheOut.close()
            pfdWt.close()
            cacheFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
