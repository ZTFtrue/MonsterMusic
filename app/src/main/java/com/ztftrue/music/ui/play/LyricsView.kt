package com.ztftrue.music.ui.play

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MainActivity
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.utils.Caption
import com.ztftrue.music.utils.LyricsType
import com.ztftrue.music.utils.Utils.openFile
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

    LaunchedEffect(musicViewModel.sliderPosition.floatValue) {
        val timeState = musicViewModel.sliderPosition.floatValue

        if (musicViewModel.hasTime == LyricsType.LRC) {
            for ((index, entry) in musicViewModel.currentCaptionList.withIndex()) {
                if (entry.timeStart > timeState) {
                    if (currentI != index) {
                        currentI = index
                        if (musicViewModel.autoScroll.value) {
                            launch(Dispatchers.Main) {
                                // TODO calculate the scroll position by　
                                listState.scrollToItem(
                                    if ((currentI - 6) < 0) 0 else (currentI - 6),
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
                if (musicViewModel.autoScroll.value) {
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
                if (musicViewModel.autoScroll.value) {
                    launch(Dispatchers.Main) {
                        listState.scrollToItem(if ((currentI - 6) < 0) 0 else (currentI - 6), 0)
                    }
                }
            }
        }

    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .onSizeChanged { sizeIt ->
                size.value = sizeIt
            }
            .padding(start = 20.dp, end = 20.dp)
            .combinedClickable(
                onLongClick = {
                    if (musicViewModel.autoScroll.value) {
                        musicViewModel.autoScroll.value = false
                        Toast
                            .makeText(context, "Auto scroll is disabled", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        musicViewModel.autoScroll.value = true
                        Toast
                            .makeText(context, "Auto scroll is enable", Toast.LENGTH_SHORT)
                            .show()
                    }

                },
                onDoubleClick = {
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
                        val id = musicViewModel.currentPlay.value?.name?.replace(regexPattern, "_")
                        val pathLyrics: String =
                            context.getExternalFilesDir(folderPath)?.absolutePath + "/$id.lrc"
                        val path: String =
                            context.getExternalFilesDir(folderPath)?.absolutePath + "/$id.txt"
                        val lyrics = File(pathLyrics)
                        val text = File(path)
                        if (lyrics.exists()) {
                            openFile(lyrics.path, context = context)
                        } else if (text.exists()) {
                            openFile(text.path, context = context)
                        } else {
                            val tempPath: String =
                                context.getExternalFilesDir(folderPath)?.absolutePath + "/$id."
                            (context as MainActivity).openFilePicker(tempPath)
                        }
                    }
                },
            ) {
                // Toast.makeText(context, "Double click to import lyrics", Toast.LENGTH_SHORT).show()
            }
    ) {
        items(musicViewModel.currentCaptionList.size) {
            Text(
                text = musicViewModel.currentCaptionList[it].text,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = if (currentI == it) MaterialTheme.typography.titleLarge.fontSize else
                    MaterialTheme.typography.titleMedium.fontSize,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp)
                    .onSizeChanged { sizeIt ->
                        size.value = sizeIt
                    }
            )
        }
    }
}

