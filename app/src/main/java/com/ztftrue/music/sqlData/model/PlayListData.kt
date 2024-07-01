package com.ztftrue.music.sqlData.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "play_list_data")
data class PlayListData(
    @PrimaryKey
    var id: Long?,
    var playListId: Long,
    var trackId: Long,
    // for sort
    var album: String,
    var artist: String,
    var duration: Long,
    var title: String,
    var year: Int,
    var priority:Int
) : Parcelable {

}