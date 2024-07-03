package com.ztftrue.music.utils.trackManager

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.OptIn
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MainActivity
import com.ztftrue.music.R
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.OperateTypeInActivity
import com.ztftrue.music.utils.model.MusicPlayList
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.ArrayList

@Suppress("DEPRECATION")
@OptIn(UnstableApi::class)
object PlaylistManager {
    fun getPlaylists(
        context: Context,
        list: LinkedHashMap<Long, MusicPlayList>,
        tracksHashMap: LinkedHashMap<Long, MusicItem>,
        result: MediaBrowserServiceCompat.Result<Bundle>?,
        sortOrder: String
    ) {
        val map: HashMap<Long, ArrayList<MusicItem>> = HashMap()
        val playListProjection = arrayOf(
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME,
        )
        val musicResolver = context.contentResolver
        val cursor = musicResolver.query(
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            playListProjection,
            null,
            null,
            sortOrder.ifBlank { null }
        )
        val playList = LinkedHashMap<Long, MusicPlayList>()
        if (cursor != null && cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndex(MediaStore.Audio.Playlists._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME)
            do {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val tracksUri = MediaStore.Audio.Playlists.Members.getContentUri(
                    "external",
                    id
                )
                musicResolver.notifyChange(tracksUri, null)
                val tracks =
                    getTracksByPlayListId(context, tracksUri, tracksHashMap, null, null, null)
                map[id] = tracks
                playList[id] = MusicPlayList(name, id, tracks.size)
            } while (cursor.moveToNext())
        }

        list.putAll(playList)
        cursor?.close()
        val bundle = Bundle()
        bundle.putParcelableArrayList("list", ArrayList(list.values))
        result?.sendResult(bundle)
    }

