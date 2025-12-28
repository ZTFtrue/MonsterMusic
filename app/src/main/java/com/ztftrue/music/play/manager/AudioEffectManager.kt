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
        // 初始化环绕设置
        spatialAudioProcessor.setActive(auxr.virtualizerEnabled)
        spatialAudioProcessor.setStrength(auxr.virtualizerStrength)

        // 3. 应用 Equalizer 设置
        equalizerAudioProcessor.setEqualizerActive(auxr.equalizer)
        equalizerAudioProcessor.setQ(auxr.equalizerQ, false)

        // 4. 加载 EQ 预设 (Preset)
        loadEqPresets()

        // 5. 加载可视化设置
        loadVisualizationSettings()
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
    }

    fun setSpatialEnabled(enable: Boolean) {
        spatialAudioProcessor.setActive(enable)
        auxr.virtualizerEnabled = enable
        updateDb()
    }
    fun setSpatialStrength(strength: Int) {
        spatialAudioProcessor.setStrength(strength)
        auxr.virtualizerStrength = strength
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

    // ==========================================
    // Helper Methods
    // ==========================================

    private fun updateDb() {
        CoroutineScope(Dispatchers.IO).launch {
            db.AuxDao().update(auxr)
        }
    }
}