package com.ztftrue.music.sqlData.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "queue")
data class MusicItem(
    @PrimaryKey
    var tableId: Long?,
    var id: Long,
    var name: String,
    var path: String,
    var duration: Long,
    var displayName: String,
    var album: String,
    var albumId: Long,
    var artist: String,
    var artistId: Long,
    var genre: String,
    var genreId: Long,
    var year: Int,
//    var discNumber:Int,
    var songNumber:Int,
    // for random
    var priority: Int = 0,
    var isFavorite: Boolean = false,
) : Parcelable {
//    @Ignore
//    var isPlaying: Boolean = false
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as MusicItem
//
//        if (id != other.id) return false
//        if (name != other.name) return false
//        if (path != other.path) return false
//        if (artist != other.artist) return false
//        if (duration != other.duration) return false
//        if (displayName != other.displayName) return false
//        return isPlaying == other.isPlaying
//    }
//
//    override fun hashCode(): Int {
//        var result = id.hashCode()
//        result = 31 * result + name.hashCode()
//        result = 31 * result + path.hashCode()
//        result = 31 * result + artist.hashCode()
//        result = 31 * result + duration.hashCode()
//        result = 31 * result + displayName.hashCode()
//        result = 31 * result + isPlaying.hashCode()
//        return result
//    }
}