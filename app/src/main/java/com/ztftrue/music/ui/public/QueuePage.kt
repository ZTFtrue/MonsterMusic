package com.ztftrue.music.ui.public

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.ztftrue.music.MusicViewModel


/**
 * show all music of playlist
 */
@UnstableApi
@Composable
fun QueuePage(
    musicViewModel: MusicViewModel,
    navController: NavHostController,
) {
    val musicList = remember { musicViewModel.musicQueue }

    Scaffold(
        modifier = Modifier.padding(all = 0.dp),
        topBar = {
                TopBar(navController, musicViewModel, content = {
//                    IconButton(
//                        modifier = Modifier.width(40.dp), onClick = {
////                            showDialog = true
//                        }) {
//                        Icon(
//                            imageVector = Icons.Default.MoreVert,
//                            contentDescription = "Operate",
//                            modifier = Modifier
//                                .size(20.dp)
//                                .clip(CircleShape),
//                        )
//                    }
                })
        },
        bottomBar = { Bottom(musicViewModel, navController) },
        floatingActionButton = {},
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                TracksListView(
                    modifier = Modifier
                        .fillMaxSize(),
                    musicViewModel, null, musicList
                )
            }

        },
    )
}
