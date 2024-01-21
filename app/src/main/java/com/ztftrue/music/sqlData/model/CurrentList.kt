package com.ztftrue.music.sqlData.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CurrentList (
    @PrimaryKey
    val id: Int?,
    var listID: Long,
    @ColumnInfo(name = "type") var type: String,
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CurrentList

        if (id != other.id) return false
        return type == other.type
    }

    override fun hashCode(): Int {
        var result = id?:0
        result = 31 * result + type.hashCode()
        return result
    }
}