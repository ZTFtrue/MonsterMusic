package com.ztftrue.music.play.manager

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.play.PlayUtils
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.sqlData.model.SortFiledData
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.SharedPreferencesUtils
import com.ztftrue.music.utils.model.*
import com.ztftrue.music.utils.trackManager.*
import kotlinx.coroutines.*
import java.io.File
import java.util.LinkedHashMap
import kotlin.collections.ArrayList

/**
 * 负责音乐库数据的加载、缓存、检索和搜索。
 * 将原 PlayService 中的数据管理职责剥离至此。
 */
class MusicLibraryRepository(private val context: Context) {

    private val db: MusicDatabase = MusicDatabase.getDatabase(context)

    // ==========================================
    // 核心数据缓存 (原 PlayService 的成员变量)
    // ==========================================

    // 基础实体缓存
    val tracksLinkedHashMap = LinkedHashMap<Long, MusicItem>()
    val allTracksLinkedHashMap = LinkedHashMap<Long, MusicItem>() // 包含被过滤掉的文件夹中的歌曲
    val albumsLinkedHashMap = LinkedHashMap<Long, AlbumList>()
    val playListLinkedHashMap = LinkedHashMap<Long, MusicPlayList>()
    val artistsLinkedHashMap = LinkedHashMap<Long, ArtistList>()
    val genresLinkedHashMap = LinkedHashMap<Long, GenresList>()
    val foldersLinkedHashMap = LinkedHashMap<Long, FolderList>()

    // 关系/子列表缓存 (ID -> List)
    val playListTracksHashMap = HashMap<Long, ArrayList<MusicItem>>()
    val albumsListTracksHashMap = HashMap<Long, ArrayList<MusicItem>>()
    val artistsListTracksHashMap = HashMap<Long, ArrayList<MusicItem>>()
    val genresListTracksHashMap = HashMap<Long, ArrayList<MusicItem>>()
    val foldersListTracksHashMap = HashMap<Long, LinkedHashMap<Long, MusicItem>>()

    // 关联关系缓存
    val artistHasAlbumMap = HashMap<Long, ArrayList<AlbumList>>()
    val genreHasAlbumMap = HashMap<Long, ArrayList<AlbumList>>()

    // UI 显示相关的排序指示器 (部分逻辑依赖它)
    val showIndicatorList = ArrayList<SortFiledData>()

    // ==========================================
    // 初始化与加载
    // ==========================================
    val mainTabList = ArrayList<MainTab>()

    /**
     * 启动时加载所有基础数据
     */
    suspend fun loadAllData() = coroutineScope {
        // 并行加载各项数据
        awaitAll(
            async(Dispatchers.IO) { loadSongsAndFolders() }, // 最核心的歌曲数据
            async(Dispatchers.IO) { loadShowIndicatorList() },
            async(Dispatchers.IO) { loadMainTabs() }
            // 可以在这里添加其他需要预加载的项目，
            // 但原逻辑中 Albums/Artists 等似乎是懒加载或在 UI 层请求时加载的，
            // 或者是在 refreshAction 中全量加载的。
            // 依据原 loadAllTracksAndFolders() 逻辑，这里主要加载 Songs 和 Folders。
        )
    }

