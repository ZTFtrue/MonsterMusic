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
        id = 0,
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

    private var musicVisualizationEnable = false

    /**
     * 初始化音效设置
     * 必须在协程中调用 (IO上下文)
     */
    suspend fun initEffects() = withContext(Dispatchers.IO) {
        // 1. 加载 Auxr 配置
        val auxTemp = db.AuxDao().findFirstAux()
        if (auxTemp == null) {
            migrateLegacyPreferences(auxr)
            db.AuxDao().insert(auxr)
        } else {
            auxr = auxTemp
            if (migrateLegacyPreferences(auxr)) {
                db.AuxDao().update(auxr)
            }
        }

        // 2. 应用 Delay 设置
        equalizerAudioProcessor.setDelay(
            auxr.delayEnabled,
            auxr.delayTime,
            auxr.delayFeedback,
            auxr.delayMix
        )

        // 3. 应用 Echo 设置
        equalizerAudioProcessor.setDelayTime(auxr.echoDelay)
        equalizerAudioProcessor.setDecay(auxr.echoDecay)
        equalizerAudioProcessor.setFeedBack(auxr.echoRevert)
        equalizerAudioProcessor.setEchoActive(auxr.echo)

        // 4. 应用环绕设置 (通过 Native C 优化版 DSP 实现)
        equalizerAudioProcessor.setVirtualizer(auxr.virtualizerEnabled, auxr.virtualizerStrength / 1000f)
        spatialAudioProcessor.setActive(false)

        // 5. 应用 Reverb, Chorus, Flanger, Polyphony 设置
        loadAdvancedEffectsSettings()

        // 6. 应用 Equalizer 设置
        equalizerAudioProcessor.setEqualizerActive(auxr.equalizer)
        equalizerAudioProcessor.setQ(auxr.equalizerQ, false)
        equalizerAudioProcessor.setEqualizerType(auxr.equalizerType)

        // 7. 加载 EQ 预设 (Preset)
        loadEqPresets()

        // 8. 加载可视化设置
        loadVisualizationSettings()
    }

    private fun migrateLegacyPreferences(target: Auxr): Boolean {
        var changed = false
        val effectsPrefs = context.getSharedPreferences("audio_effects_prefs", MODE_PRIVATE)
        if (effectsPrefs.all.isNotEmpty()) {
            if (effectsPrefs.contains("reverb_enabled")) {
                target.reverbEnabled = effectsPrefs.getBoolean("reverb_enabled", target.reverbEnabled)
                target.reverbRoomSize = effectsPrefs.getFloat("reverb_room_size", target.reverbRoomSize)
                target.reverbDamping = effectsPrefs.getFloat("reverb_damping", target.reverbDamping)
                target.reverbMix = effectsPrefs.getFloat("reverb_mix", target.reverbMix)
                changed = true
            }
            if (effectsPrefs.contains("chorus_enabled")) {
                target.chorusEnabled = effectsPrefs.getBoolean("chorus_enabled", target.chorusEnabled)
                target.chorusRate = effectsPrefs.getFloat("chorus_rate", target.chorusRate)
                target.chorusDepth = effectsPrefs.getFloat("chorus_depth", target.chorusDepth)
                target.chorusMix = effectsPrefs.getFloat("chorus_mix", target.chorusMix)
                changed = true
            }
            if (effectsPrefs.contains("flanger_enabled")) {
                target.flangerEnabled = effectsPrefs.getBoolean("flanger_enabled", target.flangerEnabled)
                target.flangerRate = effectsPrefs.getFloat("flanger_rate", target.flangerRate)
                target.flangerDepth = effectsPrefs.getFloat("flanger_depth", target.flangerDepth)
                target.flangerFeedback = effectsPrefs.getFloat("flanger_feedback", target.flangerFeedback)
                target.flangerMix = effectsPrefs.getFloat("flanger_mix", target.flangerMix)
                changed = true
            }
            if (effectsPrefs.contains("polyphony_enabled")) {
                target.polyphonyEnabled = effectsPrefs.getBoolean("polyphony_enabled", target.polyphonyEnabled)
                target.polyphonySemitones = effectsPrefs.getInt("polyphony_semitones", target.polyphonySemitones)
                target.polyphonyDetune = effectsPrefs.getFloat("polyphony_detune", target.polyphonyDetune)
                target.polyphonyMix = effectsPrefs.getFloat("polyphony_mix", target.polyphonyMix)
                changed = true
            }
            if (effectsPrefs.contains("delay_enabled")) {
                target.delayEnabled = effectsPrefs.getBoolean("delay_enabled", target.delayEnabled)
                target.delayTime = effectsPrefs.getFloat("delay_time", target.delayTime)
                target.delayFeedback = effectsPrefs.getFloat("delay_feedback", target.delayFeedback)
                target.delayMix = effectsPrefs.getFloat("delay_mix", target.delayMix)
                changed = true
            }
            if (effectsPrefs.contains("show_pitch_fine")) {
                target.showPitchFine = effectsPrefs.getBoolean("show_pitch_fine", target.showPitchFine)
                changed = true
            }
            if (effectsPrefs.contains("show_speed_fine")) {
                target.showSpeedFine = effectsPrefs.getBoolean("show_speed_fine", target.showSpeedFine)
                changed = true
            }
        }

        val eqPrefs = context.getSharedPreferences("Equalizer", MODE_PRIVATE)
        if (eqPrefs.contains("EqualizerType")) {
            target.equalizerType = eqPrefs.getInt("EqualizerType", target.equalizerType)
            changed = true
        }

        val presetPrefs = context.getSharedPreferences("SelectedPreset", MODE_PRIVATE)
        if (presetPrefs.contains("SelectedPreset")) {
            target.selectedPreset = presetPrefs.getString("SelectedPreset", target.selectedPreset) ?: target.selectedPreset
            changed = true
        }

        return changed
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
        equalizerAudioProcessor.setVisualizationAudioActive(musicVisualizationEnable)
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
        updateJob?.cancel()
        updateJob = effectScope.launch {
            db.AuxDao().update(auxr)
        }
    }

    fun release() {
        effectJob.cancel()
    }
}