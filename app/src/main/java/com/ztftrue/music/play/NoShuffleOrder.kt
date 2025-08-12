package com.ztftrue.music.play

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder

@UnstableApi
class NoShuffleOrder(private val length: Int) : ShuffleOrder {

    init {
        require(length >= 0) { "ShuffleOrder length must be >= 0" }
    }

    override fun getLength(): Int = length

    override fun getNextIndex(index: Int): Int {
        return if (index + 1 < length) index + 1 else -1
    }

    override fun getPreviousIndex(index: Int): Int {
        return if (index - 1 >= 0) index - 1 else -1
    }

    override fun getFirstIndex(): Int = if (length > 0) 0 else -1

    override fun getLastIndex(): Int = if (length > 0) length - 1 else -1

    override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
        return NoShuffleOrder(length + insertionCount)
    }

    override fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder {
        val removeCount = (indexToExclusive - indexFrom).coerceAtLeast(0)
        return NoShuffleOrder((length - removeCount).coerceAtLeast(0))
    }

    override fun cloneAndClear(): ShuffleOrder {
        return NoShuffleOrder(0)
    }

}