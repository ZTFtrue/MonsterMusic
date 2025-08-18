package com.ztftrue.music.ui.public

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ztftrue.music.utils.model.AnyListBase
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
    header: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var showSlideIndicator by remember { mutableStateOf(false) }
    var showTopIndicator by remember { mutableStateOf(false) }
    val sharedPreferences =
        context.getSharedPreferences("list_indicator_config", Context.MODE_PRIVATE)
    val itemFilterList = remember { mutableStateListOf<ItemFilterModel>() }
    var showItemFilterDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
//    LaunchedEffect(key1 = tracksList, showIndicator.value) {
    showSlideIndicator =
        sharedPreferences.getBoolean("show_slide_indicator", true) && showIndicator.value
    showTopIndicator =
        sharedPreferences.getBoolean("show_top_indicator", true) && showIndicator.value
    itemFilterList.clear()
    if (showIndicator.value) {
        tracksList.forEachIndexed { index, musicItem ->
            val currentF = musicItem.name[0]
            if (index > 0) {
                val lastM = tracksList[index - 1]
                val lastMF = lastM.name[0]
                if (lastMF != currentF) {
                    itemFilterList.add(ItemFilterModel(currentF.toString(), index))
                }
            } else {
                itemFilterList.add(ItemFilterModel(currentF.toString(), index))
            }
        }
    }
//    }
    if (showItemFilterDialog) {
        ItemFilterDialog(itemFilterList, onDismiss = {
            showItemFilterDialog = false
            if (it >= 0) {
                scope.launch {
                    listState.scrollToItem(it)
                }
            }

        })
    }
    key(tracksList, itemFilterList, showSlideIndicator, showTopIndicator) {
        if (tracksList.isEmpty() && header == null) {
            if (musicViewModel.loadingTracks.value) {
                Text(
                    text = "Loading...",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.no_music),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState(0))
                        .semantics {
                            contentDescription = "No music"
                        }
                )
            }

        } else {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp),
            ) {
                val (list, button) = createRefs()
                Row(
                    modifier = Modifier
                        .constrainAs(list) {
                            top.linkTo(parent.top)
                        }
                        .fillMaxSize()) {
                    key(itemFilterList, showSlideIndicator) {
                        if (showSlideIndicator) {
                            LazyColumn(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .fillMaxHeight()
                                    .padding(top = 10.dp, bottom = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                items(itemFilterList.size) { index ->
                                    val item = itemFilterList[index]
                                    Text(
                                        text = item.name,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier
                                            .clickable {
                                                scope.launch {
                                                    listState.scrollToItem(item.index)
                                                }
                                            }
                                            .padding(start = 8.dp, end = 5.dp)
                                            .semantics {
                                                contentDescription = item.name
                                            }
                                    )
                                }
                            }
                        }
                    }
                    key(tracksList, showTopIndicator) {
                        LazyColumn(
                            state = listState, modifier = Modifier
                                .fillMaxSize()
                        ) {
                            item {
                                if (header != null) {
                                    header()
                                }
                            }
                            items(tracksList.size) { index ->
                                val music = tracksList[index]
                                if (showTopIndicator) {
                                    val currentF = music.name[0]
                                    if (index > 0) {
                                        val lastM = tracksList[index - 1]
                                        val lastMF = lastM.name[0]
                                        if (lastMF != currentF) {
                                            Row(
                                                modifier = Modifier
                                                    .background(
                                                        MaterialTheme.colorScheme.onBackground.copy(
                                                            alpha = 0.1f
                                                        )
                                                    )
                                                    .fillMaxWidth()
                                                    .combinedClickable(onClick = {
                                                        showItemFilterDialog = true
                                                    })
                                            ) {
                                                Text(
                                                    text = currentF.toString(),
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    modifier = Modifier
                                                        .padding(start = 10.dp)

                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.onBackground.copy(
                                                        alpha = 0.1f
                                                    )
                                                )
                                                .fillMaxWidth()
                                                .combinedClickable(onClick = {
                                                    showItemFilterDialog = true
                                                })
                                        ) {
                                            Text(
                                                text = currentF.toString(),
                                                color = MaterialTheme.colorScheme.onBackground,
                                                modifier = Modifier
                                                    .padding(start = 10.dp)
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
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    thickness = 1.2.dp
                                )
                            }
                        }
                    }
                }

                if (!selectStatus) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                for ((index, entry) in tracksList.withIndex()) {
                                    if (entry.id == musicViewModel.currentPlay.value?.id) {
                                        // TODO calculate the scroll position by　
                                        listState.animateScrollToItem(if ((index - 2) < 0) 0 else (index - 2))
                                        break
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
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "find current playing music",
                            modifier = Modifier
                                .size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

    }

}

@Composable
fun ItemFilterDialog(
    itemFilterList: SnapshotStateList<ItemFilterModel>,
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
