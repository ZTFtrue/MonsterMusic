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

    @Suppress("DEPRECATION")
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        // 将 Controller 信息传回 Service (用于后续广播通知，仅针对应用内 Controller)
        if (controller.packageName == service.packageName) {
            service.setControllerInfo(controller)
        }

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
                .add(MediaCommands.COMMAND_SET_FFT_ENGINE)
                .add(MediaCommands.COMMAND_SET_EQUALIZER_TYPE)
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
                .add(MediaCommands.COMMAND_BASS_BOOST_ENABLE)
                .add(MediaCommands.COMMAND_BASS_BOOST_STRENGTH)
                .add(MediaCommands.COMMAND_VIRTUALIZER_ENABLE)
                .add(MediaCommands.COMMAND_VIRTUALIZER_STRENGTH)
                .add(MediaCommands.COMMAND_REVERB_ENABLE)
                .add(MediaCommands.COMMAND_REVERB_SET_PARAMS)
                .add(MediaCommands.COMMAND_CHORUS_ENABLE)
                .add(MediaCommands.COMMAND_CHORUS_SET_PARAMS)
                .add(MediaCommands.COMMAND_FLANGER_ENABLE)
                .add(MediaCommands.COMMAND_FLANGER_SET_PARAMS)
                .add(MediaCommands.COMMAND_POLYPHONY_ENABLE)
                .add(MediaCommands.COMMAND_POLYPHONY_SET_PARAMS)
                .add(MediaCommands.COMMAND_DELAY_ENABLE)
                .add(MediaCommands.COMMAND_DELAY_SET_PARAMS)
                .add(MediaCommands.COMMAND_DSP_SET_PRESET)
                .add(MediaCommands.COMMAND_SET_SHOW_PITCH_FINE)
                .add(MediaCommands.COMMAND_SET_SHOW_SPEED_FINE)
                .add(MediaCommands.COMMAND_SET_TRACK_EFFECT_ENABLE)
                .add(MediaCommands.COMMAND_RESET_TRACK_EFFECT)
                .add(MediaCommands.COMMAND_AUDIO_EFFECT_UPDATE)
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
            MediaCommands.COMMAND_BASS_BOOST_ENABLE.customAction -> {
                val enable = args.getBoolean(MediaCommands.KEY_ENABLE)
                effectManager.setBassBoostEnabled(enable)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_BASS_BOOST_STRENGTH.customAction -> {
                val strength = args.getInt(MediaCommands.KEY_STRENGTH)
                effectManager.setBassBoostStrength(strength)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

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
                val preset = args.getString(MediaCommands.KEY_PRESET)
                args.getIntArray("value")?.let { effectManager.setEqualizerBands(it, preset) }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_DSP_SET_PRESET.customAction -> {
                val preset = args.getString(MediaCommands.KEY_PRESET)
                if (!preset.isNullOrEmpty()) {
                    effectManager.setPreset(preset)
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_SET_SHOW_PITCH_FINE.customAction -> {
                effectManager.setShowPitchFine(args.getBoolean(MediaCommands.KEY_ENABLE))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_SET_SHOW_SPEED_FINE.customAction -> {
                effectManager.setShowSpeedFine(args.getBoolean(MediaCommands.KEY_ENABLE))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_SET_TRACK_EFFECT_ENABLE.customAction -> {
                val enable = args.getBoolean(MediaCommands.KEY_ENABLE)
                scope.launch {
                    effectManager.setTrackEffectEnabled(enable, service.currentPlayTrack?.id, service.exoPlayer)
                    service.broadcastAudioEffectUpdate()
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_RESET_TRACK_EFFECT.customAction -> {
                val currentTrackId = service.currentPlayTrack?.id
                if (currentTrackId != null) {
                    scope.launch {
                        effectManager.resetCurrentTrackEffect(currentTrackId, service.exoPlayer)
                        service.broadcastAudioEffectUpdate()
                    }
                }
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

            // --- Reverb ---
            MediaCommands.COMMAND_REVERB_ENABLE.customAction -> {
                effectManager.setReverbEnabled(args.getBoolean(MediaCommands.KEY_ENABLE))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_REVERB_SET_PARAMS.customAction -> {
                effectManager.setReverbParams(
                    args.getFloat(MediaCommands.KEY_ROOM_SIZE, 0.5f),
                    args.getFloat(MediaCommands.KEY_DAMPING, 0.5f),
                    args.getFloat(MediaCommands.KEY_MIX, 0.3f)
                )
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            // --- Chorus ---
            MediaCommands.COMMAND_CHORUS_ENABLE.customAction -> {
                effectManager.setChorusEnabled(args.getBoolean(MediaCommands.KEY_ENABLE))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_CHORUS_SET_PARAMS.customAction -> {
                effectManager.setChorusParams(
                    args.getFloat(MediaCommands.KEY_RATE, 1.5f),
                    args.getFloat(MediaCommands.KEY_DEPTH, 0.5f),
                    args.getFloat(MediaCommands.KEY_MIX, 0.5f)
                )
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            // --- Flanger ---
            MediaCommands.COMMAND_FLANGER_ENABLE.customAction -> {
                effectManager.setFlangerEnabled(args.getBoolean(MediaCommands.KEY_ENABLE))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_FLANGER_SET_PARAMS.customAction -> {
                effectManager.setFlangerParams(
                    args.getFloat(MediaCommands.KEY_RATE, 0.5f),
                    args.getFloat(MediaCommands.KEY_DEPTH, 0.7f),
                    args.getFloat(MediaCommands.KEY_FEEDBACK, 0.5f),
                    args.getFloat(MediaCommands.KEY_MIX, 0.5f)
                )
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            // --- Polyphony ---
            MediaCommands.COMMAND_POLYPHONY_ENABLE.customAction -> {
                effectManager.setPolyphonyEnabled(args.getBoolean(MediaCommands.KEY_ENABLE))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_POLYPHONY_SET_PARAMS.customAction -> {
                effectManager.setPolyphonyParams(
                    args.getInt(MediaCommands.KEY_SEMITONES, 0),
                    args.getFloat(MediaCommands.KEY_DETUNE, 0.0f),
                    args.getFloat(MediaCommands.KEY_MIX, 0.5f)
                )
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            // --- Delay Effect ---
            MediaCommands.COMMAND_DELAY_ENABLE.customAction -> {
                effectManager.setDelayEnabled(args.getBoolean(MediaCommands.KEY_ENABLE))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_DELAY_SET_PARAMS.customAction -> {
                effectManager.setDelayParams(
                    args.getFloat(MediaCommands.KEY_DELAY, 0.35f),
                    args.getFloat(MediaCommands.KEY_FEEDBACK, 0.4f),
                    args.getFloat(MediaCommands.KEY_MIX, 0.4f)
                )
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

            MediaCommands.COMMAND_SET_FFT_ENGINE.customAction -> {
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            MediaCommands.COMMAND_SET_EQUALIZER_TYPE.customAction -> {
                val eqType = args.getInt(MediaCommands.KEY_EQUALIZER_TYPE, 0)
                effectManager.setEqualizerType(eqType)
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
                    try {
                        // 等待 Service 数据加载完毕
                        service.isInitialized.await()

                        val bundle = Bundle()
                        service.fillInitializedData(bundle)
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS, bundle))
                    } catch (e: Exception) {
                        future.setException(e)
                    }
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
                    try {
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
                    } catch (e: Exception) {
                        future.setException(e)
                    }
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
                    try {
                        withContext(Dispatchers.IO) {
                            val newPlaylists =
                                repository.getPlayLists() // 强制刷新逻辑在 Repository 内部或 refreshAll
                            // 这里其实可以调用 repository.refreshAll() 或者只刷新 Playlist
                            // 为了简单，我们返回当前列表
                            val resultData = Bundle().apply {
                                putInt("new_playlist_count", newPlaylists.size)
                            }
                            future.set(SessionResult(SessionResult.RESULT_SUCCESS, resultData))
                        }
                    } catch (e: Exception) {
                        future.setException(e)
                    }
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
        val rootItem = MediaItemUtils.createFullFeaturedRoot()
        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
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
                service.isInitialized.await()

                when {
                    parentId == "root" -> {
                        val topLevel = service.createTopLevelCategories()
                        future.set(LibraryResult.ofItemList(applyPagination(topLevel, page, pageSize), params))
                    }

                    parentId == "songs_root" -> {
                        val items =
                            repository.getSongs().map { MediaItemUtils.musicItemToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(applyPagination(items, page, pageSize), params))
                    }

                    parentId == "albums_root" -> {
                        val items =
                            repository.getAlbums().map { MediaItemUtils.albumToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(applyPagination(items, page, pageSize), params))
                    }

                    parentId == "artists_root" -> {
                        val items =
                            repository.getArtists().map { MediaItemUtils.artistToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(applyPagination(items, page, pageSize), params))
                    }

                    parentId == "playlists_root" -> {
                        val items =
                            repository.getPlayLists().map { MediaItemUtils.playlistToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(applyPagination(items, page, pageSize), params))
                    }

                    parentId == "genres_root" -> {
                        val items =
                            repository.getGenres().map { MediaItemUtils.genreToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(applyPagination(items, page, pageSize), params))
                    }

                    parentId == "folders_root" -> {
                        val items =
                            repository.getFolders().map { MediaItemUtils.folderToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(applyPagination(items, page, pageSize), params))
                    }

                    // --- 具体子列表 (支持 _track_ 和 @) ---
                    extractId(parentId, PlayListType.Albums.name) != null -> {
                        val id = extractId(parentId, PlayListType.Albums.name)!!
                        val items = repository.getTracksByAlbumId(id)
                            .map { MediaItemUtils.musicItemToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(applyPagination(items, page, pageSize), params))
                    }

                    extractId(parentId, PlayListType.Artists.name) != null -> {
                        val id = extractId(parentId, PlayListType.Artists.name)!!
                        val items = repository.getTracksByArtistId(id)
                            .map { MediaItemUtils.musicItemToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(applyPagination(items, page, pageSize), params))
                    }

                    extractId(parentId, PlayListType.Genres.name) != null -> {
                        val id = extractId(parentId, PlayListType.Genres.name)!!
                        val items = repository.getTracksByGenreId(id)
                            .map { MediaItemUtils.musicItemToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(applyPagination(items, page, pageSize), params))
                    }

                    extractId(parentId, PlayListType.PlayLists.name) != null -> {
                        val id = extractId(parentId, PlayListType.PlayLists.name)!!
                        val items = repository.getTracksByPlayListId(id)
                            .map { MediaItemUtils.musicItemToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(applyPagination(items, page, pageSize), params))
                    }

                    extractId(parentId, PlayListType.Folders.name) != null -> {
                        val id = extractId(parentId, PlayListType.Folders.name)!!
                        val items = repository.getTracksByFolderId(id)
                            .map { MediaItemUtils.musicItemToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(applyPagination(items, page, pageSize), params))
                    }

                    // --- 包含关系 (Genre/Artist 包含 Albums) ---
                    parentId.startsWith(PlayListType.Genres.name + "_album_") -> {
                        handlePrefixedId(
                            parentId,
                            PlayListType.Genres.name + "_album_",
                            future
                        ) { id ->
                            repository.getTracksByGenreId(id)
                            repository.genreHasAlbumMap[id]?.map {
                                MediaItemUtils.albumToMediaItem(it)
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
                                MediaItemUtils.albumToMediaItem(it)
                            } ?: emptyList()
                        }
                    }

                    else -> future.set(LibraryResult.ofItemList(listOf(), params))
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
        if (mediaId == "root") {
            return Futures.immediateFuture(LibraryResult.ofItem(MediaItemUtils.createFullFeaturedRoot(), null))
        }

        val topCategory = service.createTopLevelCategories().find { it.mediaId == mediaId }
        if (topCategory != null) {
            return Futures.immediateFuture(LibraryResult.ofItem(topCategory, null))
        }

        val trackId = mediaId.toLongOrNull()
        if (trackId != null) {
            val track = repository.getTrackById(trackId)
            if (track != null) {
                return Futures.immediateFuture(LibraryResult.ofItem(MediaItemUtils.musicItemToMediaItem(track), null))
            }
        }

        val item = getItemByContainerMediaId(mediaId)
        if (item != null) {
            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }

        return super.onGetItem(session, browser, mediaId)
    }

    override fun onSearch(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        val future = SettableFuture.create<LibraryResult<Void>>()
        scope.launch(Dispatchers.IO) {
            try {
                service.isInitialized.await()
                val result = repository.search(query)
                val totalCount = result.tracks.size + result.albums.size + result.artists.size
                session.notifySearchResultChanged(browser, query, totalCount, params)
                future.set(LibraryResult.ofVoid(params))
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override fun onGetSearchResult(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        scope.launch(Dispatchers.IO) {
            try {
                service.isInitialized.await()
                val result = repository.search(query)
                val mediaItems = ArrayList<MediaItem>()
                result.tracks.forEach { mediaItems.add(MediaItemUtils.musicItemToMediaItem(it)) }
                result.albums.forEach { mediaItems.add(MediaItemUtils.albumToMediaItem(it)) }
                result.artists.forEach { mediaItems.add(MediaItemUtils.artistToMediaItem(it)) }

                val paged = applyPagination(mediaItems, page, pageSize)
                future.set(LibraryResult.ofItemList(paged, params))
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
        scope.launch(Dispatchers.IO) {
            try {
                service.isInitialized.await()
                val queue = service.musicQueue
                if (queue.isNotEmpty()) {
                    val mediaItems = queue.map { MediaItemUtils.musicItemToMediaItem(it) }
                    val currentIndex = queue.indexOfFirst { it.id == service.currentPlayTrack?.id }.coerceAtLeast(0)
                    val position = withContext(Dispatchers.Main) { service.exoPlayer.currentPosition }.coerceAtLeast(0L)
                    future.set(MediaSession.MediaItemsWithStartPosition(mediaItems, currentIndex, position))
                } else {
                    val allSongs = repository.getSongs()
                    if (allSongs.isNotEmpty()) {
                        val mediaItems = allSongs.map { MediaItemUtils.musicItemToMediaItem(it) }
                        future.set(MediaSession.MediaItemsWithStartPosition(mediaItems, 0, 0L))
                    } else {
                        future.setException(UnsupportedOperationException("No media to resume"))
                    }
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
        scope.launch(Dispatchers.IO) {
            try {
                service.isInitialized.await()
                if (controller.packageName == service.packageName && mediaItems.isNotEmpty() && mediaItems.all { it.localConfiguration != null }) {
                    future.set(MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs))
                    return@launch
                }

                val resolved = resolveMediaItemsWithPosition(mediaItems, startIndex, startPositionMs)
                future.set(resolved)
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        val future = SettableFuture.create<MutableList<MediaItem>>()
        scope.launch(Dispatchers.IO) {
            try {
                service.isInitialized.await()
                val resolvedList = mutableListOf<MediaItem>()
                for (item in mediaItems) {
                    if (item.localConfiguration != null) {
                        resolvedList.add(item)
                    } else {
                        val resolved = resolveSingleMediaItem(item)
                        resolvedList.addAll(resolved)
                    }
                }
                future.set(resolvedList)
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private fun <T> applyPagination(items: List<T>, page: Int, pageSize: Int): ImmutableList<T> {
        if (pageSize <= 0 || page < 0) {
            return ImmutableList.copyOf(items)
        }
        val fromIndex = (page * pageSize).coerceAtMost(items.size)
        val toIndex = (fromIndex + pageSize).coerceAtMost(items.size)
        return ImmutableList.copyOf(items.subList(fromIndex, toIndex))
    }

    private fun extractId(mediaId: String, typeName: String): Long? {
        if (mediaId.startsWith("${typeName}_track_")) {
            return mediaId.removePrefix("${typeName}_track_").toLongOrNull()
        }
        if (mediaId.startsWith("${typeName}@")) {
            return mediaId.removePrefix("${typeName}@").toLongOrNull()
        }
        return null
    }

    private fun getItemByContainerMediaId(mediaId: String): MediaItem? {
        val types = listOf(
            PlayListType.Albums.name,
            PlayListType.Artists.name,
            PlayListType.Genres.name,
            PlayListType.PlayLists.name,
            PlayListType.Folders.name
        )
        for (typeName in types) {
            val id = extractId(mediaId, typeName)
            if (id != null) {
                return when (typeName) {
                    PlayListType.Albums.name -> repository.getAlbumById(id)?.let { MediaItemUtils.albumToMediaItem(it) }
                    PlayListType.Artists.name -> repository.getArtistById(id)?.let { MediaItemUtils.artistToMediaItem(it) }
                    PlayListType.Genres.name -> repository.getGenreById(id)?.let { MediaItemUtils.genreToMediaItem(it) }
                    PlayListType.PlayLists.name -> repository.getPlaylistById(id)?.let { MediaItemUtils.playlistToMediaItem(it) }
                    PlayListType.Folders.name -> repository.getFolderById(id)?.let { MediaItemUtils.folderToMediaItem(it) }
                    else -> null
                }
            }
        }
        return null
    }

    private suspend fun resolveMediaItemsWithPosition(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): MediaSession.MediaItemsWithStartPosition {
        if (mediaItems.isEmpty()) {
            val queue = if (service.musicQueue.isNotEmpty()) {
                service.musicQueue
            } else {
                repository.getSongs()
            }
            val items = queue.map { MediaItemUtils.musicItemToMediaItem(it) }
            val currentIndex = queue.indexOfFirst { it.id == service.currentPlayTrack?.id }.coerceAtLeast(0)
            return MediaSession.MediaItemsWithStartPosition(items, currentIndex, 0L)
        }

        if (mediaItems.size == 1) {
            val item = mediaItems[0]
            val searchQuery = item.requestMetadata.searchQuery
            if (!searchQuery.isNullOrBlank()) {
                val searchResult = repository.search(searchQuery)
                val targetTracks = when {
                    searchResult.tracks.isNotEmpty() -> searchResult.tracks
                    searchResult.albums.isNotEmpty() -> repository.getTracksByAlbumId(searchResult.albums[0].id)
                    searchResult.artists.isNotEmpty() -> repository.getTracksByArtistId(searchResult.artists[0].id)
                    else -> {
                        repository.getSongs().filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.artist.contains(searchQuery, ignoreCase = true)
                        }
                    }
                }
                if (targetTracks.isNotEmpty()) {
                    val resolved = targetTracks.map { MediaItemUtils.musicItemToMediaItem(it) }
                    return MediaSession.MediaItemsWithStartPosition(resolved, 0, 0L)
                }
            }

            val mediaId = item.mediaId
            when (mediaId) {
                "root", "" -> {
                    val queue = if (service.musicQueue.isNotEmpty()) service.musicQueue else repository.getSongs()
                    val resolved = queue.map { MediaItemUtils.musicItemToMediaItem(it) }
                    val currentIdx = queue.indexOfFirst { it.id == service.currentPlayTrack?.id }.coerceAtLeast(0)
                    return MediaSession.MediaItemsWithStartPosition(resolved, currentIdx, 0L)
                }
                "songs_root" -> {
                    val songs = repository.getSongs().map { MediaItemUtils.musicItemToMediaItem(it) }
                    return MediaSession.MediaItemsWithStartPosition(songs, 0, 0L)
                }
            }

            val albumId = extractId(mediaId, PlayListType.Albums.name)
            if (albumId != null) {
                val tracks = repository.getTracksByAlbumId(albumId).map { MediaItemUtils.musicItemToMediaItem(it) }
                return MediaSession.MediaItemsWithStartPosition(tracks, 0, 0L)
            }
            val artistId = extractId(mediaId, PlayListType.Artists.name)
            if (artistId != null) {
                val tracks = repository.getTracksByArtistId(artistId).map { MediaItemUtils.musicItemToMediaItem(it) }
                return MediaSession.MediaItemsWithStartPosition(tracks, 0, 0L)
            }
            val playlistId = extractId(mediaId, PlayListType.PlayLists.name)
            if (playlistId != null) {
                val tracks = repository.getTracksByPlayListId(playlistId).map { MediaItemUtils.musicItemToMediaItem(it) }
                return MediaSession.MediaItemsWithStartPosition(tracks, 0, 0L)
            }
            val genreId = extractId(mediaId, PlayListType.Genres.name)
            if (genreId != null) {
                val tracks = repository.getTracksByGenreId(genreId).map { MediaItemUtils.musicItemToMediaItem(it) }
                return MediaSession.MediaItemsWithStartPosition(tracks, 0, 0L)
            }
            val folderId = extractId(mediaId, PlayListType.Folders.name)
            if (folderId != null) {
                val tracks = repository.getTracksByFolderId(folderId).map { MediaItemUtils.musicItemToMediaItem(it) }
                return MediaSession.MediaItemsWithStartPosition(tracks, 0, 0L)
            }

            val trackId = mediaId.toLongOrNull()
            if (trackId != null) {
                val track = repository.getTrackById(trackId)
                if (track != null) {
                    val queueIdx = service.musicQueue.indexOfFirst { it.id == trackId }
                    if (queueIdx != -1) {
                        val resolved = service.musicQueue.map { MediaItemUtils.musicItemToMediaItem(it) }
                        return MediaSession.MediaItemsWithStartPosition(resolved, queueIdx, 0L)
                    }
                    if (track.albumId > 0) {
                        val albumTracks = repository.getTracksByAlbumId(track.albumId)
                        val idx = albumTracks.indexOfFirst { it.id == trackId }
                        if (idx != -1 && albumTracks.isNotEmpty()) {
                            val resolved = albumTracks.map { MediaItemUtils.musicItemToMediaItem(it) }
                            return MediaSession.MediaItemsWithStartPosition(resolved, idx, 0L)
                        }
                    }
                    val allSongs = repository.getSongs()
                    val idx = allSongs.indexOfFirst { it.id == trackId }
                    if (idx != -1) {
                        val resolved = allSongs.map { MediaItemUtils.musicItemToMediaItem(it) }
                        return MediaSession.MediaItemsWithStartPosition(resolved, idx, 0L)
                    }
                    return MediaSession.MediaItemsWithStartPosition(listOf(MediaItemUtils.musicItemToMediaItem(track)), 0, 0L)
                }
            }
        }

        val resolved = mediaItems.mapNotNull { item ->
            if (item.localConfiguration != null) {
                item
            } else {
                val id = item.mediaId.toLongOrNull()
                if (id != null) {
                    repository.getTrackById(id)?.let { MediaItemUtils.musicItemToMediaItem(it) }
                } else {
                    null
                }
            }
        }
        val safeIndex = startIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0))
        return MediaSession.MediaItemsWithStartPosition(resolved, safeIndex, startPositionMs)
    }

    private suspend fun resolveSingleMediaItem(item: MediaItem): List<MediaItem> {
        val searchQuery = item.requestMetadata.searchQuery
        if (!searchQuery.isNullOrBlank()) {
            val searchResult = repository.search(searchQuery)
            val targetTracks = when {
                searchResult.tracks.isNotEmpty() -> searchResult.tracks
                searchResult.albums.isNotEmpty() -> repository.getTracksByAlbumId(searchResult.albums[0].id)
                searchResult.artists.isNotEmpty() -> repository.getTracksByArtistId(searchResult.artists[0].id)
                else -> {
                    repository.getSongs().filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                                it.artist.contains(searchQuery, ignoreCase = true)
                    }
                }
            }
            if (targetTracks.isNotEmpty()) {
                return targetTracks.map { MediaItemUtils.musicItemToMediaItem(it) }
            }
        }

        val mediaId = item.mediaId
        if (mediaId == "songs_root") {
            return repository.getSongs().map { MediaItemUtils.musicItemToMediaItem(it) }
        }

        val albumId = extractId(mediaId, PlayListType.Albums.name)
        if (albumId != null) {
            return repository.getTracksByAlbumId(albumId).map { MediaItemUtils.musicItemToMediaItem(it) }
        }
        val artistId = extractId(mediaId, PlayListType.Artists.name)
        if (artistId != null) {
            return repository.getTracksByArtistId(artistId).map { MediaItemUtils.musicItemToMediaItem(it) }
        }
        val playlistId = extractId(mediaId, PlayListType.PlayLists.name)
        if (playlistId != null) {
            return repository.getTracksByPlayListId(playlistId).map { MediaItemUtils.musicItemToMediaItem(it) }
        }
        val genreId = extractId(mediaId, PlayListType.Genres.name)
        if (genreId != null) {
            return repository.getTracksByGenreId(genreId).map { MediaItemUtils.musicItemToMediaItem(it) }
        }
        val folderId = extractId(mediaId, PlayListType.Folders.name)
        if (folderId != null) {
            return repository.getTracksByFolderId(folderId).map { MediaItemUtils.musicItemToMediaItem(it) }
        }

        val trackId = mediaId.toLongOrNull()
        if (trackId != null) {
            val track = repository.getTrackById(trackId)
            if (track != null) {
                return listOf(MediaItemUtils.musicItemToMediaItem(track))
            }
        }
        return emptyList()
    }

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

                val currentMediaId = service.exoPlayer.currentMediaItem?.let { MediaItemUtils.getId(it) }
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