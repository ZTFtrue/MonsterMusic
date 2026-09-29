package com.ztftrue.music.sqlData.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Keep
@Parcelize
data class FilterFolder(
    val id: String = UUID.randomUUID().toString(),
    val uri: String? = null,
    val path: String,
    val name: String,
    val bucketId: Long? = null
) : Parcelable
