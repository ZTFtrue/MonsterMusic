package com.ztftrue.music.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.media3.session.MediaBrowser
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.play.PlayService
import com.ztftrue.music.play.PlayService.Companion.COMMAND_PlAY_LIST_CHANGE
import com.ztftrue.music.play.PlayService.Companion.COMMAND_SORT_QUEUE
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


    fun sortQueue(
        musicViewModel: MusicViewModel, musicList: SnapshotStateList<MusicItem>,
        music: MusicItem, currentIndex: Int, targetIndex: Int
    ) {
        if (currentIndex == targetIndex) return
        musicList.remove(music)
        musicList.add(targetIndex, music)
        val bundle = Bundle()
        bundle.putInt("index", currentIndex)
        bundle.putInt("targetIndex", targetIndex)
        musicViewModel.browser?.sendCustomCommand(COMMAND_SORT_QUEUE, bundle)
        //
//            val index = extras.getInt("index")
//            val targetIndex = extras.getInt("targetIndex")
//            val m = musicQueue.removeAt(index)
//            musicQueue.add(targetIndex, m)
//
//            val currentPlayIndex = exoPlayer.currentMediaItemIndex
//            val currentPosition = exoPlayer.currentPosition
//            if (currentPlayIndex == index) {
//                exoPlayer.pause()
//            }
//            val im = exoPlayer.getMediaItemAt(index)
//            exoPlayer.removeMediaItem(index)
//            exoPlayer.addMediaItem(targetIndex, im)
//            if (currentPlayIndex == index) {
//                exoPlayer.seekTo(targetIndex, currentPosition)
//                exoPlayer.play()
//            }
//            val start = FastMath.min(index, targetIndex)
//            val end = FastMath.max(index, targetIndex)
//            if (SharedPreferencesUtils.getEnableShuffle(this@PlayService)) {
//                val changedQueueArray = ArrayList<MusicItem>(end - start + 1)
//                for (i in start..end) {
//                    musicQueue[i].priority = i + 1
//                    changedQueueArray.add(musicQueue[i])
//                }
//                CoroutineScope(Dispatchers.IO).launch {
//                    db.QueueDao().updateList(changedQueueArray)
//                }
//            } else {
//                val changedQueueArray = ArrayList<MusicItem>(end - start + 1)
//                for (i in start..end) {
//                    musicQueue[i].tableId = i.toLong() + 1
//                    changedQueueArray.add(musicQueue[i])
//                }
//                CoroutineScope(Dispatchers.IO).launch {
//                    db.QueueDao().updateList(changedQueueArray)
//                }
//            }
    }

    fun sortPlayLists(
        mediaBrowserCompat: MediaBrowser,
        context: Context,
        playList: AnyListBase,
        musicList: SnapshotStateList<MusicItem>,
        music: MusicItem,
        targetIndex: Int = 0,
        lifecycleScope: CoroutineScope
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
                                lifecycleScope.launch(Dispatchers.Main) {
                                    mediaBrowserCompat.sendCustomCommand(
                                        COMMAND_PlAY_LIST_CHANGE,
                                        Bundle().apply {},
                                    )
                                }
                            }
                        })
                }
            }
        }
    }
}