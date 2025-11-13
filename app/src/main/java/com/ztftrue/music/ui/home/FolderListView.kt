package com.ztftrue.music.ui.home

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.CustomMetadataKeys
import com.ztftrue.music.ui.other.FolderItemView
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.model.FolderList
import com.ztftrue.music.utils.trackManager.FolderManger


@Composable
fun FolderListView(
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel,
    navController: SnapshotStateList<Any>,
) {
    val folderList = remember { mutableStateListOf<FolderList>() }
    val treeFolderList = remember { mutableStateListOf<FolderList>() }
    val showList = remember { mutableStateListOf<FolderList>() }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(musicViewModel.folderViewTree.value) {
        if (musicViewModel.folderViewTree.value) {
            showList.clear()
            showList.addAll(treeFolderList)
        } else {
            showList.clear()
            showList.addAll(folderList)
        }
    }

    LaunchedEffect(Unit, musicViewModel.refreshFolder.value) {
        folderList.clear()
        treeFolderList.clear()
        val futureResult: ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>? =
            musicViewModel.browser?.getChildren("folders_root", 0, Integer.MAX_VALUE, null)
        futureResult?.addListener({
            try {
                val result: LibraryResult<ImmutableList<MediaItem>>? = futureResult.get()
                if (result == null || result.resultCode != LibraryResult.RESULT_SUCCESS) {
                    return@addListener
                }
                val albumMediaItems: List<MediaItem> = result.value ?: listOf()
                albumMediaItems.forEach { mediaItem ->
                    val folder = FolderList(
                        children = ArrayList(),
                        path = mediaItem.mediaMetadata.extras?.getString(
                            CustomMetadataKeys.FOLDER_PATH,
                            "/"
                        ) ?: "/",
                        id = mediaItem.mediaId.toLong(),
                        name = mediaItem.mediaMetadata.title.toString(),
                        trackNumber = mediaItem.mediaMetadata.totalTrackCount ?: 0,
                        isShow = mediaItem.mediaMetadata.extras?.getBoolean(
                            CustomMetadataKeys.FOLDER_IS_SHOW,
                            true
                        ) ?: true
                    )
                    folderList.add(folder)
                }
                try {
                    val treeFolder = FolderManger.buildFolderTreeFromPaths(folderList)
                    treeFolderList.addAll(treeFolder)
                } catch (e: Exception) {
                    Log.e("FolderListView", "Convert to tree", e)
                }
                if (musicViewModel.folderViewTree.value) {
                    showList.clear()
                    showList.addAll(treeFolderList)
                } else {
                    showList.clear()
                    showList.addAll(folderList)
                }
            } catch (e: Exception) {
                // 处理在获取结果过程中可能发生的异常 (如 ExecutionException)
                Log.e("Client", "Failed to toggle favorite status", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Column {
        Text(
            text = stringResource(R.string.warning_for_tracks_tab),
            modifier = Modifier.padding(10.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyColumn(
            state = listState, modifier = modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            items(showList.size) { index ->
                val item = showList[index]
                FolderItemView(
                    item,
                    musicViewModel,
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth(),
                    navController
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    thickness = 1.2.dp
                )
            }
        }
    }

}

