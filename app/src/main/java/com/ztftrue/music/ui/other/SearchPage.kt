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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.ACTION_SEARCH
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.ui.home.AlbumGridView
import com.ztftrue.music.ui.home.ArtistsGridView
import com.ztftrue.music.ui.public.BackButton
import com.ztftrue.music.ui.public.TracksListView
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.ScrollDirectionType
import com.ztftrue.music.utils.model.AlbumList
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.model.ArtistList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


/**
 * show all music of playlist
 */
@UnstableApi
@Composable
fun SearchPage(
    musicViewModel: MusicViewModel,
    navController: NavHostController,
) {
    val focusRequester = remember { FocusRequester() }
    val tracksList = remember { mutableStateListOf<MusicItem>() }
    val albumsList = remember { mutableStateListOf<AlbumList>() }
    val artistList = remember { mutableStateListOf<ArtistList>() }
    var modeList by remember { mutableStateOf(AnyListBase(-1, PlayListType.None)) }
    var keywords by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = LocalConfiguration.current
    val width = (configuration.screenWidthDp / 2.5) + 70
    var jobSeek: Job? = null
    val job = CoroutineScope(Dispatchers.IO)
    LaunchedEffect(keywords) {
        jobSeek?.cancel()
        if (keywords.isNotEmpty()) {
            jobSeek = job.launch {
                Thread.sleep(300)
                val bundle = Bundle()
                bundle.putString("keyword", keywords)
                if (keywords.isEmpty()) {
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
    /**
     * sometimes some the focusRequester is not initialized
     * java.lang.IllegalStateException:
     * 01-22 04:40:38.980: W/AndroidJUnitRunner(9977):    FocusRequester is not initialized. Here are some possible fixes:
     * 01-22 04:40:38.980: W/AndroidJUnitRunner(9977):    1. Remember the FocusRequester: val focusRequester = remember { FocusRequester() }
     * 01-22 04:40:38.980: W/AndroidJUnitRunner(9977):    2. Did you forget to add a Modifier.focusRequester() ?
     * 01-22 04:40:38.980: W/AndroidJUnitRunner(9977):    3. Are you attempting to request focus during composition? Focus requests
     * should be made in response to some event. Eg Modifier.clickable { focusRequester.requestFocus() }
     *   DisposableEffect(Unit) {
     *         // This block will be executed when the composable is committed
     *         onDispose {
     *             // This block will be executed when the composable is disposed (removed)
     *             // Perform any cleanup or actions needed when the composition is ending
     *         }
     *     }
     */
//    LaunchedEffect(Unit) {
//        focusRequester.requestFocus()
//    }
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
                        CompositionLocalProvider(
                            LocalContentColor provides MaterialTheme.colorScheme.onBackground
                        ) {
                                OutlinedTextField(
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
                                        Text(stringResource(R.string.enter_text_to_search))
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
                                        .focusRequester(focusRequester)
                                        .background(MaterialTheme.colorScheme.primary),
                                    suffix = {

                                    },
                                    colors = TextFieldDefaults.colors(
                                        errorTextColor = MaterialTheme.colorScheme.primary,
                                        focusedTextColor = MaterialTheme.colorScheme.primary,
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        unfocusedTextColor = MaterialTheme.colorScheme.primary,
                                        focusedContainerColor = MaterialTheme.colorScheme.background,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                        cursorColor = MaterialTheme.colorScheme.primary,
                                        errorCursorColor = MaterialTheme.colorScheme.error,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                        errorIndicatorColor = MaterialTheme.colorScheme.error,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.38f
                                        ),
                                        errorLeadingIconColor = MaterialTheme.colorScheme.error,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.38f
                                        ),
                                        errorTrailingIconColor = MaterialTheme.colorScheme.error,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        errorLabelColor = MaterialTheme.colorScheme.error,
                                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.38f
                                        )
                                    ),
                                    textStyle= MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                                )
                        }
                    }
                }
            }
        },
//        bottomBar = { Bottom(musicViewModel, navController) },
        floatingActionButton = {},
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                item(1) {


                    if (keywords.isNotEmpty() && albumsList.isEmpty() && artistList.isEmpty() && tracksList.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_music),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.horizontalScroll(rememberScrollState(0))
                        )
                    }
                    if (albumsList.isNotEmpty()) {

                        Text(text =stringResource(R.string.album,""))
                        Box(
                            modifier = Modifier
                                .height(width.dp)
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

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(color = MaterialTheme.colorScheme.onBackground)
                        )
                    }
                    if (artistList.isNotEmpty()) {
                        Text(text = stringResource(R.string.artist,""))
                        Box(
                            modifier = Modifier
                                .height(width.dp)
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
                        HorizontalDivider(
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
