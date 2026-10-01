package com.ztftrue.music.play

import android.content.ContentUris
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.model.AlbumList
import com.ztftrue.music.utils.model.ArtistList
import com.ztftrue.music.utils.model.FolderList
import com.ztftrue.music.utils.model.GenresList
import com.ztftrue.music.utils.model.MusicPlayList
import java.io.File

object CustomMetadataKeys {
    const val KEY_PATH = "com.ztftrue.music.metadata.PATH"
    const val KEY_ARTIST = "com.ztftrue.music.metadata.ARTIST"
    const val KEY_ALBUM_COUNT = "com.ztftrue.music.metadata.ALBUM_COUNT"
    const val FOLDER_IS_SHOW = "com.ztftrue.music.metadata.FOLDER_IS_SHOW"
    const val FOLDER_PATH = "com.ztftrue.music.metadata.FOLDER_PATH"
    const val KEY_FIRST_YEAR = "com.ztftrue.music.metadata.FIRST_YEAR"
    const val KEY_LAST_YEAR = "com.ztftrue.music.metadata.LAST_YEAR"

    // 你可以根据需要添加更多 Key
    const val KEY_TABLE_ID = "com.ztftrue.music.metadata.TABLE_ID"
    const val KEY_IS_FAVORITE = "com.ztftrue.music.metadata.IS_FAVORITE"
    const val KEY_DISPLAY_NAME = "com.ztftrue.music.metadata.DISPLAY_NAME"
    const val KEY_GENRE = "com.ztftrue.music.metadata.GENRE"
    const val KEY_GENRE_ID = "com.ztftrue.music.metadata.GENRE_ID"
    const val KEY_PRIORITY = "com.ztftrue.music.metadata.KEY_PRIORITY"
    const val KEY_ARTIST_ID = "com.ztftrue.music.metadata.ARTIST_ID"
    const val KEY_ALBUM_ID = "com.ztftrue.music.metadata.ALBUM_ID"
    const val KEY_FOLDER_ID = "com.ztftrue.music.metadata.FOLDER_ID"
    const val KEY_PLAYLIST_ID = "com.ztftrue.music.metadata.PLAYLIST_ID"
}

