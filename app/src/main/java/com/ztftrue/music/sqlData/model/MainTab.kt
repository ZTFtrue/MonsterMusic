package com.ztftrue.music.sqlData.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ztftrue.music.utils.PlayListType
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "main_tab")
data class MainTab(
    @PrimaryKey
    val id: Int?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: PlayListType,
    @ColumnInfo(name = "priority") var priority: Int,
    @ColumnInfo var isShow: Boolean
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MainTab

        if (id != other.id) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (priority != other.priority) return false
        return isShow == other.isShow
    }

    override fun hashCode(): Int {
        var result = id?:0
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + priority.hashCode()
        result = 31 * result + isShow.hashCode()
        return result
    }

    override fun toString(): String {
        return "MainTab(id=$id, name='$name', priority=$priority, isShow=$isShow)"
    }
}