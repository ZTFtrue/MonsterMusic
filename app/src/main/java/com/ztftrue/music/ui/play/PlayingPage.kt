package com.ztftrue.music.ui.play

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.play.ACTION_AddPlayQueue
import com.ztftrue.music.play.ACTION_PlayLIST_CHANGE
import com.ztftrue.music.play.ACTION_RemoveFromQueue
import com.ztftrue.music.play.ACTION_SEEK_TO
import com.ztftrue.music.play.ACTION_TRACKS_DELETE
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.ui.public.AddMusicToPlayListDialog
import com.ztftrue.music.ui.public.CreatePlayListDialog
import com.ztftrue.music.ui.public.DeleteTip
import com.ztftrue.music.ui.public.OperateDialog
import com.ztftrue.music.ui.public.TopBar
import com.ztftrue.music.utils.OperateType
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.PlaylistManager
import com.ztftrue.music.utils.TracksManager
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.enumToStringForPlayListType
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

const val CoverID = 0
const val LyricsID = 1
const val EqualizerID = 2

data class PlayingViewTab(
    val name: String,
    val id: Int,
    var priority: Int,
)


@OptIn(
    ExperimentalFoundationApi::class
)
@UnstableApi
@Composable
fun PlayingPage(
    navController: NavHostController,
    viewModel: MusicViewModel,
) {
    val context = LocalContext.current
    val playViewTab: Array<PlayingViewTab> = arrayOf(
        PlayingViewTab("Cover", CoverID, 1), PlayingViewTab("Lyrics", LyricsID, 1),
        PlayingViewTab("Equalizer", EqualizerID, 1)
    )
    val pagerTabState = rememberPagerState { playViewTab.size }
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var showAddPlayListDialog by remember { mutableStateOf(false) }
    var showCreatePlayListDialog by remember { mutableStateOf(false) }
    val repeatModel = remember { mutableIntStateOf(viewModel.repeatModel.intValue) }
    var music: MusicItem? = viewModel.currentPlay.value
    var showDeleteTip by remember { mutableStateOf(false) }
    LaunchedEffect(music) {
        if (music == null) {
            navController.popBackStack()
        }
    }

    if (showDeleteTip && music != null) {

        DeleteTip(viewModel, music.name, onDismiss = {
            showDeleteTip = false
            if (it) {
                if (TracksManager.removeMusicById(context, music!!.id)) {
                    val bundle = Bundle()
                    bundle.putLong("id", music!!.id)
                    viewModel.mediaBrowser?.sendCustomAction(
                        ACTION_TRACKS_DELETE,
                        bundle,
                        object : MediaBrowserCompat.CustomActionCallback() {
                            override fun onResult(
                                action: String?,
                                extras: Bundle?,
                                resultData: Bundle?
                            ) {
                                super.onResult(action, extras, resultData)
                                viewModel.refreshList.value = !viewModel.refreshList.value
                                navController.popBackStack()
                            }
                        }
                    )
                }

            }
        })
    }
    if (showDialog) {
        music = viewModel.currentPlay.value
        if (music != null) {
            OperateDialog(
                viewModel,
                music = music,
                null,
                onDismiss = {
                    showDialog = false
                    when (it) {
                        OperateType.AddToQueue -> {
                            viewModel.musicQueue.add(music)
                            val bundle = Bundle()
                            bundle.putParcelable("musicItem", music)
                            viewModel.mediaBrowser?.sendCustomAction(
                                ACTION_AddPlayQueue,
                                bundle,
                                null
                            )
                        }

                        OperateType.PlayNext -> {
                            viewModel.musicQueue.add(
                                viewModel.currentPlayQueueIndex.intValue + 1,
                                music
                            )
                            val bundle = Bundle()
                            bundle.putParcelable("musicItem", music)
                            bundle.putInt("index", viewModel.currentPlayQueueIndex.intValue + 1)
                            viewModel.mediaBrowser?.sendCustomAction(
                                ACTION_AddPlayQueue,
                                bundle,
                                null
                            )
                        }

                        OperateType.AddToPlaylist -> {
                            showAddPlayListDialog = true
                        }

                        OperateType.RemoveFromPlaylist -> {

                        }

                        OperateType.AddToFavorite -> {
                            // create playlist name is  MY favorite
                        }

                        OperateType.Artist -> {
                            viewModel.navController?.navigate(
                                Router.PlayListView.withArgs(
                                    "${music.artistId}",
                                    enumToStringForPlayListType(PlayListType.Artists)
                                ),
                            ) {
                                popUpTo(Router.MainView.route) {
                                    // Inclusive means the start destination is also popped
                                    inclusive = false
                                }
                            }
                        }

                        OperateType.Album -> {
                            viewModel.navController?.navigate(
                                Router.PlayListView.withArgs(
                                    "${music.albumId}",
                                    enumToStringForPlayListType(PlayListType.Albums)
                                )
                            ) {
                                popUpTo(Router.MainView.route) {
                                    // Inclusive means the start destination is also popped
                                    inclusive = false
                                }
                            }
                        }

                        OperateType.RemoveFromQueue -> {
                            val index = viewModel.musicQueue.indexOfFirst { it.id == music.id }
                            if (index == -1) return@OperateDialog
                            val bundle = Bundle()
                            bundle.putInt("index", index)
                            viewModel.mediaBrowser?.sendCustomAction(
                                ACTION_RemoveFromQueue,
                                bundle,
                                object : MediaBrowserCompat.CustomActionCallback() {
                                    override fun onResult(
                                        action: String?,
                                        extras: Bundle?,
                                        resultData: Bundle?
                                    ) {
                                        super.onResult(action, extras, resultData)
                                        if (ACTION_RemoveFromQueue == action && resultData == null) {
                                            viewModel.currentPlay.value = null
                                        }
                                    }
                                }
                            )
                            viewModel.musicQueue.removeAt(index)
                        }

                        OperateType.EditMusicInfo -> {
                            viewModel.navController?.navigate(Router.EditTrackPage.withArgs("${music.id}"))
                        }

                        OperateType.DeleteFromStorage -> {
                            showDeleteTip = true
                        }

                        OperateType.No -> {

                        }

                        else -> {

                        }
                    }
                },
            )
        }

    }
    if (showAddPlayListDialog) {
        if (music != null) {
            AddMusicToPlayListDialog(viewModel, music, onDismiss = {
                showAddPlayListDialog = false
                if (it != null) {
                    if (it == -1L) {
                        showCreatePlayListDialog = true
                    } else {
                        PlaylistManager.addMusicToPlaylist(context, it, music.id)
                        viewModel.mediaBrowser?.sendCustomAction(
                            ACTION_PlayLIST_CHANGE, null, null
                        )
                    }
                }
            })
        }
    }
    if (showCreatePlayListDialog) {
        CreatePlayListDialog(viewModel, onDismiss = {
            showCreatePlayListDialog = false
            if (!it.isNullOrEmpty()) {
                if (music != null) {
                    Utils.createPlayListAddTrack(it, context, music, viewModel)
                }
            }
        })
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Scaffold(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
            topBar = {
                Column(Modifier.fillMaxWidth()) {
                    key(Unit, pagerTabState.currentPage) {
                        TopBar(navController, viewModel, content = {
                            /**
                             * tabPositions[pagerTabState.currentPage]
                             * playViewTab
                             */
                            if (playViewTab[pagerTabState.currentPage].id == LyricsID) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .combinedClickable(onLongClick = {
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Switch auto scroll",
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        }) {
                                            viewModel.autoScroll.value = !viewModel.autoScroll.value
                                            viewModel.autoHighLight.value = !viewModel.autoHighLight.value
                                        }
                                        .padding(0.dp)
                                        .height(50.dp)
                                ) {
                                    Text(
                                        text = "Scroll",
                                        modifier = Modifier.padding(0.dp),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = TextUnit(12f, TextUnitType.Sp),
                                        lineHeight = TextUnit(12f, TextUnitType.Sp),
                                    )
                                    Switch(checked = viewModel.autoScroll.value,
                                        modifier = Modifier
                                            .scale(0.5f)
                                            .padding(0.dp),
                                        onCheckedChange = {
                                            viewModel.autoScroll.value = it
                                            viewModel.autoHighLight.value = it
                                        }
                                    )
                                }
                            }


                            IconButton(
                                modifier = Modifier.width(50.dp), onClick = {
                                    showDialog = true
                                }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More operate",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape),
                                )
                            }
                        })
                    }
                    key(viewModel.currentPlay.value) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp)
                        ) {
                            viewModel.currentPlay.value?.let { it1 ->
                                Text(
                                    text = it1.name,
                                    modifier = Modifier
                                        .padding(0.dp)
                                        .horizontalScroll(rememberScrollState(0))
                                        .fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = MaterialTheme.typography.titleSmall.fontSize
                                )
                            }
                        }
                    }

                    key(Unit) {
                        ScrollableTabRow(
                            selectedTabIndex = pagerTabState.currentPage,
                            modifier = Modifier.fillMaxWidth(),
                            indicator = { tabPositions ->
                                if (tabPositions.isNotEmpty()) {
                                    TabRowDefaults.Indicator(
                                        Modifier
                                            .height(3.0.dp)
                                            .tabIndicatorOffset(tabPositions[pagerTabState.currentPage]),
                                        height = 3.0.dp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                } else {
                                    TabRowDefaults.Indicator(
                                        Modifier.height(3.0.dp),
                                        height = 3.0.dp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            },
                        ) {
                            playViewTab.forEachIndexed { index, item ->
                                Tab(selected = pagerTabState.currentPage == index, onClick = {
                                    coroutineScope.launch {
                                        pagerTabState.animateScrollToPage(index)
                                    }
                                }, text = {
                                    Text(
                                        text = item.name,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 14.sp,
                                    )
                                })
                            }
                        }
                    }
                }
            },
            content =
            {
                Row {

                    HorizontalPager(
                        state = pagerTabState,
                        Modifier
                            .fillMaxHeight()
                            .padding(it)
                            .pointerInput(Unit) {

                            },
                        userScrollEnabled = false
                    ) { id ->
                        when (playViewTab[id].id) {
                            CoverID -> {
                                CoverView(viewModel)
                            }

                            LyricsID -> {
                                LyricsView(viewModel)
                            }

                            EqualizerID -> {
                                EqualizerView(viewModel)
                            }
                        }

                    }
                }

            },
            bottomBar =
            {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    if (viewModel.currentDuration.longValue > 0) {
                        Slider(
                            modifier = Modifier
                                .semantics { contentDescription = "Slider" }
                                .motionEventSpy {
                                    when (it.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            viewModel.sliderTouching = true
                                        }

                                        MotionEvent.ACTION_MOVE -> {
                                        }

                                        MotionEvent.ACTION_UP -> {
                                        }
                                    }
                                },
                            value = viewModel.sliderPosition.floatValue,
                            onValueChange = {
                                viewModel.sliderPosition.floatValue =
                                    it.roundToLong().toFloat()
                            },
                            valueRange = 0f..viewModel.currentDuration.longValue.toFloat(),
                            steps = 100,
                            onValueChangeFinished = {
                                val bundle = Bundle()
                                bundle.putLong(
                                    "position",
                                    viewModel.sliderPosition.floatValue.toLong()
                                )
                                viewModel.mediaBrowser?.sendCustomAction(
                                    ACTION_SEEK_TO,
                                    bundle,
                                    null
                                )
                                viewModel.sliderTouching = false
                            },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Utils.formatTime(viewModel.sliderPosition.floatValue.toLong()),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = Utils.formatTime(viewModel.currentDuration.longValue),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        if (viewModel.currentPlay.value != null) {
                            Text(
                                text = "Get duration failed",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        key(Unit) {
                            Image(
                                painter = painterResource(
                                    R.drawable.ic_queue
                                ),
                                contentDescription = "Queue Page",
                                modifier = Modifier
                                    .clickable {
                                        navController.navigate(
                                            Router.QueuePage.route
                                        ) {
                                            popUpTo(Router.MainView.route) {
                                                // Inclusive means the start destination is also popped
                                                inclusive = false
                                            }
                                        }
                                    }
                                    .width(50.dp)
                                    .height(50.dp)
                                    .padding(10.dp),
                                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.skip_previous),
                                contentDescription = "skip previous",
                                modifier = Modifier
                                    .clickable {
                                        viewModel.mediaController?.transportControls?.skipToPrevious()
                                    }
                                    .width(50.dp)
                                    .height(50.dp)
                                    .padding(10.dp),
                                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                            )
                            Image(
                                painter = painterResource(
                                    if (viewModel.playStatus.value) {
                                        R.drawable.pause
                                    } else {
                                        R.drawable.play
                                    }
                                ),
                                contentDescription = "Pause",
                                modifier = Modifier
                                    .clickable {
                                        val pbState =
                                            viewModel.mediaController?.playbackState?.state
                                        if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                                            viewModel.mediaController?.transportControls?.pause()
                                        } else {
                                            viewModel.mediaController?.transportControls?.play()
                                        }
                                    }
                                    .width(60.dp)
                                    .height(60.dp)
                                    .padding(5.dp),
                                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                            )
                            Image(
                                painter = painterResource(R.drawable.skip_next),
                                contentDescription = "skip next",
                                modifier = Modifier
                                    .clickable {
                                        viewModel.mediaController?.transportControls?.skipToNext()
                                    }
                                    .width(50.dp)
                                    .height(50.dp)
                                    .padding(10.dp),
                                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                            )
                        }
                        key(repeatModel.intValue) {
                            Image(
                                painter = painterResource(
                                    when (repeatModel.intValue) {
                                        Player.REPEAT_MODE_ALL -> {
                                            R.drawable.ic_repeat_all
                                        }

                                        Player.REPEAT_MODE_ONE -> {
                                            R.drawable.ic_repeat_one
                                        }

                                        else -> {
                                            R.drawable.ic_repeat_off
                                        }
                                    }
                                ),
                                contentDescription = "Repeat model",
                                modifier = Modifier
                                    .clickable {
                                        when (repeatModel.intValue) {
                                            Player.REPEAT_MODE_ALL -> {
                                                repeatModel.intValue = Player.REPEAT_MODE_ONE
                                                viewModel.mediaController?.transportControls?.setRepeatMode(
                                                    PlaybackStateCompat.REPEAT_MODE_ONE
                                                )
                                            }

                                            Player.REPEAT_MODE_ONE -> {
                                                repeatModel.intValue = Player.REPEAT_MODE_OFF
                                                viewModel.mediaController?.transportControls?.setRepeatMode(
                                                    PlaybackStateCompat.REPEAT_MODE_NONE
                                                )
                                            }

                                            else -> {
                                                repeatModel.intValue = Player.REPEAT_MODE_ALL
                                                viewModel.mediaController?.transportControls?.setRepeatMode(
                                                    PlaybackStateCompat.REPEAT_MODE_ALL
                                                )
                                            }
                                        }
                                    }
                                    .width(50.dp)
                                    .height(50.dp)
                                    .padding(10.dp),
                                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                            )
                        }
                    }
                }
            }
        )
    }

}

