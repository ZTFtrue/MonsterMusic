package com.ztftrue.music.effects

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 自定义虚化环绕处理器
 * 原理：Mid-Side (M/S) 处理算法
 * 增强 Side (差分) 信号，使声场变宽，产生环绕和虚化感。
 */
@UnstableApi
class SpatialAudioProcessor : BaseAudioProcessor() {

    private var strength: Float = 0f // 0f (关闭) ~ 1f (最大)
    private var enabled: Boolean = false

    /**
     * 设置强度
     * @param value 0 到 1000 的整数
     */
    fun setStrength(value: Int) {
        // 将 0-1000 映射到 0.0 - 2.0 (增益倍数)
        // 1.0 表示原声，>1.0 表示增强 Side
        this.strength = (value / 1000f) * 1.5f
    }

    fun setActive(active: Boolean) {
        if (this.enabled != active) {
            this.enabled = active
            flush() // 状态改变时刷新缓冲区
        }
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // 仅支持 16-bit PCM 立体声
        // 如果是单声道 (Mono)，M/S 算法无法工作（因为 L=R，Side=0），直接透传
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        // 如果是单声道，ExoPlayer 会继续使用 outputAudioFormat，
        // 但我们在 queueInput 里会做判断不做处理
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        // --- 修正点开始 ---
        // 如果未开启，或不是立体声，执行“透传”逻辑（Copy Input to Output）
        if (!enabled || inputAudioFormat.channelCount != 2) {
            // 1. 获取大小合适的输出缓冲区
            val buffer = replaceOutputBuffer(remaining)
            // 2. 将输入数据复制到输出
            buffer.put(inputBuffer)
            // 3. 切换为读模式 (Flip)
            buffer.flip()
            return
        }
        // --- 修正点结束 ---

        // 下面是正常的 DSP 处理逻辑
        val buffer = replaceOutputBuffer(remaining)

        while (inputBuffer.hasRemaining()) {
            val lLow = inputBuffer.get().toInt()
            val lHigh = inputBuffer.get().toInt()
            val rLow = inputBuffer.get().toInt()
            val rHigh = inputBuffer.get().toInt()

            val leftSample = ((lHigh shl 8) or (lLow and 0xFF)).toShort()
            val rightSample = ((rHigh shl 8) or (rLow and 0xFF)).toShort()

            val mid = (leftSample + rightSample) / 2
            val side = (leftSample - rightSample) / 2

            val sideFactor = 1.0f + strength
            val newSide = (side * sideFactor).toInt()

            var newLeft = mid + newSide
            var newRight = mid - newSide

            newLeft = newLeft.coerceIn(-32768, 32767)
            newRight = newRight.coerceIn(-32768, 32767)

            buffer.put((newLeft and 0xFF).toByte())
            buffer.put(((newLeft shr 8) and 0xFF).toByte())
            buffer.put((newRight and 0xFF).toByte())
            buffer.put(((newRight shr 8) and 0xFF).toByte())
        }

        buffer.flip()
    }
}