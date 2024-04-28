package com.ztftrue.music.ui.other

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.ui.public.BackButton
import com.ztftrue.music.utils.trackManager.TracksManager


/**
 * https://stackoverflow.com/questions/57804074/how-to-update-metadata-of-audio-file-in-android-q-media-store
 *  // TODO this is disable,  because cant  updating real file in storage
 */
@OptIn(
    ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@UnstableApi
@Composable
fun EditTrackPage(
    musicViewModel: MusicViewModel,
    navController: NavHostController,
    musicId: Long
) {
    var title by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var coverBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(Unit) {
        val musicItem = TracksManager.getMusicById(context, musicId)
        if (musicItem != null) {
            coverBitmap = musicViewModel.getCover(musicItem.path)
            title = musicItem.name
            album = musicItem.album
            artist = musicItem.artist
            genre = musicItem.genre
            year = if (musicItem.year == 0) "" else musicItem.year.toString()
        } else {
            navController.popBackStack()
        }
    }
//    fun saveTrackMessage() {
//        TracksManager.saveTrackInfo(context, musicId, title, album, artist, genre, year)
//        val bundleTemp = Bundle()
//        bundleTemp.putLong("id", musicId)
//        musicViewModel.mediaBrowser?.sendCustomAction(
//            ACTION_TRACKS_UPDATE,
//            bundleTemp,
//            object : MediaBrowserCompat.CustomActionCallback() {
//                override fun onResult(
//                    action: String?,
//                    extras: Bundle?,
//                    resultData: Bundle?
//                ) {
//                    super.onResult(action, extras, resultData)
//                    if (ACTION_TRACKS_UPDATE == action) {
//                        musicViewModel.refreshList.value =
//                            !musicViewModel.refreshList.value
//                    }
//                }
//            }
//        )
//    }
    Scaffold(
        modifier = Modifier.padding(all = 0.dp),
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(navController) },
                title = {
                    Text(text = title, color = MaterialTheme.colorScheme.onBackground)
                })
        },

        content = { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    if (coverBitmap != null) Image(
                        painter = rememberAsyncImagePainter(
                            coverBitmap
                        ), contentDescription = "Cover",
                        modifier = Modifier
                            .fillMaxSize()
                            .aspectRatio(1f)
                            .background(color = Color.Black)
                            .combinedClickable(
                                onLongClick = {

                                },
                                onClick = {

                                }
                            )
                    )
                }
                item {
                    ProvideTextStyle(TextStyle(color = MaterialTheme.colorScheme.onPrimary)) {
                        TextField(
                            enabled = false,
                            value = title,
                            onValueChange = {
                                val newText = it.ifEmpty {
                                    ""
                                }
                                if (title != newText) {
                                    title = newText
                                }
                            },
                            label = {
                                Text(stringResource(id = R.string.title))
                            }, // Placeholder or hint text
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Text
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusRequester.freeFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .focusRequester(focusRequester),
                        )
                    }
                }
                item {
                    ProvideTextStyle(TextStyle(color = MaterialTheme.colorScheme.onPrimary)) {
                        TextField(
                            enabled = false,
                            value = album,
                            onValueChange = {
                                val newText = it.ifEmpty {
                                    ""
                                }
                                if (album != newText) {
                                    album = newText
                                }
                            },
                            label = {
                                Text(stringResource(R.string.album))
                            }, // Placeholder or hint text
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Text
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusRequester.freeFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .focusRequester(focusRequester),
                        )
                    }
                }
                item {
                    ProvideTextStyle(TextStyle(color = MaterialTheme.colorScheme.onPrimary)) {
                        TextField(
                            enabled = false,
                            value = artist,
                            onValueChange = {
                                val newText = it.ifEmpty {
                                    ""
                                }
                                if (artist != newText) {
                                    artist = newText
                                }
                            },
                            label = {
                                Text(stringResource(id = R.string.artist, ""))
                            }, // Placeholder or hint text
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Text
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusRequester.freeFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .focusRequester(focusRequester),
                        )
                    }
                }
                item {
                    ProvideTextStyle(TextStyle(color = MaterialTheme.colorScheme.onPrimary)) {
                        TextField(
                            enabled = false,
                            value = genre,
                            onValueChange = {
                                val newText = it.ifEmpty {
                                    ""
                                }
                                if (genre != newText) {
                                    genre = newText
                                }
                            },
                            label = {
                                Text(stringResource(R.string.genre))
                            }, // Placeholder or hint text
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Text
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusRequester.freeFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .focusRequester(focusRequester),
                        )
                    }
                }
                item {
                    ProvideTextStyle(TextStyle(color = MaterialTheme.colorScheme.onPrimary)) {
                        TextField(
                            enabled = false,
                            value = year,
                            onValueChange = {
                                val newText = it.ifEmpty {
                                    "0000"
                                }
                                if (year != newText) {
                                    year = newText
                                }
                            },
                            label = {
                                Text(stringResource(R.string.year))
                            }, // Placeholder or hint text
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Text
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusRequester.freeFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .focusRequester(focusRequester),
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .height((LocalConfiguration.current.screenHeightDp / 3).dp)
                            .fillMaxWidth()
                    ) {

                    }
                }
            }
        },
    )
}
