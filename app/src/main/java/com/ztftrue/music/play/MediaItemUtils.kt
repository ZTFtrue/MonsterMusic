package com.ztftrue.music.play

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.model.AlbumList
import com.ztftrue.music.utils.model.ArtistList
import com.ztftrue.music.utils.model.FolderList
import com.ztftrue.music.utils.model.GenresList
import com.ztftrue.music.utils.model.MusicPlayList

object CustomMetadataKeys {
    const val KEY_ORIGINAL_ID = "com.ztftrue.music.metadata.ORIGINAL_ID"
    const val KEY_ORIGINAL_TYPE = "com.ztftrue.music.metadata.ORIGINAL_TYPE"
    const val KEY_PATH = "com.ztftrue.music.metadata.PATH"
    const val KEY_ARTIST = "com.ztftrue.music.metadata.ARTIST"
    const val KEY_ALBUM_COUNT = "com.ztftrue.music.metadata.ALBUM_COUNT"
    const val KEY_FIRST_YEAR = "com.ztftrue.music.metadata.FIRST_YEAR"
    const val KEY_LAST_YEAR = "com.ztftrue.music.metadata.LAST_YEAR"

    // 你可以根据需要添加更多 Key
    const val KEY_TABLE_ID = "com.ztftrue.music.metadata.TABLE_ID"
    const val KEY_IS_FAVORITE = "com.ztftrue.music.metadata.IS_FAVORITE"
    const val KEY_DISPLAY_NAME = "com.ztftrue.music.metadata.DISPLAY_NAME"
    const val KEY_GENRE = "com.ztftrue.music.metadata.GENRE"
    const val KEY_GENRE_ID = "com.ztftrue.music.metadata.GENRE_ID"
    const val KEY_ARTIST_ID = "com.ztftrue.music.metadata.ARTIST_ID"
    const val KEY_ALBUM_ID = "com.ztftrue.music.metadata.ALBUM_ID"
}

object MediaItemUtils {
    fun albumToMediaItem(album: AlbumList): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
        metadataBuilder.setTitle(album.name)
            .setArtist(album.artist)
            .setTotalTrackCount(album.trackNumber)
            .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
            .setIsBrowsable(true)
            .setIsPlayable(false)
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
        }
        metadataBuilder.setExtras(extras)
        val mediaItemBuilder = MediaItem.Builder()
        mediaItemBuilder.setMediaId("album_${album.id}")
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
            .setIsPlayable(false) // 播放列表通常是浏览，也可以设为true以播放全部
            .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
            .setExtras(Bundle().apply {
                putLong(CustomMetadataKeys.KEY_ORIGINAL_ID, playlist.id)
                putString(CustomMetadataKeys.KEY_ORIGINAL_TYPE, playlist.type.name)
                putString(CustomMetadataKeys.KEY_PATH, playlist.path)
            })
            .build()

        return MediaItem.Builder()
            .setMediaId("playlist_${playlist.id}")
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
            .setIsPlayable(false)
