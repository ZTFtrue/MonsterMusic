package com.ztftrue.music.ui.play

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MainActivity
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.utils.Caption
import com.ztftrue.music.utils.CustomTextToolbar
import com.ztftrue.music.utils.LyricsType
import com.ztftrue.music.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

const val Lyrics = "lyrics"
var size = mutableStateOf(IntSize.Zero)

@UnstableApi
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsView(
    musicViewModel: MusicViewModel,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var currentI by remember { mutableIntStateOf(0) }
    var isSelected by remember { mutableStateOf(true) }
    LaunchedEffect(musicViewModel.sliderPosition.floatValue) {
        val timeState = musicViewModel.sliderPosition.floatValue

        if (musicViewModel.hasTime == LyricsType.LRC) {
            for ((index, entry) in musicViewModel.currentCaptionList.withIndex()) {
                if (entry.timeStart > timeState) {
                    if (currentI != index) {
                        currentI = index
                        if (musicViewModel.autoScroll.value && isSelected) {
                            launch(Dispatchers.Main) {
                                // TODO calculate the scroll position by　
                                listState.scrollToItem(
                                    if ((currentI - 1) < 0) 0 else (currentI - 1),
                                    0
                                )
                            }
                        }
                    }
                    break
                }
            }
        } else if (musicViewModel.hasTime == LyricsType.VTT || musicViewModel.hasTime == LyricsType.SRT) {
            val cIndex = musicViewModel.currentCaptionList.binarySearch {
                if (it.timeStart <= timeState.toLong() && it.timeEnd >= timeState.toLong()) 0
                else if (it.timeStart > timeState) {
                    1
                } else {
                    -1
                }
            }
            if (cIndex >= 0 && cIndex != currentI) {
                currentI = cIndex
                if (musicViewModel.autoScroll.value && isSelected) {
                    launch(Dispatchers.Main) {
                        // TODO calculate the scroll position by　
                        listState.scrollToItem(if ((currentI - 1) < 0) 0 else (currentI - 1), 0)
                    }
                }
            }
        } else {
            currentI = (timeState / musicViewModel.itemDuration).toInt()
            if (musicViewModel.currentCaptionList.getOrElse(currentI) {
                    Caption("", 0)
                }.text.isNotBlank()) {
                if (musicViewModel.autoScroll.value && isSelected) {
                    launch(Dispatchers.Main) {
                        listState.scrollToItem(if ((currentI - 1) < 0) 0 else (currentI - 1), 0)
                    }
                }
            }
        }

    }
    if (musicViewModel.currentCaptionList.size == 0) {
        Text(
            text = "No Lyrics, Click to import lyrics",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .onSizeChanged { sizeIt ->
                    size.value = sizeIt
                }
                .clickable() {
                    if (musicViewModel.currentPlay.value != null) {
                        val regexPattern = Regex("[<>\"/~'{}?,+=)(^&*%!@#\$]")
                        val artistsFolder = musicViewModel.currentPlay.value?.artist
                            ?.replace(
                                regexPattern,
                                "_"
                            )
                        val folderPath = "$Lyrics/$artistsFolder"
                        val folder = context.getExternalFilesDir(
                            folderPath
                        )
                        folder?.mkdirs()
                        val id =
                            musicViewModel.currentPlay.value?.name?.replace(regexPattern, "_")
                        val pathLyrics: String =
                            context.getExternalFilesDir(folderPath)?.absolutePath + "/$id.lrc"
                        val path: String =
                            context.getExternalFilesDir(folderPath)?.absolutePath + "/$id.txt"
                        val lyrics = File(pathLyrics)
                        val text = File(path)
                        if (lyrics.exists()) {
                            Utils.openFile(lyrics.path, context = context)
                        } else if (text.exists()) {
                            Utils.openFile(text.path, context = context)
                        } else {
                            val tempPath: String =
                                context.getExternalFilesDir(folderPath)?.absolutePath + "/$id."
                            (context as MainActivity).openFilePicker(tempPath)
                        }
                    }
                }
        )
    } else {
        CompositionLocalProvider(
            LocalTextToolbar provides CustomTextToolbar(LocalView.current)
        ) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .onSizeChanged { sizeIt ->
                            size.value = sizeIt
                        }
                        .padding(start = 20.dp, end = 20.dp)
                ) {
                    items(musicViewModel.currentCaptionList.size) {
                        ClickableText(
                            text = AnnotatedString(musicViewModel.currentCaptionList[it].text),
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = if (currentI == it) MaterialTheme.typography.titleLarge.fontSize else
                                    MaterialTheme.typography.titleMedium.fontSize,
                                textAlign = TextAlign.Center,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(2.dp)
                                .onSizeChanged { sizeIt ->
                                    size.value = sizeIt
                                },
                            onClick = { offset ->
                                Log.d("ClickableText", "$offset -th character is clicked.")
                            })
                    }
                }
            }
        }

    }


}