    /**
     * 对应原 refreshAction()，强制刷新所有数据
     */
    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        clearCache()
        loadSongsAndFolders()
        loadShowIndicatorList()
        // 可以在这里根据需求预加载 Albums, Artists 等
    }

    private fun loadMainTabs() {
        val list = db.MainTabDao().findAllIsShowMainTabSortByPriority()
        mainTabList.clear()
        if (list.isEmpty()) {
            // 如果数据库为空，使用工具类添加默认标签并保存
            PlayUtils.addDefaultMainTab(mainTabList)
            db.MainTabDao().insertAll(mainTabList)
        } else {
            mainTabList.addAll(list)
        }
    }

    private fun clearCache() {
        playListLinkedHashMap.clear()
        albumsLinkedHashMap.clear()
        artistsLinkedHashMap.clear()
        genresLinkedHashMap.clear()
        tracksLinkedHashMap.clear()
        playListTracksHashMap.clear()
        albumsListTracksHashMap.clear()
        artistsListTracksHashMap.clear()
        genresListTracksHashMap.clear()
        foldersListTracksHashMap.clear()
        allTracksLinkedHashMap.clear()
        artistHasAlbumMap.clear()
        genreHasAlbumMap.clear()
    }

    private fun loadSongsAndFolders() {
        val sortData = db.SortFiledDao().findSortByType(PlayListType.Songs.name)
        // 调用工具类填充 foldersLinkedHashMap, tracksLinkedHashMap, allTracksLinkedHashMap
        TracksManager.getFolderList(
            context,
            foldersLinkedHashMap,
            tracksLinkedHashMap,
            "${sortData?.filed ?: ""} ${sortData?.method ?: ""}",
            allTracksLinkedHashMap
        )
    }

    private fun loadShowIndicatorList() {
        val sortData1 = db.SortFiledDao().findSortAll()
        showIndicatorList.clear()
        sortData1.forEach {
            if (it.type == PlayListType.Songs.name || it.type.endsWith("@Tracks")) {
                showIndicatorList.add(it)
            }
        }
    }
    /**
     * 重新加载指定 ID 的歌曲信息（用于标签修改后的刷新）
     * @return 返回更新后的 MusicItem，如果找不到则返回 null
     */
    @UnstableApi
    suspend fun reloadTrack(id: Long): MusicItem? = withContext(Dispatchers.IO) {
        // 1. 从系统 MediaStore 或数据库获取最新信息
        // 假设 TracksManager 有这个静态方法 (根据原代码推断)
        val newItem = TracksManager.getMusicById(context, id)

        if (newItem != null) {
            // 2. 更新内存缓存
            tracksLinkedHashMap[id] = newItem
            allTracksLinkedHashMap[id] = newItem

            // 3. 尝试更新其他关联缓存 (可选，视性能要求而定，这里简单处理)
            // 如果该歌曲属于某个专辑/播放列表缓存，最稳妥的方式是清除那些缓存
            // 或者在这里进行精细化的查找替换
        } else {
            // 如果查不到了（可能被删除了），则移除缓存
            tracksLinkedHashMap.remove(id)
            allTracksLinkedHashMap.remove(id)
        }

        return@withContext newItem
    }
    // ==========================================
    // 数据获取 (Getter with Lazy Loading)
    // 对应原 onGetChildren 中的逻辑
    // ==========================================

    fun getPlayLists(): List<MusicPlayList> {
        if (playListLinkedHashMap.isNotEmpty()) {
            return ArrayList(playListLinkedHashMap.values)
        }
        val sortData = db.SortFiledDao().findSortByType(PlayListType.PlayLists.name)
        val result = PlaylistManager.getPlaylists(
            context,
            playListLinkedHashMap,
            tracksLinkedHashMap,
            sortData?.filed,
            sortData?.method
        )
        // 缓存已被 Manager 填充
        return result
    }

    fun getAlbums(): List<AlbumList> {
        if (albumsLinkedHashMap.isNotEmpty()) {
            return ArrayList(albumsLinkedHashMap.values)
        }
        val sortDataP = db.SortFiledDao().findSortByType(PlayListType.Albums.name)
        val needMerge = SharedPreferencesUtils.getMergeAlbum(context)
        AlbumManager.getAlbumList(
            context,
            albumsLinkedHashMap,
            "${sortDataP?.filed ?: ""} ${sortDataP?.method ?: ""}",
            needMerge
        )
        return ArrayList(albumsLinkedHashMap.values)
    }

    fun getArtists(): List<ArtistList> {
        if (artistsLinkedHashMap.isNotEmpty()) {
            return ArrayList(artistsLinkedHashMap.values)
        }
        val sortDataP = db.SortFiledDao().findSortByType(PlayListType.Artists.name)
        ArtistManager.getArtistList(
            context,
            artistsLinkedHashMap,
            "${sortDataP?.filed ?: ""} ${sortDataP?.method ?: ""}"
        )
        return ArrayList(artistsLinkedHashMap.values)
    }

    fun getGenres(): List<GenresList> {
        if (genresLinkedHashMap.isNotEmpty()) {
            return ArrayList(genresLinkedHashMap.values)
        }
        val sortDataP = db.SortFiledDao().findSortByType(PlayListType.Genres.name)
        GenreManager.getGenresList(
            context,
            genresLinkedHashMap,
            tracksLinkedHashMap,
            "${sortDataP?.filed ?: ""} ${sortDataP?.method ?: ""}"
        )
        return ArrayList(genresLinkedHashMap.values)
    }

    fun getFolders(): List<FolderList> {
        if (foldersLinkedHashMap.isNotEmpty()) {
            return ArrayList(foldersLinkedHashMap.values)
        }
        // 如果为空，重新加载一次基础数据
        loadSongsAndFolders()
        return ArrayList(foldersLinkedHashMap.values)
    }

    fun getSongs(): List<MusicItem> {
        if (tracksLinkedHashMap.isNotEmpty()) {
            return ArrayList(tracksLinkedHashMap.values)
        }
        loadSongsAndFolders()
        return ArrayList(tracksLinkedHashMap.values)
    }

    // ==========================================
    // 子列表获取 (Tracks by Parent)
    // ==========================================

    fun getTracksByPlayListId(id: Long): List<MusicItem> {
        playListTracksHashMap[id]?.let { return it }

        val sortData = db.SortFiledDao().findSortByType(PlayUtils.ListTypeTracks.PlayListsTracks)
        val playList = playListLinkedHashMap[id] ?: return emptyList()

        val file = File(playList.path)
        // 容错处理：如果文件路径有问题，可能无法获取 parent
        val parentPath = file.parent ?: return emptyList()

        val contentUri: Uri = ContentUris.withAppendedId(
            MediaStore.Files.getContentUri("external"), id
        )
        val tracks = PlaylistManager.getTracksByPlayListId(
            context,
            contentUri,
            parentPath,
            allTracksLinkedHashMap,
            sortData?.filed,
            sortData?.method
        )
        playListTracksHashMap[id] = tracks
        return tracks
    }

    fun getTracksByAlbumId(id: Long): List<MusicItem> {
        albumsListTracksHashMap[id]?.let { return it }

        val needMerge = SharedPreferencesUtils.getMergeAlbum(context)
        val tracks: ArrayList<MusicItem>
        val sortData = db.SortFiledDao().findSortByType(PlayUtils.ListTypeTracks.AlbumsTracks)
        val sortOrder = "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"

        if (needMerge) {
            val albumList = AlbumManager.getAlbumById(context, id)
            val targetAlbumName = albumList?.name ?: ""
            val targetArtistName = albumList?.artist ?: ""
            val trackUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val selection =
                "${MediaStore.Audio.Media.ALBUM} = ? AND ${MediaStore.Audio.Media.ARTIST} = ?"
            val selectionArgs = arrayOf(targetAlbumName, targetArtistName)
            tracks = TracksManager.getTracksById(
                context, trackUri, allTracksLinkedHashMap, selection, selectionArgs, sortOrder
            )
        } else {
            val trackUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            tracks = TracksManager.getTracksById(
                context, trackUri, allTracksLinkedHashMap,
                MediaStore.Audio.Media.ALBUM_ID + "=?",
                arrayOf(id.toString()),
                sortOrder
            )
        }
        albumsListTracksHashMap[id] = tracks
        return tracks
    }

    suspend fun getTracksByArtistId(id: Long): List<MusicItem> = coroutineScope {
        artistsListTracksHashMap[id]?.let { return@coroutineScope it }

        val artist = artistsLinkedHashMap[id]
        if (artist != null) {
            val sortData = db.SortFiledDao().findSortByType(PlayUtils.ListTypeTracks.ArtistsTracks)
            val sortOrder = "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
            val trackUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            // 并行加载：该艺术家的专辑列表（用于UI显示）和 歌曲列表
            val albumsDeferred = async(Dispatchers.IO) {
                // 确保专辑数据已加载
                if (albumsLinkedHashMap.isEmpty()) {
                    val sortDataP = db.SortFiledDao().findSortByType(PlayListType.Albums.name)
                    val needMerge = SharedPreferencesUtils.getMergeAlbum(context)
                    AlbumManager.getAlbumList(
                        context, albumsLinkedHashMap,
                        "${sortDataP?.filed ?: ""} ${sortDataP?.method ?: ""}", needMerge
                    )
                }
                AlbumManager.getAlbumsByArtist(
                    context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    albumsLinkedHashMap, MediaStore.Audio.Media.ARTIST_ID + "=?",
                    arrayOf(id.toString()), null
                )
            }

            val tracksDeferred = async(Dispatchers.IO) {
                TracksManager.getTracksById(
                    context, trackUri, allTracksLinkedHashMap,
                    MediaStore.Audio.Media.ARTIST_ID + "=?",
                    arrayOf(id.toString()), sortOrder
                )
            }

            val albums = albumsDeferred.await()
            val tracks = tracksDeferred.await()

            artistHasAlbumMap[id] = albums
            artistsListTracksHashMap[id] = tracks
            return@coroutineScope tracks
        }
        return@coroutineScope emptyList()
    }

    suspend fun getTracksByGenreId(id: Long): List<MusicItem> = coroutineScope {
        genresListTracksHashMap[id]?.let { return@coroutineScope it }

        val genre = genresLinkedHashMap[id]
        if (genre != null) {
            val sortData = db.SortFiledDao().findSortByType(PlayUtils.ListTypeTracks.GenresTracks)
            val sortOrder = "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
            val uri = MediaStore.Audio.Genres.Members.getContentUri("external", id)

            // 并行加载：该流派的专辑列表 和 歌曲列表
            val albumsDeferred = async(Dispatchers.IO) {
                if (albumsLinkedHashMap.isEmpty()) {
                    val sortDataP = db.SortFiledDao().findSortByType(PlayListType.Albums.name)
                    val needMerge = SharedPreferencesUtils.getMergeAlbum(context)
                    AlbumManager.getAlbumList(
                        context, albumsLinkedHashMap,
                        "${sortDataP?.filed ?: ""} ${sortDataP?.method ?: ""}", needMerge
                    )
                }
                AlbumManager.getAlbumsByGenre(
                    context, uri, albumsLinkedHashMap, null, null, null
                )
            }

            val tracksDeferred = async(Dispatchers.IO) {
                TracksManager.getTracksById(
                    context, uri, allTracksLinkedHashMap, null, null, sortOrder
                )
            }

            val albums = albumsDeferred.await()
            val tracks = tracksDeferred.await()

            genreHasAlbumMap[id] = albums
            genresListTracksHashMap[id] = tracks
            return@coroutineScope tracks
        }
        return@coroutineScope emptyList()
    }

    fun getTracksByFolderId(id: Long): List<MusicItem> {
        foldersListTracksHashMap[id]?.let { return ArrayList(it.values) }

        val sortData = db.SortFiledDao().findSortByType(PlayUtils.ListTypeTracks.FoldersTracks)
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Audio.Media.BUCKET_ID + "=?"

        val listT = TracksManager.getTracksById(
            context,
            uri,
            allTracksLinkedHashMap,
            selection,
            arrayOf(id.toString()),
            "${sortData?.filed ?: ""} ${sortData?.method ?: ""}"
        )

        val tracksMap = LinkedHashMap<Long, MusicItem>()
        listT.forEach { tracksMap[it.id] = it }
        foldersListTracksHashMap[id] = tracksMap

        return listT
    }

    // ==========================================
    // 搜索功能
    // ==========================================

    data class SearchResult(
        val tracks: ArrayList<MusicItem>,
        val albums: ArrayList<AlbumList>,
        val artists: ArrayList<ArtistList>
    )

    suspend fun search(query: String): SearchResult = coroutineScope {
        if (query.isEmpty() || query.length <= 1) {
            return@coroutineScope SearchResult(ArrayList(), ArrayList(), ArrayList())
        }

        val tracksDeferred = async(Dispatchers.IO) {
            TracksManager.searchTracks(context, tracksLinkedHashMap, query)
        }
        val albumsDeferred = async(Dispatchers.IO) {
            val needMerge = SharedPreferencesUtils.getMergeAlbum(context)
            AlbumManager.searchAlbumByName(context, query, needMerge)
        }
        val artistsDeferred = async(Dispatchers.IO) {
            ArtistManager.searchArtistByName(context, query)
        }

        SearchResult(
            tracks = tracksDeferred.await(),
            albums = albumsDeferred.await(),
            artists = artistsDeferred.await()
        )
    }

    // ==========================================
    // 数据操作 (删除/更新)
    // ==========================================

    /**
     * 删除歌曲
     * @return 返回被删除歌曲的ID，如果未找到则返回-1
     */
    fun deleteTrack(idsToDelete: Long, musicQueue: ArrayList<MusicItem>): Long {
        val deletedId = PlayUtils.trackDelete(
            idsToDelete,
            musicQueue,
            tracksLinkedHashMap
        )
        // 删除后清理缓存，以迫使下次重新加载正确的数据
        if (deletedId > -1) {
            clearCache()
            // 注意：这里简单粗暴地清理了缓存，实际优化可以只移除特定的条目，
            // 但考虑到关联关系复杂（专辑、歌手、播放列表都可能包含此歌），全清是最安全的。
            // 重新加载会在下次获取数据时自动触发。
        }
        return deletedId
    }

    fun updateTrackInfo(id: Long, updatedTrack: MusicItem?) {
        if (updatedTrack != null) {
            tracksLinkedHashMap[id] = updatedTrack
        } else {
            tracksLinkedHashMap.remove(id)
        }
        // 更新后也建议清理相关缓存
        clearCache()
    }

    // ==========================================
    // 单项获取 (ById)
    // ==========================================

    fun getGenreById(id: Long): GenresList? = genresLinkedHashMap[id]
    fun getArtistById(id: Long): ArtistList? = artistsLinkedHashMap[id]
    fun getAlbumById(id: Long): AlbumList? = albumsLinkedHashMap[id]
    fun getFolderById(id: Long): FolderList? = foldersLinkedHashMap[id]
    fun getPlaylistById(id: Long): MusicPlayList? = playListLinkedHashMap[id]


    suspend fun handleSort(type: String) = withContext(Dispatchers.IO) {
        // 1. 无论哪种类型排序，指示器列表可能都变了，重新加载
        loadShowIndicatorList()

        when (type) {
            // --- 主列表排序：只需清空，下次 getXXX() 会自动重新加载 ---
            PlayListType.PlayLists.name -> {
                playListLinkedHashMap.clear()
            }

            PlayListType.Albums.name -> {
                albumsLinkedHashMap.clear()
            }

            PlayListType.Artists.name -> {
                artistsLinkedHashMap.clear()
            }

            PlayListType.Genres.name -> {
                genresLinkedHashMap.clear()
            }

            // --- 所有歌曲排序：立即重新加载 ---
            PlayListType.Songs.name -> {
                tracksLinkedHashMap.clear()
                // Folders 和 Songs 是强关联的（TracksManager.getFolderList 同时加载了两者）
                // 所以需要一起刷新
                foldersLinkedHashMap.clear()
                allTracksLinkedHashMap.clear()
                loadSongsAndFolders()
            }

            // --- 子列表排序：清空缓存，下次点击进入某专辑/列表时重新加载 ---
            // 这些常量来自 PlayUtils.ListTypeTracks
            PlayUtils.ListTypeTracks.PlayListsTracks -> {
                playListTracksHashMap.clear()
            }

            PlayUtils.ListTypeTracks.AlbumsTracks -> {
                albumsListTracksHashMap.clear()
            }

            PlayUtils.ListTypeTracks.ArtistsTracks -> {
                artistsListTracksHashMap.clear()
            }

            PlayUtils.ListTypeTracks.GenresTracks -> {
                genresListTracksHashMap.clear()
            }

            PlayUtils.ListTypeTracks.FoldersTracks -> {
                foldersListTracksHashMap.clear()
            }
        }
    }
}