package com.ztftrue.music.effects

import kotlin.math.absoluteValue
import kotlin.math.atan

class Limiter(// 限制阈值 (-3 dB = 0.707)

) {


    object Limiter {
        fun processData(data: Float, limit: Float = 0.8f): Float {
//            if (data.absoluteValue > limit) {
//                val t: Float = (data.absoluteValue - limit) / 1f
//                val gain = 1f - 0.5f * tanh(t)
//                return (data * gain)
//            }
            return atan(data)
        }

        fun process(buffer: FloatArray): Float {
            var max = 1.0f
            buffer.forEachIndexed { index, it ->
                buffer[index] = processData(it, 0.8f)
                max = max.coerceAtLeast(buffer[index].absoluteValue)
            }
            return max
        }
    }

}