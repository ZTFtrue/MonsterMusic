package com.ztftrue.music.play


import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 一个单例的数据仓库，用于在应用内广播音频处理数据。
 * 这是 Service 和 ViewModel 之间的解耦层和通信桥梁。
 */
object AudioDataRepository {

    private val dataLock = Any()
    private val latestFftData = FloatArray(32)
    @Volatile
    private var dataTimestampNs = 0L

    // 1. 创建一个私有的、可变的 SharedFlow
    //    - FloatArray: 我们要传输的数据类型
    //    - replay = 0: 这是一个“热”流，新的订阅者不会收到之前已经发出的数据。这对于实时数据非常重要。
    //    - extraBufferCapacity = 1: 缓冲区大小。
    //    - onBufferOverflow = BufferOverflow.DROP_OLDEST: 永远不会拒绝新数据，保证实时性并避免丢弃最新帧
    private val _visualizationDataFlow =
        MutableSharedFlow<FloatArray>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    // 2. 暴露一个公有的、不可变的 SharedFlow
    //    这遵循了 Kotlin 的封装原则，外部只能订阅，不能发射
    val visualizationDataFlow = _visualizationDataFlow.asSharedFlow()

    /**
     * 由数据生产者（如 AudioProcessor）调用，用于发射新的可视化数据。
     * @param data 新的 FFT 数据数组。
     */
    fun postVisualizationData(data: FloatArray) {
        synchronized(dataLock) {
            val len = minOf(data.size, latestFftData.size)
            System.arraycopy(data, 0, latestFftData, 0, len)
            dataTimestampNs = System.nanoTime()
        }
        _visualizationDataFlow.tryEmit(data)
    }

    /**
     * 将最新的 FFT 频段数据复制到调用方提供的 [destination] 数组中，
     * 避免在渲染循环中产生任何堆对象分配和装箱开销（Zero-allocation）。
     * @return 如果成功复制了有效音频数据则返回 true，否则返回 false
     */
    fun getLatestVisualizationData(destination: FloatArray): Boolean {
        synchronized(dataLock) {
            if (dataTimestampNs == 0L) return false
            val len = minOf(destination.size, latestFftData.size)
            System.arraycopy(latestFftData, 0, destination, 0, len)
            return true
        }
    }

    /**
     * 清理缓存的可视化数据（如播放停止或重置时调用）。
     */
    fun clearVisualizationData() {
        synchronized(dataLock) {
            latestFftData.fill(0f)
            dataTimestampNs = 0L
        }
    }
}