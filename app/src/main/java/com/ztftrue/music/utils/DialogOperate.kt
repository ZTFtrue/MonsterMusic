package com.ztftrue.music.utils

import android.content.Context
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.ztftrue.music.Router
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.Utils.replaceCurrent
import com.ztftrue.music.utils.model.AlbumList
import com.ztftrue.music.utils.model.ArtistList
import com.ztftrue.music.utils.trackManager.ArtistManager

object DialogOperate {

    fun openArtist(context: Context, artistName: String, navController: SnapshotStateList<Any>) {
        ArtistManager.getArtistIdByName(
            context,
            artistName
        )?.let { artistId ->
            val artistList =
                ArtistList(
                    id = artistId,
                    type = PlayListType.Artists,
                    name = artistName,
                    trackNumber = 0,
                    albumNumber = 0
                )
            navController.add(Router.PlayListView(artistList))
        }
    }

    fun openOpenArtistById(artistId: Long, navController: SnapshotStateList<Any>) {
        val artistList =
            ArtistList(
                id = artistId,
                name = "",
                trackNumber = 0,
                albumNumber = 0
            )

//                        viewModel.navController.clearExceptFirst()
        navController.replaceCurrent(Router.PlayListView(artistList))
    }

    fun openOpenAlbumById(music: MusicItem, navController: SnapshotStateList<Any>) {

        val albumList =
            AlbumList(
                id = music.albumId,
                name = "",
                trackNumber = 0,
                artist = music.artist,
                firstYear = "",
                lastYear = "",
            )
        navController.replaceCurrent(Router.PlayListView(albumList))
//                        viewModel.navController.clearExceptFirst()
    }
}