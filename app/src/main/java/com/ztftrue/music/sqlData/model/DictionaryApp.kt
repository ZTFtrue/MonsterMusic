package com.ztftrue.music.sqlData.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "dictionary_app")
data class DictionaryApp(
    @PrimaryKey
    var id: Int?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo var isShow: Boolean,
    @ColumnInfo var autoGo: Boolean
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DictionaryApp

        if (id != other.id) return false
        if (name != other.name) return false
        if (packageName != other.packageName) return false
        if (label != other.label) return false
        if (isShow != other.isShow) return false
        return autoGo == other.autoGo
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + name.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + isShow.hashCode()
        result = 31 * result + autoGo.hashCode()
        return result
    }
}