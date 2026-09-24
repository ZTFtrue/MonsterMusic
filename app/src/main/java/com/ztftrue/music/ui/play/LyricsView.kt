package com.ztftrue.music.ui.play

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material3.Surface
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.utils.LyricsType
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.isCjkLike
import com.ztftrue.music.utils.model.ListStringCaption
import com.ztftrue.music.utils.textToolbar.CustomTextToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt


const val Lyrics = "lyrics"

private data class WordInfo(
    val word: String,
    val start: Int,
    val end: Int,
    val wordIndex: Int
)

private suspend fun LazyListState.centerItem(
    index: Int,
    animate: Boolean = true
) {
    if (index < 0) return
    var retryCount = 0
    while (layoutInfo.viewportSize.height <= 0 && retryCount < 10) {
        retryCount++
        delay(16)
    }
    val viewportHeight = layoutInfo.viewportSize.height
    if (viewportHeight <= 0) return
    val totalItems = layoutInfo.totalItemsCount
    if (index !in 0 until totalItems) return

    val targetCenter = viewportHeight / 2
    var visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    var didJump = false
    if (visibleItem == null) {
        scrollToItem(index, 0)
        didJump = true
        retryCount = 0
        while (visibleItem == null && retryCount < 10) {
            retryCount++
            delay(16)
            visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        }
    }

    if (visibleItem != null) {
        val itemScreenCenter = visibleItem.offset + layoutInfo.beforeContentPadding + visibleItem.size / 2
        val delta = (itemScreenCenter - targetCenter).toFloat()
        if (abs(delta) > 1f) {
            if (animate && !didJump) {
                animateScrollBy(delta)
            } else {
                scrollBy(delta)
            }
        }
    }
}

private fun getActiveLyricIndex(
    timeState: Float,
    captions: List<ListStringCaption>,
    lyricsType: LyricsType,
    itemDuration: Long
): Int {
    if (captions.isEmpty()) return -1
    return when (lyricsType) {
        LyricsType.LRC -> {
            var cIndex = 0
            for (index in captions.size - 1 downTo 0) {
                if (timeState >= captions[index].timeStart) {
                    cIndex = index
                    break
                }
            }
            cIndex
        }
        LyricsType.VTT, LyricsType.SRT -> {
            val time = timeState.toLong()
            val cIndex = captions.binarySearch {
                if (it.timeStart <= time && it.timeEnd >= time) 0
                else if (it.timeStart > timeState) 1
                else -1
            }
            if (cIndex >= 0) {
                cIndex
            } else {
                val insertIndex = -cIndex - 1
                when {
                    insertIndex <= 0 -> 0
                    insertIndex >= captions.size -> captions.size - 1
                    else -> {
                        val prev = captions[insertIndex - 1]
                        val next = captions[insertIndex]
                        if ((time - prev.timeEnd) <= (next.timeStart - time)) {
                            insertIndex - 1
                        } else {
                            insertIndex
                        }
                    }
                }
            }
        }
        else -> {
            if (itemDuration > 0L) {
                (timeState / itemDuration).toInt().coerceIn(0, captions.size - 1)
            } else {
                0
            }
        }
    }
}

