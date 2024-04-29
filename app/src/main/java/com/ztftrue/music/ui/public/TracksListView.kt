package com.ztftrue.music.ui.public

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.model.AnyListBase
import kotlinx.coroutines.launch

/**
 * @Description:  music list
 * all songs and queue list
 */

@UnstableApi
@Composable
fun TracksListView(
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel,
    playList: AnyListBase,
    tracksList: SnapshotStateList<MusicItem>,
    selectStatus: Boolean = false,
    selectList: SnapshotStateList<MusicItem>? = null,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    key(tracksList) {
        if (tracksList.size == 0) {
            Text(
                text = stringResource(R.string.no_music),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState(0))
                    .semantics {
                        contentDescription = "No music"
                    }
            )
        }else{
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp),
            ) {
                val (list, button) = createRefs()
                LazyColumn(
                    state = listState, modifier = Modifier
                        .constrainAs(list) {
                            top.linkTo(parent.top)
                        }
                        .fillMaxSize()
                ) {
                    items(tracksList.size) { index ->
                        val music = tracksList[index]
                        MusicItemView(
                            music,
                            index,
                            musicViewModel,
                            playList,
                            modifier = Modifier
                                .padding(10.dp)
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
                if (!selectStatus) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                for ((index, entry) in tracksList.withIndex()) {
                                    if (entry.id == musicViewModel.currentPlay.value?.id) {
                                        // TODO calculate the scroll position by　
                                        listState.animateScrollToItem(if ((index - 4) < 0) 0 else (index - 4))
                                        break
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(10.dp)
                            .size(56.dp)
                            .zIndex(10f)
                            .constrainAs(button) {
                                bottom.linkTo(parent.bottom)
                                end.linkTo(parent.end)
                            }
                            .offset(x = (-1).dp, y = (-40).dp),
                        shape = CircleShape,
                    ) {
                        Image(
                            painter = painterResource(
                                R.drawable.icon_location
                            ),
                            contentDescription = "find current playing music",
                            modifier = Modifier
                                .size(30.dp),
                            colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onPrimaryContainer)
                        )
                    }
                }
            }
        }

    }

}