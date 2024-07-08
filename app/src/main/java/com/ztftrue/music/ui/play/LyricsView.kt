package com.ztftrue.music.ui.play

import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MainActivity
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.utils.LyricsType
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.model.ListStringCaption
import com.ztftrue.music.utils.textToolbar.CustomTextToolbar
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
    var isSelected by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    LaunchedEffect(musicViewModel.sliderPosition.floatValue) {
        val timeState = musicViewModel.sliderPosition.floatValue
        if (musicViewModel.lyricsType == LyricsType.LRC) {
            var cIndex = 0
            for (index in musicViewModel.currentCaptionList.size - 1 downTo 0) {
                val entry = musicViewModel.currentCaptionList[index]
                if (timeState > entry.timeStart) {
                    cIndex = index
                    break
                }
            }
            if (cIndex != currentI) {
                currentI = cIndex
                if (musicViewModel.autoScroll.value && !isSelected && !showMenu) {
                    launch(Dispatchers.Main) {
                        // TODO calculate the scroll position by　
                        listState.scrollToItem(
                            if (currentI < 2) 0 else (currentI - 2),
                            0
                        )
                    }
                }
            }

        } else if (musicViewModel.lyricsType == LyricsType.VTT || musicViewModel.lyricsType == LyricsType.SRT) {
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
                if (musicViewModel.autoScroll.value && !isSelected && !showMenu) {
                    launch(Dispatchers.Main) {
                        // TODO calculate the scroll position by　
                        listState.scrollToItem(if (currentI < 2) 0 else (currentI - 2), 0)
                    }
                }
            }
        } else {
            currentI = (timeState / musicViewModel.itemDuration).toInt()
            if (musicViewModel.currentCaptionList.getOrElse(currentI) {
                    ListStringCaption(arrayListOf(), 0)
                }.text.isNotEmpty()) {
                if (musicViewModel.autoScroll.value && !isSelected && !showMenu) {
                    launch(Dispatchers.Main) {
                        listState.scrollToItem(if (currentI < 2) 0 else (currentI - 2), 0)
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
    DisposableEffect(Unit) {
        onDispose {

        }
    }
    LaunchedEffect(showMenu) {
        if (showMenu) {
            val list = musicViewModel.dictionaryAppList
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
        }
    }
    key(showMenu) {
        if (showMenu) {
            val list = musicViewModel.dictionaryAppList
            if (list.isEmpty()) {
                showMenu = false
            } else {

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
    }

    if (musicViewModel.currentCaptionList.size == 0) {
        Column {
            Text(
                text = stringResource(R.string.no_lyrics_import_tip),
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
            Text(
                text = stringResource(R.string.no_lyrics_set_folder),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        (context as MainActivity).folderPickerLauncher.launch(intent)
                    }
            )
        }

    } else {
        CompositionLocalProvider(
            LocalTextToolbar provides CustomTextToolbar(
                LocalView.current,
                musicViewModel.dictionaryAppList,
                LocalFocusManager.current,
                LocalClipboardManager.current
            ),
        ) {
            val textToolbar = LocalTextToolbar.current
            val focusManager = LocalFocusManager.current
            ConstraintLayout {
                val (playIndicator) = createRefs()
                SelectionContainer(
                    modifier = Modifier.zIndex(1f),
                    content = {
                        val nestedScrollConnection = remember {
                            object : NestedScrollConnection {
                                override fun onPreScroll(
                                    available: Offset,
                                    source: NestedScrollSource
                                ): Offset {
//                                if (textToolbar.status == TextToolbarStatus.Shown) {
                                    try {
                                        focusManager.clearFocus()
                                        textToolbar.hide()
                                    } catch (_: Exception) {

                                    }
//                                }
                                    return super.onPreScroll(available, source)
                                }
                            }
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .nestedScroll(nestedScrollConnection)
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

                                        MotionEvent.ACTION_UP -> {
                                            if (showMenu) {
                                                showMenu = false
                                                isSelected = false
                                                selectedTag = ""
                                                word = ""
                                            }
                                        }
                                    }
                                    false
                                }
                                .motionEventSpy {
                                    if (it.action == MotionEvent.ACTION_DOWN && textToolbar.status == TextToolbarStatus.Shown) {
                                        textToolbar.hide()
                                        focusManager.clearFocus()
                                    }
                                }
                                .onSizeChanged { sizeIt ->
                                    size.value = sizeIt
                                }
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
                                                MaterialTheme.colorScheme.onTertiaryContainer
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
                                            .background(
                                                if (currentI == listIndex && musicViewModel.autoHighLight.value) MaterialTheme.colorScheme.tertiaryContainer.copy(
                                                    alpha = 0.3f
                                                ) else MaterialTheme.colorScheme.background
                                            )
                                            .padding(
                                                start = 20.dp,
                                                end = 20.dp,
                                                top = 2.dp,
                                                bottom = 2.dp
                                            )
                                    ) { offset ->
                                        if (textToolbar.status == TextToolbarStatus.Shown) {
                                            textToolbar.hide()
                                            focusManager.clearFocus()
                                        } else if (showMenu) {
                                            showMenu = false
                                        } else {
                                            val annotations =
                                                annotatedString.getStringAnnotations(offset, offset)
                                            annotations.firstOrNull()?.let { itemAnnotations ->
                                                if (itemAnnotations.tag.startsWith("word")) {
                                                    selectedTag =
                                                        "$listIndex ${itemAnnotations.tag}"
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
                )
                key(musicViewModel.isEmbeddedLyrics.value) {
                    if (musicViewModel.isEmbeddedLyrics.value) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = "This is embedded lyrics",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .zIndex(2f)
                                .clickable {
                                    Toast
                                        .makeText(
                                            context,
                                            "This is embedded lyrics",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                                .constrainAs(playIndicator) {
                                    top.linkTo(anchor = parent.top, margin = 5.dp)
                                    start.linkTo(anchor = parent.start, margin = 10.dp)
                                },
                        )
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

