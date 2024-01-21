package com.ztftrue.music.ui.other

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.ACTION_GET_TRACKS
import com.ztftrue.music.play.ACTION_PlayLIST_CHANGE
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.ui.public.BackButton
import com.ztftrue.music.ui.public.TracksListView
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.PlaylistManager


/**
 * show all music of playlist
 */
@UnstableApi
@Composable
fun TracksSelectPage(
    musicViewModel: MusicViewModel,
    navController: NavHostController,
    playListName: String?,
    playListId: Long?
) {

    val musicList = remember { mutableStateListOf<MusicItem>() }
    val context = LocalContext.current
    val selectList = remember { mutableStateListOf<MusicItem>() }
    LaunchedEffect(Unit) {
        val bundle = Bundle()
        bundle.putString("type", PlayListType.Songs.name)
        musicViewModel.mediaBrowser?.sendCustomAction(
            ACTION_GET_TRACKS,
            bundle,
            object : MediaBrowserCompat.CustomActionCallback() {
                override fun onResult(
                    action: String?,
                    extras: Bundle?,
                    resultData: Bundle?
                ) {
                    super.onResult(action, extras, resultData)
                    if (ACTION_GET_TRACKS == action && resultData != null) {
                        val tracksList = resultData.getParcelableArrayList<MusicItem>("list")
                        musicList.addAll(
                            tracksList ?: emptyList()
                        )
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.padding(all = 0.dp),
        topBar = {
            Column {
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
                            } else if (selectList.size > 0) {
                                selectList.clear()
                                selectList.addAll(musicList)
                            } else {
                                selectList.addAll(musicList)
                            }
                        }) {
                            Image(
                                painter = painterResource(
                                    if (selectList.size == musicList.size) {
                                        R.drawable.ic_checked_box
                                    } else if (selectList.size > 0) {
                                        R.drawable.ic_indeterminate_check_box
                                    } else {
                                        R.drawable.ic_nocheck_box
                                    }
                                ),
                                contentDescription = "Contact profile picture",
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape),
                                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                            )
                        }
                        IconButton(onClick = {
                            if (playListId == -1L) {// create
                                if (!playListName.isNullOrEmpty()) {
                                    val id = PlaylistManager.createPlaylist(context,playListName)
                                    if (id != -1L) {
                                        val ids = ArrayList<Long>(selectList.size)
                                        selectList.forEach { ids.add(it.id) }
                                        PlaylistManager.addMusicsToPlaylist(context,id, ids)
                                        selectList.clear()
                                        musicViewModel.mediaBrowser?.sendCustomAction(
                                            ACTION_PlayLIST_CHANGE,null,null
                                        )
                                        navController.popBackStack()

                                    } else {
                                        Toast.makeText(context, "创建失败", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                            } else {
                                if (playListId != null) {
                                    val ids = ArrayList<Long>(selectList.size)
                                    selectList.forEach { ids.add(it.id) }
                                    if(PlaylistManager.addMusicsToPlaylist(context,playListId, ids)){
                                        selectList.clear()
                                    }
                                    musicViewModel.mediaBrowser?.sendCustomAction(
                                        ACTION_PlayLIST_CHANGE,null,null
                                    )
                                    navController.popBackStack()
                                }
                            }
                        }) {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = "Save playlist",
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }

                }
            }
        },
        bottomBar = { },
        floatingActionButton = {},
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )
                TracksListView(
                    modifier = Modifier
                        .fillMaxSize(),
                    musicViewModel, null, musicList,
                    true,
                    selectList
                )
            }

        },
    )
}