    fun getTracksByPlayListId(
        context: Context,
        playlistUri: Uri,
        tracksHashMap: LinkedHashMap<Long, MusicItem>,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): ArrayList<MusicItem> {
        // Define the columns to retrieve from the media store for the number of tracks
        val trackProjection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Playlists.Members.AUDIO_ID,
            MediaStore.Audio.Playlists.Members._ID,
        )
        // Create a cursor to query the media store for tracks in the genre
        val trackCursor = context.contentResolver.query(
            playlistUri,
            trackProjection,
            selection,
            selectionArgs,
            sortOrder?.ifBlank { null }
        )
        // Process the cursor and count the number of tracks
        val list = ArrayList<MusicItem>()
        if (trackCursor != null && trackCursor.moveToFirst()) {
            val idColumn =
                trackCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID)
//            val idMColumn =
//                trackCursor.getColumnIndex(MediaStore.Audio.Playlists.Members._ID)
            do {
                val trackId: Long = trackCursor.getLong(idColumn)
//                val trackMId: Long = trackCursor.getLong(idMColumn)
                tracksHashMap[trackId]?.let { list.add(it) }
            } while (trackCursor.moveToNext())
            trackCursor.close()
        }
        return list
    }

    // 创建播放列表
    fun createPlaylist(context: Context, playlistName: String?): Long {
        val resolver: ContentResolver = context.contentResolver
        val values = ContentValues()
        values.put(MediaStore.Audio.Playlists.NAME, playlistName)
        val playlistUri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values)
        if (playlistUri != null) {
            // 获取新创建的播放列表的ID
            val projection = arrayOf(MediaStore.Audio.Playlists._ID)
            val cursor = resolver.query(playlistUri, projection, null, null, null)
            cursor?.use {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID))
                }
            }
        }
        // 创建失败，返回 -1 表示失败
        return -1
    }


    // 将音乐添加到播放列表
    fun addMusicToPlaylist(context: Context, playlistId: Long, musicId: Long): Boolean {
        val resolver: ContentResolver = context.contentResolver
        val values = ContentValues()
        values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, getPlayOrder(context, playlistId))
        values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, musicId)
        var uri: Uri = MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        uri = ContentUris.withAppendedId(uri, playlistId)
        if (context.checkUriPermission(
                uri,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            resolver.insert(uri, values)
        } else {
            if (context is MainActivity) {
                val bundle = context.bundle
                bundle.putString("action", OperateTypeInActivity.InsertTrackToPlaylist.name)
                bundle.putParcelable("uri", uri)
                val contentValues = ArrayList<ContentValues>(1)
                contentValues.add(values)
                bundle.putParcelableArrayList("values", contentValues)
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
                    operateForQ(resolver, context, playlistId)
                }
            }
            return false
        }
        return true
    }

    // 将音乐添加到播放列表
    fun addMusicsToPlaylist(
        context: Context,
        playlistId: Long,
        musicIds: ArrayList<Long>
    ): Boolean {
        val resolver: ContentResolver = context.contentResolver
        val contentValues = ArrayList<ContentValues>(musicIds.size)
        var index = getPlayOrder(context, playlistId)
        for (musicId in musicIds) {
            val values = ContentValues()
            values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, index)
            values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, musicId)
            contentValues.add(values)
            index += 1
        }
        var uri: Uri = MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        uri = ContentUris.withAppendedId(uri, playlistId)
        if (context.checkUriPermission(
                uri,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            resolver.bulkInsert(uri, contentValues.toTypedArray<ContentValues>())
        } else {
            if (context is MainActivity) {
                val bundle = context.bundle
                bundle.putString("action", OperateTypeInActivity.InsertTrackToPlaylist.name)
                bundle.putParcelable("uri", uri)
                bundle.putParcelableArrayList("values", contentValues)
                try {
                    val pendingIntent = MediaStore.createWriteRequest(resolver, listOf(uri))
                    val intentSenderRequest: IntentSenderRequest =
                        IntentSenderRequest.Builder(pendingIntent.intentSender)
                            .setFlags(
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            .build()
                    context.modifyMediaLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    operateForQ(resolver, context, playlistId)
                }
            }
            return false
        }
        return true
    }

    // 获取当前播放列表的下一个播放顺序
    private fun getPlayOrder(context: Context, playlistId: Long): Int {
        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
        val projection = arrayOf(MediaStore.Audio.Playlists.Members.PLAY_ORDER)
        val resolver: ContentResolver = context.contentResolver
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            // moveToFirst and last sometimes get error
            if (cursor.moveToLast()) {
                return cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.PLAY_ORDER)) + 1
            }
        }
        return 0
    }


    // Method to remove a track from a playlist
    fun modifyTrackFromPlayList(
        context: Context,
        playlistId: Long,
        arrayList: ArrayList<MusicItem>,
        tracksPath: String
    ): String? {
        val contentResolver: ContentResolver = context.contentResolver
        val path = getPlayListPath(context, playlistId)
        if (path != null) {
            var uri: Uri =
                MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            uri = ContentUris.withAppendedId(uri, playlistId)
            if (context.checkUriPermission(
                    uri,
                    Process.myPid(),
                    Process.myUid(),
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                modifyTrackFromM3U(context, uri, path, arrayList, tracksPath)
                return path
            } else {
                if (context is MainActivity) {
                    val bundle = context.bundle
                    bundle.putString(
                        "action",
                        OperateTypeInActivity.ModifyTrackFromPlayList.name
                    )
                    bundle.putString("playListPath", path)
                    bundle.putString("uri", uri.toString())
                    bundle.putString("tracksPath", tracksPath)
                    bundle.putParcelableArrayList("list", arrayList)
                    try {
                        val pendingIntent =
                            MediaStore.createWriteRequest(contentResolver, listOf(uri))
                        val intentSenderRequest: IntentSenderRequest =
                            IntentSenderRequest.Builder(pendingIntent.intentSender)
                                .setFlags(
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                                .build()
                        context.modifyMediaLauncher.launch(intentSenderRequest)
                    } catch (e: Exception) {
                        operateForQ(contentResolver, context, playlistId)
                    }
                }
                return null
            }
        }
        return null
    }


    fun renamePlaylist(context: Context, playlistId: Long, newName: String?): Boolean {
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
                    operateForQ(resolver, context, playlistId)
                }
            }
            return false
        }
        return true
    }

    private fun operateForQ(resolver: ContentResolver, context: MainActivity, playlistId: Long) {
        try {
            val pendingIntent = MediaStore.createWriteRequest(
                resolver, listOf(
                    Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        playlistId.toString()
                    )
                )
            )
            val intentSenderRequest: IntentSenderRequest =
                IntentSenderRequest.Builder(pendingIntent.intentSender)
                    .setFlags(
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    .build()
            context.modifyMediaLauncher.launch(intentSenderRequest)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.cant_support_feature_tip), Toast.LENGTH_SHORT
            )
                .show()
            e.printStackTrace()
        }
    }

    fun deletePlaylist(context: Context, playlistId: Long): Boolean {
        val resolver: ContentResolver = context.contentResolver
        var uri: Uri = MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        uri = ContentUris.withAppendedId(uri, playlistId)
        if (context.checkUriPermission(
                uri,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            resolver.delete(uri, null, null)
        } else {
            if (context is MainActivity) {
                val bundle = (context).bundle
                bundle.putString("action", OperateTypeInActivity.DeletePlayList.name)
                bundle.putParcelable("uri", uri)
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(resolver, setOf(uri))
                    val intentSenderRequest: IntentSenderRequest =
                        IntentSenderRequest.Builder(pendingIntent.intentSender)
                            .setFlags(
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            .build()
                    (context).modifyMediaLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    operateForQ(resolver, context, playlistId)
                }
            }
            return false
        }
        return true
    }

    private fun getPlayListPath(
        context: Context,
        playlistId: Long
    ): String? {

        // Define the columns to retrieve from the media store for the number of tracks
        val playList = arrayOf(
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.DATA
        )
        val whereV = arrayOf(playlistId.toString())
        // Create a cursor to query the media store for tracks in the genre
        context.contentResolver.query(
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            playList,
            MediaStore.Audio.Playlists._ID + "=?",
            whereV,
            null
        )?.use {
            if (it.moveToFirst()) {
                val dataPlaylistColumn = it.getColumnIndex(MediaStore.Audio.Playlists.DATA)
                return it.getString(dataPlaylistColumn)
            }
        }
        return null
    }


    fun modifyTrackFromM3U(
        context: Context, uri: Uri, m3uPath: String,
        arrayList: ArrayList<MusicItem>,
        tracksPath: String
    ) {
        val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
        val file = (File(m3uPath).parent ?: "") + "/"
        val stringBu = StringBuilder("#EXTM3U").append("\n")
        val hashMap = HashMap<String, Boolean>()
        hashMap[tracksPath.replace(file, "")] = true
        arrayList.forEach {
            val p = it.path.replace(file, "")
            stringBu.append(p).append("\n")
            hashMap[p] = true
        }
        if (pfd != null) {
            val fileInputStream = FileInputStream(pfd.fileDescriptor)
            val reader = BufferedReader(InputStreamReader(fileInputStream))
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                if (line == "" || line == "#EXTM3U") {
                    continue
                }
                // save origin data of don't exist storage
                if (hashMap[line] == null) {
                    stringBu.append(line).append("\n")
                }
            }
            reader.close()
            fileInputStream.close()
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


    @Suppress("unused")
    fun getM3uFile(context: Context, filePath: String): Long? {
        val m3uUri = MediaStore.Files.getContentUri("external")

        // Define the columns you need in the projection

        // Define the columns you need in the projection
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.TITLE
        )

        // Define the selection criteria

        // Define the selection criteria
        val selection =
            MediaStore.Files.FileColumns.MIME_TYPE + "=?" + " AND " + MediaStore.Files.FileColumns.DATA + " = ?"
        val selectionArgs = arrayOf("audio/x-mpegurl", filePath)
        val contentResolver = context.contentResolver
        contentResolver.query(
            m3uUri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use {
//            val dataIndex = it.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            val idIndex = it.getColumnIndex(MediaStore.Files.FileColumns._ID)
            if (it.moveToFirst()) {
//                val filePath = it.getString(dataIndex)
                return it.getLong(idIndex)
            }
        }
        return null
    }
}