package com.ztftrue.music.ui.public

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.ui.other.FolderItemView
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.model.FolderList
import com.ztftrue.music.utils.model.ItemFilterModel
import com.ztftrue.music.utils.SortUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @Description:  music list
 * all songs and queue list
 */
@UnstableApi
@Composable
fun TracksListView(
    musicViewModel: MusicViewModel,
    playList: AnyListBase,
    tracksList: SnapshotStateList<MusicItem>,
    showIndicator: MutableState<Boolean>,
    selectStatus: Boolean = false,
    selectList: SnapshotStateList<MusicItem>? = null,
    folderData: List<FolderList>? = null,
    header: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val listStateFilter = rememberLazyListState()

    // 读取 showIndicator 的值，避免多处绑定 state
    val showIndicatorValue = showIndicator.value

    // folderData 不会修改 → stable list，避免重组
    val stableFolderData = remember(folderData) { folderData?.toList() }

    // Slide / Top indicator 开关
    val sharedPreferences =
        context.getSharedPreferences("list_indicator_config", Context.MODE_PRIVATE)
    val showSlideIndicator = remember(showIndicatorValue) {
        sharedPreferences.getBoolean("show_slide_indicator", true) && showIndicatorValue
    }
    val showTopIndicator = remember(showIndicatorValue) {
        sharedPreferences.getBoolean("show_top_indicator", true) && showIndicatorValue
    }

    // ---- 生成 ItemFilterModel ----
    val (itemFilterList, itemFilterMap) = remember(tracksList.size, showIndicatorValue) {
        if (showIndicatorValue) generateItemFilters(tracksList) else emptyList<ItemFilterModel>() to hashMapOf()
    }

    // ---- 滑动指示器 Dialog ----
    var showItemFilterDialog by remember { mutableStateOf(false) }
    if (showItemFilterDialog) {
        ItemFilterDialog(itemFilterList, onDismiss = {
            showItemFilterDialog = false
            if (it >= 0) {
                scope.launch {
                    listState.scrollToItem(
                        it + (stableFolderData?.size ?: 0) + if (header != null) 1 else 0
                    )
                }
            }
        })
    }

    // ---- 跟踪当前可见区域对应的大写字母并同步右侧指示器 ----
    var activeSection by remember { mutableStateOf("") }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                val offset = if (header != null) 1 else 0
                val folderSize = stableFolderData?.size ?: 0
                val trackIndex = index - folderSize - offset
                if (trackIndex >= 0 && tracksList.size > trackIndex) {
                    val section = SortUtils.getSectionLabel(tracksList[trackIndex].name)
                    activeSection = section
                    val itemFilterModel = itemFilterMap[section]
                    itemFilterModel?.selfIndex?.let { selfIndex ->
                        if (itemFilterList.size > 28) {
                            listStateFilter.scrollToItem(selfIndex)
                        }
                    }
                }
            }
    }
    var itemHeight by remember { mutableStateOf(0) }

    // ---- TracksListView UI ----
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (listRef, button) = createRefs()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .constrainAs(listRef) { top.linkTo(parent.top) }
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    end = if (showSlideIndicator && itemFilterList.isNotEmpty()) 36.dp else 0.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                if (header != null) {
                    item { header() }
                }

                // FolderData
                stableFolderData?.let { folders ->
                    items(
                        folders.size,
                        key = { "${folders[it].id}${folders[it].children.size}${folders[it].trackNumber}${it}" }
                    ) { index ->
                        FolderItemView(
                            folders[index],
                            musicViewModel,
                            modifier = Modifier
                                .wrapContentHeight()
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    if (itemHeight == 0) {
                                        itemHeight = coordinates.size.height
                                    }
                                },
                            musicViewModel.navController
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 74.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            thickness = 0.8.dp
                        )
                    }
                }

                // TracksList
                itemsIndexed(
                    tracksList,
                    key = { index, item -> "${item.id}_$index" }
                ) { index, music ->
                    // Top indicator (Modern section header)
                    if (showTopIndicator) {
                        val currentSection = SortUtils.getSectionLabel(music.name)
                        val prevSection = if (index > 0) SortUtils.getSectionLabel(tracksList[index - 1].name) else null
                        if (prevSection != currentSection) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clickable { showItemFilterDialog = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                    tonalElevation = 1.dp
                                ) {
                                    Text(
                                        text = currentSection,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp, end = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }

                    MusicItemView(
                        music,
                        index,
                        musicViewModel,
                        playList,
                        modifier = Modifier.fillMaxWidth(),
                        tracksList,
                        selectStatus,
                        selectList,
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 74.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        thickness = 0.8.dp
                    )
                }
            }

            // Right-side Alphabet Scroller with Floating Preview Bubble
            if (showSlideIndicator && itemFilterList.isNotEmpty()) {
                AlphabetScroller(
                    itemFilterList = itemFilterList,
                    activeSection = activeSection,
                    listStateFilter = listStateFilter,
                    onSectionSelected = { targetModel ->
                        activeSection = targetModel.name
                        scope.launch {
                            val position = targetModel.index + (stableFolderData?.size ?: 0) + if (header != null) 1 else 0
                            listState.scrollToItem(position)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 8.dp, bottom = 8.dp, end = 2.dp)
                )
            }
        }

        // Floating Action Button: scroll to current playing
        if (!selectStatus) {
            SmallFloatingActionButton(
                onClick = {
                    scope.launch {
                        tracksList.forEachIndexed { index, item ->
                            if (item.id == musicViewModel.currentPlay.value?.id) {
                                val position = index + (stableFolderData?.size
                                    ?: 0) + if (header != null) 1 else 0
                                listState.animateScrollToItem(position.coerceAtLeast(0))
                                return@forEachIndexed
                            }
                        }
                    }
                },
                modifier = Modifier
                    .padding(0.dp)
                    .size(46.dp)
                    .zIndex(10f)
                    .constrainAs(button) {
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                    }
                    .offset(x = (-38).dp, y = (-36).dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "find current playing music",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Modern fast-scrub alphabetical index scroller.
 * Automatically adapts:
 * - When items <= 28: renders a sleek drag-scrubber with continuous drag gestures and floating preview magnifier.
 * - When items > 28: renders a scrollable LazyColumn with touch fling/drag and list synchronization, ensuring smooth scrolling for large libraries without crowding.
 */
@Composable
fun AlphabetScroller(
    itemFilterList: List<ItemFilterModel>,
    activeSection: String,
    listStateFilter: LazyListState,
    onSectionSelected: (ItemFilterModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    var currentLetter by remember { mutableStateOf(activeSection) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var totalHeightPx by remember { mutableFloatStateOf(0f) }
    var showTapBubble by remember { mutableStateOf(false) }

    LaunchedEffect(activeSection) {
        if (!isDragging) {
            currentLetter = activeSection
        }
    }

    LaunchedEffect(showTapBubble) {
        if (showTapBubble) {
            delay(700)
            showTapBubble = false
        }
    }

    val bubbleVisible = (isDragging || showTapBubble) && currentLetter.isNotEmpty()

    Box(
        modifier = modifier
            .wrapContentWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Floating preview bubble (magnifier badge)
        AnimatedVisibility(
            visible = bubbleVisible,
            enter = fadeIn(animationSpec = tween(120)) + scaleIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(180)) + scaleOut(animationSpec = tween(180)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 42.dp)
                .offset {
                    val targetY = if (totalHeightPx > 0 && isDragging) {
                        (touchY - totalHeightPx / 2f).coerceIn(
                            -totalHeightPx / 2f + 40.dp.roundToPx(),
                            totalHeightPx / 2f - 40.dp.roundToPx()
                        )
                    } else 0f
                    IntOffset(x = 0, y = targetY.toInt())
                }
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
                tonalElevation = 6.dp,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = currentLetter,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (itemFilterList.size <= 28) {
            // Compact drag-scrubber for <= 28 sections (e.g. A-Z, #)
            Column(
                modifier = Modifier
                    .width(32.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isDragging) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                    .onGloballyPositioned { coordinates ->
                        totalHeightPx = coordinates.size.height.toFloat()
                    }
                    .pointerInput(itemFilterList) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isDragging = true

                            fun updateTouch(y: Float) {
                                touchY = y
                                if (totalHeightPx > 0 && itemFilterList.isNotEmpty()) {
                                    val fraction = (y / totalHeightPx).coerceIn(0f, 1f)
                                    val index = (fraction * itemFilterList.size).toInt()
                                        .coerceIn(0, itemFilterList.size - 1)
                                    val model = itemFilterList[index]
                                    if (model.name != currentLetter) {
                                        currentLetter = model.name
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSectionSelected(model)
                                    }
                                }
                            }

                            updateTouch(down.position.y)

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.pressed) {
                                    change.consume()
                                    updateTouch(change.position.y)
                                } else {
                                    break
                                }
                            }
                            isDragging = false
                        }
                    },
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemFilterList.forEach { item ->
                    val isSelected = (isDragging && item.name == currentLetter) || (!isDragging && item.name == activeSection)
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = if (itemFilterList.size > 22) 10.sp else 11.sp,
                            lineHeight = if (itemFilterList.size > 22) 12.sp else 13.sp
                        ),
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // Scrollable LazyColumn indicator for large datasets (> 28 sections)
            LazyColumn(
                state = listStateFilter,
                modifier = Modifier
                    .width(32.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .onGloballyPositioned { coordinates ->
                        totalHeightPx = coordinates.size.height.toFloat()
                    }
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(itemFilterList.size, key = { itemFilterList[it].name + "_" + it }) { index ->
                    val item = itemFilterList[index]
                    val isSelected = item.name == activeSection
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable {
                                currentLetter = item.name
                                showTapBubble = true
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSectionSelected(item)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 13.sp
                            ),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemFilterDialog(
    itemFilterList: List<ItemFilterModel>,
    onDismiss: (index: Int) -> Unit
) {
    fun onConfirmation(index: Int) {
        onDismiss(index)
    }
    Dialog(
        onDismissRequest = { onDismiss(-1) },
        properties = DialogProperties(
            usePlatformDefaultWidth = true, dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.where_are_we_going),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                val windowInfo = LocalWindowInfo.current
                val density = LocalDensity.current
                val containerHeightDp = with(density) { windowInfo.containerSize.height.toDp() }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(10.dp),
                    modifier = Modifier
                        .height(containerHeightDp / 2f)
                ) {
                    items(itemFilterList.size) { item ->
                        val iFilter = itemFilterList[item]
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ElevatedButton(
                                onClick = { onConfirmation(iFilter.index) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = iFilter.name,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismiss(-1) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(
                            stringResource(id = R.string.cancel),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    )
}

fun generateItemFilters(
    tracks: List<MusicItem>
): Pair<List<ItemFilterModel>, HashMap<String, ItemFilterModel>> {

    val filterList = ArrayList<ItemFilterModel>()
    val filterMap = HashMap<String, ItemFilterModel>()

    if (tracks.isEmpty()) return filterList to filterMap

    var lastSection: String? = null

    tracks.forEachIndexed { index, item ->
        val section = SortUtils.getSectionLabel(item.name)
        if (section != lastSection) {
            val model = ItemFilterModel(
                name = section,
                index = index,
                selfIndex = filterList.size
            )
            filterList.add(model)
            if (!filterMap.containsKey(section)) {
                filterMap[section] = model
            }
            lastSection = section
        }
    }

    return filterList to filterMap
}