object MediaItemUtils {
    fun albumToMediaItem(album: AlbumList): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
        metadataBuilder.setTitle(album.name)
            .setArtist(album.artist)
            .setTotalTrackCount(album.trackNumber)
            .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
            .setIsBrowsable(true)
            .setIsPlayable(true)
        if (album.id > 0) {
            val artworkUri = ContentUris.withAppendedId(
                "content://media/external/audio/albumart".toUri(),
                album.id
            )
            metadataBuilder.setArtworkUri(artworkUri)
        }
        val yearString = if (album.firstYear == album.lastYear) {
            album.lastYear
        } else {
            "${album.firstYear}-${album.lastYear}"
        }
        yearString.toIntOrNull()?.let {
            metadataBuilder.setReleaseYear(it)
        }
        val extras = Bundle().apply {
            putString("album_artist", album.artist)
            putString("album_first_year", album.firstYear)
            putString("album_last_year", album.lastYear)
            putString("original_type", album.type.name)
            putLong(CustomMetadataKeys.KEY_ALBUM_ID, album.id)
        }
        metadataBuilder.setExtras(extras)
        val mediaItemBuilder = MediaItem.Builder()
        mediaItemBuilder.setMediaId("${PlayListType.Albums.name}_track_${album.id}")
        mediaItemBuilder.setMediaMetadata(metadataBuilder.build())
        return mediaItemBuilder.build()
    }

    /**
     * 将 MusicPlayList 转换为 MediaItem。
     */
    fun playlistToMediaItem(playlist: MusicPlayList): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(playlist.name)
            .setTotalTrackCount(playlist.trackNumber)
            .setIsBrowsable(true)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
            .setExtras(Bundle().apply {
                putString(CustomMetadataKeys.KEY_PATH, playlist.path)
                putLong(CustomMetadataKeys.KEY_PLAYLIST_ID, playlist.id)
            })
            .build()

        return MediaItem.Builder()
            .setMediaId("${PlayListType.PlayLists.name}_track_${playlist.id}")
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * 将 FolderList 转换为 MediaItem。
     */
    fun folderToMediaItem(folder: FolderList): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(folder.name)
            .setTotalTrackCount(folder.trackNumber)
            .setIsBrowsable(true)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
            .setExtras(Bundle().apply {
                putBoolean(CustomMetadataKeys.FOLDER_IS_SHOW, folder.isShow)
                putString(CustomMetadataKeys.FOLDER_PATH, folder.path)
                putLong(CustomMetadataKeys.KEY_FOLDER_ID, folder.id)
            })
            .build()

        return MediaItem.Builder()
            .setMediaId("${PlayListType.Folders.name}_track_${folder.id}")
            .setMediaMetadata(metadata)
            .build()
    }

    fun artistToMediaItem(artist: ArtistList): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(artist.name)
            .setTotalTrackCount(artist.trackNumber)
            .setIsBrowsable(true)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
            .setExtras(Bundle().apply {
                putInt(CustomMetadataKeys.KEY_ALBUM_COUNT, artist.albumNumber)
                putLong(CustomMetadataKeys.KEY_ARTIST_ID, artist.id)
            })
            .build()
        return MediaItem.Builder()
            .setMediaId("${PlayListType.Artists.name}_track_${artist.id}")
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * 将 GenresList 转换为 MediaItem。
     */
    fun genreToMediaItem(genre: GenresList): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(genre.name)
            .setGenre(genre.name)
            .setTotalTrackCount(genre.trackNumber)
            .setIsBrowsable(true)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_GENRE)
            .setExtras(Bundle().apply {
                putInt(CustomMetadataKeys.KEY_ALBUM_COUNT, genre.albumNumber)
                putLong(CustomMetadataKeys.KEY_GENRE_ID, genre.id)
            })
            .build()

        return MediaItem.Builder()
            .setMediaId("${PlayListType.Genres.name}_track_${genre.id}")
            .setMediaMetadata(metadata)
            .build()
    }

    fun createFullFeaturedRoot(): MediaItem {
        val rootExtras = Bundle().apply {
            putBoolean("android.media.browse.SEARCH_SUPPORTED", true)
            putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
            putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle("Music")
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setExtras(rootExtras)
            .build()

        return MediaItem.Builder()
            .setMediaId("root")
            .setMediaMetadata(metadata)
            .build()
    }

    fun musicItemToMediaItem(musicItem: MusicItem): MediaItem {

        // --- 1. 构建 MediaMetadata (用于UI显示和分类的信息) ---
        val metadataBuilder = MediaMetadata.Builder()

        // 映射标准字段
        metadataBuilder.setTitle(musicItem.name)               // 歌曲标题
            .setArtist(musicItem.artist)            // 艺术家名称
            .setAlbumTitle(musicItem.album)         // 专辑标题
            .setTrackNumber(musicItem.songNumber)   // 轨道号
            .setReleaseYear(musicItem.year)         // 发行年份
            .setGenre(musicItem.genre)
            .setDurationMs(musicItem.duration)
        if (musicItem.albumId > 0) {
            val artworkUri = ContentUris.withAppendedId(
                "content://media/external/audio/albumart".toUri(),
                musicItem.albumId
            )
            metadataBuilder.setArtworkUri(artworkUri)
        }
        metadataBuilder.setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
        metadataBuilder.setIsPlayable(true)
        metadataBuilder.setIsBrowsable(false)
        val extras = Bundle().apply {
            // 存储数据库的主键
            musicItem.tableId?.let { putLong(CustomMetadataKeys.KEY_TABLE_ID, it) }
            putBoolean(CustomMetadataKeys.KEY_IS_FAVORITE, musicItem.isFavorite)
            putString(CustomMetadataKeys.KEY_DISPLAY_NAME, musicItem.displayName)
            putLong(CustomMetadataKeys.KEY_ALBUM_ID, musicItem.albumId)
            putLong(CustomMetadataKeys.KEY_ARTIST_ID, musicItem.artistId)
            putLong(CustomMetadataKeys.KEY_GENRE_ID, musicItem.genreId)
            putInt(CustomMetadataKeys.KEY_PRIORITY, musicItem.priority)
            putString("PATH", musicItem.path)
        }
        metadataBuilder.setExtras(extras)


        // --- 3. 构建 MediaItem 本身 ---
        val mediaUri = if (musicItem.path.startsWith("content://") ||
            musicItem.path.startsWith("http://") ||
            musicItem.path.startsWith("https://")
        ) {
            musicItem.path.toUri()
        } else {
            File(musicItem.path).toUri()
        }
        return MediaItem.Builder()
            .setMediaId(musicItem.id.toString())
            .setUri(mediaUri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }


    fun extractId(mediaId: String?): Long? {
        if (mediaId.isNullOrEmpty()) return null
        mediaId.toLongOrNull()?.let { return it }
        val cleaned = when {
            mediaId.contains("_track_") -> mediaId.substringAfterLast("_track_")
            mediaId.contains("_album_") -> mediaId.substringAfterLast("_album_")
            mediaId.contains('@') -> mediaId.substringAfterLast('@')
            mediaId.contains('_') -> mediaId.substringAfterLast('_')
            else -> mediaId
        }
        return cleaned.toLongOrNull()
    }

    fun getId(mediaItem: MediaItem): Long {
        val extras = mediaItem.mediaMetadata.extras
        if (extras != null) {
            if (extras.containsKey(CustomMetadataKeys.KEY_FOLDER_ID)) {
                return extras.getLong(CustomMetadataKeys.KEY_FOLDER_ID)
            }
            if (extras.containsKey(CustomMetadataKeys.KEY_GENRE_ID)) {
                return extras.getLong(CustomMetadataKeys.KEY_GENRE_ID)
            }
            if (extras.containsKey(CustomMetadataKeys.KEY_ALBUM_ID)) {
                return extras.getLong(CustomMetadataKeys.KEY_ALBUM_ID)
            }
            if (extras.containsKey(CustomMetadataKeys.KEY_ARTIST_ID)) {
                return extras.getLong(CustomMetadataKeys.KEY_ARTIST_ID)
            }
            if (extras.containsKey(CustomMetadataKeys.KEY_PLAYLIST_ID)) {
                return extras.getLong(CustomMetadataKeys.KEY_PLAYLIST_ID)
            }
        }
        return extractId(mediaItem.mediaId) ?: 0L
    }

    fun mediaItemToMusicItem(mediaItem: MediaItem): MusicItem? {
        val metadata = mediaItem.mediaMetadata

        // mediaId 是必须的，我们用它来填充 id 字段
        val id = mediaItem.mediaId.toLongOrNull() ?: extractId(mediaItem.mediaId) ?: return null

        val extras = metadata.extras ?: Bundle.EMPTY

        return MusicItem(
            // tableId 是数据库主键，通常在插入时由 Room 自动生成或在队列同步时设置，
            // 这里我们从 extras 中尝试读取，如果不存在则设为 null。
            tableId = extras.getLong(CustomMetadataKeys.KEY_TABLE_ID).takeIf { it != 0L },

            id = id,

            // 从标准元数据字段获取信息，并提供默认值
            name = metadata.title?.toString() ?: "Unknown Title",
            path = extras.getString("PATH", ""),
            // duration 在 MediaItem 中不直接可用，需要播放器准备好后才能获取。
            // 如果你之前把它存入了 extras，可以在这里读取。
            duration = metadata.durationMs ?: 0L,
            displayName = extras.getString(
                CustomMetadataKeys.KEY_DISPLAY_NAME,
                "Unknown Display Name"
            ),
            album = metadata.albumTitle?.toString() ?: "Unknown Album",
            albumId = extras.getLong(CustomMetadataKeys.KEY_ALBUM_ID, 0L),
            artist = metadata.artist?.toString() ?: "Unknown Artist",
            artistId = extras.getLong(CustomMetadataKeys.KEY_ARTIST_ID, 0L),
            genre = metadata.genre?.toString() ?: "Unknown Genre",
            genreId = extras.getLong(CustomMetadataKeys.KEY_GENRE_ID, 0L),
            year = metadata.releaseYear ?: 0,
            songNumber = metadata.trackNumber ?: 0,
            priority = extras.getInt(CustomMetadataKeys.KEY_PRIORITY, 0),
            isFavorite = extras.getBoolean(CustomMetadataKeys.KEY_IS_FAVORITE, false)
        )
    }

    fun mediaItemToAlbumList(mediaItem: MediaItem): AlbumList? {
        val metadata = mediaItem.mediaMetadata

        // 1. 确保这是一个专辑类型的 MediaItem (可选但推荐)
        if (metadata.mediaType != MediaMetadata.MEDIA_TYPE_ALBUM) {
            // 或者可以只打印警告而不是直接返回 null，取决于你的业务逻辑
            Log.w(
                "Converter",
                "Attempted to convert a non-album MediaItem to AlbumList. mediaId: ${mediaItem.mediaId}"
            )
        }

        // 2. 提取 album id
        val id = if (mediaItem.mediaMetadata.extras?.containsKey(CustomMetadataKeys.KEY_ALBUM_ID) == true) {
            mediaItem.mediaMetadata.extras!!.getLong(CustomMetadataKeys.KEY_ALBUM_ID)
        } else {
            extractId(mediaItem.mediaId)
        } ?: return null

        val extras = metadata.extras ?: Bundle.EMPTY

        return AlbumList(
            id = id,
            name = metadata.title?.toString() ?: "Unknown Album",
            artist = metadata.artist?.toString()
            // 如果标准 artist 字段为空，尝试从 extras 回退
                ?: extras.getString("album_artist", "Unknown Artist"),

            trackNumber = metadata.totalTrackCount ?: 0,

            // 从 extras 中获取我们自定义存储的信息
            firstYear = extras.getString(
                "album_first_year",
                metadata.releaseYear?.toString() ?: "0"
            ),
            lastYear = extras.getString("album_last_year", metadata.releaseYear?.toString() ?: "0"),

            )
    }
}