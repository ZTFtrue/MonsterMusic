package com.ztftrue.music.effects


object Limiter {

    // 阈值：超过这个音量开始进行软压缩
    // 0.8f 是一个比较通用的值，意味着 0~0.8 的声音完全保留原样，0.8 以上开始变圆
    private const val THRESHOLD = 0.8f

    fun process(buffer: FloatArray): Float {
        var maxAbs = 0.001f // 防止除以0
        val size = buffer.size

        // 使用原生循环，零 GC
        for (i in 0 until size) {
            val sample = buffer[i]
            val absSample = if (sample < 0f) -sample else sample

            // --- 软削波核心逻辑 ---
            if (absSample > THRESHOLD) {
                // 如果超过阈值，不直接切平，而是用曲线压缩
                // 算法逻辑：输入越大，增长越慢，模拟模拟电路的饱和感
                // 这里使用简化的软膝算法，避免昂贵的三角函数

                // 1. 计算超出部分
                val over = absSample - THRESHOLD

                // 2. 压缩超出部分 (使用简单的衰减函数)
                // 原始逻辑：sample
                // 软化逻辑：threshold + (超出部分 / (1 + 超出部分 * 软化系数))
                // 0.5f 是软化力度，越大压得越扁
                val compressedOver = over / (1.0f + over * 0.5f)

                // 3. 重组信号 (保持符号)
                val newAbs = THRESHOLD + compressedOver
                val sign = if (sample > 0f) 1.0f else -1.0f

                buffer[i] = sign * newAbs

                // 更新最大值用于外部归一化
                // 注意：软削波后的值通常会略大于 1.0 (比如 1.1)，
                if (newAbs > maxAbs) {
                    maxAbs = newAbs
                }
            } else {
                // 未超过阈值，保持原样 (线性区)，保证音质纯净
                if (absSample > maxAbs) {
                    maxAbs = absSample
                }
            }
        }
        return maxAbs
    }
}