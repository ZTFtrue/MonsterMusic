package com.ztftrue.music.play


import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 一个单例的数据仓库，用于在应用内广播音频处理数据。
 * 这是 Service 和 ViewModel 之间的解耦层和通信桥梁。
 */
object AudioDataRepository {

    // 1. 创建一个私有的、可变的 SharedFlow
    //    - FloatArray: 我们要传输的数据类型
    //    - replay = 0: 这是一个“热”流，新的订阅者不会收到之前已经发出的数据。这对于实时数据非常重要。
    //    - extraBufferCapacity = 1: 缓冲区大小。设置一个小缓冲区可以帮助处理背压，
    //      允许生产者在消费者处理慢时，缓存一个最新的值。
    private val _visualizationDataFlow =
        MutableSharedFlow<FloatArray>(replay = 0, extraBufferCapacity = 1)

    // 2. 暴露一个公有的、不可变的 SharedFlow
    //    这遵循了 Kotlin 的封装原则，外部只能订阅，不能发射
    val visualizationDataFlow = _visualizationDataFlow.asSharedFlow()

    /**
     * 由数据生产者（如 AudioProcessor）调用，用于发射新的可视化数据。
     * @param data 新的 FFT 数据数组。
     */
    fun postVisualizationData(data: FloatArray) {
        // tryEmit 是一个非挂起的、线程安全的方法。
        // 如果缓冲区满了，它会失败并返回 false，但不会阻塞生产者。
        // 这对于实时音频处理至关重要，我们宁愿丢弃一帧，也不愿阻塞音频线程。
       /* val emitted =*/ _visualizationDataFlow.tryEmit(data)
        // if (!emitted) {
        //     Log.w("AudioDataRepository", "Visualization data buffer overflow. Dropping frame.")
        // }
    }
}