@UnstableApi
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LyricsView(
    musicViewModel: MusicViewModel,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val popupOffsetX = with(density) { 60.dp.roundToPx() }
    val popupOffsetY = with(density) { 115.dp.roundToPx() }
    val listState = rememberLazyListState()
    var currentI by remember { mutableIntStateOf(-1) }
    var isSelected by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var arrowOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingArrow by remember { mutableStateOf(false) }
    var pointedIndex by remember { mutableIntStateOf(-1) }
    var showSlideIndicators by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = musicViewModel.showSlideIndicators.value) {
        showSlideIndicators = musicViewModel.showSlideIndicators.value
    }
    var showRightIndicator by remember { mutableStateOf(true) }
    LaunchedEffect(key1 = musicViewModel.showRightIndicator.value) {
        showRightIndicator = musicViewModel.showRightIndicator.value
    }

    // Reset current active line when switching songs
    LaunchedEffect(musicViewModel.currentPlay.value?.id) {
        currentI = -1
    }

    val captionCount = musicViewModel.currentCaptionList.size
    val lyricsType = musicViewModel.lyricsType

    // Compute active lyric index when playback position, captions, or song changes
    LaunchedEffect(
        musicViewModel.sliderPosition.floatValue,
        captionCount,
        lyricsType,
        musicViewModel.currentPlay.value?.id
    ) {
        if (captionCount > 0) {
            val newIndex = getActiveLyricIndex(
                timeState = musicViewModel.sliderPosition.floatValue,
                captions = musicViewModel.currentCaptionList,
                lyricsType = lyricsType,
                itemDuration = musicViewModel.itemDuration
            )
            if (newIndex in 0 until captionCount && newIndex != currentI) {
                currentI = newIndex
            }
        }
    }

    // Smoothly keep the active lyric line centered as the song advances
    LaunchedEffect(currentI, musicViewModel.autoScroll.value) {
        if (currentI in 0 until captionCount) {
            if (musicViewModel.autoScroll.value && !isSelected && !showMenu && !isDraggingArrow) {
                listState.centerItem(currentI, animate = true)
            }
        }
    }

    // When lyrics list loads/updates, immediately center active lyric line without waiting
    LaunchedEffect(captionCount) {
        if (captionCount > 0 && currentI in 0 until captionCount && !isDraggingArrow) {
            listState.centerItem(currentI, animate = false)
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
    val clearSelection = {
        isSelected = false
        selectedTag = ""
        word = ""
    }

    var popupOffset by remember {
        mutableStateOf(IntOffset(0, 0))
    }

    LaunchedEffect(showMenu) {
        if (showMenu) {
            val list = musicViewModel.dictionaryAppList
            list.forEach {
                if (it.autoGo) {
                    if (musicViewModel.autoDismissDicPop.value) {
                        // 可以不显示pop
                        showMenu = false
                    }
                    val intent = Intent()
                    intent.action = Intent.ACTION_PROCESS_TEXT
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
                    alignment = Alignment.TopStart,
                    properties = PopupProperties(),
                    offset = popupOffset,
                    onDismissRequest = {
                        showMenu = false
                        clearSelection()
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
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        ) {
                            items(list.size) { index ->
                                val resolveInfo = list[index]
                                Button(
                                    onClick = {
                                        val intent = Intent()
                                        intent.action = Intent.ACTION_PROCESS_TEXT
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
                                        text = resolveInfo.label,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    if (musicViewModel.currentCaptionList.isEmpty()) {
        Column {
            if (musicViewModel.currentCaptionListLoading.value) {
                Text(
                    text = stringResource(R.string.loading),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.no_lyrics_import_tip),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp)
                        .clickable {
                            Utils.setLyricsFile(musicViewModel, context)
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
                            Utils.setLyricsFolder(context)
                        }
                )
            }

        }

    } else {
        val view = LocalView.current
        val focusManager = LocalFocusManager.current
        val clipboardManager = LocalClipboardManager.current
        val customTextToolbar = remember(view, focusManager, clipboardManager) {
            CustomTextToolbar(
                view = view,
                customApp = musicViewModel.dictionaryAppList,
                focusManager = focusManager,
                clipboardManager = clipboardManager
            )
        }
        customTextToolbar.onShow = {
            isSelected = true
        }
        customTextToolbar.onDismiss = {
            clearSelection()
        }
        val primaryColor = MaterialTheme.colorScheme.primary
        val customTextSelectionColors = remember(primaryColor) {
            TextSelectionColors(
                handleColor = primaryColor,
                backgroundColor = primaryColor.copy(alpha = 0.4f)
            )
        }
        val textToolbarProvider = LocalTextToolbar provides customTextToolbar
        CompositionLocalProvider(
            textToolbarProvider,
            LocalTextSelectionColors provides customTextSelectionColors
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                val halfHeight = maxHeight / 2
                val viewportHeightPx = with(density) { maxHeight.toPx() }
                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            try {
                                customTextToolbar.hide()
                                focusManager.clearFocus()
                            } catch (_: Exception) {

                            }
                            return super.onPreScroll(available, source)
                        }
                    }
                }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = halfHeight, bottom = halfHeight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    items(musicViewModel.currentCaptionList.size, key = { it }) { listIndex ->
                        val tex = musicViewModel.currentCaptionList[listIndex].text
                        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                        var textCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        val words = remember(tex) {
                            val list = ArrayList<WordInfo>(tex.size)
                            var currentOffset = 0
                            for ((index, text) in tex.withIndex()) {
                                val start = currentOffset
                                val end = currentOffset + text.length
                                list.add(WordInfo(text, start, end, index))
                                currentOffset = end
                                if (index < tex.size - 1) {
                                    val nextText = tex[index + 1]
                                    val punctuationRegex = Regex("[\\p{P}\\p{S}]")
                                    val currentIsCjk = isCjkLike(text)
                                    val nextIsPunctuation = punctuationRegex.matches(nextText)
                                    if (!nextIsPunctuation && !currentIsCjk) {
                                        currentOffset += 1
                                    }
                                }
                            }
                            list
                        }
                        val annotatedString = remember(tex, selectedTag, showMenu) {
                            buildAnnotatedString {
                                for ((index, text) in tex.withIndex()) {
                                    val isWordTapped = (selectedTag == text && showMenu)
                                    if (isWordTapped) {
                                        withStyle(
                                            style = SpanStyle(
                                                textDecoration = TextDecoration.Underline
                                            )
                                        ) {
                                            append(text)
                                        }
                                    } else {
                                        append(text)
                                    }
                                    if (index < tex.size - 1) {
                                        val nextText = tex[index + 1]
                                        val punctuationRegex = Regex("[\\p{P}\\p{S}]")
                                        val currentIsCjk = isCjkLike(text)
                                        val nextIsPunctuation = punctuationRegex.matches(nextText)
                                        if (!nextIsPunctuation && !currentIsCjk) {
                                            append(" ")
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .heightIn(min = 40.dp)
                                .background(
                                    if (isDraggingArrow && pointedIndex == listIndex) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    } else if (currentI == listIndex && musicViewModel.autoHighLight.value) {
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(
                                            alpha = 0.3f
                                        )
                                    } else {
                                        MaterialTheme.colorScheme.background
                                    }
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (customTextToolbar.status == TextToolbarStatus.Shown || isSelected || showMenu) {
                                        focusManager.clearFocus()
                                        customTextToolbar.hide()
                                        clearSelection()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (showSlideIndicators) {
                                IconButton(
                                    modifier = Modifier.width(40.dp), onClick = {
                                        musicViewModel.sliderPosition.floatValue =
                                            musicViewModel.currentCaptionList[listIndex].timeStart.toFloat() + 100
                                        musicViewModel.browser?.seekTo(musicViewModel.currentCaptionList[listIndex].timeStart)
                                    }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Adjust,
                                        contentDescription = stringResource(id = R.string.operate_more_will_open_dialog),
                                        tint = if (isDraggingArrow && pointedIndex == listIndex) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                alpha = 0.5f
                                            )
                                        } else if (currentI == listIndex && musicViewModel.autoHighLight.value) {
                                            MaterialTheme.colorScheme.onTertiaryContainer.copy(
                                                alpha = 0.3f
                                            )
                                        } else {
                                            MaterialTheme.colorScheme.onBackground.copy(
                                                alpha = 0.3f
                                            )
                                        },
                                        modifier = Modifier
                                            .zIndex(2.0f),
                                    )
                                }
                            }
                            SelectionContainer(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(
                                        top = 2.dp,
                                        bottom = 2.dp,
                                        start = if (showSlideIndicators) 10.dp else 20.dp,
                                        end = if (showRightIndicator) 44.dp else 20.dp,
                                    )
                            ) {
                                BasicText(
                                    text = annotatedString,
                                    onTextLayout = { textLayoutResult = it },
                                    style = TextStyle(
                                        color = if (isDraggingArrow && pointedIndex == listIndex) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else if (currentI == listIndex && musicViewModel.autoHighLight.value) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onBackground
                                        },
                                        fontSize = fontSize.sp,
                                        textAlign = musicViewModel.textAlign.value,
                                        lineHeight = (fontSize * 1.5).sp,
                                        textIndent = if (musicViewModel.textAlign.value == TextAlign.Justify || musicViewModel.textAlign.value == TextAlign.Start) {
                                            TextIndent(fontSize.sp * 2)
                                        } else {
                                            TextIndent.None
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { textCoordinates = it }
                                        .pointerInput(listIndex, tex) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(pass = PointerEventPass.Main, requireUnconsumed = false)
                                                val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                                    waitForUpOrCancellation(pass = PointerEventPass.Main)
                                                }
                                                if (up != null && !up.isConsumed) {
                                                    val distance = (up.position - down.position).getDistance()
                                                    if (distance <= viewConfiguration.touchSlop) {
                                                        if (customTextToolbar.status == TextToolbarStatus.Shown || isSelected || showMenu) {
                                                            focusManager.clearFocus()
                                                            customTextToolbar.hide()
                                                            clearSelection()
                                                            return@awaitEachGesture
                                                        }
                                                        val layout = textLayoutResult ?: return@awaitEachGesture
                                                        val line = layout.getLineForVerticalPosition(down.position.y)
                                                        if (down.position.x < layout.getLineLeft(line) || down.position.x > layout.getLineRight(line)) {
                                                            return@awaitEachGesture
                                                        }
                                                        val charOffset = layout.getOffsetForPosition(down.position)
                                                        val wordInfo = words.find { charOffset in it.start until it.end }
                                                            ?: words.find { charOffset in it.start..it.end }
                                                            ?: return@awaitEachGesture
                                                        val clickedWord = wordInfo.word.trim()
                                                        if (clickedWord.isEmpty()) return@awaitEachGesture

                                                        val coords = textCoordinates
                                                        if (coords != null && coords.isAttached) {
                                                            val rootPos = coords.localToRoot(down.position)
                                                            popupOffset = IntOffset(
                                                                (rootPos.x - popupOffsetX).toInt().coerceAtLeast(0),
                                                                (rootPos.y - popupOffsetY).toInt().coerceAtLeast(0)
                                                            )
                                                        }
                                                        selectedTag = clickedWord
                                                        word = clickedWord
                                                        showMenu = true
                                                    }
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }
                }

                val handleSizeDp = 40.dp
                val handleSizePx = with(density) { handleSizeDp.toPx() }
                val maxTravel = (viewportHeightPx / 2f) - with(density) { 48.dp.toPx() }
                val totalCaptions = musicViewModel.currentCaptionList.size
                val coroutineScope = rememberCoroutineScope()

                val centerItemIndex by remember {
                    derivedStateOf {
                        val layout = listState.layoutInfo
                        val vh = layout.viewportSize.height
                        if (vh <= 0 || layout.visibleItemsInfo.isEmpty()) {
                            currentI
                        } else {
                            val targetCenter = vh / 2
                            val beforePadding = layout.beforeContentPadding
                            val closest = layout.visibleItemsInfo.minByOrNull {
                                val itemScreenCenter = it.offset + beforePadding + it.size / 2
                                abs(itemScreenCenter - targetCenter)
                            }
                            closest?.index ?: currentI
                        }
                    }
                }

                // Auto-scroll list when arrow is dragged near the top or bottom edge
                LaunchedEffect(isDraggingArrow) {
                    if (!isDraggingArrow) return@LaunchedEffect
                    val edgeThreshold = maxTravel * 0.65f
                    while (isDraggingArrow) {
                        val currentOffset = arrowOffsetY
                        if (currentOffset < -edgeThreshold) {
                            val ratio = ((-currentOffset - edgeThreshold) / (maxTravel - edgeThreshold)).coerceIn(0f, 1f)
                            val scrollPx = -(6f + ratio * 20f)
                            listState.scrollBy(scrollPx)
                            val pointedY = (viewportHeightPx / 2f) + currentOffset
                            val beforePadding = listState.layoutInfo.beforeContentPadding
                            val closest = listState.layoutInfo.visibleItemsInfo.minByOrNull {
                                val itemScreenCenter = it.offset + beforePadding + it.size / 2
                                abs(itemScreenCenter - pointedY)
                            }
                            if (closest != null) {
                                pointedIndex = closest.index
                            }
                        } else if (currentOffset > edgeThreshold) {
                            val ratio = ((currentOffset - edgeThreshold) / (maxTravel - edgeThreshold)).coerceIn(0f, 1f)
                            val scrollPx = 6f + ratio * 20f
                            listState.scrollBy(scrollPx)
                            val pointedY = (viewportHeightPx / 2f) + currentOffset
                            val beforePadding = listState.layoutInfo.beforeContentPadding
                            val closest = listState.layoutInfo.visibleItemsInfo.minByOrNull {
                                val itemScreenCenter = it.offset + beforePadding + it.size / 2
                                abs(itemScreenCenter - pointedY)
                            }
                            if (closest != null) {
                                pointedIndex = closest.index
                            }
                        }
                        delay(16)
                    }
                }

                if (showRightIndicator && totalCaptions > 0) {
                    // Center guide line when dragging arrow icon
                    if (isDraggingArrow) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .align(Alignment.Center)
                                .offset { IntOffset(0, arrowOffsetY.roundToInt()) }
                                .zIndex(1.5f)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        )
                    }

                    // Floating timestamp bubble during drag
                    if (isDraggingArrow && pointedIndex in musicViewModel.currentCaptionList.indices) {
                        val time = musicViewModel.currentCaptionList.getOrNull(pointedIndex)?.timeStart ?: 0L
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset {
                                    IntOffset(
                                        x = with(density) { (-48).dp.roundToPx() },
                                        y = arrowOffsetY.roundToInt()
                                    )
                                }
                                .zIndex(3f)
                        ) {
                            Text(
                                text = Utils.formatTime(time),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Draggable Arrow handle at middle right
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset {
                                IntOffset(
                                    x = with(density) { (-4).dp.roundToPx() },
                                    y = arrowOffsetY.roundToInt()
                                )
                            }
                            .size(handleSizeDp)
                            .shadow(if (isDraggingArrow) 6.dp else 2.dp, CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .zIndex(2.5f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowLeft,
                            contentDescription = "Lyric Position Arrow",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Touch gesture detector at CenterEnd
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(56.dp)
                            .zIndex(3f)
                            .pointerInput(totalCaptions, viewportHeightPx) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    isDraggingArrow = true
                                    pointedIndex = centerItemIndex

                                    var totalMoved = 0f
                                    var lastY = down.position.y

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (change.pressed) {
                                            change.consume()
                                            val deltaY = change.position.y - lastY
                                            totalMoved += abs(deltaY)
                                            lastY = change.position.y

                                            val newOffset = (arrowOffsetY + deltaY).coerceIn(-maxTravel, maxTravel)
                                            arrowOffsetY = newOffset

                                            val pointedY = (viewportHeightPx / 2f) + newOffset
                                            val beforePadding = listState.layoutInfo.beforeContentPadding
                                            val closest = listState.layoutInfo.visibleItemsInfo.minByOrNull {
                                                val itemScreenCenter = it.offset + beforePadding + it.size / 2
                                                abs(itemScreenCenter - pointedY)
                                            }
                                            if (closest != null) {
                                                pointedIndex = closest.index
                                            }
                                        } else {
                                            change.consume()
                                            break
                                        }
                                    }

                                    if (totalMoved <= viewConfiguration.touchSlop) {
                                        // Tap gesture on center arrow
                                        val targetIndex = centerItemIndex
                                        pointedIndex = targetIndex
                                        coroutineScope.launch {
                                            animate(
                                                initialValue = arrowOffsetY,
                                                targetValue = 0f,
                                                animationSpec = tween(200)
                                            ) { value, _ ->
                                                arrowOffsetY = value
                                            }
                                        }
                                        coroutineScope.launch {
                                            listState.centerItem(targetIndex, animate = true)
                                        }
                                        if (targetIndex in musicViewModel.currentCaptionList.indices) {
                                            val caption = musicViewModel.currentCaptionList[targetIndex]
                                            if (caption.timeStart >= 0) {
                                                musicViewModel.sliderPosition.floatValue = caption.timeStart.toFloat() + 100
                                                musicViewModel.browser?.seekTo(caption.timeStart)
                                            }
                                            currentI = targetIndex
                                        }
                                    } else {
                                        // Drag release
                                        val targetIndex = pointedIndex
                                        coroutineScope.launch {
                                            animate(
                                                initialValue = arrowOffsetY,
                                                targetValue = 0f,
                                                animationSpec = tween(300)
                                            ) { value, _ ->
                                                arrowOffsetY = value
                                            }
                                        }
                                        coroutineScope.launch {
                                            if (targetIndex in musicViewModel.currentCaptionList.indices) {
                                                listState.centerItem(targetIndex, animate = true)
                                            }
                                        }
                                        if (targetIndex in musicViewModel.currentCaptionList.indices) {
                                            val caption = musicViewModel.currentCaptionList[targetIndex]
                                            if (caption.timeStart >= 0) {
                                                musicViewModel.sliderPosition.floatValue = caption.timeStart.toFloat() + 100
                                                musicViewModel.browser?.seekTo(caption.timeStart)
                                            }
                                            currentI = targetIndex
                                        }
                                    }
                                    isDraggingArrow = false
                                }
                            }
                    )
                }

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
                                .align(Alignment.TopStart)
                                .padding(start = 10.dp, top = 5.dp)
                                .clickable {
                                    Toast
                                        .makeText(
                                            context,
                                            "This is embedded lyrics",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                        )
                    }
                }

            }

        }
    }


}


