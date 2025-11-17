package com.ztftrue.music.utils.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.ztftrue.music.utils.PlayListType
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class MusicPlayList(
    override var name: String,
    override var id: Long,
    var path: String,
    override var trackNumber: Int,
    override var type: PlayListType = PlayListType.PlayLists,
) : ListBase(id, name, trackNumber, type)
@Immutable
@Parcelize
data class FolderList(
    var children: ArrayList<FolderList> = ArrayList(),
    var path: String,
    override var name: String,
    override var id: Long,
    override var trackNumber: Int,
    override var type: PlayListType = PlayListType.Folders,
    var isShow: Boolean = true,
    var parent:FolderList?=null
) : ListBase(id, name, trackNumber, type){
    override fun toString(): String {
        return path
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FolderList

        if (id != other.id) return false
        if (trackNumber != other.trackNumber) return false
        if (isShow != other.isShow) return false
        if (path != other.path) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (parent != other.parent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + trackNumber
        result = 31 * result + isShow.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        return result
    }

}

@Parcelize
data class AlbumList(
    override var id: Long,
    override var name: String,
    var artist: String,
    var firstYear: String,
    var lastYear: String,
    override var trackNumber: Int,
    override var type: PlayListType = PlayListType.Albums,
) : ListBase(id, name, trackNumber, type)

@Parcelize
data class ArtistList(
    override var id: Long,
    override var name: String,
    override var trackNumber: Int,
    var albumNumber: Int,
    override var type: PlayListType = PlayListType.Artists,
) : ListBase(id, name, trackNumber, type)

@Parcelize
class GenresList(
    override var id: Long,
    override var name: String,
    override var trackNumber: Int,
    var albumNumber: Int,
    override var type: PlayListType = PlayListType.Genres
) : ListBase(id, name, trackNumber, type)

@Parcelize
open class ListBase(
    override var id: Long,
    open var name: String,
    open var trackNumber: Int,
    override var type: PlayListType = PlayListType.Genres,
) : AnyListBase(id, type), Parcelable, Serializable

@Parcelize
open class AnyListBase(
    override var id: Long,
    override var type: PlayListType = PlayListType.Genres,
) : AbstractListBase(), Parcelable, Serializable

abstract class AbstractListBase : Parcelable, Serializable {
    abstract var id: Long
    abstract var type: PlayListType
    var playing: Boolean = false
}


@Parcelize
data class EqualizerBand(
    val id: Int,
    val name: String,
    var value: Int,
) : Parcelable

data class Caption(
    var text: String,
    val timeStart: Long,
    val timeEnd: Long = 0
)

data class ListStringCaption(
    val text: ArrayList<String>,
    val timeStart: Long,
    val timeEnd: Long = 0
)

data class LanguageModel(
    val name: String,
    val code: String
)