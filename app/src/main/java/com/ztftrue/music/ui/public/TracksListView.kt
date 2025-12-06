package com.ztftrue.music.ui.public

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    // ---- 滚动同步到右侧字母索引 ----
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                val offset = if (header != null) 1 else 0
                val folderSize = stableFolderData?.size ?: 0
                val trackIndex = index - folderSize - offset
                if (trackIndex >= 0 && tracksList.size > trackIndex) {
                    val itemFilterModel = itemFilterMap[tracksList[trackIndex].name[0].toString()]
                    itemFilterModel?.selfIndex?.let { listStateFilter.scrollToItem(it) }
                }
            }
    }
    var itemHeight by remember { mutableStateOf(0) }
    // ---- TracksListView UI ----
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (list, button) = createRefs()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .constrainAs(list) { top.linkTo(parent.top) }
        ) {
            // Slide indicator
            if (showSlideIndicator) {
                LazyColumn(
                    state = listStateFilter,
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .wrapContentWidth()
                        .fillMaxHeight()
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    if (stableFolderData != null && itemHeight > 0) {
                        item {
                            val totalHeight = itemHeight * stableFolderData.size
                            Spacer(
                                modifier = Modifier.size(
                                    height = with(LocalDensity.current) { totalHeight.toDp() },
                                    width = 1.dp
                                )
                            )
                        }
                    }
                    items(itemFilterList.size) { index ->
                        val item = itemFilterList[index]
                        Text(
                            text = item.name,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .clickable {
                                    scope.launch {
                                        listState.scrollToItem(
                                            item.index + (stableFolderData?.size
                                                ?: 0) + if (header != null) 1 else 0
                                        )
                                    }
                                }
                                .padding(start = 8.dp, end = 5.dp)
                                .semantics { contentDescription = item.name }
                        )
                    }
                }
            }

            // Tracks + folder LazyColumn
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                if (header != null) {
                    item { header() }
                }

                // FolderData
                stableFolderData?.let { folders ->
                    items(folders.size, key = { folders[it].id + it }) { index ->
                        FolderItemView(
                            folders[index], musicViewModel, modifier = Modifier
                                .wrapContentHeight()
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    if (itemHeight == 0) {
                                        itemHeight = coordinates.size.height
                                    }
                                    println("Composable Height in pixels: ")
                                },
                            musicViewModel.navController
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            thickness = 1.2.dp
                        )
                    }
                }

                // TracksList
                itemsIndexed(tracksList, key = { index, item -> item.id + index }) { index, music ->

                    // Top indicator
                    if (showTopIndicator) {
                        val currentF = music.name[0]
                        val prevF = if (index > 0) tracksList[index - 1].name[0] else null
                        if (prevF != currentF) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                                    .combinedClickable(onClick = { showItemFilterDialog = true })
                            ) {
                                Text(
                                    text = currentF.toString(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }
                        }
                    }
                    MusicItemView(
                        music,
                        index,
                        musicViewModel,
                        playList,
                        modifier = Modifier
                            .fillMaxWidth(),
                        tracksList,
                        selectStatus,
                        selectList,
                    )
                    // Music item
//                    MusicItemView(
//                        music = music,
//                        index = index,
//                        onUpdate = { newItem -> tracksList[index] = newItem },
//                        onDelete = { tracksList.removeAt(index) },
//                        onMove = { from, to ->
//                            tracksList.add(to, tracksList.removeAt(from))
//                        },
//                        selectStatus = selectStatus,
//                        isSelected = selectList?.contains(music) == true
//                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        thickness = 1.2.dp
                    )
                }
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
                    .size(50.dp)
                    .zIndex(10f)
                    .constrainAs(button) {
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                    }
                    .offset(x = (-10).dp, y = (-40).dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "find current playing music",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                    .background(color = MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.where_are_we_going), modifier = Modifier
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                )
                val windowInfo = LocalWindowInfo.current
//                val containerHeightPx = windowInfo.containerSize.height
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
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ElevatedButton(
                                onClick = { onConfirmation(iFilter.index) },
                                modifier = Modifier
                            ) {
                                Text(
                                    text = iFilter.name,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismiss(-1) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(
                            stringResource(id = R.string.cancel),
                            color = MaterialTheme.colorScheme.onBackground
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

    var lastChar: Char? = null

    tracks.forEachIndexed { index, item ->
        val firstChar = item.name.firstOrNull() ?: return@forEachIndexed
        if (firstChar != lastChar) {
            // 生成一个新的 Filter Model
            val model = ItemFilterModel(
                name = firstChar.toString(),
                index = index,
                selfIndex = filterList.size
            )
            filterList.add(model)
            filterMap[firstChar.toString()] = model
            lastChar = firstChar
        }
    }

    return filterList to filterMap
}