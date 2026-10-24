package com.ztftrue.music.play.manager

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.ztftrue.music.effects.EqualizerAudioProcessor
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.model.Auxr
import com.ztftrue.music.sqlData.model.PlayConfig
import com.ztftrue.music.utils.SharedPreferencesUtils
import com.ztftrue.music.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class AudioEffectManager(private val context: Context) {

    // 核心音频处理器，需要在 Service 创建 ExoPlayer 时通过 RenderersFactory 传入
    val equalizerAudioProcessor: EqualizerAudioProcessor = EqualizerAudioProcessor()
    private val db: MusicDatabase = MusicDatabase.getDatabase(context)

    companion object {
        const val CLEANUP_PREFS = "app_internal_cleanup"
        const val KEY_AUDIO_EFFECTS_CLEANED_V3 = "legacy_audio_effects_cleaned_v3"

        fun cleanLegacyPreferences(context: Context) {
            val prefs = listOf("audio_effects_prefs", "Equalizer", "SelectedPreset")
            for (name in prefs) {
                try {
                    context.getSharedPreferences(name, MODE_PRIVATE).edit().clear().apply()
                    context.deleteSharedPreferences(name)
                } catch (_: Exception) {}
            }
        }

        fun createDefaultAuxr(id: Long = 0L) = Auxr(
            id = id,
            speed = 1f,
            pitch = 1f,
            echo = false,
            echoDelay = 0.2f,
            echoDecay = 0.5f,
            echoRevert = true,
            equalizer = false,
            equalizerBand = IntArray(10), // 10段EQ
            equalizerQ = Utils.Q,
            virtualizerEnabled = false,
            virtualizerStrength = 0,
            bassBoostEnabled = false,
            bassBoostStrength = 0,
            equalizerType = 0,
            selectedPreset = Utils.custom,
            showPitchFine = false,
            showSpeedFine = false,
            delayEnabled = false,
            delayTime = 0.35f,
            delayFeedback = 0.4f,
            delayMix = 0.4f,
            reverbEnabled = false,
            reverbRoomSize = 0.5f,
            reverbDamping = 0.5f,
            reverbMix = 0.3f,
            chorusEnabled = false,
            chorusRate = 1.5f,
            chorusDepth = 0.5f,
            chorusMix = 0.5f,
            flangerEnabled = false,
            flangerRate = 0.5f,
            flangerDepth = 0.7f,
            flangerFeedback = 0.5f,
            flangerMix = 0.5f,
            polyphonyEnabled = false,
            polyphonySemitones = 0,
            polyphonyDetune = 0.0f,
            polyphonyMix = 0.5f
        )
    }

    // 默认配置，稍后会从数据库覆盖
    var auxr = createDefaultAuxr(0)

    var globalAuxr: Auxr = auxr.copy(id = 0)
    var trackEffectEnabled: Boolean = false
    var currentTrackId: Long? = null

    @Volatile
    private var musicVisualizationEnable = false
    @Volatile
    private var isVisualizationUiConnected = false

    /**
     * 初始化音效设置
     * 必须在协程中调用 (IO上下文)
     */
    suspend fun initEffects(currentPlayingTrackId: Long? = null, exoPlayer: ExoPlayer? = null) = withContext(Dispatchers.IO) {
        cleanLegacyPreferences(context)

        val cleanupPrefs = context.getSharedPreferences(CLEANUP_PREFS, MODE_PRIVATE)
        if (!cleanupPrefs.getBoolean(KEY_AUDIO_EFFECTS_CLEANED_V3, false)) {
            // One-time cleanup to ensure all effects and equalizer are disabled by default
            // and old settings/track-specific effect data are removed
            auxr = createDefaultAuxr(0)
            globalAuxr = auxr.copy(id = 0)
            db.AuxDao().upsert(globalAuxr)
            db.AuxDao().deleteAllTrackAux()

            val config = db.PlayConfigDao().findConfig()
            if (config != null) {
                config.trackEffectEnabled = false
                db.PlayConfigDao().update(config)
            } else {
                db.PlayConfigDao().insert(PlayConfig(id = 1, repeatModel = 0, trackEffectEnabled = false))
            }
            trackEffectEnabled = false
            cleanupPrefs.edit().putBoolean(KEY_AUDIO_EFFECTS_CLEANED_V3, true).apply()
        } else {
            // 0. 加载播放配置 (是否开启每首歌专属音效)
            val config = db.PlayConfigDao().findConfig()
            trackEffectEnabled = config?.trackEffectEnabled ?: false

            // 1. 加载全局 Auxr 配置 (id = 0)
            val auxTemp = db.AuxDao().findGlobalAux() ?: db.AuxDao().findFirstAux()
            if (auxTemp == null) {
                auxr = createDefaultAuxr(0)
                db.AuxDao().upsert(auxr)
                globalAuxr = auxr.copy(id = 0)
            } else {
                auxr = auxTemp
                globalAuxr = auxr.copy(id = 0)
            }
        }

        // 2. 如果开启了每首歌专属音效且有当前播放歌曲，则载入该歌曲的 Auxr
        currentTrackId = currentPlayingTrackId
        if (trackEffectEnabled && currentPlayingTrackId != null && currentPlayingTrackId > 0) {
            val trackAux = db.AuxDao().findAuxById(currentPlayingTrackId)
            auxr = trackAux ?: createDefaultAuxr(currentPlayingTrackId)
        }

        // 3. 应用所有音效
        withContext(Dispatchers.Main) {
            applyAllDspEffects(exoPlayer)
        }

        // 4. 加载可视化设置
        loadVisualizationSettings()
    }

    fun applyAllDspEffects(exoPlayer: ExoPlayer? = null) {
        // 1. Delay
        equalizerAudioProcessor.setDelay(
            auxr.delayEnabled,
            auxr.delayTime,
            auxr.delayFeedback,
            auxr.delayMix
        )

        // 2. Echo
        equalizerAudioProcessor.setDelayTime(auxr.echoDelay)
        equalizerAudioProcessor.setDecay(auxr.echoDecay)
        equalizerAudioProcessor.setFeedBack(auxr.echoRevert)
        equalizerAudioProcessor.setEchoActive(auxr.echo)

        // 3. Bass Boost & Virtualizer
        equalizerAudioProcessor.setBassBoost(auxr.bassBoostEnabled, auxr.bassBoostStrength / 1000f)
        equalizerAudioProcessor.setVirtualizer(auxr.virtualizerEnabled, auxr.virtualizerStrength / 1000f)

        // 4. Reverb, Chorus, Flanger, Polyphony
        loadAdvancedEffectsSettings()

        // 5. Equalizer
        equalizerAudioProcessor.setEqualizerActive(auxr.equalizer)
        equalizerAudioProcessor.setQ(auxr.equalizerQ, false)
        equalizerAudioProcessor.setEqualizerType(auxr.equalizerType)
        loadEqPresets()

        // 6. PlaybackParameters (Speed & Pitch)
        if (exoPlayer != null) {
            applyPlaybackParameters(exoPlayer)
        }

        // 7. Reset Limiter
        equalizerAudioProcessor.resetLimiter()
    }

    suspend fun switchTrack(trackId: Long, exoPlayer: ExoPlayer?) = withContext(Dispatchers.IO) {
        flushDb()
        currentTrackId = trackId
        if (!trackEffectEnabled) {
            return@withContext
        }
        val trackAux = db.AuxDao().findAuxById(trackId)
        auxr = trackAux ?: createDefaultAuxr(trackId)
        withContext(Dispatchers.Main) {
            applyAllDspEffects(exoPlayer)
        }
    }

    suspend fun setTrackEffectEnabled(enable: Boolean, currentPlayingTrackId: Long?, exoPlayer: ExoPlayer?) = withContext(Dispatchers.IO) {
        flushDb()
        trackEffectEnabled = enable
        currentTrackId = currentPlayingTrackId

        val config = db.PlayConfigDao().findConfig()
        if (config != null) {
            config.trackEffectEnabled = enable
            db.PlayConfigDao().update(config)
        } else {
            db.PlayConfigDao().insert(PlayConfig(id = 1, repeatModel = 0, trackEffectEnabled = enable))
        }

        if (enable && currentPlayingTrackId != null && currentPlayingTrackId > 0) {
            val trackAux = db.AuxDao().findAuxById(currentPlayingTrackId)
            auxr = trackAux ?: createDefaultAuxr(currentPlayingTrackId)
        } else {
            val globalTemp = db.AuxDao().findGlobalAux() ?: db.AuxDao().findFirstAux()
            auxr = globalTemp ?: globalAuxr.copy(id = 0)
            globalAuxr = auxr.copy(id = 0)
        }

        withContext(Dispatchers.Main) {
            applyAllDspEffects(exoPlayer)
        }
    }

    suspend fun resetCurrentTrackEffect(trackId: Long, exoPlayer: ExoPlayer?) = withContext(Dispatchers.IO) {
        flushDb()
        db.AuxDao().deleteAuxById(trackId)
        auxr = createDefaultAuxr(trackId)
        withContext(Dispatchers.Main) {
            applyAllDspEffects(exoPlayer)
        }
    }



    private fun loadAdvancedEffectsSettings() {
        equalizerAudioProcessor.setReverb(
            auxr.reverbEnabled,
            auxr.reverbRoomSize,
            auxr.reverbDamping,
            auxr.reverbMix
        )
        equalizerAudioProcessor.setChorus(
            auxr.chorusEnabled,
            auxr.chorusRate,
            auxr.chorusDepth,
            auxr.chorusMix
        )
        equalizerAudioProcessor.setFlanger(
            auxr.flangerEnabled,
            auxr.flangerRate,
            auxr.flangerDepth,
            auxr.flangerFeedback,
            auxr.flangerMix
        )
        equalizerAudioProcessor.setPolyphony(
            auxr.polyphonyEnabled,
            auxr.polyphonySemitones,
            auxr.polyphonyDetune,
            auxr.polyphonyMix
        )
    }

    private fun loadEqPresets() {
        val selectedPreset = auxr.selectedPreset
        if (selectedPreset == Utils.custom) {
            // 如果是自定义，使用数据库中保存的 band 值
            for (i in auxr.equalizerBand.indices) {
                if (i < 10) {
                    equalizerAudioProcessor.setBand(i, auxr.equalizerBand[i])
                }
            }
        } else {
            // 如果是预设，从 Utils 中获取并应用
            Utils.eqPreset[selectedPreset]?.forEachIndexed { index, value ->
                equalizerAudioProcessor.setBand(index, value)
            }
        }
    }

    private fun loadVisualizationSettings() {
        musicVisualizationEnable = SharedPreferencesUtils.getEnableMusicVisualization(context)
        equalizerAudioProcessor.setVisualizationAudioActive(musicVisualizationEnable && isVisualizationUiConnected)
    }

    fun setBassBoostEnabled(enable: Boolean) {
        auxr.bassBoostEnabled = enable
        equalizerAudioProcessor.setBassBoost(enable, auxr.bassBoostStrength / 1000f)
        updateDb()
    }

    fun setBassBoostStrength(strength: Int) {
        auxr.bassBoostStrength = strength
        equalizerAudioProcessor.setBassBoost(auxr.bassBoostEnabled, strength / 1000f)
        updateDb()
    }

    fun setSpatialEnabled(enable: Boolean) {
        auxr.virtualizerEnabled = enable
        equalizerAudioProcessor.setVirtualizer(enable, auxr.virtualizerStrength / 1000f)
        updateDb()
    }

    fun setSpatialStrength(strength: Int) {
        auxr.virtualizerStrength = strength
        equalizerAudioProcessor.setVirtualizer(auxr.virtualizerEnabled, strength / 1000f)
        updateDb()
    }

    fun resetLimiter() {
        equalizerAudioProcessor.resetLimiter()
    }
    // ==========================================
    // Playback Parameters (Speed & Pitch)
    // ==========================================

    fun setPitch(exoPlayer: ExoPlayer, pitch: Float) {
        // 保持当前速度，只改变音调
        val currentSpeed = auxr.speed
        exoPlayer.playbackParameters = PlaybackParameters(currentSpeed, pitch)
        auxr.pitch = pitch
        updateDb()
    }

    /**
     * 在 Service 初始化 ExoPlayer 后调用此方法，应用存储的 Pitch 和 Speed
     */
    fun applyPlaybackParameters(exoPlayer: ExoPlayer) {
        val params = PlaybackParameters(auxr.speed, auxr.pitch)
        exoPlayer.playbackParameters = params
    }

    fun changedPlaPlaybackParameters(parameters: PlaybackParameters) {
        val currentSpeed = parameters.speed
        val currentPitch = parameters.pitch
        if (auxr.speed != currentSpeed || auxr.pitch != currentPitch) {
            auxr.speed = currentSpeed
            auxr.pitch = currentPitch
            updateDb()
        }
    }
    // ==========================================
    // Equalizer (EQ)
    // ==========================================

    fun setEqualizerEnabled(enable: Boolean) {
        equalizerAudioProcessor.setEqualizerActive(enable)
        auxr.equalizer = enable
        updateDb()
    }

    fun setEqualizerBand(index: Int, value: Int) {
        if (index in auxr.equalizerBand.indices) {
            equalizerAudioProcessor.setBand(index, value)
            auxr.equalizerBand[index] = value
            auxr.selectedPreset = Utils.custom
            updateDb()
        }
    }

    fun setEqualizerBands(values: IntArray, presetName: String? = null) {
        values.forEachIndexed { index, value ->
            if (index in auxr.equalizerBand.indices) {
                equalizerAudioProcessor.setBand(index, value)
                auxr.equalizerBand[index] = value
            }
        }
        auxr.selectedPreset = presetName ?: Utils.custom
        updateDb()
    }

    fun setPreset(name: String) {
        auxr.selectedPreset = name
        if (name == Utils.custom) {
            for (i in auxr.equalizerBand.indices) {
                if (i < 10) equalizerAudioProcessor.setBand(i, auxr.equalizerBand[i])
            }
        } else {
            Utils.eqPreset[name]?.forEachIndexed { index, value ->
                if (index in auxr.equalizerBand.indices) {
                    auxr.equalizerBand[index] = value
                }
                equalizerAudioProcessor.setBand(index, value)
            }
        }
        updateDb()
    }

    fun setQ(q: Float) {
        equalizerAudioProcessor.setQ(q)
        auxr.equalizerQ = q
        updateDb()
    }

    /**
     * 将均衡器重置为平直 (0)
     * @return 如果操作成功返回 true
     */
    fun flattenEqualizer(): Boolean {
        if (equalizerAudioProcessor.flatBand()) {
            for (i in auxr.equalizerBand.indices) {
                auxr.equalizerBand[i] = 0
            }
            auxr.selectedPreset = Utils.custom
            updateDb()
            return true
        }
        return false
    }

    // ==========================================
    // Echo / Reverb
    // ==========================================

    fun setEchoEnabled(enable: Boolean) {
        equalizerAudioProcessor.setEchoActive(enable)
        auxr.echo = enable
        updateDb()
    }

    fun setEchoDelay(delay: Float) {
        equalizerAudioProcessor.setDelayTime(delay)
        auxr.echoDelay = delay
        updateDb()
    }

    fun setEchoDecay(decay: Float) {
        equalizerAudioProcessor.setDecay(decay)
        auxr.echoDecay = decay
        updateDb()
    }

    fun setEchoFeedback(enable: Boolean) {
        equalizerAudioProcessor.setFeedBack(enable)
        auxr.echoRevert = enable
        updateDb()
    }

    // ==========================================
    // Visualization & UI Settings
    // ==========================================

    fun setVisualizationEnabled(enable: Boolean) {
        musicVisualizationEnable = enable
        equalizerAudioProcessor.setVisualizationAudioActive(enable && isVisualizationUiConnected)
        SharedPreferencesUtils.saveEnableMusicVisualization(context, enable)
    }

    fun onVisualizationConnected() {
        isVisualizationUiConnected = true
        if (musicVisualizationEnable) {
            equalizerAudioProcessor.setVisualizationAudioActive(true)
        }
    }

    fun onVisualizationDisconnected() {
        isVisualizationUiConnected = false
        equalizerAudioProcessor.setVisualizationAudioActive(false)
    }

    fun setEqualizerType(type: Int) {
        equalizerAudioProcessor.setEqualizerType(type)
        auxr.equalizerType = type
        updateDb()
    }

    fun setShowPitchFine(enable: Boolean) {
        auxr.showPitchFine = enable
        updateDb()
    }

    fun setShowSpeedFine(enable: Boolean) {
        auxr.showSpeedFine = enable
        updateDb()
    }

    // ==========================================
    // Advanced Effects (Reverb, Chorus, Flanger, Polyphony, Delay)
    // ==========================================

    fun setReverbEnabled(enable: Boolean) {
        auxr.reverbEnabled = enable
        equalizerAudioProcessor.setReverb(
            enable,
            auxr.reverbRoomSize,
            auxr.reverbDamping,
            auxr.reverbMix
        )
        updateDb()
    }

    fun setReverbParams(roomSize: Float, damping: Float, mix: Float) {
        auxr.reverbRoomSize = roomSize
        auxr.reverbDamping = damping
        auxr.reverbMix = mix
        equalizerAudioProcessor.setReverb(
            auxr.reverbEnabled,
            roomSize,
            damping,
            mix
        )
        updateDb()
    }

    fun setChorusEnabled(enable: Boolean) {
        auxr.chorusEnabled = enable
        equalizerAudioProcessor.setChorus(
            enable,
            auxr.chorusRate,
            auxr.chorusDepth,
            auxr.chorusMix
        )
        updateDb()
    }

    fun setChorusParams(rate: Float, depth: Float, mix: Float) {
        auxr.chorusRate = rate
        auxr.chorusDepth = depth
        auxr.chorusMix = mix
        equalizerAudioProcessor.setChorus(
            auxr.chorusEnabled,
            rate,
            depth,
            mix
        )
        updateDb()
    }

    fun setFlangerEnabled(enable: Boolean) {
        auxr.flangerEnabled = enable
        equalizerAudioProcessor.setFlanger(
            enable,
            auxr.flangerRate,
            auxr.flangerDepth,
            auxr.flangerFeedback,
            auxr.flangerMix
        )
        updateDb()
    }

    fun setFlangerParams(rate: Float, depth: Float, feedback: Float, mix: Float) {
        auxr.flangerRate = rate
        auxr.flangerDepth = depth
        auxr.flangerFeedback = feedback
        auxr.flangerMix = mix
        equalizerAudioProcessor.setFlanger(
            auxr.flangerEnabled,
            rate,
            depth,
            feedback,
            mix
        )
        updateDb()
    }

    fun setPolyphonyEnabled(enable: Boolean) {
        auxr.polyphonyEnabled = enable
        equalizerAudioProcessor.setPolyphony(
            enable,
            auxr.polyphonySemitones,
            auxr.polyphonyDetune,
            auxr.polyphonyMix
        )
        updateDb()
    }

    fun setPolyphonyParams(semitones: Int, detune: Float, mix: Float) {
        auxr.polyphonySemitones = semitones
        auxr.polyphonyDetune = detune
        auxr.polyphonyMix = mix
        equalizerAudioProcessor.setPolyphony(
            auxr.polyphonyEnabled,
            semitones,
            detune,
            mix
        )
        updateDb()
    }

    fun setDelayEnabled(enable: Boolean) {
        auxr.delayEnabled = enable
        equalizerAudioProcessor.setDelay(
            enable,
            auxr.delayTime,
            auxr.delayFeedback,
            auxr.delayMix
        )
        updateDb()
    }

    fun setDelayParams(time: Float, feedback: Float, mix: Float) {
        auxr.delayTime = time
        auxr.delayFeedback = feedback
        auxr.delayMix = mix
        equalizerAudioProcessor.setDelay(
            auxr.delayEnabled,
            time,
            feedback,
            mix
        )
        updateDb()
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private val effectJob = SupervisorJob()
    private val effectScope = CoroutineScope(Dispatchers.IO + effectJob)
    private var updateJob: Job? = null

    private fun updateDb() {
        if (!trackEffectEnabled && auxr.id == 0L) {
            globalAuxr = auxr.copy()
        }
        updateJob?.cancel()
        updateJob = effectScope.launch {
            db.AuxDao().upsert(auxr)
        }
    }

    suspend fun flushDb() = withContext(Dispatchers.IO) {
        updateJob?.cancel()
        if (!trackEffectEnabled && auxr.id == 0L) {
            globalAuxr = auxr.copy()
        }
        db.AuxDao().upsert(auxr)
    }

    fun setBitPerfectMode(enabled: Boolean) {
        equalizerAudioProcessor.bitPerfectMode = enabled
    }

    fun isBitPerfectMode(): Boolean {
        return equalizerAudioProcessor.bitPerfectMode
    }

    fun release() {
        effectJob.cancel()
    }
}