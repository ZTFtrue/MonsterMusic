package com.ztftrue.music.utils.trackManager

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.OptIn
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MainActivity
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.OperateTypeInActivity
import com.ztftrue.music.utils.model.MusicPlayList
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

@Suppress("UNUSED")
@OptIn(UnstableApi::class)
object PlaylistManager {
    fun getPlaylists(
        context: Context,
        list: LinkedHashMap<Long, MusicPlayList>,
        tracksHashMap: LinkedHashMap<Long, MusicItem>,
        result: MediaBrowserServiceCompat.Result<Bundle>?,
        sortFiled: String?,
        sortMethod: String?
    ): List<MusicPlayList> {
        val playlistFiles = mutableListOf<MusicPlayList>()
        val map: HashMap<Long, ArrayList<MusicItem>> = HashMap()
        val collection =
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA
        )
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.m3u' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.m3u8'"
        val selectionArgs = null
        val playList = LinkedHashMap<Long, MusicPlayList>()

        val contentResolver = context.contentResolver
        try {
            val cursor = contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                null
            )
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val pathColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val path = it.getString(pathColumn)
                    val file = File(path)
                    if (file.exists()) {
                        // 获取文件的 Content URI
                        val contentUri: Uri = ContentUris.withAppendedId(
                            MediaStore.Files.getContentUri("external"), id
                        )
//                        contentResolver.notifyChange(contentUri, null)
                        val tracks =
                            getTracksByPlayListId(
                                context,
                                contentUri,
                                file.parent!!,
                                tracksHashMap,
                                sortFiled,
                                sortMethod
                            )
                        map[id] = tracks
                        playList[id] =
                            MusicPlayList(name.substringBeforeLast("."), id, path, tracks.size)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlaylistFinder", "Error finding playlist files", e)
        }
        list.putAll(playList)
        val bundle = Bundle()
        bundle.putParcelableArrayList("list", ArrayList(list.values))
        result?.sendResult(bundle)
        return playlistFiles
    }


    fun getTracksByPlayListId(
        context: Context,
        playlistUri: Uri,
        playlistDir: String,
        tracksHashMap: LinkedHashMap<Long, MusicItem>,
        sortFiled: String?,
        sortMethod: String?
    ): ArrayList<MusicItem> {
        val list = arrayListOf<MusicItem>()
        val trackMapPath = LinkedHashMap<String, MusicItem>()
        tracksHashMap.values.forEach {
            trackMapPath[File(it.path).canonicalPath] = it
        }
        try {
            context.contentResolver.openInputStream(playlistUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.forEachLine { line ->
                        if (line.isNotBlank() && !line.startsWith("#")) {
                            val rawSongPath = line.trim()
                            val songFile = File(rawSongPath)
                            val absoluteSongPath = if (songFile.isAbsolute) {
                                songFile.canonicalPath
                            } else {
                                File(playlistDir, rawSongPath).canonicalPath
                            }
                            val foundSong = trackMapPath[absoluteSongPath]
                            if (foundSong != null) {
                                list.add(foundSong)
                            } else {
                                Log.w(
                                    "PlaylistMatcher",
                                    "Path not found after resolving to absolute: $absoluteSongPath (from raw: $rawSongPath)"
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlaylistParser", "Error reading playlist file: ${playlistUri}", e)
        }

        return sortSongs(list, sortFiled, sortMethod)
    }

    fun createPlaylist(
        context: Context,
        playlistName: String?,
        tracks: ArrayList<MusicItem>,
        removeDuplicate: Boolean
    ): Uri? {
        val fileNameWithExtension = "$playlistName.m3u"
        val contentResolver = context.contentResolver
        val playlistValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileNameWithExtension)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/x-mpegurl")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection =
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        var newPlaylistUri: Uri? = null
        try {
            newPlaylistUri = contentResolver.insert(collection, playlistValues)
            if (newPlaylistUri == null) {
                Log.e("PlaylistCreator", "Failed to create new MediaStore entry.")
                return null
            }
            var checkDuplicateMap = HashMap<String, Boolean>()
            contentResolver.openOutputStream(newPlaylistUri)?.use { outputStream ->
                outputStream.bufferedWriter().use { writer ->
                    writer.write("#EXTM3U\n")
                    for (item in tracks) {
                        if (removeDuplicate && checkDuplicateMap[item.path] == null) {
                            checkDuplicateMap.put(item.path, true)
                            writer.write("${item.path}\n")
                        } else {
                            writer.write("${item.path}\n")
                        }
                    }
                }
            }
            playlistValues.clear()
            playlistValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(newPlaylistUri, playlistValues, null, null)
            Log.i(
                "PlaylistCreator",
                "Playlist '$playlistName' created successfully at $newPlaylistUri"
            )
            return newPlaylistUri

        } catch (e: Exception) {
            Log.e("PlaylistCreator", "IOException while creating playlist", e)
            if (newPlaylistUri != null) {
                contentResolver.delete(newPlaylistUri, null, null)
            }
            return null
        }
    }

    fun addMusicsToPlaylist(
        context: Context,
        playlistId: Long,
        musicIds: ArrayList<MusicItem>,
        removeDuplicate: Boolean,
        permissionGranted: Boolean = false
    ): Boolean {
        if (musicIds.isEmpty()) {
            return true // 没有新歌，也算成功
        }
        val playlistUri = ContentUris.withAppendedId(
            MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            playlistId
        )
        val contentResolver: ContentResolver = context.contentResolver
        if (context.checkUriPermission(
                playlistUri,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED || permissionGranted
        ) {
            try {
                val existingPaths = if (removeDuplicate) {
                    mutableSetOf<String>()
                } else {
                    mutableListOf<String>()
                }
                contentResolver.openInputStream(playlistUri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.forEachLine { line ->
                            if (line.isNotBlank() && !line.startsWith("#")) {
                                existingPaths.add(line.trim())
                            }
                        }
                    }
                }
                existingPaths.addAll(musicIds.map { it.path })
                contentResolver.openOutputStream(playlistUri, "w")?.use { outputStream ->
                    outputStream.bufferedWriter().use { writer ->
                        writer.write("#EXTM3U\n")
                        existingPaths.forEach { path ->
                            writer.write("$path\n")
                        }
                    }
                }
                contentResolver.notifyChange(playlistUri, null)
                return true
            } catch (e: IOException) {
                return false
            } catch (e: SecurityException) {
                Log.e(
                    "PlaylistEditor",
                    "SecurityException: Do not have permission to write to $playlistUri",
                    e
                )
                return false
            }
        } else {
            if (context is MainActivity) {
                val bundle = context.bundle
                bundle.putString("action", OperateTypeInActivity.InsertTrackToPlaylist.name)
                bundle.putParcelable("uri", playlistUri)
                bundle.putLong("id", playlistId)
                bundle.putParcelableArrayList("values", musicIds)
                bundle.putBoolean("removeDuplicate", removeDuplicate)
                try {
                    val pendingIntent =
                        MediaStore.createWriteRequest(contentResolver, listOf(playlistUri))
                    val intentSenderRequest: IntentSenderRequest =
                        IntentSenderRequest.Builder(pendingIntent.intentSender)
                            .setFlags(
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            .build()
                    context.modifyMediaLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return false
        }
    }

    fun cleanDuplicateTrackFromPlayList(
        context: Context,
        playListId: Long,
        playListPath: String,
        tracks: List<MusicItem>
    ): Boolean {
        val list = arrayListOf<MusicItem>()
        val trackMapPath = LinkedHashMap<String, MusicItem>()
        tracks.forEach {
            trackMapPath[File(it.path).canonicalPath] = it
        }
        val hashMapAlreadyAdd = LinkedHashMap<Long, MusicItem>()
        try {
            val playlistUri: Uri = ContentUris.withAppendedId(
                MediaStore.Files.getContentUri("external"), playListId
            )
            val playListFile = File(playListPath)
            if (!playListFile.exists()) {
                return false
            }
            context.contentResolver.openInputStream(playlistUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.forEachLine { line ->
                        if (line.isNotBlank() && !line.startsWith("#")) {
                            val rawSongPath = line.trim()
                            val songFile = File(rawSongPath)
                            val absoluteSongPath = if (songFile.isAbsolute) {
                                songFile.canonicalPath // canonicalPath可以解析 ".." 等
                            } else {
                                File(playListFile.parent, rawSongPath).canonicalPath
                            }
                            val foundSong = trackMapPath[absoluteSongPath]
                            if (foundSong != null && hashMapAlreadyAdd.get(foundSong.id) == null) {
                                list.add(foundSong)
                                hashMapAlreadyAdd.put(foundSong.id, foundSong)
                            } else {
                                Log.w(
                                    "PlaylistMatcher",
                                    "Path not found after resolving to absolute: $absoluteSongPath (from raw: $rawSongPath)"
                                )
                            }
                        }
                    }
                }
            }
            return resortOrRemoveTrackFromPlayList(
                context,
                playListId,
                list, playListPath
            ) != null
        } catch (e: Exception) {
//            Log.e("PlaylistParser", "Error reading playlist file: ${playlistUri}", e)
        }
        return false

    }

    fun resortOrRemoveTrackFromPlayList(
        context: Context,
        playListId: Long,
        arrayList: ArrayList<MusicItem>,
        playListPath: String
    ): String? {
        val contentResolver: ContentResolver = context.contentResolver
        val playlistUri = ContentUris.withAppendedId(
            MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            playListId
        )
        if (context.checkUriPermission(
                playlistUri,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            resortOrRemoveTrackFromM3U(context, playlistUri, playListPath, arrayList)
            return playListPath
        } else {
            if (context is MainActivity) {
                val bundle = context.bundle
                bundle.putString(
                    "action",
                    OperateTypeInActivity.ModifyTrackFromPlayList.name
                )
                bundle.putString("playListPath", playListPath)
                bundle.putString("uri", playlistUri.toString())
                bundle.putParcelableArrayList("list", arrayList)
                try {
                    val pendingIntent =
                        MediaStore.createWriteRequest(contentResolver, listOf(playlistUri))
                    val intentSenderRequest: IntentSenderRequest =
                        IntentSenderRequest.Builder(pendingIntent.intentSender)
                            .setFlags(
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            .build()
                    context.modifyMediaLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return null
        }
    }

    fun renamePlaylist(context: Context, playlistId: Long, newName: String?): Boolean {
        if (newName.isNullOrEmpty() || newName.isBlank()) {
            return false
        }
        val resolver: ContentResolver = context.contentResolver
        val values = ContentValues()
        values.put(MediaStore.Audio.Playlists.NAME, newName)
        var uri: Uri = MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        uri = ContentUris.withAppendedId(uri, playlistId)
        if (context.checkUriPermission(
                uri,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            resolver.update(uri, values, null, null)
        } else {
            if (context is MainActivity) {
                val bundle = context.bundle
                bundle.putString("action", OperateTypeInActivity.RenamePlaylist.name)
                bundle.putParcelable("uri", uri)
                bundle.putParcelable("values", values)
                try {
                    val pendingIntent = MediaStore.createWriteRequest(resolver, setOf(uri))
                    val intentSenderRequest: IntentSenderRequest =
                        IntentSenderRequest.Builder(pendingIntent.intentSender)
                            .setFlags(
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            .build()
                    context.modifyMediaLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return false
        }
        return true
    }


    fun deletePlaylist(context: Context, playlistId: Long): Boolean {
        val resolver: ContentResolver = context.contentResolver
        val contentUri: Uri = ContentUris.withAppendedId(
            MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            playlistId
        )
        if (context.checkUriPermission(
                contentUri,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            resolver.delete(contentUri, null, null)
        } else {
            if (context is MainActivity) {
                val bundle = (context).bundle
                bundle.putString("action", OperateTypeInActivity.DeletePlayList.name)
                bundle.putParcelable("uri", contentUri)
                try {
                    val pendingIntent =
                        MediaStore.createDeleteRequest(resolver, setOf(contentUri))
                    val intentSenderRequest: IntentSenderRequest =
                        IntentSenderRequest.Builder(pendingIntent.intentSender)
                            .setFlags(
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            .build()
                    (context).modifyMediaLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return false
        }
        return true
    }

    fun resortOrRemoveTrackFromM3U(
        context: Context, uri: Uri, m3uPath: String,
        arrayList: ArrayList<MusicItem>,
    ) {
        val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
        val file = (File(m3uPath).parent ?: "") + "/"
        val stringBu = StringBuilder("#EXTM3U").append("\n")
        arrayList.forEach {
            val p = it.path.replace(file, "")
            stringBu.append(p).append("\n")
        }
        if (pfd != null) {
            val pfdWT = context.contentResolver.openFileDescriptor(uri, "wt")
            if (pfdWT != null) {
                val fileOutputStream = FileOutputStream(pfdWT.fileDescriptor)
                val a = stringBu.toString().toByteArray()
                fileOutputStream.write(a)
                fileOutputStream.flush()
                fileOutputStream.close()
                pfdWT.close()
            }
            pfd.close()
        }
    }

    fun sortSongs(
        list: ArrayList<MusicItem>,
        field: String?,
        order: String?
    ): ArrayList<MusicItem> {
        val comparator: Comparator<MusicItem> = when (field) {
            MediaStore.Audio.Media.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            MediaStore.Audio.Media.ALBUM -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.album }
            MediaStore.Audio.Media.ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
            MediaStore.Audio.Media.DURATION -> compareBy { it.duration }
            MediaStore.Audio.Media.YEAR -> compareBy { it.year }
            else -> {
                return list
            }
        }
        val sortedList = when (order?.uppercase()) {
            "ASC" -> list.sortedWith(comparator)
            "DESC" -> list.sortedWith(comparator.reversed())
            else -> return list
        }
        return ArrayList(sortedList)
    }

}