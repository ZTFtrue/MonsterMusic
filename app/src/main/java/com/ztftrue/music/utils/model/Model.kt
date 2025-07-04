package com.ztftrue.music.utils.model

import android.os.Parcelable
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

@Parcelize
data class FolderList(
    override var name: String,
    override var id: Long,
    override var trackNumber: Int,
    override var type: PlayListType = PlayListType.Folders,
    var isShow: Boolean = true
) : ListBase(id, name, trackNumber, type)

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