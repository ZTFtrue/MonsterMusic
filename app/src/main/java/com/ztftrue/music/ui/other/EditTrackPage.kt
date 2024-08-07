package com.ztftrue.music.ui.other

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.text.isDigitsOnly
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.ztftrue.music.MainActivity
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.ACTION_TRACKS_UPDATE
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.ui.public.BackButton
import com.ztftrue.music.utils.Utils.getCover
import com.ztftrue.music.utils.trackManager.TracksManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * https://stackoverflow.com/questions/57804074/how-to-update-metadata-of-audio-file-in-android-q-media-store
 *  // TODO this is disable,  because cant  updating real file in storage
 */
@OptIn(
    ExperimentalFoundationApi::class,
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
    var lyrics by remember { mutableStateOf("") }
    var musicPath by remember { mutableStateOf("") }
    var enableEdit by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val coverBitmap = remember {
        mutableStateOf<Bitmap?>(null)
    }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = musicViewModel.editTrackEnable.value) {
        enableEdit = musicViewModel.editTrackEnable.value
    }
    DisposableEffect(key1 = Unit) {
        onDispose {
            musicViewModel.editTrackEnable.value = false
            coverBitmap.value = null
        }
    }
    LaunchedEffect(Unit) {
        val musicItem = TracksManager.getMusicById(context, musicId)
        if (musicItem != null) {
            musicPath = musicItem.path
            coverBitmap.value = getCover(context, musicId)
            title = musicItem.name
            album = musicItem.album
            artist = musicItem.artist
            genre = musicItem.genre
            year = if (musicItem.year == 0) "" else musicItem.year.toString()
        } else {
            navController.popBackStack()
        }
    }

    fun saveTrackMessage() {
        saving = true
        val success = TracksManager.saveTrackInfo(
            context,
            musicId,
            musicPath,
            title,
            album,
            artist,
            genre,
            year,
            coverBitmap.value,
            lyrics
        )
        saving = false
        enableEdit = !success
        if (success) {
            val bundleTemp = Bundle()
            bundleTemp.putLong("id", musicId)
            musicViewModel.mediaBrowser?.sendCustomAction(
                ACTION_TRACKS_UPDATE,
                bundleTemp,
                object : MediaBrowserCompat.CustomActionCallback() {
                    override fun onResult(
                        action: String?,
                        extras: Bundle?,
                        resultData: Bundle?
                    ) {
                        super.onResult(action, extras, resultData)
                        if (ACTION_TRACKS_UPDATE == action) {
                            musicViewModel.refreshPlayList.value =
                                !musicViewModel.refreshPlayList.value
                            musicViewModel.refreshAlbum.value =
                                !musicViewModel.refreshAlbum.value
                            musicViewModel.refreshArtist.value =
                                !musicViewModel.refreshArtist.value
                            musicViewModel.refreshGenre.value =
                                !musicViewModel.refreshGenre.value
                            musicViewModel.refreshFolder.value =
                                !musicViewModel.refreshFolder.value
                            if (resultData != null) {
                                resultData.getParcelableArrayList<MusicItem>(
                                    "songsList"
                                )?.also {
                                    musicViewModel.songsList.clear()
                                    musicViewModel.songsList.addAll(it)
                                }
                                resultData.getParcelable<MusicItem>("item")?.also {
                                    musicViewModel.currentPlay.value = it
                                    musicViewModel.musicQueue.forEach { mIt ->
                                        if (mIt.id == it.id) {
                                            mIt.name = it.name
                                            mIt.path = it.path
                                            mIt.duration = it.duration
                                            mIt.displayName = it.displayName
                                            mIt.album = it.album
                                            mIt.albumId = it.albumId
                                            mIt.artist = it.artist
                                            mIt.artistId = it.artistId
                                            mIt.genre = it.genre
                                            mIt.genreId = it.genreId
                                            mIt.year = it.year
                                            mIt.songNumber = it.songNumber
                                        }
                                    }
                                }
                            }
//                        musicViewModel..value =
//                            !musicViewModel.refreshAlbum.value
//                        musicViewModel..value =
//                            !musicViewModel.refreshAlbum.value
                            //   navController.popBackStack()
                        }
                    }
                }
            )
        }

    }
    Scaffold(
        modifier = Modifier.padding(all = 0.dp),
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(navController) },
                title = {
                    Text(text = title, color = MaterialTheme.colorScheme.onBackground)
                }, actions = {
                    IconButton(
                        modifier = Modifier
                            .semantics {
                                contentDescription = "Enable Edit"
                            },
                        onClick = {
                            if (enableEdit) {
                                saveTrackMessage()
                            } else {
                                enableEdit =
                                    TracksManager.requestEditPermission(context, musicId, musicPath)
                            }
                        }) {
                        Icon(
                            imageVector = if (enableEdit) Icons.Outlined.Done else Icons.Outlined.Edit,
                            contentDescription = "Operate More, will open dialog",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                })

        },

        content = { padding ->

            ConstraintLayout(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                val (list, progress) = createRefs()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .constrainAs(list) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }

                ) {
                    item {
                        ConstraintLayout {

                            val (cover, edit) = createRefs()
                            Image(
                                painter = rememberAsyncImagePainter(
                                    if (coverBitmap.value == null) {
                                        R.drawable.broken_image
                                    } else {
                                        coverBitmap.value
                                    }
                                ),
                                contentDescription = "Cover",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .constrainAs(cover) {
                                        top.linkTo(parent.top)
                                        start.linkTo(parent.start)
                                        end.linkTo(parent.end)
                                    }
                            )
                            if (enableEdit) {
                                IconButton(
                                    modifier = Modifier
                                        .semantics {
                                            contentDescription = "Edit Cover"
                                        }
                                        .padding(5.dp)
                                        .constrainAs(edit) {
                                            bottom.linkTo(parent.bottom)
                                            end.linkTo(parent.end)
                                        },
                                    onClick = {

                                        if (context is MainActivity) {
                                            (context).openImagePicker(coverBitmap)
                                        }
                                    }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit icon",
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                        }
                    }
                    item {
                        TextField(
                            enabled = enableEdit,
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
                                Text(
                                    stringResource(id = R.string.title),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
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
                                .fillMaxWidth(1f)
                                .focusRequester(focusRequester),
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
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.38f
                                ),
                                disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.12f
                                ),
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
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        )
                    }
                    item {
                        TextField(
                            enabled = enableEdit,
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
                                Text(
                                    stringResource(R.string.album, ""),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
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
                                .fillMaxWidth(1f)
                                .focusRequester(focusRequester),
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
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.38f
                                ),
                                disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.12f
                                ),
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
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        )
                    }
                    item {
                        TextField(
                            enabled = enableEdit,
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
                                Text(
                                    stringResource(id = R.string.artist, ""),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
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
                                .fillMaxWidth(1f)
                                .focusRequester(focusRequester),
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
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.38f
                                ),
                                disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.12f
                                ),
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
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        )
                    }
                    item {
                        TextField(
                            enabled = enableEdit,
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
                                Text(
                                    stringResource(R.string.genre),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
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
                                .fillMaxWidth(1f)
                                .focusRequester(focusRequester),
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
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.38f
                                ),
                                disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.12f
                                ),
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
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        )
                    }
                    item {
                        TextField(
                            enabled = enableEdit,
                            value = year,
                            onValueChange = {
                                if (it.isNotEmpty()) {
                                    if (it.isDigitsOnly() && !it.contains(".") && it.length < 6 && (it.toLong() > 0) && year.toIntOrNull() != null) {
                                        year = it
                                    }
                                } else {
                                    year = ""
                                }
                            },
                            label = {
                                Text(
                                    stringResource(R.string.year),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }, // Placeholder or hint text
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Number
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
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.38f
                                ),
                                disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.12f
                                ),
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
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        )
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .height((LocalConfiguration.current.screenHeightDp / 2).dp)
                                .fillMaxWidth()
                        ) {

                        }
                    }
                }
                if (saving) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .width((LocalConfiguration.current.screenWidthDp / 2).dp)
                            .constrainAs(progress) {
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                            },
                    )
                }
            }

        },
    )
}
