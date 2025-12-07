package com.ztftrue.music.sqlData.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "sort_filed_data")
data class SortFiledData(
    @PrimaryKey
    var type: String,
    var filed: String,
    var method: String,
    var methodName: String,
    var filedName: String,
) : Parcelable