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

}