//            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER)
            .setExtras(Bundle().apply {
                putLong(CustomMetadataKeys.KEY_ORIGINAL_ID, folder.id)
                putString(CustomMetadataKeys.KEY_ORIGINAL_TYPE, folder.type.name)
                // folder.isShow 这种UI状态信息通常不放在这里，但如果需要也可以放
            })
            .build()

        return MediaItem.Builder()
            .setMediaId("folder_${folder.id}")
            .setMediaMetadata(metadata)
            .build()
    }

    fun artistToMediaItem(artist: ArtistList): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(artist.name) // 艺术家名称作为标题
            .setTotalTrackCount(artist.trackNumber) // 艺术家作品总数
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
            .setExtras(Bundle().apply {
                putLong(CustomMetadataKeys.KEY_ORIGINAL_ID, artist.id)
                putString(CustomMetadataKeys.KEY_ORIGINAL_TYPE, artist.type.name)
                putInt(CustomMetadataKeys.KEY_ALBUM_COUNT, artist.albumNumber)
            })
            .build()

        return MediaItem.Builder()
            .setMediaId("artist_${artist.id}")
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * 将 GenresList 转换为 MediaItem。
     */
    fun genreToMediaItem(genre: GenresList): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(genre.name) // 流派名称作为标题
            .setGenre(genre.name) // 也可以设置到标准流派字段
            .setTotalTrackCount(genre.trackNumber)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_GENRE)
            .setExtras(Bundle().apply {
                putLong(CustomMetadataKeys.KEY_ORIGINAL_ID, genre.id)
                putString(CustomMetadataKeys.KEY_ORIGINAL_TYPE, genre.type.name)
                putInt(CustomMetadataKeys.KEY_ALBUM_COUNT, genre.albumNumber)
            })
            .build()

        return MediaItem.Builder()
            .setMediaId("genre_${genre.id}")
            .setMediaMetadata(metadata)
            .build()
    }

    fun createFullFeaturedRoot(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle("Music") // 这个标题通常不会在你的App里直接显示
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()

        return MediaItem.Builder()
            // 使用一个清晰的 Media ID
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
            .setGenre(musicItem.genre)              // 流派（这是一个标准字段）

        // (可选) 如果你能从 albumId 获取到专辑封面 URI，在这里设置
        // val artworkUri = getArtworkUriForAlbum(musicItem.albumId)
        // metadataBuilder.setArtworkUri(artworkUri)

        // 设置媒体类型，明确这是一首歌曲
        metadataBuilder.setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)

        // 对于歌曲，它通常是可播放的，但不可浏览（它下面没有子项）
        metadataBuilder.setIsPlayable(true)
        metadataBuilder.setIsBrowsable(false)

        // --- 2. 将非标准或需要额外保留的字段存入 extras ---
        val extras = Bundle().apply {
            // 存储数据库的主键
            musicItem.tableId?.let { putLong(CustomMetadataKeys.KEY_TABLE_ID, it) }
            // 存储原始的媒体库ID
            putLong(CustomMetadataKeys.KEY_ORIGINAL_ID, musicItem.id)
            // 存储自定义状态
            putBoolean(CustomMetadataKeys.KEY_IS_FAVORITE, musicItem.isFavorite)
            // 存储其他有用的信息
            putString(CustomMetadataKeys.KEY_DISPLAY_NAME, musicItem.displayName)
            putLong(CustomMetadataKeys.KEY_ALBUM_ID, musicItem.albumId)
            putLong(CustomMetadataKeys.KEY_ARTIST_ID, musicItem.artistId)
            putLong(CustomMetadataKeys.KEY_GENRE_ID, musicItem.genreId)
        }
        metadataBuilder.setExtras(extras)


        // --- 3. 构建 MediaItem 本身 ---
        return MediaItem.Builder()
            // 关键！将你的唯一 ID 设置为 MediaItem 的 mediaId。
            // 使用 toString() 确保类型为 String。
            .setMediaId(musicItem.id.toString())

            // 关键！设置播放 URI，这样 ExoPlayer 才知道要播放哪个文件。
            // 假设 'path' 是一个文件路径或 content:// URI 字符串。
            .setUri(musicItem.path.toUri())

            // 将上面构建好的元数据设置给 MediaItem
            .setMediaMetadata(metadataBuilder.build())

            .build()
    }
    fun musicItemToMediaMetadata(musicItem: MusicItem): MediaMetadata {
        val metadataBuilder = MediaMetadata.Builder()

        // --- 1. 设置所有可用的标准元数据字段 ---
        metadataBuilder
            .setTitle(musicItem.name)
            .setArtist(musicItem.artist)
            .setAlbumTitle(musicItem.album)
            .setAlbumArtist(musicItem.artist) // 通常专辑艺术家和曲目艺术家是同一个人
            .setGenre(musicItem.genre)
            .setTrackNumber(musicItem.songNumber)
            .setReleaseYear(musicItem.year)
        // (可选) 如果你能从 ContentResolver 获取到专辑封面 URI
        // .setArtworkUri(getAlbumArtUri(musicItem.albumId))

        // --- 2. (可选但推荐) 将一些无法在标准字段中表示的、
        //     但又很有用的信息存入 extras Bundle。---
        val extras = Bundle().apply {
            // 保存原始的数据库 ID，以便调试或特殊用途
            putLong(CustomMetadataKeys.KEY_ORIGINAL_ID, musicItem.id)
            // 保存艺术家和专辑的 ID，可能用于快速跳转
            putLong("artist_id", musicItem.artistId)
            putLong("album_id", musicItem.albumId)
            // 保存其他自定义数据
            // putBoolean(CustomMetadataKeys.KEY_IS_FAVORITE, musicItem.isFavorite)
            // putInt(CustomMetadataKeys.KEY_BITRATE, musicItem.bitrate)
        }
        metadataBuilder.setExtras(extras)

        // --- 3. 对于歌曲，明确其媒体类型 ---
        // 它们是可播放的，但不可浏览（下面没有子项）
        metadataBuilder
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)

        return metadataBuilder.build()
    }

}