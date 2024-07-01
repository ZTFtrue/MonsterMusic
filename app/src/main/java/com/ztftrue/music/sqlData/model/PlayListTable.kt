package com.ztftrue.music.sqlData.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "play_list")
data class PlayListTable(
    @PrimaryKey
    var id: Long,
    var systemId:Long,
    var name: String,
    var tracksNumber: Int,
) : Parcelable {

}