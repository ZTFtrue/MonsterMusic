package com.ztftrue.music.ui.other

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.play.ACTION_SEARCH
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.ui.home.AlbumGridView
import com.ztftrue.music.ui.home.ArtistsGridView
import com.ztftrue.music.ui.public.BackButton
import com.ztftrue.music.ui.public.Bottom
import com.ztftrue.music.ui.public.TracksListView
import com.ztftrue.music.utils.AlbumList
import com.ztftrue.music.utils.AnyListBase
import com.ztftrue.music.utils.ArtistList
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.ScrollDirectionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


/**
 * show all music of playlist
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun SearchPage(
    musicViewModel: MusicViewModel,
    navController: NavHostController,
) {
    val tracksList = remember { mutableStateListOf<MusicItem>() }
    val albumsList = remember { mutableStateListOf<AlbumList>() }
    val artistList = remember { mutableStateListOf<ArtistList>() }
    var modeList by remember { mutableStateOf(AnyListBase(-1, PlayListType.None)) }
    var keywords by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    var jobSeek: Job? = null
    val job = CoroutineScope(Dispatchers.IO)
    LaunchedEffect(keywords) {
        jobSeek?.cancel()
        if (keywords.isNotEmpty() && keywords.length >= 1) {
            jobSeek = job.launch {
                Thread.sleep(300)
                val bundle = Bundle()
                bundle.putString("keyword", keywords)
                if (keywords.isEmpty() || keywords.length < 1) {
                    return@launch
                }
                if (!isActive) {
                    return@launch
                }
                musicViewModel.mediaBrowser?.sendCustomAction(
                    ACTION_SEARCH,
                    bundle,
                    object : MediaBrowserCompat.CustomActionCallback() {
                        override fun onResult(
                            action: String?,
                            extras: Bundle?,
                            resultData: Bundle?
                        ) {
                            super.onResult(action, extras, resultData)
                            if (ACTION_SEARCH == action && resultData != null) {
                                tracksList.clear()
                                albumsList.clear()
                                artistList.clear()
                                modeList = AnyListBase(modeList.id - 1, PlayListType.None)
                                val tracksListResult =
                                    resultData.getParcelableArrayList<MusicItem>("tracks")
                                val albumListsResult =
                                    resultData.getParcelableArrayList<AlbumList>("albums")
                                val artistListsResult =
                                    resultData.getParcelableArrayList<ArtistList>("artist")
                                tracksList.addAll(tracksListResult ?: emptyList())
                                albumsList.addAll(albumListsResult ?: emptyList())
                                artistList.addAll(artistListsResult ?: emptyList())
                            }
                        }
                    }
                )
            }
        }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProvideTextStyle(TextStyle(color = MaterialTheme.colorScheme.onPrimary)) {
                            TextField(
                                value = keywords,
                                onValueChange = {
                                    val newText = it.ifEmpty {
                                        ""
                                    }
                                    if (keywords != newText) {
                                        keywords = newText
                                    }
                                },
                                placeholder = {
                                    Text("Enter text")
                                }, // Placeholder or hint text
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = ImeAction.Done,
                                    keyboardType = KeyboardType.Text
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusRequester.freeFocus()
                                        keyboardController?.hide()
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                suffix = {

                                },
                                colors = TextFieldDefaults.colors(
                                    focusedPlaceholderColor = Color.Red, // Set your desired text color here
                                    unfocusedPlaceholderColor = Color.Red, // Set your desired text color here
                                    disabledPlaceholderColor = Color.Red, // Set your desired text color here
                                    errorPlaceholderColor = Color.Red, // Set your desired text color here
                                )
                            )
                        }
//                        TextField(
//                            value = keywords,
//                            onValueChange = {
//                                val newText = it.ifEmpty {
//                                    ""
//                                }
//                                if (keywords != newText) {
//                                    keywords = newText
//                                }
//                            },
//                            placeholder = {
//                                Text("Enter text")
//                            }, // Placeholder or hint text
//                            keyboardOptions = KeyboardOptions.Default.copy(
//                                imeAction = ImeAction.Done,
//                                keyboardType = KeyboardType.Text
//                            ),
//                            keyboardActions = KeyboardActions(
//                                onDone = {
//                                    focusRequester.freeFocus()
//                                    keyboardController?.hide()
//                                }
//                            ),
//                            modifier = Modifier
//                                .fillMaxWidth(0.9f)
//                                .focusRequester(focusRequester),
//                            suffix = {
//
//                            },
//                            colors = TextFieldDefaults.colors(
//                                focusedPlaceholderColor = Color.Red, // Set your desired text color here
//                                unfocusedPlaceholderColor = Color.Red, // Set your desired text color here
//                                disabledPlaceholderColor = Color.Red, // Set your desired text color here
//                                errorPlaceholderColor = Color.Red, // Set your desired text color here
//                            )
//                        )
//                        IconButton(onClick = {
//                            focusRequester.freeFocus()
//                            keyboardController?.hide()
//                        }) {
//                            Icon(
//                                Icons.Filled.Search, contentDescription = "Search",
//                                modifier = Modifier.size(40.dp)
//                            )
//                        }

                    }
                }
            }
        },
        bottomBar = { Bottom(musicViewModel, navController) },
        floatingActionButton = {},
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                item(1) {
                    val configuration = LocalConfiguration.current
                    if (keywords.isNotEmpty() && albumsList.isEmpty() && artistList.isEmpty() && tracksList.isEmpty()) {
                        Text(
                            text = "No result",
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.horizontalScroll(rememberScrollState(0))
                        )
                    }
                    if (albumsList.isNotEmpty()) {
                        Text(text = "Album")
                        Box(
                            modifier = Modifier
                                .height((configuration.screenWidthDp / musicViewModel.albumItemsCount.intValue + 40).dp)
                                .background(MaterialTheme.colorScheme.secondary)
                                .fillMaxWidth()
                        ) {
                            AlbumGridView(
                                musicViewModel = musicViewModel,
                                navController = navController,
                                albumListDefault = albumsList,
                                scrollDirection = ScrollDirectionType.GRID_HORIZONTAL
                            )
                        }

                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(color = MaterialTheme.colorScheme.onBackground)
                        )
                    }
                    if (artistList.isNotEmpty()) {
                        Text(text = "Artist")
                        Box(
                            modifier = Modifier
                                .height((configuration.screenWidthDp / musicViewModel.albumItemsCount.intValue + 40).dp)
                                .background(MaterialTheme.colorScheme.secondary)
                                .fillMaxWidth()
                        ) {
                            ArtistsGridView(
                                musicViewModel = musicViewModel,
                                navController = navController,
                                artistListDefault = artistList,
                                scrollDirection = ScrollDirectionType.GRID_HORIZONTAL
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(color = MaterialTheme.colorScheme.onBackground)
                        )
                    }
                    if (tracksList.isNotEmpty()) {
                        // TODO why this? (tracksList.size+2) * (60 + 1.2 +25))
                        Box(modifier = Modifier.height(((tracksList.size + 2) * (60 + 1.2 + 25)).dp)) {
                            TracksListView(
                                modifier = Modifier,
                                musicViewModel, modeList, tracksList
                            )
                        }
                    }
                }

            }

        },
    )
}
