package com.ztftrue.music.utils

import androidx.compose.runtime.snapshots.SnapshotStateList

object MutableListExtension {
    fun <T> SnapshotStateList<T>.clearExceptFirst() {
        if (this.size > 1) {
            this.subList(1, this.size).clear()
        }
    }

    fun <T> SnapshotStateList<T>.replaceCurrent(current: T) {
        if (this.size > 1) {
            val currentIndex = this.size - 1
            this.add(current)
            this.removeAt(currentIndex)
        } else {
            this.add(current)
        }
    }

    fun <T> MutableList<T>.removeLastSafe() {
        if (this.size > 1) {
            this.removeLastOrNull()
        }
    }
//    fun <T : Any> MutableList<T>.replaceByType(newData: T) {
//        // 1. Remove OLD data if the Class Type matches the new data
//        // (e.g., if both are 'HeaderItem', remove the old one)
//        this.removeAll { existingItem ->
//            existingItem::class == newData::class
//        }
//        // 2. Add NEW data to the end
//        this.add(newData)
//    }
//    fun <T : Any> MutableList<T>.replaceByData(newData: T) {
//        // 1. Remove OLD data if the Class Type matches the new data
//        // (e.g., if both are 'HeaderItem', remove the old one)
//        this.removeAll { existingItem ->
//            existingItem::class == newData::class
//        }
//        // 2. Add NEW data to the end
//        this.add(newData)
//    }
}