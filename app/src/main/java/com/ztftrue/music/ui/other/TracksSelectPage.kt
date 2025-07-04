package com.ztftrue.music.ui.other

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.IndeterminateCheckBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.ACTION_PlayLIST_CHANGE
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.ui.public.BackButton
import com.ztftrue.music.ui.public.TracksListView
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.trackManager.PlaylistManager


/**
 * show all music of playlist
 */
@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun TracksSelectPage(
    musicViewModel: MusicViewModel,
    navController: NavHostController,
    playListName: String?,
    playListId: Long?
) {
    val showIndicator = remember { mutableStateOf(false) }
    LaunchedEffect(key1 = musicViewModel.showIndicatorMap) {
        showIndicator.value =
            musicViewModel.showIndicatorMap.getOrDefault(PlayListType.Songs.toString(), false)
    }
    val musicList = remember { mutableStateListOf<MusicItem>() }
    val context = LocalContext.current
    val selectList = remember { mutableStateListOf<MusicItem>() }
    LaunchedEffect(Unit) {
        musicList.addAll(
             musicViewModel.songsList
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.padding(all = 0.dp),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton(navController)
                },
                title = {},
                actions = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BackButton(navController)
                        Row {
                            IconButton(onClick = {
                                if (selectList.size == musicList.size) {
                                    selectList.clear()
                                } else if (selectList.isNotEmpty()) {
                                    selectList.clear()
                                    selectList.addAll(musicList)
                                } else {
                                    selectList.addAll(musicList)
                                }
                            }) {
                                Icon(
                                    imageVector = if (selectList.size == musicList.size) {
                                        Icons.Outlined.CheckBox
                                    } else if (selectList.isNotEmpty()) {
                                        Icons.Outlined.IndeterminateCheckBox
                                    } else {
                                        Icons.Outlined.CheckBoxOutlineBlank
                                    },
                                    contentDescription = "Operate More, will open dialog",
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            IconButton(onClick = {
                                if (playListId == -1L) {// create
                                    if (!playListName.isNullOrEmpty()) {
                                        val ids = ArrayList<MusicItem>(selectList.size)
                                        selectList.forEach { ids.add(it) }
                                        val id =
                                            PlaylistManager.createPlaylist(
                                                context,
                                                playListName,
                                                ids,
                                                false
                                            )
                                        if (id != null) {
                                            selectList.clear()
                                            musicViewModel.mediaBrowser?.sendCustomAction(
                                                ACTION_PlayLIST_CHANGE, null, null
                                            )
                                            navController.popBackStack()

                                        } else {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.create_failed),
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }
                                    }
                                } else {
                                    if (playListId != null) {
                                        val ids = ArrayList<MusicItem>(selectList.size)
                                        selectList.forEach { ids.add(it) }
                                        if (PlaylistManager.addMusicsToPlaylist(
                                                context,
                                                playListId,
                                                ids,
                                                false
                                            )
                                        ) {
                                            selectList.clear()
                                            Utils.refreshPlaylist(musicViewModel)
                                        }
                                        navController.popBackStack()
                                    }
                                }
                            }) {
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = "Save playlist",
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                    }
                }
            )

        },
        bottomBar = { },
        floatingActionButton = {},
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                TracksListView(
                    musicViewModel,
                    AnyListBase(0, PlayListType.None), musicList, showIndicator = showIndicator,
                    selectStatus = true, selectList = selectList
                )
            }

        },
    )
}
