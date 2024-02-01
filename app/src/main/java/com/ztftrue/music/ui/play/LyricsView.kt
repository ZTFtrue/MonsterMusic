package com.ztftrue.music.ui.play

import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MainActivity
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.utils.ListStringCaption
import com.ztftrue.music.utils.textToolbar.CustomTextToolbar
import com.ztftrue.music.utils.LyricsType
import com.ztftrue.music.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


const val Lyrics = "lyrics"
var size = mutableStateOf(IntSize.Zero)

@UnstableApi
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LyricsView(
    musicViewModel: MusicViewModel,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var currentI by remember { mutableIntStateOf(0) }
    var isSelected by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    LaunchedEffect(musicViewModel.sliderPosition.floatValue) {
        val timeState = musicViewModel.sliderPosition.floatValue

        if (musicViewModel.hasTime == LyricsType.LRC) {
            for ((index, entry) in musicViewModel.currentCaptionList.withIndex()) {
                if (entry.timeStart > timeState) {
                    if (currentI != index) {
                        currentI = index
                        if (musicViewModel.autoScroll.value && isSelected && !showMenu) {
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
                if (musicViewModel.autoScroll.value && isSelected && !showMenu) {
                    launch(Dispatchers.Main) {
                        // TODO calculate the scroll position by　
                        listState.scrollToItem(if ((currentI - 1) < 0) 0 else (currentI - 1), 0)
                    }
                }
            }
        } else {
            currentI = (timeState / musicViewModel.itemDuration).toInt()
            if (musicViewModel.currentCaptionList.getOrElse(currentI) {
                    ListStringCaption(arrayListOf(), 0)
                }.text.isNotEmpty()) {
                if (musicViewModel.autoScroll.value && isSelected && !showMenu) {
                    launch(Dispatchers.Main) {
                        listState.scrollToItem(if ((currentI - 1) < 0) 0 else (currentI - 1), 0)
                    }
                }
            }
        }

    }
    val fontSize by remember {
        musicViewModel.fontSize
    }
    var word by remember {
        mutableStateOf("")
    }
    var selectedTag by remember {
        mutableStateOf("")
    }

    var popupOffset by remember {
        mutableStateOf(IntOffset(0, 0))
    }

    if (showMenu) {
        val list = musicViewModel.dictionaryAppList
        if (list.isEmpty()) {
            showMenu = false
        } else {
            list.forEach {
                if (it.autoGo) {
                    val intent = Intent()
                    intent.setAction(Intent.ACTION_PROCESS_TEXT)
                    intent.setClassName(
                        it.packageName,
                        it.name
                    )
                    intent.putExtra(
                        Intent.EXTRA_PROCESS_TEXT,
                        word
                    )
                    context.startActivity(intent)
                    return@forEach
                }
            }
            Popup(
                // on below line we are adding
                // alignment and properties.
                alignment = Alignment.TopCenter,
                properties = PopupProperties(),
                offset = popupOffset,
                onDismissRequest = {
                    showMenu = false
                    isSelected = false
                    selectedTag = ""
                    word = ""
                }
            ) {
                val rowListSate = rememberLazyListState()
                Column(
                    Modifier
                        .height(60.dp)
                        .padding(top = 5.dp)
                        // on below line we are adding background color
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            RoundedCornerShape(10.dp)
                        )
                        // on below line we are adding border.
                        .border(
                            1.dp,
                            color = Color.Black,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(5.dp),
                        state = rowListSate,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .fillMaxWidth()
                    ) {
                        items(list.size) { index ->
                            val resolveInfo = list[index]
                            Button(
                                onClick = {
                                    val intent = Intent()
                                    intent.setAction(Intent.ACTION_PROCESS_TEXT)
                                    intent.setClassName(
                                        resolveInfo.packageName,
                                        resolveInfo.name
                                    )
                                    intent.putExtra(
                                        Intent.EXTRA_PROCESS_TEXT,
                                        word
                                    )
                                    context.startActivity(intent)
                                }
                            ) {
                                Text(
                                    text = resolveInfo.label
                                )
                            }
                        }
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
                .clickable {
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
                        .pointerInteropFilter {
                            when (it.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    if (it.action == MotionEvent.ACTION_DOWN) {
                                        val a = if (it.y > size.value.height / 2) {
                                            it.y - fontSize * 3 - 60.dp.toPx(context)
                                        } else {
                                            it.y + fontSize * 3
                                        }
                                        popupOffset = IntOffset(0, a.toInt())
                                    }
                                }
                            }
                            false
                        }
                        .motionEventSpy {

                        }
                        .onSizeChanged { sizeIt ->
                            size.value = sizeIt
                        }
                        .padding(start = 20.dp, end = 20.dp)
                ) {
                    items(musicViewModel.currentCaptionList.size) { listIndex ->
                        key(Unit) {
                            val tex = musicViewModel.currentCaptionList[listIndex].text
                            val annotatedString = buildAnnotatedString {
                                for ((index, text) in tex.withIndex()) {
                                    val pattern = Regex("[,:;.\"]")
                                    val tItem = text.replace(pattern, "")
                                    pushStringAnnotation("word$tItem$index", tItem)
                                    withStyle(
                                        style = SpanStyle(
                                            textDecoration = if (selectedTag == "$listIndex word$tItem$index") {
                                                TextDecoration.Underline
                                            } else {
                                                TextDecoration.None
                                            }
                                        )
                                    ) {
                                        append(text)
                                    }
                                    pop()
                                    pushStringAnnotation("space", "")
                                    append(" ")
                                    pop()
                                }
                            }
                            ClickableText(
                                text = annotatedString,
                                style = TextStyle(
                                    color = if (currentI == listIndex && musicViewModel.autoHighLight.value) {
                                        Color.Blue
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    },
                                    fontSize = fontSize.sp,
                                    textAlign = musicViewModel.textAlign.value,
                                    lineHeight = (fontSize * 1.5).sp,
                                    textIndent = if (musicViewModel.textAlign.value == TextAlign.Justify || musicViewModel.textAlign.value == TextAlign.Left) {
                                        TextIndent(fontSize.sp * 2)
                                    } else {
                                        TextIndent.None
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(2.dp)
                            ) { offset ->

                                if (showMenu) {
                                    showMenu = false
                                } else {
                                    val annotations =
                                        annotatedString.getStringAnnotations(offset, offset)
                                    annotations.firstOrNull()?.let { itemAnnotations ->
                                        if (itemAnnotations.tag.startsWith("word")) {
                                            selectedTag = "$listIndex ${itemAnnotations.tag}"
                                            word = itemAnnotations.item
                                            showMenu = true
                                        }
                                    }
                                }

                            }
                        }

                    }
                }
            }
        }
    }


}

fun Dp.toPx(context: Context): Int {
    val displayMetrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.value, displayMetrics)
        .toInt()
}

