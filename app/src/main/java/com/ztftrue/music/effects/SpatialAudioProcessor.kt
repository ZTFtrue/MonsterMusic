package com.ztftrue.music.effects

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi

/**
 * 自定义虚化环绕处理器 (已合并入 EqualizerAudioProcessor)
 * 保留此类以维护向后兼容性。
 */
@UnstableApi
@Deprecated("Merged into EqualizerAudioProcessor. Use EqualizerAudioProcessor directly.", ReplaceWith("EqualizerAudioProcessor"))
class SpatialAudioProcessor(
    private val delegate: EqualizerAudioProcessor = EqualizerAudioProcessor()
) : AudioProcessor by delegate {

    fun setStrength(value: Int) {
        delegate.setSpatialStrength(value)
    }

    fun setActive(active: Boolean) {
        delegate.setSpatialEnabled(active)
    }
}