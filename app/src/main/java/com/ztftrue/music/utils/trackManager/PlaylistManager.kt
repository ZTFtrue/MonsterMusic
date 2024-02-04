package com.ztftrue.music.utils.trackManager

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MainActivity
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.model.MusicPlayList
import com.ztftrue.music.utils.OperateTypeInActivity
import java.io.File


@Suppress("DEPRECATION")
@OptIn(UnstableApi::class)
object PlaylistManager {
    fun getPlaylists(
        context: Context,
        list: LinkedHashMap<Long, MusicPlayList>,
        map: HashMap<Long, ArrayList<MusicItem>>,
        tracksHashMap: LinkedHashMap<Long, MusicItem>,
        result: MediaBrowserServiceCompat.Result<Bundle>?
    ) {
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
            null
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
                val tracks = getTracksByPlayListId(context, tracksUri, tracksHashMap)
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
            null,
            null,
            null
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
        var uri: Uri?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            uri = MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
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
        } else {
            uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
            resolver.insert(ContentUris.withAppendedId(uri, playlistId), values)
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

        var uri: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val contentValues = ArrayList<ContentValues>(musicIds.size)
            var index = getPlayOrder(context, playlistId)
            for (musicId in musicIds) {
                val values = ContentValues()
                values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, index)
                values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, musicId)
                contentValues.add(values)
                index += 1
            }
            uri = MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
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
                    Log.i("addMusicsToPlaylist", uri.toString())
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
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            uri = MediaStore.Audio.Playlists.getContentUri("external")
            uri = ContentUris.withAppendedId(uri, playlistId)
            var index = getPlayOrder(context, playlistId)
            for (musicId in musicIds) {
                val values = ContentValues()
                values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, index)
                values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, musicId)
                try {
                    resolver.insert(
                        uri,
                        values
                    )
                } catch (e: Exception) {
                    if (e is RecoverableSecurityException) {
                        val intentSender = e.userAction.actionIntent.intentSender
                        try {
                            if (context is MainActivity) {
                                val contentValues = ArrayList<ContentValues>(musicIds.size)
                                contentValues.add(values)
                                val bundle = context.bundle
                                bundle.putString(
                                    "action",
                                    OperateTypeInActivity.InsertTrackToPlaylist.name
                                )
                                bundle.putParcelable("uri", uri)
                                bundle.putParcelableArrayList("values", contentValues)
                                val intentSenderRequest: IntentSenderRequest =
                                    IntentSenderRequest.Builder(intentSender)
                                        .setFlags(
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                        .build()
                                context.modifyMediaLauncher.launch(intentSenderRequest)
                            }
//                            context.startIntentSender(intentSender, null, 0, 0, 0)
                        } catch (sendIntentException: SendIntentException) {
                            sendIntentException.printStackTrace()
                        }
                    }
                }
                index += 1
            }
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
    fun removeTrackFromPlayList(
        context: Context,
        playlistId: Long,
        trackIndex: Int
    ): String? {
        val contentResolver: ContentResolver = context.contentResolver
        val path = getPlayListPath(context, playlistId)
        if (path != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                var playlistUriTMD =
                    MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                playlistUriTMD = ContentUris.withAppendedId(playlistUriTMD!!, playlistId)
                if (context.checkUriPermission(
                        playlistUriTMD,
                        Process.myPid(),
                        Process.myUid(),
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    removeTrackFromM3U(path, trackIndex)
                    return path
                } else {
                    if (context is MainActivity) {
                        val bundle = context.bundle
                        bundle.putString(
                            "action",
                            OperateTypeInActivity.RemoveTrackFromPlayList.name
                        )
                        bundle.putInt("trackIndex", trackIndex)
                        bundle.putString("playListPath", path)
                        try {
                            val pendingIntent =
                                MediaStore.createWriteRequest(
                                    contentResolver,
                                    setOf(Uri.fromFile(File(path)))
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
                            operateForQ(contentResolver, context, playlistId)
                        }
                    }
                    return null
                }
            } else {
                try {
                    if (playlistId != -1L) {
                        val membersUri =
                            MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
                        var memberId = -1L
                        contentResolver.query(
                            membersUri, arrayOf(MediaStore.Audio.Playlists.Members._ID),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToPosition(trackIndex)) {
                                val memberIdIndex =
                                    cursor.getColumnIndex(MediaStore.Audio.Playlists.Members._ID)
                                memberId =
                                    cursor.getLong(memberIdIndex)
                            }
                        }
                        val deleteUri = ContentUris.withAppendedId(membersUri, memberId)
                        contentResolver.delete(deleteUri, null, null)
                    }
                    var uri = MediaStore.Audio.Playlists.getContentUri("external")
                    uri = ContentUris.withAppendedId(uri, playlistId)
                    return uri.toString()
                } catch (e: Exception) {
                    if (e is RecoverableSecurityException) {
                        if (context is MainActivity) {
                            val bundle = context.bundle
                            bundle.putString(
                                "action",
                                OperateTypeInActivity.RemoveTrackFromPlayList.name
                            )
                            bundle.putInt("trackIndex", trackIndex)
                            bundle.putString("playListPath", path)
                            val intentSender = e.userAction.actionIntent.intentSender
                            val intentSenderRequest: IntentSenderRequest =
                                IntentSenderRequest.Builder(intentSender)
                                    .setFlags(
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                    .build()
                            context.modifyMediaLauncher.launch(intentSenderRequest)
                        }
                    }
                }
                return ""
            }
        }
        return null
    }


    fun renamePlaylist(context: Context, playlistId: Long, newName: String?): Boolean {
        val resolver: ContentResolver = context.contentResolver
        var uri: Uri?
        val values = ContentValues()
        values.put(MediaStore.Audio.Playlists.NAME, newName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            uri = MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
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
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
            resolver.update(uri, values, null, null)
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun operateForQ(resolver: ContentResolver, context: MainActivity, playlistId: Long) {
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
            Toast.makeText(context, "You system don't support this feature", Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        }
    }

    // 将音乐添加到播放列表
    fun deletePlaylist(context: Context, playlistId: Long): Boolean {
        val resolver: ContentResolver = context.contentResolver
        var uri: Uri?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            uri = MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
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
        } else {
            uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
            resolver.delete(uri, null, null)
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

    fun removeTrackFromM3U(filePath: String, trackIndex: Int) {
        /**
         *if need delete multiple tracks  use list to store will delete racks
         */
        var tIndex = trackIndex

        // Read the contents of the existing playlist
        val file = File(filePath)
        val playlistContent = StringBuilder()
        file.bufferedReader().useLines { lines ->
            lines.forEachIndexed { index, line ->
                if (index == 0 && line == "#EXTM3U") {
                    tIndex = trackIndex + 1
                }
                if (tIndex != index) {
                    playlistContent.append(line).append("\n")
                } else {
                    println("Track removed from playlist: $line")
                }
            }
        }
        // Write the updated content back to the playlist file
        file.bufferedWriter().use { writer ->
            writer.write(playlistContent.toString())
        }
        println("Playlist edited successfully.")
//            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
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