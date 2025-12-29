package com.ztftrue.music.play

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.ztftrue.music.MainActivity
import com.ztftrue.music.PlayMusicWidget
import com.ztftrue.music.R
import com.ztftrue.music.play.manager.AudioEffectManager
import com.ztftrue.music.play.manager.MediaCommands
import com.ztftrue.music.play.manager.MusicLibraryRepository
import com.ztftrue.music.play.manager.PlaySessionCallback
import com.ztftrue.music.play.manager.SleepTimerManager
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.model.CurrentList
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.SharedPreferencesUtils
import com.ztftrue.music.utils.model.AnyListBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CompletableDeferred

@UnstableApi
class PlayService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null
    lateinit var exoPlayer: ExoPlayer

    // --- 组件管理器 ---
    lateinit var repository: MusicLibraryRepository
    lateinit var effectManager: AudioEffectManager
    lateinit var sleepManager: SleepTimerManager
    val isInitialized = CompletableDeferred<Unit>()

    // --- 协程作用域 (核心补充) ---
    // SupervisorJob 确保一个子协程失败不会导致整个 Scope 取消
    private val serviceJob = SupervisorJob()

    // 默认使用 Main 线程，因为 Media3/ExoPlayer 的大多数操作需要在主线程执行
    // 耗时操作需使用 withContext(Dispatchers.IO)
    val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    // --- 播放状态数据 ---
    var musicQueue = ArrayList<MusicItem>()
    var playListCurrent: AnyListBase? = null
    var currentPlayTrack: MusicItem? = null
    private var autoHandleFocus = true

    // 数据库引用 (仅用于简单的状态保存，复杂查询走 Repository)
    private lateinit var db: MusicDatabase
    private val dbQueueWriteMutex = Mutex()

    // 回调通知用
    private var mControllerInfo: MediaSession.ControllerInfo? = null
    private var headsetCallback: HeadsetConnectionCallback? = null
    private lateinit var audioManager: AudioManager

    override fun onCreate() {
        super.onCreate()
        db = MusicDatabase.getDatabase(this)

        // 1. 初始化各功能模块
        repository = MusicLibraryRepository(this)
        effectManager = AudioEffectManager(this)
        sleepManager = SleepTimerManager(
            onTimerTick = { remaining -> broadcastSleepUpdate(remaining) },
            onTimerFinish = { playCompleted -> handleSleepFinish(playCompleted) }
        )

        // 2. 初始化播放器 (需要 effectManager 准备好)
        initExoPlayer()

        // 3. 构建 Session
        val contentIntent = Intent(this, MainActivity::class.java)
        val pendingContentIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaLibrarySession.Builder(
            this,
            exoPlayer,
            // 传入所有 Manager 给 Callback
            PlaySessionCallback(this, repository, effectManager, sleepManager),
        ).setSessionActivity(pendingContentIntent)
            .build()

        // 4. 初始化其他系统服务
        setupSystemServices()

        // 5. 启动数据加载
        initializePlayerData()
    }

    private fun setupSystemServices() {
        if (SharedPreferencesUtils.getAutoPlayEnable(this)) {
            headsetCallback = HeadsetConnectionCallback(mediaSession?.player, this)
            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.registerAudioDeviceCallback(
                headsetCallback,
                Handler(Looper.getMainLooper())
            )
        }
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))

        // Widget 状态初始化
        if (com.ztftrue.music.utils.Utils.hasAppWidget(this, PlayMusicWidget::class.java)) {
            SharedPreferencesUtils.setWidgetEnable(this, true)
        } else {
            SharedPreferencesUtils.setWidgetEnable(this, false)
        }
    }

    private fun initializePlayerData() {
        serviceScope.launch {
            // 在 IO 线程并发加载数据
            withContext(Dispatchers.IO) {
                // 等待 Repository 和 EffectManager 加载完成
                repository.loadAllData()
                effectManager.initEffects()
                loadLastQueue() // 加载上次播放队列
                loadPlayConfig() // 加载播放配置
            }
            // 回到主线程应用到播放器
            applyLoadedDataToPlayer()
            isInitialized.complete(Unit)
        }
    }

    private fun initExoPlayer() {
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink {
                // 关键：将 AudioEffectManager 中的 Processor 注入
                val au = DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(effectManager.equalizerAudioProcessor, effectManager.spatialAudioProcessor ))
                    .build()

                return object : ForwardingAudioSink(au) {
                    override fun configure(
                        inputFormat: Format,
                        specifiedBufferSize: Int,
                        outputChannels: IntArray?
                    ) {
                        // 每次配置音频 sink 时保存时长等信息
                        serviceScope.launch {
                            SharedPreferencesUtils.saveCurrentDuration(
                                this@PlayService,
                                exoPlayer.duration
                            )
                        }
                        super.configure(inputFormat, specifiedBufferSize, outputChannels)
                    }
                }
            }
        }

        val trackSelector = DefaultTrackSelector(this, AdaptiveTrackSelection.Factory())

        exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector)
            .build()

        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true // 初始 focus 处理
        )

        exoPlayer.shuffleOrder = NoShuffleOrder(0)
        exoPlayer.addListener(playerListener)
    }

    // --- Player Listener (精简版，主要逻辑保持不变) ---
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateWidget(isPlaying)
            SharedPreferencesUtils.saveCurrentDuration(this@PlayService, exoPlayer.currentPosition)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (musicQueue.isEmpty() || exoPlayer.currentMediaItemIndex >= musicQueue.size) return
            val nextTrack = musicQueue[exoPlayer.currentMediaItemIndex]

            if (currentPlayTrack?.id != nextTrack.id) {
                currentPlayTrack = nextTrack
                SharedPreferencesUtils.saveCurrentPlayMusicId(this@PlayService, nextTrack.id)
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    SharedPreferencesUtils.saveCurrentDuration(this@PlayService, 0)
                }
                updateWidget(exoPlayer.isPlaying)
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (!timeline.isEmpty && Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED == reason) {
                syncQueueWithPlayer(timeline)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e("PlayService", "Player Error", error)
            Toast.makeText(
                this@PlayService,
                getString(R.string.play_error_play_next),
                Toast.LENGTH_SHORT
            ).show()
            if (exoPlayer.hasNextMediaItem()) {
                exoPlayer.seekToNextMediaItem()
                exoPlayer.prepare()
            }
        }

        override fun onVolumeChanged(volume: Float) {
            SharedPreferencesUtils.saveVolume(this@PlayService, (volume * 100).toInt())
        }
    }

    // --- 供 Callback 调用的 Bridge 方法 ---

    fun setControllerInfo(controller: MediaSession.ControllerInfo) {
        this.mControllerInfo = controller
    }

    fun setAutoHandleAudioFocus(autoHandle: Boolean) {
        this.autoHandleFocus = autoHandle
        SharedPreferencesUtils.setAutoHandleAudioFocus(this, autoHandle)
        exoPlayer.setAudioAttributes(exoPlayer.audioAttributes, autoHandle)
    }

    fun fillInitializedData(bundle: Bundle) {
        // --- 1. 播放器基础状态 ---
        bundle.putInt("volume", (exoPlayer.volume * 100).toInt())
        bundle.putBoolean("isPlaying", exoPlayer.isPlaying)
        bundle.putInt("index", exoPlayer.currentMediaItemIndex)
        bundle.putInt("repeat", exoPlayer.repeatMode)
        bundle.putLong("playListID", playListCurrent?.id ?: -1)
        bundle.putString("playListType", playListCurrent?.type?.name)
        bundle.putSerializable("playListCurrent", playListCurrent)

        // --- 2. 播放队列与当前歌曲 ---
        bundle.putParcelableArrayList("musicQueue", musicQueue)
        bundle.putParcelable("musicItem", currentPlayTrack)

        // --- 3. 音效设置 (来自 AudioEffectManager) ---
        bundle.putFloat("pitch", effectManager.auxr.pitch)
        bundle.putFloat("speed", effectManager.auxr.speed)
        bundle.putFloat("Q", effectManager.auxr.equalizerQ)
        bundle.putBoolean("equalizerEnable", effectManager.equalizerAudioProcessor.isSetActive())
        bundle.putIntArray("equalizerValue", effectManager.equalizerAudioProcessor.getBandLevels())
        bundle.putFloat("delayTime", effectManager.auxr.echoDelay)
        bundle.putFloat("decay", effectManager.auxr.echoDecay)
        bundle.putBoolean("echoActive", effectManager.auxr.echo)
        bundle.putBoolean("echoFeedBack", effectManager.auxr.echoRevert)
        bundle.putInt("virtualStrength", effectManager.auxr.virtualizerStrength)
        bundle.putBoolean("enableVirtual", effectManager.auxr.virtualizerEnabled)

        // --- 4. 睡眠定时器 (来自 SleepTimerManager) ---
        bundle.putLong("sleepTime", sleepManager.sleepTime)
        bundle.putLong("remaining", sleepManager.remainingTime)
        // 补充：告知前端是否要在播放完成后暂停
        bundle.putBoolean("play_completed", sleepManager.playCompleted)

        // --- 5. UI 配置与列表数据 (来自 MusicLibraryRepository) ---
        // 【补充】首页标签列表
        bundle.putParcelableArrayList("mainTabList", repository.mainTabList)
        // 【补充】排序指示器列表
        bundle.putParcelableArrayList("showIndicatorList", repository.showIndicatorList)

    }

    fun handleSortQueue(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        // 这里的逻辑稍微复杂，ExoPlayer 的 moveMediaItem 会触发 TimelineChange，
        // 从而触发 syncQueueWithPlayer。
        // 但为了保持 musicQueue 状态的一致性，通常先更新 UI 队列，再更新 Player
        if (exoPlayer.currentMediaItemIndex in 0 until musicQueue.size) {
            // 直接操作 Player，依赖 onTimelineChanged 同步回 musicQueue
            exoPlayer.moveMediaItem(fromIndex, toIndex)
        }
    }

    fun handleClearQueue() {
        exoPlayer.pause()
        exoPlayer.clearMediaItems()
        playListCurrent = null
        currentPlayTrack = null
        musicQueue.clear()
        SharedPreferencesUtils.saveCurrentPlayMusicId(this, -1)

        serviceScope.launch(Dispatchers.IO) {
            db.QueueDao().deleteAllQueue()
            db.CurrentListDao().delete()
        }
    }

    fun handleChangePlaylist(args: Bundle) {
        val playList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            args.getParcelable("playList", AnyListBase::class.java)
        } else {
            @Suppress("deprecation")
            args.getParcelable("playList")
        }

        if (playList != null) {
            playListCurrent = playList
            serviceScope.launch(Dispatchers.IO) {
                var currentList = db.CurrentListDao().findCurrentList()
                if (currentList == null) {
                    currentList = CurrentList(null, playList.id, playList.type.name)
                    db.CurrentListDao().insert(currentList)
                } else {
                    currentList.listID = playList.id
                    currentList.type = playList.type.name
                    db.CurrentListDao().update(currentList)
                }
            }
        }
    }

    fun removeTrackFromPlayer(id: Long) {
        // 查找队列中所有匹配该 ID 的索引 (倒序删除防止索引偏移)
        val indicesToRemove = mutableListOf<Int>()
        for (i in 0 until exoPlayer.mediaItemCount) {
            if (exoPlayer.getMediaItemAt(i).mediaId == id.toString()) {
                indicesToRemove.add(i)
            }
        }
        // ExoPlayer 没有 removeMediaItems(indices)，只能逐个删或删范围
        // 简单处理：找到第一个删除
        if (indicesToRemove.isNotEmpty()) {
            exoPlayer.removeMediaItem(indicesToRemove[0])
        }

        if (exoPlayer.mediaItemCount == 0) {
            playListCurrent = null
        }
    }

    fun createTopLevelCategories(): List<MediaItem> {
        return listOf(
            createBrowsableItem("songs_root", "歌曲"),
            createBrowsableItem("albums_root", "专辑"),
            createBrowsableItem("artists_root", "艺术家"),
            createBrowsableItem("playlists_root", "播放列表"),
            createBrowsableItem("genres_root", "流派"),
            createBrowsableItem("folders_root", "文件夹")
        )
    }

    // --- 内部辅助方法 ---

    private fun createBrowsableItem(id: String, title: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()
        return MediaItem.Builder().setMediaId(id).setMediaMetadata(metadata).build()
    }

    private fun syncQueueWithPlayer(timeline: Timeline) {
        if (timeline.isEmpty) return
        val newQueue = mutableListOf<MusicItem>()
        for (i in 0 until timeline.windowCount) {
            val window = timeline.getWindow(i, Timeline.Window())
            val mediaItem = window.mediaItem
            MediaItemUtils.mediaItemToMusicItem(mediaItem)?.let {
                // 【关键】同步时，必须根据当前 Timeline 的位置重置 tableId
                it.tableId = i.toLong()
                newQueue.add(it)
            }
        }
        // 简单的 Diff 检查
        // 注意：如果我们在 handleSortQueue 中已经更新了 musicQueue，
        // 这里的 newQueue 应该和 musicQueue 内容一致（除了对象引用可能不同）。
        // 我们主要比较 ID 序列是否一致。
        val currentIds = musicQueue.map { it.id }
        val newIds = newQueue.map { it.id }

        if (currentIds != newIds) {
            musicQueue.clear()
            musicQueue.addAll(newQueue)

            // 异步写入数据库
            serviceScope.launch(Dispatchers.IO) {
                dbQueueWriteMutex.withLock {
                    db.QueueDao().deleteAllQueue()
                    if (musicQueue.isNotEmpty()) {
                        db.QueueDao().insertAll(musicQueue)
                    }
                }
            }
        }
    }

    private fun broadcastSleepUpdate(remaining: Long) {
        val bundle = Bundle().apply { putLong("remaining", remaining) }
        mControllerInfo?.let {
            mediaSession?.sendCustomCommand(it, MediaCommands.COMMAND_SLEEP_STATE_UPDATE, bundle)
        }
    }

    private fun handleSleepFinish(playCompleted: Boolean) {
        if (!playCompleted) {
            exoPlayer.pause()
        }
        // 如果 playCompleted 为 true，则等待当前歌曲播放完毕 (逻辑需在 onMediaItemTransition 中处理标记位)
        broadcastSleepUpdate(0)
    }

    private fun updateWidget(isPlaying: Boolean) {
        if (!SharedPreferencesUtils.getWidgetEnable(this)) return

        val intent = Intent(this, PlayMusicWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra("source", packageName)
            putExtra("playingStatus", isPlaying)
            putExtra("title", currentPlayTrack?.name ?: "")
            putExtra("author", currentPlayTrack?.artist ?: "")
            putExtra("path", currentPlayTrack?.path ?: "")
            putExtra("id", currentPlayTrack?.id ?: 0L)

            val ids = AppWidgetManager.getInstance(application)
                .getAppWidgetIds(ComponentName(application, PlayMusicWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }

        SharedPreferencesUtils.setWidgetData(this, isPlaying, currentPlayTrack)
        sendBroadcast(intent)
    }

    private fun loadLastQueue() {
        val queue = if (SharedPreferencesUtils.getEnableShuffle(this)) {
            db.QueueDao().findQueueShuffle()
        } else {
            db.QueueDao().findQueue()
        }
        musicQueue.clear()

        // 过滤有效歌曲
        queue.forEach {
            if (repository.allTracksLinkedHashMap.containsKey(it.id)) {
                musicQueue.add(it)
            }
        }

        if (musicQueue.isNotEmpty()) {
            val idCurrent = SharedPreferencesUtils.getCurrentPlayId(this)
            currentPlayTrack = repository.allTracksLinkedHashMap[idCurrent]

            val plaC = db.CurrentListDao().findCurrentList()
            if (plaC != null) {
                playListCurrent = AnyListBase(plaC.listID, enumValueOf(plaC.type))
            }
        }
    }

    private suspend fun loadPlayConfig() {
        val config = db.PlayConfigDao().findConfig()
        autoHandleFocus = SharedPreferencesUtils.getAutoHandleAudioFocus(this)
        // 应用到 Player (需切换回主线程)
        withContext(Dispatchers.Main) {
            exoPlayer.repeatMode = config?.repeatModel ?: Player.REPEAT_MODE_ALL
            setAutoHandleAudioFocus(autoHandleFocus)
        }
    }

    private fun applyLoadedDataToPlayer() {
        // 音量
        val volume = SharedPreferencesUtils.getVolume(this)
        exoPlayer.volume = volume / 100f

        // 应用音效参数
        effectManager.applyPlaybackParameters(exoPlayer)

        // 恢复队列
        if (musicQueue.isNotEmpty()) {
            val mediaItems = musicQueue.map { MediaItemUtils.musicItemToMediaItem(it) }

            exoPlayer.shuffleModeEnabled = SharedPreferencesUtils.getEnableShuffle(this)
//            exoPlayer.setMediaItems(mediaItems)

            // 恢复位置
            val currentIndex = musicQueue.indexOfFirst { it.id == currentPlayTrack?.id }
                .let { if (it == -1) 0 else it }
            var position = SharedPreferencesUtils.getCurrentPosition(this)
            if (currentPlayTrack != null && position >= currentPlayTrack!!.duration) position = 0

//            exoPlayer.seekTo(currentIndex, position)
            exoPlayer.setMediaItems(mediaItems, currentIndex, position)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = false // 默认不自动播放
        }
    }
    /**
     * 处理歌曲信息更新
     * 1. 更新 Repository 缓存
     * 2. 更新播放队列 musicQueue
     * 3. 如果是当前播放歌曲，刷新 UI/Widget
     * 4. 更新队列数据库
     */
    suspend fun handleTrackUpdate(id: Long): MusicItem? {
        // 1. 获取最新数据
        val updatedItem = repository.reloadTrack(id) ?: return null

        // 2. 更新播放队列 (在主线程操作以避免并发问题，因为 musicQueue 是 UI 数据源)
        // 需切换到 Main，因为 musicQueue 可能正在被 Adapter 或 ExoPlayer 访问
        withContext(Dispatchers.Main) {
            var isCurrentTrackUpdated = false

            // 遍历队列，找到所有 ID 匹配的项进行更新
            // (队列中可能存在重复的同一首歌，所以要遍历整个列表)
            musicQueue.forEach { item ->
                if (item.id == id) {
                    // 逐个属性复制，保持对象引用不变，或者直接替换对象
                    // 这里推荐更新属性，这样引用的地方都能感知
                    item.name = updatedItem.name
                    item.path = updatedItem.path
                    item.duration = updatedItem.duration
                    item.displayName = updatedItem.displayName
                    item.album = updatedItem.album
                    item.albumId = updatedItem.albumId
                    item.artist = updatedItem.artist
                    item.artistId = updatedItem.artistId
                    item.genre = updatedItem.genre
                    item.genreId = updatedItem.genreId
                    item.year = updatedItem.year
                    item.songNumber = updatedItem.songNumber
                    // tableId 和 priority 不需要变
                }
            }

            // 3. 检查是否需要更新当前播放信息
            if (currentPlayTrack?.id == id) {
                // 更新 currentPlayTrack 的引用或属性
                currentPlayTrack?.let {
                    it.name = updatedItem.name
                    it.artist = updatedItem.artist
                    it.album = updatedItem.album
                    // ... 其他属性
                }
                isCurrentTrackUpdated = true

                // 刷新 Widget 和 通知栏
                updateWidget(exoPlayer.isPlaying)
                // 如果你需要刷新系统媒体通知的 Metadata，Media3 通常会自动读取
                // 但如果属性变了，可能需要 invalidate
//                mediaSession?.player?.invalidateMediaLibraryInformation()
            }

            // 4. 持久化更新到数据库 (切换回 IO)
            serviceScope.launch(Dispatchers.IO) {
                // 如果 QueueDao 支持 update(item)，则直接更新
                // 考虑到队列中该 ID 可能有多条，且我们已经修改了内存对象
                // 最简单暴力且安全的方法是重写队列 (或者 updateAll)
                dbQueueWriteMutex.withLock {
                    // 方法 A: update 单个对象 (如果 Room Entity 主键是 tableId 而不是 songId，这样做很难)
                    // 方法 B: 既然 musicQueue 已经在内存中更新了，直接覆盖保存
                    // 考虑到性能，如果队列很长，最好是 QueueDao().update(musicItem) WHERE songId = :id
                    // 但为了保持一致性，这里沿用之前的全量保存逻辑
                    db.QueueDao().deleteAllQueue()
                    if (musicQueue.isNotEmpty()) {
                        db.QueueDao().insertAll(musicQueue)
                    }
                }
            }
        }

        return updatedItem
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onDestroy() {
        // 取消所有协程
        serviceJob.cancel()

        stopForeground(STOP_FOREGROUND_REMOVE)
        sleepManager.stopTimer()

        SharedPreferencesUtils.saveCurrentDuration(this, exoPlayer.currentPosition)

        mediaSession?.release()
        exoPlayer.release()

        if (headsetCallback != null) {
            audioManager.unregisterAudioDeviceCallback(headsetCallback)
        }
        super.onDestroy()
    }
}