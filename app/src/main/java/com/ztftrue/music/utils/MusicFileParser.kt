package com.ztftrue.music.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.ztftrue.music.sqlData.model.MusicItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MusicFileParser {
    suspend fun parse(context: Context, uri: Uri): MusicItem= withContext(Dispatchers.IO)  {
        // 1. 尝试从 MediaStore 数据库查询 (这是获取 ArtistId, AlbumId 的唯一准确方法)
        val musicFromStore = queryMediaStore(context, uri)

        if (musicFromStore != null) {
            return@withContext musicFromStore
        }

        // 2. 如果数据库没查到 (例如是从文件管理器直接打开的未扫描文件)
        // 使用 MediaMetadataRetriever 从文件头读取 ID3 标签
        return@withContext parseFromMetadata(context, uri)
    }

    // --- 方法 A: 查 MediaStore 数据库 ---
    private fun queryMediaStore(context: Context, uri: Uri): MusicItem? {
        // 只有 content 协议且属于 media provider 才能查数据库
        if (uri.scheme != "content" || !uri.toString().contains("media")) return null

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA, // Path (在 Android 10+ 可能受限)
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            // Genre 需要单独查，这里为了性能通常忽略或单独处理，通常 MediaStore 主表没有 Genre
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val title =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                    val path =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                    val duration =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val displayName =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                    val album =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                    val albumId =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                    val artist =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    val artistId =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID))
                    val year =
                        cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR))
                    val track =
                        cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK))

                    return MusicItem(
                        tableId = null,
                        id = id,
                        name = title ?: displayName,
                        path = path ?: uri.toString(),
                        duration = duration,
                        displayName = displayName ?: "Unknown",
                        album = album ?: "<unknown>",
                        albumId = albumId,
                        artist = artist ?: "<unknown>",
                        artistId = artistId,
                        genre = "", // 数据库主表通常不含 Genre
                        genreId = 0,
                        year = year,
                        songNumber = track
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MusicParser", "Error querying MediaStore", e)
        }
        return null
    }

    // --- 方法 B: 读取文件头 (ID3 Tags) ---
    private fun parseFromMetadata(context: Context, uri: Uri): MusicItem {

        val music = MusicItem(
            tableId = null,
            id = System.currentTimeMillis(),
            name = "",
            path = uri.toString(),
            duration = 0,
            displayName = getFileName(context, uri),
            album = "",
            albumId = 0,
            artist = "",
            artistId = 0,
            genre = "", // 数据库主表通常不含 Genre
            genreId = 0,
            year = 0,
            songNumber = 0
        )
        // 2. 读取元数据
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durationStr =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            val yearStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            val trackStr =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)

            music.name = if (!title.isNullOrEmpty()) title else music.displayName
            music.artist = artist ?: "<unknown>"
            music.album = album ?: "<unknown>"
            music.duration = durationStr?.toLongOrNull() ?: 0L
            music.genre = genre ?: ""
            music.year = yearStr?.toIntOrNull() ?: 0
            // 解析 Track Number (格式可能是 "1/12" 或 "1")
            music.songNumber = parseTrackNumber(trackStr)
        } catch (e: Exception) {
            Log.e("MusicParser", "Error parsing metadata", e)
            // 出错保底：用文件名当标题
            music.name = music.displayName
        } finally {
            retriever.release()
        }

        return music
    }

    // 辅助：从 ContentResolver 获取文件名
    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) result = cursor.getString(index)
                    }
                }
            } catch (e: Exception) { /* ignore */
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) result = result?.substring(cut + 1)
        }
        return result ?: "Unknown Song"
    }

    // 辅助：解析音轨号
    private fun parseTrackNumber(trackStr: String?): Int {
        if (trackStr.isNullOrEmpty()) return 0
        return try {
            if (trackStr.contains("/")) {
                // 处理 "4/12" 这种格式
                trackStr.split("/")[0].toInt()
            } else {
                trackStr.toInt()
            }
        } catch (e: Exception) {
            0
        }
    }
}