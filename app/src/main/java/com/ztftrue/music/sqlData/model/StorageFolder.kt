package com.ztftrue.music.sqlData.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

const val LYRICS_TYPE = 0
const val GENRE_TYPE = 1
const val ARTIST_TYPE = 2

@Parcelize
@Entity(tableName = "storage_folder")
data class StorageFolder(
    @PrimaryKey
    val id: Int?,
    @ColumnInfo(name = "uri") val uri: String,
    @ColumnInfo(name = "type", defaultValue = "0") val type: Int = 0
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StorageFolder

        if (id != other.id) return false
        if (uri != other.uri) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + uri.hashCode()
        return result
    }
}