package com.ztftrue.music.sqlData.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PlayConfig(
    @PrimaryKey
    @ColumnInfo val id: Int,
    @ColumnInfo var repeatModel: Int,
    @ColumnInfo(defaultValue = "0") var trackEffectEnabled: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayConfig

        if (id != other.id) return false
        if (repeatModel != other.repeatModel) return false
        return trackEffectEnabled == other.trackEffectEnabled
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + repeatModel
        result = 31 * result + trackEffectEnabled.hashCode()
        return result
    }
}