package com.ztftrue.music.play.manager

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.ztftrue.music.effects.EqualizerAudioProcessor
import com.ztftrue.music.effects.SpatialAudioProcessor
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.model.Auxr
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
    val spatialAudioProcessor = SpatialAudioProcessor()
    private val db: MusicDatabase = MusicDatabase.getDatabase(context)

    // 默认配置，稍后会从数据库覆盖
    var auxr = Auxr(
        0, 1f, 1f, false, 0.2f, 0.5f,
        echoRevert = true,
        equalizer = false,
        equalizerBand = IntArray(10), // 假设是10段EQ
        equalizerQ = Utils.Q
    )

    private var musicVisualizationEnable = false

    /**
     * 初始化音效设置
     * 必须在协程中调用 (IO上下文)
     */
    suspend fun initEffects() = withContext(Dispatchers.IO) {
        // 1. 加载 Auxr 配置
        val auxTemp = db.AuxDao().findFirstAux()
        if (auxTemp == null) {
            db.AuxDao().insert(auxr)
        } else {
            auxr = auxTemp
        }

        // 2. 应用 Echo/Delay 设置
        equalizerAudioProcessor.setDelayTime(auxr.echoDelay)
        equalizerAudioProcessor.setDecay(auxr.echoDecay)
        equalizerAudioProcessor.setFeedBack(auxr.echoRevert)
        equalizerAudioProcessor.setEchoActive(auxr.echo)
        // 初始化环绕设置 (通过 Native C 优化版 DSP 实现)
        equalizerAudioProcessor.setVirtualizer(auxr.virtualizerEnabled, auxr.virtualizerStrength / 1000f)
        spatialAudioProcessor.setActive(false)

        // 3. 应用 Reverb, Chorus, Flanger, Polyphony 设置
        loadAdvancedEffectsSettings()

        // 4. 应用 Equalizer 设置
        equalizerAudioProcessor.setEqualizerActive(auxr.equalizer)
        equalizerAudioProcessor.setQ(auxr.equalizerQ, false)

        // 5. 加载 EQ 预设 (Preset)
        loadEqPresets()

        // 6. 加载可视化设置
        loadVisualizationSettings()
    }

    private fun loadAdvancedEffectsSettings() {
        equalizerAudioProcessor.setReverb(
            SharedPreferencesUtils.getReverbEnabled(context),
            SharedPreferencesUtils.getReverbRoomSize(context),
            SharedPreferencesUtils.getReverbDamping(context),
            SharedPreferencesUtils.getReverbMix(context)
        )
        equalizerAudioProcessor.setChorus(
            SharedPreferencesUtils.getChorusEnabled(context),
            SharedPreferencesUtils.getChorusRate(context),
            SharedPreferencesUtils.getChorusDepth(context),
            SharedPreferencesUtils.getChorusMix(context)
        )
        equalizerAudioProcessor.setFlanger(
            SharedPreferencesUtils.getFlangerEnabled(context),
            SharedPreferencesUtils.getFlangerRate(context),
            SharedPreferencesUtils.getFlangerDepth(context),
            SharedPreferencesUtils.getFlangerFeedback(context),
            SharedPreferencesUtils.getFlangerMix(context)
        )
        equalizerAudioProcessor.setPolyphony(
            SharedPreferencesUtils.getPolyphonyEnabled(context),
            SharedPreferencesUtils.getPolyphonySemitones(context),
            SharedPreferencesUtils.getPolyphonyDetune(context),
            SharedPreferencesUtils.getPolyphonyMix(context)
        )
    }

    private fun loadEqPresets() {
        val sharedPreferences = context.getSharedPreferences("SelectedPreset", MODE_PRIVATE)
        val selectedPreset = sharedPreferences.getString("SelectedPreset", Utils.custom)

        if (selectedPreset == Utils.custom) {
            // 如果是自定义，使用数据库中保存的 band 值
            // 注意：确保 auxr.equalizerBand 长度与处理器支持的一致，这里做个安全遍历
            for (i in auxr.equalizerBand.indices) {
                if (i < 10) { // 假设最大10段
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
        equalizerAudioProcessor.setVisualizationAudioActive(musicVisualizationEnable)
        val eqType = SharedPreferencesUtils.getEqualizerType(context)
        equalizerAudioProcessor.setEqualizerType(eqType)
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
            updateDb()
        }
    }

    fun setEqualizerBands(values: IntArray) {
        values.forEachIndexed { index, value ->
            if (index in auxr.equalizerBand.indices) {
                equalizerAudioProcessor.setBand(index, value)
                // 注意：这里可能需要更新内存中的 auxr 数组
                auxr.equalizerBand[index] = value
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
    // Visualization
    // ==========================================

    fun setVisualizationEnabled(enable: Boolean) {
        musicVisualizationEnable = enable
        equalizerAudioProcessor.setVisualizationAudioActive(enable)
        SharedPreferencesUtils.saveEnableMusicVisualization(context, enable)
    }

    fun onVisualizationConnected() {
        if (musicVisualizationEnable) {
            equalizerAudioProcessor.setVisualizationAudioActive(true)
        }
    }

    fun onVisualizationDisconnected() {
        equalizerAudioProcessor.setVisualizationAudioActive(false)
    }

    fun setEqualizerType(type: Int) {
        equalizerAudioProcessor.setEqualizerType(type)
        SharedPreferencesUtils.saveEqualizerType(context, type)
    }

    // ==========================================
    // Advanced Effects (Reverb, Chorus, Flanger, Polyphony)
    // ==========================================

    fun setReverbEnabled(enable: Boolean) {
        SharedPreferencesUtils.saveReverbEnabled(context, enable)
        equalizerAudioProcessor.setReverb(
            enable,
            SharedPreferencesUtils.getReverbRoomSize(context),
            SharedPreferencesUtils.getReverbDamping(context),
            SharedPreferencesUtils.getReverbMix(context)
        )
    }

    fun setReverbParams(roomSize: Float, damping: Float, mix: Float) {
        SharedPreferencesUtils.saveReverbRoomSize(context, roomSize)
        SharedPreferencesUtils.saveReverbDamping(context, damping)
        SharedPreferencesUtils.saveReverbMix(context, mix)
        equalizerAudioProcessor.setReverb(
            SharedPreferencesUtils.getReverbEnabled(context),
            roomSize,
            damping,
            mix
        )
    }

    fun setChorusEnabled(enable: Boolean) {
        SharedPreferencesUtils.saveChorusEnabled(context, enable)
        equalizerAudioProcessor.setChorus(
            enable,
            SharedPreferencesUtils.getChorusRate(context),
            SharedPreferencesUtils.getChorusDepth(context),
            SharedPreferencesUtils.getChorusMix(context)
        )
    }

    fun setChorusParams(rate: Float, depth: Float, mix: Float) {
        SharedPreferencesUtils.saveChorusRate(context, rate)
        SharedPreferencesUtils.saveChorusDepth(context, depth)
        SharedPreferencesUtils.saveChorusMix(context, mix)
        equalizerAudioProcessor.setChorus(
            SharedPreferencesUtils.getChorusEnabled(context),
            rate,
            depth,
            mix
        )
    }

    fun setFlangerEnabled(enable: Boolean) {
        SharedPreferencesUtils.saveFlangerEnabled(context, enable)
        equalizerAudioProcessor.setFlanger(
            enable,
            SharedPreferencesUtils.getFlangerRate(context),
            SharedPreferencesUtils.getFlangerDepth(context),
            SharedPreferencesUtils.getFlangerFeedback(context),
            SharedPreferencesUtils.getFlangerMix(context)
        )
    }

    fun setFlangerParams(rate: Float, depth: Float, feedback: Float, mix: Float) {
        SharedPreferencesUtils.saveFlangerRate(context, rate)
        SharedPreferencesUtils.saveFlangerDepth(context, depth)
        SharedPreferencesUtils.saveFlangerFeedback(context, feedback)
        SharedPreferencesUtils.saveFlangerMix(context, mix)
        equalizerAudioProcessor.setFlanger(
            SharedPreferencesUtils.getFlangerEnabled(context),
            rate,
            depth,
            feedback,
            mix
        )
    }

    fun setPolyphonyEnabled(enable: Boolean) {
        SharedPreferencesUtils.savePolyphonyEnabled(context, enable)
        equalizerAudioProcessor.setPolyphony(
            enable,
            SharedPreferencesUtils.getPolyphonySemitones(context),
            SharedPreferencesUtils.getPolyphonyDetune(context),
            SharedPreferencesUtils.getPolyphonyMix(context)
        )
    }

    fun setPolyphonyParams(semitones: Int, detune: Float, mix: Float) {
        SharedPreferencesUtils.savePolyphonySemitones(context, semitones)
        SharedPreferencesUtils.savePolyphonyDetune(context, detune)
        SharedPreferencesUtils.savePolyphonyMix(context, mix)
        equalizerAudioProcessor.setPolyphony(
            SharedPreferencesUtils.getPolyphonyEnabled(context),
            semitones,
            detune,
            mix
        )
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private val effectJob = SupervisorJob()
    private val effectScope = CoroutineScope(Dispatchers.IO + effectJob)
    private var updateJob: Job? = null

    private fun updateDb() {
        updateJob?.cancel()
        updateJob = effectScope.launch {
            db.AuxDao().update(auxr)
        }
    }

    fun release() {
        effectJob.cancel()
    }
}