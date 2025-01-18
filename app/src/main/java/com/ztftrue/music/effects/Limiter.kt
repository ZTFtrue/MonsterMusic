package com.ztftrue.music.effects

import kotlin.math.absoluteValue

class Limiter(// 限制阈值 (-3 dB = 0.707)
    private val threshold: Float,  // 限制阈值 (-3 dB = 0.707)
    private val knee: Float,       // 软拐点，让限制更柔和
    private val attack: Float,     // 攻击时间 (0.05 = 50ms)
    private val release: Float     // 释放时间 (0.3 = 300ms)
) {
    private var gain = 1.0f       // 当前增益
    private var lastGain = 1.0f   // 上一帧增益

    fun process(buffer: FloatArray) {
        for (i in buffer.indices) {
            val absSample = buffer[i].absoluteValue

            // **Step 1: 计算 Soft-Knee 阈值**
            val softThreshold = threshold - knee // 软拐点调整
            var targetGain = 1.0f

            targetGain = when {
                absSample > threshold -> threshold / absSample // 硬限制
                absSample > softThreshold -> {
                    val diff = absSample - softThreshold
                    (softThreshold / absSample) + (diff / knee) * 0.5f // 软拐点过渡
                }
                else -> 1.0f
            }

            // **Step 2: 平滑增益变化**
            gain = if (targetGain < lastGain) {
                attack * targetGain + (1 - attack) * lastGain // Attack (降低增益)
            } else {
                release * targetGain + (1 - release) * lastGain // Release (恢复增益)
            }
            lastGain = gain

            // **Step 3: 应用增益**
            buffer[i] *= gain
        }
    }
}