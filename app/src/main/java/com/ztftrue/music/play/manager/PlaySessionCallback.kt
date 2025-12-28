package com.ztftrue.music.play.manager

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.*
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.ztftrue.music.play.MediaItemUtils
import com.ztftrue.music.play.PlayService
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.SharedPreferencesUtils
import kotlinx.coroutines.*
import kotlin.math.max

@UnstableApi
class PlaySessionCallback(
    private val service: PlayService,
    private val repository: MusicLibraryRepository,
    private val effectManager: AudioEffectManager,
    private val sleepManager: SleepTimerManager
) : MediaLibraryService.MediaLibrarySession.Callback {

    // 使用 Service 的 Scope 来执行协程任务
    private val scope = service.serviceScope

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        // 将 Controller 信息传回 Service (用于后续广播通知)
        service.setControllerInfo(controller)

        val availableCommands =
            MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(MediaCommands.COMMAND_CHANGE_PITCH)
                .add(MediaCommands.COMMAND_CHANGE_Q)
                .add(MediaCommands.COMMAND_DSP_ENABLE)
                .add(MediaCommands.COMMAND_DSP_SET_BAND)
                .add(MediaCommands.COMMAND_DSP_FLATTEN)
                .add(MediaCommands.COMMAND_DSP_SET_BANDS)
                .add(MediaCommands.COMMAND_ECHO_ENABLE)
                .add(MediaCommands.COMMAND_ECHO_SET_DELAY)
                .add(MediaCommands.COMMAND_ECHO_SET_DECAY)
                .add(MediaCommands.COMMAND_ECHO_SET_FEEDBACK)
                .add(MediaCommands.COMMAND_SEARCH)
                .add(MediaCommands.COMMAND_VISUALIZATION_ENABLE)
                .add(MediaCommands.COMMAND_SET_SLEEP_TIMER)
                .add(MediaCommands.COMMAND_VISUALIZATION_CONNECTED)
                .add(MediaCommands.COMMAND_VISUALIZATION_DISCONNECTED)
                .add(MediaCommands.COMMAND_GET_INITIALIZED_DATA)
                .add(MediaCommands.COMMAND_APP_EXIT)
                .add(MediaCommands.COMMAND_TRACKS_UPDATE)
                .add(MediaCommands.COMMAND_SORT_TRACKS)
                .add(MediaCommands.COMMAND_CHANGE_PLAYLIST)
                .add(MediaCommands.COMMAND_GET_CURRENT_PLAYLIST)
                .add(MediaCommands.COMMAND_SMART_SHUFFLE)
                .add(MediaCommands.COMMAND_SORT_QUEUE)
                .add(MediaCommands.COMMAND_CLEAR_QUEUE)
                .add(MediaCommands.COMMAND_GET_PLAY_LIST_ITEM)
                .add(MediaCommands.COMMAND_REFRESH_ALL)
                .add(MediaCommands.COMMAND_TRACK_DELETE)
                .add(MediaCommands.COMMAND_PlAY_LIST_CHANGE)
                .add(MediaCommands.COMMAND_SLEEP_STATE_UPDATE)
                .add(MediaCommands.COMMAND_VIRTUALIZER_ENABLE)
                .add(MediaCommands.COMMAND_VIRTUALIZER_STRENGTH)
                .build()

        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(availableCommands)
            .build()
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        val future = SettableFuture.create<SessionResult>()

        // 简化：对于立即返回的 void 操作，可以直接 return
        // 对于需要异步数据的操作，使用 scope.launch 并设置 future

        when (customCommand.customAction) {
            // --- DSP & Effects (委托给 EffectManager) ---
            MediaCommands.COMMAND_CHANGE_PITCH.customAction -> {
                effectManager.setPitch(service.exoPlayer, args.getFloat("pitch", 1f))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_CHANGE_Q.customAction -> {
                effectManager.setQ(args.getFloat("Q", 3.2f))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_DSP_ENABLE.customAction -> {
                effectManager.setEqualizerEnabled(args.getBoolean("enable"))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
// 在 onCustomCommand 中处理
            MediaCommands.COMMAND_VIRTUALIZER_ENABLE.customAction -> {
                val enable = args.getBoolean(MediaCommands.KEY_ENABLE)
                effectManager.setSpatialEnabled(enable)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_VIRTUALIZER_STRENGTH.customAction -> {
                val strength = args.getInt("strength") // 前端传 0-1000
                effectManager.setSpatialStrength(strength)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_DSP_SET_BAND.customAction -> {
                effectManager.setEqualizerBand(
                    args.getInt(MediaCommands.KEY_INDEX),
                    args.getInt(MediaCommands.KEY_VALUE)
                )
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_DSP_FLATTEN.customAction -> {
                val success = effectManager.flattenEqualizer()
                val resultData = Bundle().apply { putBoolean("result", success) }
                return Futures.immediateFuture(
                    SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        resultData
                    )
                )
            }

            MediaCommands.COMMAND_DSP_SET_BANDS.customAction -> {
                args.getIntArray("value")?.let { effectManager.setEqualizerBands(it) }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_ECHO_ENABLE.customAction -> {
                effectManager.setEchoEnabled(args.getBoolean(MediaCommands.KEY_ENABLE))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_ECHO_SET_DELAY.customAction -> {
                effectManager.setEchoDelay(args.getFloat(MediaCommands.KEY_DELAY))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_ECHO_SET_DECAY.customAction -> {
                effectManager.setEchoDecay(args.getFloat(MediaCommands.KEY_DECAY))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_ECHO_SET_FEEDBACK.customAction -> {
                effectManager.setEchoFeedback(args.getBoolean(MediaCommands.KEY_ENABLE))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            // --- Visualization (委托给 EffectManager) ---
            MediaCommands.COMMAND_VISUALIZATION_ENABLE.customAction -> {
                effectManager.setVisualizationEnabled(args.getBoolean(MediaCommands.KEY_ENABLE))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_VISUALIZATION_CONNECTED.customAction -> {
                effectManager.onVisualizationConnected()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_VISUALIZATION_DISCONNECTED.customAction -> {
                effectManager.onVisualizationDisconnected()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            // --- Sleep Timer (委托给 SleepManager) ---
            MediaCommands.COMMAND_SET_SLEEP_TIMER.customAction -> {
                sleepManager.setTimer(args)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            // --- Service / Player Control ---
            MediaCommands.COMMAND_SET_AUTO_HANDLE_AUDIO_FOCUS.customAction -> {
                service.setAutoHandleAudioFocus(args.getBoolean("auto_handle", true))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_APP_EXIT.customAction -> {
                service.stopSelf()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            // --- Complex Logic (Queue & Data) ---
            MediaCommands.COMMAND_SMART_SHUFFLE.customAction -> {
                scope.launch {
                    handleSmartShuffle(args, future)
                }
                return future
            }

            MediaCommands.COMMAND_SORT_QUEUE.customAction -> {
                service.handleSortQueue(args.getInt("index", 0), args.getInt("targetIndex", 0))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_CLEAR_QUEUE.customAction -> {
                service.handleClearQueue()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_CHANGE_PLAYLIST.customAction -> {
                service.handleChangePlaylist(args)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            // --- Data Retrieval (委托给 Repository) ---
            MediaCommands.COMMAND_GET_INITIALIZED_DATA.customAction -> {
                scope.launch {
                    // 等待 Service 数据加载完毕
                    service.isInitialized.await()

                    val bundle = Bundle()
                    service.fillInitializedData(bundle)
                    future.set(SessionResult(SessionResult.RESULT_SUCCESS, bundle))
                }
                return future
            }

            MediaCommands.COMMAND_SEARCH.customAction -> {
                scope.launch {
                    try {
                        val query = args.getString(MediaCommands.KEY_SEARCH_QUERY, "")
                        val searchResult = repository.search(query)
                        val resultData = Bundle().apply {
                            putParcelableArrayList("tracks", searchResult.tracks)
                            putParcelableArrayList("albums", searchResult.albums)
                            putParcelableArrayList("artist", searchResult.artists)
                        }
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, resultData))
                    } catch (e: Exception) {
                        future.setException(e)
                    }
                }
                return future
            }

            MediaCommands.COMMAND_TRACK_DELETE.customAction -> {
                scope.launch {
                    val idToDelete = args.getLong("id")
                    // 调用 Repository 删除，并在 Service 中处理播放状态
                    val deletedId = repository.deleteTrack(idToDelete, service.musicQueue)
                    val wasInQueue = deletedId > -1

                    if (wasInQueue) {
                        withContext(Dispatchers.Main) {
                            service.removeTrackFromPlayer(deletedId)
                        }
                    }

                    val resultData = Bundle().apply {
                        putBoolean("success", true)
                        putBoolean("wasInQueue", wasInQueue)
                        putInt("playIndex", service.exoPlayer.currentMediaItemIndex)
                        putLong("id", deletedId)
                        putParcelableArrayList("queue", service.musicQueue)
                    }
                    future.set(SessionResult(SessionResult.RESULT_SUCCESS, resultData))
                }
                return future
            }

            MediaCommands.COMMAND_TRACKS_UPDATE.customAction -> {
                scope.launch {
                    try {
                        val id = args.getLong(MediaCommands.KEY_TRACK_ID)
                        // 调用 Service 处理更新逻辑
                        val updatedItem = service.handleTrackUpdate(id)
                        // 准备返回结果
                        if (updatedItem != null) {
                            val resultData = Bundle().apply {
                                // 1. 返回更新后的单个 Item
                                putParcelable("item", updatedItem)
                                // 2. 原代码似乎也返回了整个列表，视前端需求而定
                                // 如果前端列表很大，返回整个 list 可能会慢，建议前端只更新单项
                                // 这里保留原逻辑：
//                                putParcelableArrayList(
//                                    "list",
//                                    ArrayList(repository.tracksLinkedHashMap.values)
//                                )
                            }
                            future.set(SessionResult(SessionResult.RESULT_SUCCESS, resultData))
                        } else {
                            // 没找到歌曲（可能被删除了）
                            future.set(SessionResult(SessionError.ERROR_BAD_VALUE))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        future.setException(e)
                    }
                }
                return future
            }

            MediaCommands.COMMAND_GET_CURRENT_PLAYLIST.customAction -> {
                val bundle = Bundle().apply {
                    putParcelable("playList", service.playListCurrent)
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, bundle))
            }

            MediaCommands.COMMAND_REFRESH_ALL.customAction -> {
                scope.launch {
                    try {
                        repository.refreshAll()
                        val resultData = Bundle().apply {
                            putParcelableArrayList(
                                "songsList",
                                ArrayList(repository.allTracksLinkedHashMap.values)
                            )
                        }
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, resultData))
                    } catch (e: Exception) {
                        future.setException(e)
                    }
                }
                return future
            }

            MediaCommands.COMMAND_GET_PLAY_LIST_ITEM.customAction -> {
                handleGetPlayListItem(args, future)
                return future
            }

            MediaCommands.COMMAND_PlAY_LIST_CHANGE.customAction -> {
                scope.launch {
                    val newPlaylists =
                        repository.getPlayLists() // 强制刷新逻辑在 Repository 内部或 refreshAll
                    // 这里其实可以调用 repository.refreshAll() 或者只刷新 Playlist
                    // 为了简单，我们返回当前列表
                    val resultData = Bundle().apply {
                        putInt("new_playlist_count", newPlaylists.size)
                    }
                    future.set(SessionResult(SessionResult.RESULT_SUCCESS, resultData))
                }
                return future
            }

            MediaCommands.COMMAND_SORT_TRACKS.customAction -> {
                scope.launch {
                    try {
                        val type = args.getString("type")
                        if (!type.isNullOrEmpty()) {
                            // 调用 Repository 执行清理和重载
                            repository.handleSort(type)
                        }
                        // 返回成功，UI 收到结果后通常会触发数据刷新（如重新 getChildren 或 getInitializedData）
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS))
                    } catch (e: Exception) {
                        future.setException(e)
                    }
                }
                return future
            }

            else -> return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    // ==========================================
    // Library Navigation (Media Browser)
    // ==========================================

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val clientPackageName = browser.packageName
        if (clientPackageName == service.packageName) {
            val rootItem = MediaItemUtils.createFullFeaturedRoot()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, null))
        }
        return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_SESSION_DISCONNECTED))
    }

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        scope.launch(Dispatchers.IO) {
            try {
                // 等待初始化完成
                service.isInitialized.await() // 如果你保留了 Service 的初始化锁

                when {
                    parentId == "root" -> {
                        val topLevel = service.createTopLevelCategories()
                        future.set(LibraryResult.ofItemList(topLevel, null))
                    }

                    parentId == "songs_root" -> {
                        val items =
                            repository.getSongs().map { MediaItemUtils.musicItemToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(items, null))
                    }

                    parentId == "albums_root" -> {
                        val items =
                            repository.getAlbums().map { MediaItemUtils.albumToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(items, null))
                    }

                    parentId == "artists_root" -> {
                        val items =
                            repository.getArtists().map { MediaItemUtils.artistToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(items, null))
                    }

                    parentId == "playlists_root" -> {
                        val items =
                            repository.getPlayLists().map { MediaItemUtils.playlistToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(items, null))
                    }

                    parentId == "genres_root" -> {
                        val items =
                            repository.getGenres().map { MediaItemUtils.genreToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(items, null))
                    }

                    parentId == "folders_root" -> {
                        val items =
                            repository.getFolders().map { MediaItemUtils.folderToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(items, null))
                    }

                    // --- 具体子列表 ---
                    parentId.startsWith(PlayListType.Albums.name + "_track_") -> {
                        handlePrefixedId(
                            parentId,
                            PlayListType.Albums.name + "_track_",
                            future
                        ) { id ->
                            repository.getTracksByAlbumId(id)
                                .map { MediaItemUtils.musicItemToMediaItem(it) }
                        }
                    }

                    parentId.startsWith(PlayListType.Artists.name + "_track_") -> {
                        handlePrefixedId(
                            parentId,
                            PlayListType.Artists.name + "_track_",
                            future
                        ) { id ->
                            repository.getTracksByArtistId(id)
                                .map { MediaItemUtils.musicItemToMediaItem(it) }
                        }
                    }

                    parentId.startsWith(PlayListType.Genres.name + "_track_") -> {
                        handlePrefixedId(
                            parentId,
                            PlayListType.Genres.name + "_track_",
                            future
                        ) { id ->
                            repository.getTracksByGenreId(id)
                                .map { MediaItemUtils.musicItemToMediaItem(it) }
                        }
                    }

                    parentId.startsWith(PlayListType.PlayLists.name + "_track_") -> {
                        handlePrefixedId(
                            parentId,
                            PlayListType.PlayLists.name + "_track_",
                            future
                        ) { id ->
                            repository.getTracksByPlayListId(id)
                                .map { MediaItemUtils.musicItemToMediaItem(it) }
                        }
                    }

                    parentId.startsWith(PlayListType.Folders.name + "_track_") -> {
                        handlePrefixedId(
                            parentId,
                            PlayListType.Folders.name + "_track_",
                            future
                        ) { id ->
                            repository.getTracksByFolderId(id)
                                .map { MediaItemUtils.musicItemToMediaItem(it) }
                        }
                    }

                    // --- 包含关系 (Genre/Artist 包含 Albums) ---
                    parentId.startsWith(PlayListType.Genres.name + "_album_") -> {
                        handlePrefixedId(
                            parentId,
                            PlayListType.Genres.name + "_album_",
                            future
                        ) { id ->
                            // 确保触发加载
                            repository.getTracksByGenreId(id)
                            repository.genreHasAlbumMap[id]?.map {
                                MediaItemUtils.albumToMediaItem(
                                    it
                                )
                            } ?: emptyList()
                        }
                    }

                    parentId.startsWith(PlayListType.Artists.name + "_album_") -> {
                        handlePrefixedId(
                            parentId,
                            PlayListType.Artists.name + "_album_",
                            future
                        ) { id ->
                            repository.getTracksByArtistId(id)
                            repository.artistHasAlbumMap[id]?.map {
                                MediaItemUtils.albumToMediaItem(
                                    it
                                )
                            } ?: emptyList()
                        }
                    }

                    else -> future.set(LibraryResult.ofItemList(listOf(), null))
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override fun onGetItem(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        // 解析 type@id 格式
        val parts = mediaId.split("@")
        if (parts.size == 2) {
            val type = parts[0]
            val id = parts[1].toLongOrNull() ?: return super.onGetItem(session, browser, mediaId)

            val item = when (type) {
                PlayListType.Genres.name -> repository.getGenreById(id)
                    ?.let { MediaItemUtils.genreToMediaItem(it) }

                PlayListType.Artists.name -> repository.getArtistById(id)
                    ?.let { MediaItemUtils.artistToMediaItem(it) }

                PlayListType.Albums.name -> repository.getAlbumById(id)
                    ?.let { MediaItemUtils.albumToMediaItem(it) }

                PlayListType.Folders.name -> repository.getFolderById(id)
                    ?.let { MediaItemUtils.folderToMediaItem(it) }

                PlayListType.PlayLists.name -> repository.getPlaylistById(id)
                    ?.let { MediaItemUtils.playlistToMediaItem(it) }

                else -> null
            }

            if (item != null) {
                return Futures.immediateFuture(LibraryResult.ofItem(item, null))
            }
        }
        return super.onGetItem(session, browser, mediaId)
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private suspend fun handlePrefixedId(
        parentId: String,
        prefix: String,
        future: SettableFuture<LibraryResult<ImmutableList<MediaItem>>>,
        loader: suspend (id: Long) -> List<MediaItem>
    ) {
        val id = parentId.removePrefix(prefix).toLongOrNull()
        if (id != null) {
            val items = loader(id)
            future.set(LibraryResult.ofItemList(items, null))
        } else {
            future.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
        }
    }

    private fun handleGetPlayListItem(args: Bundle, future: SettableFuture<SessionResult>) {
        val type = args.getString("type")
        val id = args.getLong("id")
        val bundle = Bundle()

        val data = when (type) {
            PlayListType.Genres.name -> repository.getGenreById(id)
            PlayListType.Artists.name -> repository.getArtistById(id)
            PlayListType.Albums.name -> repository.getAlbumById(id)
            PlayListType.Folders.name -> repository.getFolderById(id)
            PlayListType.PlayLists.name -> repository.getPlaylistById(id)
            else -> null
        }

        if (data != null) {
            bundle.putParcelable("data", data)
            future.set(SessionResult(SessionResult.RESULT_SUCCESS, bundle))
        } else {
            future.set(SessionResult(SessionError.ERROR_BAD_VALUE))
        }
    }

    /**
     * 处理复杂的智能随机逻辑
     * 这部分逻辑依然依赖 Repository 获取数据，然后操作 Service 的播放队列
     */
    private suspend fun handleSmartShuffle(args: Bundle, future: SettableFuture<SessionResult>) {
        val enable = args.getBoolean("enable")

        if (enable) {
            val isQueue = args.getBoolean("queue")
            val autoPlay = args.getBoolean("autoPlay")
            val playListType = args.getString("playListType", "")
            val playListId = args.getLong("playListId", 0L)

            val newMusicItems: ArrayList<MusicItem>? = if (isQueue) {
                service.musicQueue
            } else {
                if (playListType.isNullOrEmpty() || playListId == 0L) null
                else when (playListType) {
                    PlayListType.Songs.name -> ArrayList(repository.getSongs())
                    PlayListType.PlayLists.name -> ArrayList(
                        repository.getTracksByPlayListId(
                            playListId
                        )
                    )

                    PlayListType.Albums.name -> ArrayList(repository.getTracksByAlbumId(playListId))
                    PlayListType.Artists.name -> ArrayList(repository.getTracksByArtistId(playListId))
                    PlayListType.Genres.name -> ArrayList(repository.getTracksByGenreId(playListId))
                    PlayListType.Folders.name -> ArrayList(repository.getTracksByFolderId(playListId))
                    else -> null
                }
            }

            if (newMusicItems.isNullOrEmpty()) {
                future.set(SessionResult(SessionError.ERROR_BAD_VALUE))
                return
            }

            // 对列表进行 ID 标记 (tableId 用于恢复顺序)
            newMusicItems.forEachIndexed { index, musicItem ->
                musicItem.tableId = index.toLong() + 1
            }

            val startMediaId: Long = args.getLong(MediaCommands.KEY_START_MEDIA_ID)

            // 切换到主线程操作 Player
            withContext(Dispatchers.Main) {
                SharedPreferencesUtils.enableShuffle(service, true)

                val currentMusicItem: MusicItem? = newMusicItems.find { it.id == startMediaId }
                val autoToTopEnabled = SharedPreferencesUtils.getAutoToTopRandom(service)

                val finalShuffledQueue = if (autoToTopEnabled && currentMusicItem != null) {
                    val otherItems = newMusicItems.filter { it.id != currentMusicItem.id }
                    mutableListOf<MusicItem>().apply {
                        add(currentMusicItem)
                        addAll(otherItems.shuffled())
                    }
                } else {
                    newMusicItems.shuffled()
                }

                val newMediaItems =
                    finalShuffledQueue.map { MediaItemUtils.musicItemToMediaItem(it) }

                // 确定播放位置
                val newStartIndex = if (currentMusicItem != null) {
                    finalShuffledQueue.indexOfFirst { it.id == currentMusicItem.id }
                        .let { if (it == -1) 0 else it }
                } else {
                    0
                }

                val position = if (isQueue) service.exoPlayer.currentPosition else 0
                val needPlay = service.exoPlayer.isPlaying || autoPlay

                service.exoPlayer.shuffleModeEnabled = true
                service.exoPlayer.setMediaItems(newMediaItems, newStartIndex, position)
                service.exoPlayer.playWhenReady = needPlay
                service.exoPlayer.prepare()
                if (needPlay) service.exoPlayer.play()
            }

        } else {
            // 关闭随机
            withContext(Dispatchers.Main) {
                SharedPreferencesUtils.enableShuffle(service, false)
                val needPlay = service.exoPlayer.isPlaying
                val position = service.exoPlayer.currentPosition

                service.exoPlayer.shuffleModeEnabled = false

                // 恢复顺序
                service.musicQueue.sortBy { it.tableId }

                val currentMediaId = service.exoPlayer.currentMediaItem?.mediaId?.toLong()
                val newStartIndex =
                    service.musicQueue.indexOfFirst { it.id == currentMediaId }.let { max(0, it) }

                val newMediaItems =
                    service.musicQueue.map { MediaItemUtils.musicItemToMediaItem(it) }

                service.exoPlayer.setMediaItems(newMediaItems, newStartIndex, position)
                service.exoPlayer.playWhenReady = needPlay
                service.exoPlayer.prepare()
                if (needPlay) service.exoPlayer.play()
            }
        }

        future.set(SessionResult(SessionResult.RESULT_SUCCESS))
    }
}