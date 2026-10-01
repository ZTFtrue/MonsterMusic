package com.ztftrue.music.utils

import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.ztftrue.music.sqlData.model.MusicItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object MusicFileParser {
    suspend fun parse(context: Context, uri: Uri): MusicItem = withContext(Dispatchers.IO) {
        // 1. 尝试从 MediaStore 数据库查询 (这是获取 ArtistId, AlbumId 的唯一准确方法)
        val musicFromStore = queryMediaStore(context, uri)

        if (musicFromStore != null) {
            return@withContext musicFromStore
        }

        // 2. 如果数据库没查到 (例如是从文件管理器直接打开的未扫描文件)
        // 使用 MediaMetadataRetriever 从文件头读取 ID3 标签
        return@withContext parseFromMetadata(context, uri)
    }

    private fun parseCursorToMusicItem(cursor: Cursor): MusicItem {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
        val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
        val album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
        val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
        val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
        val artistId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID))
        val year = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR))
        val track = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK))

        return MusicItem(
            tableId = null,
            id = id,
            name = title ?: displayName,
            path = path ?: "",
            duration = duration,
            displayName = displayName ?: "Unknown",
            album = album ?: "<unknown>",
            albumId = albumId,
            artist = artist ?: "<unknown>",
            artistId = artistId,
            genre = "",
            genreId = 0,
            year = year,
            songNumber = track
        )
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
                    val item = parseCursorToMusicItem(cursor)
                    if (item.path.isEmpty()) {
                        item.path = uri.toString()
                    }
                    return item
                }
            }
        } catch (e: Exception) {
            Log.e("MusicParser", "Error querying MediaStore", e)
        }
        return null
    }

    // --- 方法 B: 读取文件头 (ID3 Tags) ---
    fun parseFromMetadata(context: Context, uri: Uri): MusicItem {

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
            if (cut != null && cut != -1) result = result.substring(cut + 1)
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

    fun queryMediaStoreByPath(context: Context, filePath: String): MusicItem? {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK
        )
        try {
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val selection = "${MediaStore.Audio.Media.DATA} = ?"
            val selectionArgs = arrayOf(filePath)
            context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return parseCursorToMusicItem(cursor)
                }
            }
        } catch (e: Exception) {
            Log.e("MusicParser", "Error querying MediaStore by path", e)
        }
        return null
    }

    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }
        if (uri.scheme == "content") {
            try {
                val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                        if (idx != -1) {
                            return cursor.getString(idx)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("MusicParser", "Cannot query MediaStore for file path: $uri", e)
            }
        }
        return null
    }

    val M3U_MIME_TYPES = setOf(
        "audio/x-mpegurl",
        "audio/mpegurl",
        "application/x-mpegurl",
        "application/vnd.apple.mpegurl"
    )

    fun isM3uPlaylist(context: Context, uri: Uri, mimeType: String?): Boolean {
        if (!mimeType.isNullOrBlank()) {
            val normalizedMime = mimeType.trim().lowercase()
            if (M3U_MIME_TYPES.contains(normalizedMime)) {
                return true
            }
        }
        val fileName = getFileName(context, uri).lowercase()
        if (fileName.endsWith(".m3u") || fileName.endsWith(".m3u8")) {
            return true
        }
        val path = uri.path?.lowercase() ?: ""
        return path.endsWith(".m3u") || path.endsWith(".m3u8")
    }

    val EXCLUDED_MIME_TYPES = setOf(
        "audio/amr",
        "audio/amr-wb",
        "audio/3gpp",
        "audio/3gpp2",
        "audio/midi",
        "audio/mid",
        "audio/x-midi",
        "audio/sp-midi",
        "audio/basic",
        "audio/qcelp",
        "audio/evrc"
    )

    val EXCLUDED_EXTENSIONS = setOf(
        "amr",
        "awb",
        "3gp",
        "3gpp",
        "3g2",
        "mid",
        "midi",
        "m4r"
    )

    fun isSupportedMusicFormat(context: Context, uri: Uri, mimeType: String?): Boolean {
        if (isM3uPlaylist(context, uri, mimeType)) {
            return true
        }
        if (!mimeType.isNullOrBlank()) {
            val normalizedMime = mimeType.trim().lowercase()
            if (EXCLUDED_MIME_TYPES.contains(normalizedMime)) {
                return false
            }
        }
        val fileName = getFileName(context, uri).lowercase()
        val extension = fileName.substringAfterLast('.', "")
        if (extension.isNotEmpty() && EXCLUDED_EXTENSIONS.contains(extension)) {
            return false
        }
        return true
    }

    fun resolveM3uTrack(
        rawPath: String,
        playlistDir: File?,
        pathMap: Map<String, MusicItem>,
        canonicalPathMap: Map<String, MusicItem>,
        fileNameMap: Map<String, MusicItem>,
        extDuration: Long?,
        extArtist: String?,
        extTitle: String?,
        localMetadataFetcher: ((File) -> MusicItem?)?
    ): MusicItem? {
        val trimmed = rawPath.trim()
        if (trimmed.isEmpty()) return null

        // 1. Streaming URLs or content URIs
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("content://", ignoreCase = true)
        ) {
            val title = extTitle ?: trimmed.substringAfterLast('/')
            val id = (trimmed.hashCode().toLong() and 0x7fffffffffffffffL)
            return MusicItem(
                tableId = null,
                id = id,
                name = title,
                path = trimmed,
                duration = extDuration ?: 0L,
                displayName = title,
                album = "<stream>",
                albumId = 0,
                artist = extArtist ?: "<unknown>",
                artistId = 0,
                genre = "",
                genreId = 0,
                year = 0,
                songNumber = 0
            )
        }

        // 2. File paths (strip file:// if present)
        val cleanPath = if (trimmed.startsWith("file://", ignoreCase = true)) {
            trimmed.substring(7)
        } else {
            trimmed
        }

        val songFile = File(cleanPath)
        val resolvedFile = if (songFile.isAbsolute) {
            songFile
        } else if (playlistDir != null) {
            File(playlistDir, cleanPath)
        } else {
            songFile
        }

        // Check library by direct path
        pathMap[resolvedFile.path]?.let { return it }
        pathMap[cleanPath]?.let { return it }

        // Check library by canonical path
        try {
            canonicalPathMap[resolvedFile.canonicalPath]?.let { return it }
        } catch (_: Exception) {}

        // Check library by filename
        val fileName = resolvedFile.name.lowercase()
        fileNameMap[fileName]?.let { return it }

        // Local metadata fetcher (e.g. MediaStore query or ID3 parser)
        val fetched = localMetadataFetcher?.invoke(resolvedFile)
        if (fetched != null) {
            if (fetched.name == fetched.displayName && !extTitle.isNullOrEmpty()) {
                fetched.name = extTitle
            }
            if ((fetched.artist == "<unknown>" || fetched.artist.isEmpty()) && !extArtist.isNullOrEmpty()) {
                fetched.artist = extArtist
            }
            if (fetched.duration == 0L && extDuration != null && extDuration > 0) {
                fetched.duration = extDuration
            }
            return fetched
        }

        // Fallback: create item from path and available EXTINF
        val name = extTitle ?: resolvedFile.nameWithoutExtension
        val id = (resolvedFile.absolutePath.hashCode().toLong() and 0x7fffffffffffffffL)
        return MusicItem(
            tableId = null,
            id = id,
            name = name,
            path = resolvedFile.absolutePath,
            duration = extDuration ?: 0L,
            displayName = resolvedFile.name,
            album = "<unknown>",
            albumId = 0,
            artist = extArtist ?: "<unknown>",
            artistId = 0,
            genre = "",
            genreId = 0,
            year = 0,
            songNumber = 0
        )
    }

    fun parseM3uLines(
        lines: Sequence<String>,
        playlistDir: File? = null,
        libraryTracks: Collection<MusicItem>? = null,
        localMetadataFetcher: ((File) -> MusicItem?)? = null
    ): List<MusicItem> {
        val pathMap = HashMap<String, MusicItem>()
        val canonicalPathMap = HashMap<String, MusicItem>()
        val fileNameMap = HashMap<String, MusicItem>()

        libraryTracks?.forEach { item ->
            pathMap[item.path] = item
            try {
                val f = File(item.path)
                canonicalPathMap[f.canonicalPath] = item
                fileNameMap[f.name.lowercase()] = item
            } catch (_: Exception) {}
        }

        val resultList = ArrayList<MusicItem>()
        var lastExtInfDuration: Long? = null
        var lastExtInfArtist: String? = null
        var lastExtInfTitle: String? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val extInfData = line.substring(8).trim()
                val commaIndex = extInfData.indexOf(',')
                if (commaIndex != -1) {
                    val durationStr = extInfData.substring(0, commaIndex).trim()
                    val infoStr = extInfData.substring(commaIndex + 1).trim()
                    val durationSec = durationStr.toDoubleOrNull()
                    if (durationSec != null && durationSec > 0) {
                        lastExtInfDuration = (durationSec * 1000).toLong()
                    } else {
                        lastExtInfDuration = null
                    }
                    if (infoStr.contains(" - ")) {
                        val parts = infoStr.split(" - ", limit = 2)
                        lastExtInfArtist = parts[0].trim()
                        lastExtInfTitle = parts[1].trim()
                    } else if (infoStr.isNotEmpty()) {
                        lastExtInfArtist = null
                        lastExtInfTitle = infoStr
                    }
                }
                continue
            }

            if (line.startsWith("#")) {
                continue
            }

            val trackItem = resolveM3uTrack(
                rawPath = line,
                playlistDir = playlistDir,
                pathMap = pathMap,
                canonicalPathMap = canonicalPathMap,
                fileNameMap = fileNameMap,
                extDuration = lastExtInfDuration,
                extArtist = lastExtInfArtist,
                extTitle = lastExtInfTitle,
                localMetadataFetcher = localMetadataFetcher
            )
            if (trackItem != null) {
                resultList.add(trackItem)
            }

            lastExtInfDuration = null
            lastExtInfArtist = null
            lastExtInfTitle = null
        }

        return resultList
    }

    suspend fun parseM3u(
        context: Context,
        uri: Uri,
        libraryTracks: Collection<MusicItem>? = null
    ): List<MusicItem> = withContext(Dispatchers.IO) {
        val playlistDir: File? = when (uri.scheme) {
            "file" -> uri.path?.let { File(it).parentFile }
            "content" -> {
                val path = getFilePathFromUri(context, uri)
                if (path != null) File(path).parentFile else null
            }
            else -> null
        }

        val localMetadataFetcher: (File) -> MusicItem? = { file ->
            var item: MusicItem? = null
            try {
                item = queryMediaStoreByPath(context, file.canonicalPath)
                    ?: queryMediaStoreByPath(context, file.path)
            } catch (_: Exception) {}

            if (item == null && file.exists()) {
                try {
                    item = parseFromMetadata(context, Uri.fromFile(file))
                } catch (e: Exception) {
                    Log.e("MusicParser", "Error reading metadata from ${file.path}", e)
                }
            }
            item
        }

        val result = ArrayList<MusicItem>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    result.addAll(
                        parseM3uLines(
                            lines = reader.lineSequence(),
                            playlistDir = playlistDir,
                            libraryTracks = libraryTracks,
                            localMetadataFetcher = localMetadataFetcher
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MusicParser", "Error opening M3U stream for $uri", e)
        }
        result
    }
}