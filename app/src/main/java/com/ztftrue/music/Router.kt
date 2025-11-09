package com.ztftrue.music

import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.model.FolderList
import com.ztftrue.music.utils.model.MusicPlayList


sealed class Router() {
    data object MainView : Router()
    data object MusicPlayerView : Router()
    data object SettingsPage : Router()
    data object QueuePage : Router()
    data object SearchPage : Router()

    data class TracksSelectPage(val listBase: MusicPlayList)
    data class PlayListView(val listBase: AnyListBase)
    data class EditTrackPage(val music: MusicItem)
    data class FolderListPage(val folderList: FolderList)
}