package com.ztftrue.music.effects

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * 自定义虚化环绕处理器
 * 原理：Mid-Side (M/S) 处理算法
 * 增强 Side (差分) 信号，使声场变宽，产生环绕和虚化感。
 */
@UnstableApi
class SpatialAudioProcessor : BaseAudioProcessor() {

    private var strength: Float = 0f // 0f (关闭) ~ 1f (最大)
    private var enabled: Boolean = false
    // 预留余量系数：强度越高，我们预先降低的音量越多，防止爆音
    private var headroomFactor: Float = 1.0f
    /**
     * 设置强度
     * @param value 0 到 1000 的整数
     */
    fun setStrength(value: Int) {
        // 将 0-1000 映射到 0.0 - 2.0 (增益倍数)
        // 1.0 表示原声，>1.0 表示增强 Side
        this.strength = (value / 1000f) * 1.5f
        this.headroomFactor = 1.0f - (this.strength * 0.2f)
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

            val leftRaw = ((lHigh shl 8) or (lLow and 0xFF)).toShort()
            val rightRaw = ((rHigh shl 8) or (rLow and 0xFF)).toShort()

            // 1. 应用 Headroom (衰减输入)，转为 Float 计算
            val left = leftRaw * headroomFactor
            val right = rightRaw * headroomFactor

            // 2. M/S 处理
            val mid = (left + right) / 2
            val side = (left - right) / 2

            // 增强 Side
            val newSide = side * (1.0f + strength)

            // 还原 L/R
            var outL = mid + newSide
            var outR = mid - newSide

            // 3. 【关键策略 B：简单的软限幅 (Soft Limiting)】
            // 防止数值硬冲过 32767
            outL = softLimit(outL)
            outR = softLimit(outR)

            // 转换回 Short 写入
            val outLShort = outL.toInt().toShort()
            val outRShort = outR.toInt().toShort()

            buffer.put((outLShort.toInt() and 0xFF).toByte())
            buffer.put(((outLShort.toInt() shr 8) and 0xFF).toByte())
            buffer.put((outRShort.toInt() and 0xFF).toByte())
            buffer.put(((outRShort.toInt() shr 8) and 0xFF).toByte())
        }

        buffer.flip()
    }
    /**
     * 软限幅函数
     * 当数值在安全范围内 (-28000 ~ 28000) 线性输出
     * 当数值接近极限时，进行平滑压缩，避免硬切
     */
    private fun softLimit(sample: Float): Float {
        val threshold = 29000f // 阈值，保留一点头部空间
        val maxVal = 32767f

        // 快速路径：如果在阈值内，直接返回
        if (sample in -threshold..threshold) {
            return sample
        }

        // 超过阈值，使用简单的压缩算法
        // 这里使用一个简化的曲线，避免复杂的 tanh 计算 (太耗 CPU)
        if (sample > threshold) {
            val diff = sample - threshold
            // 让超过的部分按比例衰减，无限趋近于 maxVal 但不硬切
            return threshold + (diff / (1 + (diff / (maxVal - threshold))))
        } else {
            // 负方向同理
            val diff = sample + threshold // diff is negative
            return -threshold + (diff / (1 + (abs(diff) / (maxVal - threshold))))
        }
    }
}