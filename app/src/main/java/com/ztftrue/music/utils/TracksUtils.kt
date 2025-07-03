package com.ztftrue.music.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.widget.Toast
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.ztftrue.music.play.ACTION_PlayLIST_CHANGE
import com.ztftrue.music.play.ACTION_Sort_Queue
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.sqlData.model.SortFiledData
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.model.MusicPlayList
import com.ztftrue.music.utils.trackManager.PlaylistManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TracksUtils {

    fun currentPlayToTop(
        mediaBrowserCompat: MediaBrowserCompat, musicList: SnapshotStateList<MusicItem>,
        music: MusicItem, currentIndex: Int
    ) {
        sortQueue(
            mediaBrowserCompat, musicList,
            music, currentIndex, 0
        )
    }

    fun sortQueue(
        mediaBrowserCompat: MediaBrowserCompat, musicList: SnapshotStateList<MusicItem>,
        music: MusicItem, currentIndex: Int, targetIndex: Int
    ) {
        if (currentIndex == targetIndex) return
        musicList.remove(music)
        musicList.add(targetIndex, music)
        val bundle = Bundle()
        bundle.putInt("index", currentIndex)
        bundle.putInt("targetIndex", targetIndex)
        mediaBrowserCompat.sendCustomAction(
            ACTION_Sort_Queue,
            bundle,
            object :
                MediaBrowserCompat.CustomActionCallback() {
                override fun onResult(
                    action: String?,
                    extras: Bundle?,
                    resultData: Bundle?
                ) {
                    super.onResult(
                        action,
                        extras,
                        resultData
                    )
                }
            }
        )
    }

    fun sortPlayLists(
        mediaBrowserCompat: MediaBrowserCompat,
        context: Context,
        playList: AnyListBase,
        musicList: SnapshotStateList<MusicItem>,
        music: MusicItem,
        targetIndex: Int = 0
    ) {
        if (playList is MusicPlayList) {
            musicList.remove(music)
            musicList.add(targetIndex, music)
            val playListPath =
                PlaylistManager.resortOrRemoveTrackFromPlayList(
                    context,
                    playList.id,
                    ArrayList(musicList.toList()),
                    playList.path
                )
            CoroutineScope(Dispatchers.IO).launch {
                val sortDb =
                    MusicDatabase
                        .getDatabase(context)
                        .SortFiledDao()
                var sortData =
                    sortDb.findSortByType(playList.type.name + "@Tracks")
                if (sortData != null) {
                    if (sortData.method != "" || sortData.filed != "") {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast
                                .makeText(
                                    context,
                                    "Already change you sort order to default",
                                    Toast.LENGTH_LONG
                                )
                                .show()
                        }
                    }
                    sortData.method = ""
                    sortData.methodName = ""
                    sortData.filed = ""
                    sortData.filedName = ""
                    sortDb.update(sortData)
                } else {
                    sortData = SortFiledData(
                        playList.type.name + "@Tracks",
                        "", "", "", ""
                    )
                    sortDb.insert(sortData)
                }
                if (!playListPath.isNullOrEmpty()) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(playListPath),
                        arrayOf("*/*"),
                        object :
                            MediaScannerConnection.MediaScannerConnectionClient {
                            override fun onMediaScannerConnected() {}
                            override fun onScanCompleted(
                                path: String,
                                uri: Uri
                            ) {
                                mediaBrowserCompat.sendCustomAction(
                                    ACTION_PlayLIST_CHANGE,
                                    null,
                                    object :
                                        MediaBrowserCompat.CustomActionCallback() {
                                        override fun onResult(
                                            action: String?,
                                            extras: Bundle?,
                                            resultData: Bundle?
                                        ) {
                                            super.onResult(
                                                action,
                                                extras,
                                                resultData
                                            )
                                        }
                                    }
                                )
                            }
                        })
                }
            }

        }
    }
}