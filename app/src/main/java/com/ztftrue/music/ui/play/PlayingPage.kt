package com.ztftrue.music.ui.play

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatAlignJustify
import androidx.compose.material.icons.outlined.FormatShapes
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SwipeVertical
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.Router
import com.ztftrue.music.play.ACTION_AddPlayQueue
import com.ztftrue.music.play.ACTION_RemoveFromQueue
import com.ztftrue.music.play.ACTION_SEEK_TO
import com.ztftrue.music.play.ACTION_SWITCH_SHUFFLE
import com.ztftrue.music.play.ACTION_TRACKS_DELETE
import com.ztftrue.music.play.ACTION_VISUALIZATION_ENABLE
import com.ztftrue.music.sqlData.model.DictionaryApp
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.ui.public.AddMusicToPlayListDialog
import com.ztftrue.music.ui.public.CreatePlayListDialog
import com.ztftrue.music.ui.public.DeleteTip
import com.ztftrue.music.ui.public.OperateDialog
import com.ztftrue.music.ui.public.TopBar
import com.ztftrue.music.utils.CustomSlider
import com.ztftrue.music.utils.OperateType
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.SharedPreferencesUtils
import com.ztftrue.music.utils.TracksUtils
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.deleteTrackUpdate
import com.ztftrue.music.utils.Utils.toPx
import com.ztftrue.music.utils.enumToStringForPlayListType
import com.ztftrue.music.utils.trackManager.PlaylistManager
import com.ztftrue.music.utils.trackManager.SongsUtils
import com.ztftrue.music.utils.trackManager.TracksManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

const val CoverID = 0
const val LyricsID = 1
const val EqualizerID = 2
const val EffectID = 3

data class PlayingViewTab(
    val name: String,
    val id: Int,
    var priority: Int,
)


@OptIn(
    ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class
)
@UnstableApi
@Composable
fun PlayingPage(
    navController: NavHostController,
    musicViewModel: MusicViewModel,
) {
    val context = LocalContext.current
    val playViewTab: Array<PlayingViewTab> = arrayOf(
        PlayingViewTab("Cover", CoverID, 1), PlayingViewTab("Lyrics", LyricsID, 1),
        PlayingViewTab("Equalizer", EqualizerID, 1),
        PlayingViewTab("Effect", EffectID, 1),
    )
    val pagerTabState = rememberPagerState { playViewTab.size }
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var showAddPlayListDialog by remember { mutableStateOf(false) }
    var showCreatePlayListDialog by remember { mutableStateOf(false) }
    var repeatModel by remember { mutableIntStateOf(musicViewModel.repeatModel.intValue) }
    var music: MusicItem? = musicViewModel.currentPlay.value
    var showDeleteTip by remember { mutableStateOf(false) }

    LaunchedEffect(music) {
        if (music == null) {
            navController.popBackStack()
        }
    }

    if (showDeleteTip && music != null) {
        DeleteTip(music.name, onDismiss = {
            showDeleteTip = false
            if (it) {
                if (TracksManager.removeMusicById(context, music!!.id)) {
                    val bundle = Bundle()
                    bundle.putLong("id", music!!.id)
                    musicViewModel.mediaBrowser?.sendCustomAction(
                        ACTION_TRACKS_DELETE,
                        bundle,
                        object : MediaBrowserCompat.CustomActionCallback() {
                            override fun onResult(
                                action: String?,
                                extras: Bundle?,
                                resultData: Bundle?
                            ) {
                                super.onResult(action, extras, resultData)
                                if (ACTION_TRACKS_DELETE == action) {
                                    deleteTrackUpdate(musicViewModel, resultData)
                                }
                                navController.popBackStack()
                            }
                        }
                    )
                }

            }
        })
    }
    if (showDialog) {
        music = musicViewModel.currentPlay.value
        if (music != null) {
            OperateDialog(
                musicViewModel,
                music = music,
                null,
                onDismiss = { operateType ->
                    showDialog = false
                    when (operateType) {
                        OperateType.AddToQueue -> {
                            musicViewModel.musicQueue.add(music)
                            val bundle = Bundle()
                            bundle.putParcelable("musicItem", music)
                            musicViewModel.mediaBrowser?.sendCustomAction(
                                ACTION_AddPlayQueue,
                                bundle,
                                null
                            )
                        }

                        OperateType.PlayNext -> {
                            musicViewModel.musicQueue.add(
                                musicViewModel.currentPlayQueueIndex.intValue + 1,
                                music
                            )
                            val bundle = Bundle()
                            bundle.putParcelable("musicItem", music)
                            bundle.putInt(
                                "index",
                                musicViewModel.currentPlayQueueIndex.intValue + 1
                            )
                            musicViewModel.mediaBrowser?.sendCustomAction(
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

                            musicViewModel.navController?.navigate(
                                Router.PlayListView.withArgs(
                                    "id" to "${music.artistId}",
                                   "itemType" to  enumToStringForPlayListType(PlayListType.Artists)
                                ),
                            ) {
                                popUpTo(Router.MainView.route) {
                                    // Inclusive means the start destination is also popped
                                    inclusive = false
                                }
                            }
                        }

                        OperateType.Album -> {
                            musicViewModel.navController?.navigate(
                                Router.PlayListView.withArgs(
                                   "id" to "${music.albumId}",
                                   "itemType" to enumToStringForPlayListType(PlayListType.Albums)
                                )
                            ) {
                                popUpTo(Router.MainView.route) {
                                    // Inclusive means the start destination is also popped
                                    inclusive = false
                                }
                            }
                        }

                        OperateType.RemoveFromQueue -> {
                            val index = musicViewModel.musicQueue.indexOfFirst { it.id == music.id }
                            if (index == -1) return@OperateDialog
                            val bundle = Bundle()
                            bundle.putInt("index", index)
                            musicViewModel.mediaBrowser?.sendCustomAction(
                                ACTION_RemoveFromQueue,
                                bundle,
                                object : MediaBrowserCompat.CustomActionCallback() {
                                    override fun onResult(
                                        action: String?,
                                        extras: Bundle?,
                                        resultData: Bundle?
                                    ) {
                                        super.onResult(action, extras, resultData)
                                        if (ACTION_RemoveFromQueue == action) {
                                            if (musicViewModel.currentPlay.value?.id == music.id) {
                                                musicViewModel.currentMusicCover.value = null
                                                musicViewModel.currentPlayQueueIndex.intValue =
                                                    (index) % (musicViewModel.musicQueue.size + 1)
                                                musicViewModel.currentPlay.value =
                                                    musicViewModel.musicQueue[musicViewModel.currentPlayQueueIndex.intValue]
                                            }
                                        }
                                    }
                                }
                            )
                            musicViewModel.musicQueue.removeAt(index)
                        }

                        OperateType.EditMusicInfo -> {
                            musicViewModel.navController?.navigate(Router.EditTrackPage.withArgs("id" to "${music.id}"))
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
            AddMusicToPlayListDialog(
                musicViewModel,
                music,
                onDismiss = { playListId, removeDuplicate ->
                    showAddPlayListDialog = false
                    if (playListId != null) {
                        if (playListId == -1L) {
                            showCreatePlayListDialog = true
                        } else {
                            val musics = java.util.ArrayList<MusicItem>(1)
                            musics.add(music)
                            if (PlaylistManager.addMusicsToPlaylist(
                                    context,
                                    playListId,
                                    musics,
                                    removeDuplicate
                                )
                            ) {
                                SongsUtils.refreshPlaylist(musicViewModel)
                            }
                        }
                    }
                })
        }
    }
    if (showCreatePlayListDialog) {
        CreatePlayListDialog(onDismiss = {
            showCreatePlayListDialog = false
            if (!it.isNullOrEmpty()) {
                if (music != null) {
                    Utils.createPlayListAddTrack(it, context, music, musicViewModel, false)
                }
            }
        })
    }
    var popupWindow by remember {
        mutableStateOf(false)
    }
    var visualizationPopupWindow by remember {
        mutableStateOf(false)
    }
    var popupWindowDictionary by remember {
        mutableStateOf(false)
    }
    if (popupWindow) {
        Popup(
            // on below line we are adding
            // alignment and properties.
            alignment = Alignment.TopCenter,
            properties = PopupProperties(),
            offset = IntOffset(
                0.dp.toPx(context),
                40.dp.toPx(context)
            ),
            onDismissRequest = {
                popupWindow = false
            }
        ) {
            val color = MaterialTheme.colorScheme.secondary
            val configuration = LocalConfiguration.current
            Column(
                modifier = Modifier
                    .width(
                        (configuration.screenWidthDp - 20.dp.toPx(
                            context
                        )).dp
                    )
                    .padding(top = 5.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(60.dp), // Number of columns in the grid
                    contentPadding = PaddingValues(5.dp),
                    state = rememberLazyGridState(),
                    modifier = Modifier
                ) {
                    item {
                        IconButton(
                            modifier = Modifier.width(50.dp),
                            onClick = {
                                musicViewModel.textAlign.value =
                                    TextAlign.Start
                                SharedPreferencesUtils.saveDisplayAlign(
                                    context,
                                    TextAlign.Start
                                )
                            }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.FormatAlignLeft,
                                contentDescription = "Set lyrics display align left",
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(24.dp)
                                    .drawBehind {
                                        if (musicViewModel.textAlign.value == TextAlign.Start) {
                                            drawRect(
                                                color = color,
                                                topLeft = Offset(
                                                    0f,
                                                    0f
                                                ),
                                                size = Size(
                                                    size.width,
                                                    size.height
                                                ),
                                                style = Stroke(4f)
                                            )
                                        }
                                    },
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    item {
                        IconButton(
                            modifier = Modifier.width(50.dp),
                            onClick = {
                                musicViewModel.textAlign.value =
                                    TextAlign.Center
                                SharedPreferencesUtils.saveDisplayAlign(
                                    context,
                                    TextAlign.Center
                                )
                            }) {
                            Icon(
                                imageVector = Icons.Outlined.FormatAlignCenter,
                                contentDescription = "Set lyrics display  align center",
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(24.dp)
                                    .drawBehind {
                                        if (musicViewModel.textAlign.value == TextAlign.Center) {
                                            drawRect(
                                                color = color,
                                                topLeft = Offset(
                                                    0f,
                                                    0f
                                                ),
                                                size = Size(
                                                    size.width,
                                                    size.height
                                                ),
                                                style = Stroke(4f)
                                            )
                                        }
                                    },
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    item {
                        IconButton(
                            modifier = Modifier.width(50.dp),
                            onClick = {
                                musicViewModel.textAlign.value =
                                    TextAlign.End
                                SharedPreferencesUtils.saveDisplayAlign(
                                    context,
                                    TextAlign.End
                                )
                            }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.FormatAlignRight,
                                contentDescription = "Set lyrics display align right",
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(24.dp)
                                    .drawBehind {
                                        if (musicViewModel.textAlign.value == TextAlign.End) {
                                            drawRect(
                                                color = color,
                                                topLeft = Offset(
                                                    0f,
                                                    0f
                                                ),
                                                size = Size(
                                                    size.width,
                                                    size.height
                                                ),
                                                style = Stroke(4f)
                                            )
                                        }
                                    },
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    item {
                        IconButton(
                            modifier = Modifier.width(50.dp),
                            onClick = {
                                musicViewModel.textAlign.value =
                                    TextAlign.Justify
                                SharedPreferencesUtils.saveDisplayAlign(
                                    context,
                                    TextAlign.Justify
                                )
                            }) {
                            Icon(
                                imageVector = Icons.Outlined.FormatAlignJustify,
                                contentDescription = "Set lyrics display align justify",
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(24.dp)
                                    .drawBehind {
                                        if (musicViewModel.textAlign.value == TextAlign.Justify)
                                            drawRect(
                                                color = color,
                                                topLeft = Offset(
                                                    0f,
                                                    0f
                                                ),
                                                size = Size(
                                                    size.width,
                                                    size.height
                                                ),
                                                style = Stroke(4f)
                                            )
                                    },
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    item {
                        IconButton(
                            modifier = Modifier.width(50.dp),
                            onClick = {
                                musicViewModel.fontSize.intValue -= 1
                                SharedPreferencesUtils.saveFontSize(
                                    context,
                                    musicViewModel.fontSize.intValue
                                )
                            }) {
                            Icon(
                                imageVector = Icons.Outlined.TextDecrease,
                                contentDescription = "Font size decrease",
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(24.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    item {
                        IconButton(
                            modifier = Modifier.width(50.dp),
                            onClick = {
                                musicViewModel.fontSize.intValue += 1
                                SharedPreferencesUtils.saveFontSize(
                                    context,
                                    musicViewModel.fontSize.intValue
                                )
                            }) {
                            Icon(
                                imageVector = Icons.Outlined.TextIncrease,
                                contentDescription = "Font size increase",
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(24.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    item {
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
                                    musicViewModel.autoScroll.value =
                                        !musicViewModel.autoScroll.value
                                    SharedPreferencesUtils.saveAutoScroll(
                                        context,
                                        musicViewModel.autoScroll.value
                                    )
                                }
                                .padding(0.dp)
                                .height(50.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.scroll),
                                modifier = Modifier.padding(0.dp),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = TextUnit(
                                    12f,
                                    TextUnitType.Sp
                                ),
                                lineHeight = TextUnit(
                                    12f,
                                    TextUnitType.Sp
                                ),
                            )
                            Switch(
                                checked = musicViewModel.autoScroll.value,
                                modifier = Modifier
                                    .scale(0.5f)
                                    .padding(0.dp),
                                onCheckedChange = {
                                    musicViewModel.autoScroll.value = it
                                    SharedPreferencesUtils.saveAutoScroll(
                                        context,
                                        it
                                    )
                                }
                            )
                        }
                    }
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .combinedClickable(onLongClick = {
                                    Toast
                                        .makeText(
                                            context,
                                            "Switch auto high light",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }) {
                                    musicViewModel.autoHighLight.value =
                                        !musicViewModel.autoHighLight.value
                                    SharedPreferencesUtils.saveAutoHighLight(
                                        context,
                                        musicViewModel.autoHighLight.value
                                    )
                                }
                                .padding(0.dp)
                                .height(50.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.highlight),
                                modifier = Modifier.padding(0.dp),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = TextUnit(
                                    12f,
                                    TextUnitType.Sp
                                ),
                                lineHeight = TextUnit(
                                    12f,
                                    TextUnitType.Sp
                                ),
                            )
                            Switch(
                                checked = musicViewModel.autoHighLight.value,
                                modifier = Modifier
                                    .scale(0.5f)
                                    .padding(0.dp),
                                onCheckedChange = {
                                    musicViewModel.autoHighLight.value = it
                                    SharedPreferencesUtils.saveAutoHighLight(
                                        context,
                                        it
                                    )
                                }
                            )
                        }
                    }
                    item {
                        IconButton(
                            modifier = Modifier.width(50.dp), onClick = {
                                if (!popupWindowDictionary) {
                                    popupWindowDictionary = true
                                    popupWindow = false
                                }
                            }) {
                            Image(
                                painter = painterResource(
                                    R.drawable.ic_dictionary
                                ),
                                contentDescription = "Set lyrics display format",
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(24.dp),
                                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                            )
                        }
                    }
                    item {
                        IconButton(
                            modifier = Modifier.width(50.dp), onClick = {
                                musicViewModel.showSlideIndicators.value =
                                    !musicViewModel.showSlideIndicators.value
                                SharedPreferencesUtils.setShowSlideIndicators(
                                    context,
                                    musicViewModel.showSlideIndicators.value
                                )
                            }) {
                            Icon(
                                imageVector = Icons.Outlined.Adjust,
                                contentDescription = "Set show slider indicator",
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(24.dp),
                                tint = if (musicViewModel.showSlideIndicators.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        modifier = Modifier.width(50.dp),
                        onClick = {
                            popupWindow = false
                        }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close display set popup",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

            }
        }
    }
    val list = remember {
        mutableStateListOf<DictionaryApp>()
    }
    var selectedOption by remember { mutableStateOf("Matrix") }
    if (visualizationPopupWindow) {
        Popup(
            // on below line we are adding
            // alignment and properties.
            alignment = Alignment.TopCenter,
            properties = PopupProperties(),
            offset = IntOffset(
                0.dp.toPx(context),
                40.dp.toPx(context)
            ),
            onDismissRequest = {
                popupWindow = false
            }
        ) {
            val configuration = LocalConfiguration.current
            Column(
                modifier = Modifier
                    .width(
                        (configuration.screenWidthDp - 20.dp.toPx(
                            context
                        )).dp
                    )
                    .padding(top = 5.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(5.dp),
                    modifier = Modifier
                ) {
                    item {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Visualization ${if (musicViewModel.musicVisualizationEnable.value) "ON" else "OFF"}",
                                Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = musicViewModel.musicVisualizationEnable.value,
                                onCheckedChange = {
                                    musicViewModel.musicVisualizationEnable.value = it
                                    val bundleTemp = Bundle()
                                    bundleTemp.putBoolean("enable", it)
                                    musicViewModel.mediaBrowser?.sendCustomAction(
                                        ACTION_VISUALIZATION_ENABLE,
                                        bundleTemp,
                                        null
                                    )
                                }
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Cover ${if (musicViewModel.showMusicCover.value) "Show" else "Hide"}",
                                Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                enabled = musicViewModel.musicVisualizationEnable.value,
                                checked = musicViewModel.showMusicCover.value,
                                onCheckedChange = {
                                    musicViewModel.showMusicCover.value = it
                                    SharedPreferencesUtils.saveShowMusicCover(context, it)
                                }
                            )
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                enabled = musicViewModel.musicVisualizationEnable.value,
                                selected = selectedOption == "Matrix",
                                onClick = { selectedOption = "Matrix" }
                            )
                            Text(
                                text = "Matrix", Modifier.padding(start = 10.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        modifier = Modifier.width(50.dp),
                        onClick = {
                            visualizationPopupWindow = false
                        }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close display popup",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

            }
        }
    }
    LaunchedEffect(Unit) {
        val hashMap = HashMap<String, DictionaryApp>()
        musicViewModel.dictionaryAppList.forEach {
            hashMap[it.packageName] = it
        }
        list.addAll(musicViewModel.dictionaryAppList)
        Utils.getAllDictionaryActivity(context)
            .forEachIndexed { index, it ->
                if (hashMap[it.activityInfo.packageName] == null) {
                    list.add(
                        DictionaryApp(
                            index,
                            it.activityInfo.name,
                            it.activityInfo.packageName,
                            it.loadLabel(context.packageManager).toString(),
                            isShow = false,
                            autoGo = false
                        )
                    )
                }
            }
    }
    if (popupWindowDictionary) {
        if (list.isEmpty()) {
            Toast.makeText(
                context,
                stringResource(R.string.no_dictionary_app_tip),
                Toast.LENGTH_SHORT
            ).show()
            popupWindowDictionary = false
        } else {
            Dialog(
                onDismissRequest = {
                    popupWindowDictionary = false
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = true,
                    dismissOnBackPress = true,
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
                            text = stringResource(R.string.manage_dictionary_app),
                            modifier = Modifier
                                .padding(2.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(color = MaterialTheme.colorScheme.onBackground)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        )
                        {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)

                                ) { }
                                Box(modifier = Modifier.width(80.dp)) { }
                            }
                            Text(
                                text = stringResource(R.string.show),
                                modifier = Modifier.width(50.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.auto_go),
                                modifier = Modifier.width(80.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                        ) {

                            items(list.size) { listIndex ->
                                val item = list[listIndex]
                                var offset by remember { mutableFloatStateOf(0f) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .graphicsLayer(
                                            translationY = offset,
                                        ),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                )
                                {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .height(60.dp)
                                                .draggable(
                                                    orientation = Orientation.Vertical,
                                                    state = rememberDraggableState { delta ->
                                                        offset += delta
                                                    },
                                                    onDragStopped = { _ ->
                                                        var position =
                                                            listIndex + (offset / 60.dp.toPx(context)).toInt()
                                                        if (position < 0) {
                                                            position = 0
                                                        }
                                                        if (position > list.size - 1) {
                                                            position = list.size - 1
                                                        }
                                                        if (position != listIndex) {
                                                            list.remove(item)
                                                            list.add(position, item)
                                                        }
                                                        offset = 0f
                                                    }
                                                )) {
                                            Icon(
                                                imageVector = Icons.Outlined.SwipeVertical,
                                                contentDescription = "Down ${item.label} app priority",
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .padding(15.dp)
                                                    .clip(
                                                        CircleShape
                                                    ),
                                                tint = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                        Box(modifier = Modifier.width(80.dp)) {
                                            Text(
                                                text = item.label,
                                                modifier = Modifier
                                                    .horizontalScroll(rememberScrollState(0)),
                                                color = MaterialTheme.colorScheme.onBackground,
                                                maxLines = 1
                                            )
                                        }

                                    }
                                    var isChecked by remember {
                                        mutableStateOf(false)
                                    }
                                    isChecked = item.isShow
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { v ->
                                            isChecked = v
                                            item.isShow = v
                                        },
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .semantics {
                                                contentDescription =
                                                    if (isChecked) {
                                                        "Show this ${item.name}"
                                                    } else {
                                                        "Hide this ${item.name}"
                                                    }
                                            }
                                    )
                                    var autoGo by remember {
                                        mutableStateOf(false)
                                    }
                                    autoGo = item.autoGo
                                    Column {
                                        Checkbox(
                                            checked = autoGo,
                                            onCheckedChange = { v ->
                                                for ((index, it) in list.withIndex()) {
                                                    if (it.autoGo) {
                                                        list.removeAt(index)
                                                        it.autoGo = false
                                                        list.add(index, it)
                                                        break
                                                    }
                                                }
                                                autoGo = v
                                                item.autoGo = v
                                            },
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .semantics {
                                                    contentDescription =
                                                        if (isChecked) {
                                                            "Auto go ${item.label}"
                                                        } else {
                                                            "Don't auto go ${item.label}"
                                                        }
                                                }
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
                                onClick = {
                                    popupWindowDictionary = false
                                },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(0.5f),
                            ) {
                                Text(
                                    stringResource(id = R.string.cancel),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.onBackground)
                                    .width(1.dp)
                                    .height(50.dp)
                            )
                            TextButton(
                                onClick = {
                                    val result =
                                        ArrayList<DictionaryApp>()
                                    list.forEach {
                                        if (it.isShow) {
                                            result.add(it)
                                        }
                                    }
                                    result.forEachIndexed { index, item ->
                                        item.id = index
                                    }
                                    CoroutineScope(Dispatchers.IO).launch {
                                        musicViewModel.getDb(context).DictionaryAppDao()
                                            .deleteAll()
                                        musicViewModel.getDb(context).DictionaryAppDao()
                                            .insertAll(result)
                                    }
                                    musicViewModel.dictionaryAppList.clear()
                                    musicViewModel.dictionaryAppList.addAll(
                                        result
                                    )
                                    popupWindowDictionary = false
                                },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                            ) {
                                Text(
                                    "Ok",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            )
        }

    }

    Scaffold(
        topBar = {
            Column(Modifier.fillMaxWidth()) {
                key(Unit, pagerTabState.currentPage) {
                    TopBar(navController, musicViewModel, content = {
                        if (playViewTab[pagerTabState.currentPage].id == CoverID) {
                            IconButton(
                                modifier = Modifier.width(50.dp), onClick = {
                                    visualizationPopupWindow = !visualizationPopupWindow
                                }) {
                                Icon(
                                    imageVector = Icons.Outlined.Equalizer,
                                    contentDescription = "Set music visualization",
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(24.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        if (playViewTab[pagerTabState.currentPage].id == LyricsID) {
                            IconButton(
                                modifier = Modifier.width(50.dp), onClick = {
                                    popupWindow = !popupWindow
                                }) {
                                Icon(
                                    imageVector = Icons.Outlined.FormatShapes,
                                    contentDescription = "Set lyrics display format",
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(24.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
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
                                    .size(30.dp)
                                    .clip(CircleShape),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    })
                }
                key(musicViewModel.currentPlay.value) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp)
                    ) {
                        musicViewModel.currentPlay.value?.let { it1 ->
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
                    SecondaryScrollableTabRow(
                        selectedTabIndex = pagerTabState.currentPage,
                        modifier = Modifier.fillMaxWidth(),
                        indicator = {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier
                                    .height(3.0.dp)
                                    .tabIndicatorOffset(pagerTabState.currentPage),
                                height = 3.0.dp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                    ) {
                        playViewTab.forEachIndexed { index, item ->
                            Tab(selected = pagerTabState.currentPage == index, onClick = {
                                coroutineScope.launch {
                                    pagerTabState.animateScrollToPage(index)
                                }
                            }, text = {
                                Text(
                                    text = stringResource(
                                        id = Utils.translateMap[item.name] ?: R.string.app_name
                                    ),
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
                HorizontalPager(
                    state = pagerTabState,
                    beyondViewportPageCount = playViewTab.size,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(it)
                        .pointerInput(Unit) {

                        },
                    userScrollEnabled = false
                ) { id ->
                    when (playViewTab[id].id) {
                        CoverID -> {
                            CoverView(musicViewModel)
                        }

                        LyricsID -> {
                            LyricsView(musicViewModel)
                        }

                        EqualizerID -> {
                            EqualizerView(musicViewModel)
                        }

                        EffectID -> {
                            EffectView(musicViewModel)
                        }
                    }

                }
            },
        bottomBar =
            {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            (150.dp + WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                                .asPaddingValues().calculateBottomPadding())
                        )
                        .padding(0.dp), // padding 为 0
                    containerColor = Color.Transparent, // 透明背景
                    tonalElevation = 0.dp, // 阴影去掉（可选）
                    contentPadding = PaddingValues(0.dp), // 内容也没有内边距
                    actions = {
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .height(
                                    (150.dp + WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                                        .asPaddingValues().calculateBottomPadding())
                                )
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (musicViewModel.currentDuration.longValue > 0) {
                                CustomSlider(
                                    modifier = Modifier
                                        .semantics { contentDescription = "Slider" }
                                        .motionEventSpy {
                                            when (it.action) {
                                                MotionEvent.ACTION_DOWN -> {
                                                    musicViewModel.sliderTouching = true
                                                }

                                                MotionEvent.ACTION_MOVE -> {
                                                }

                                                MotionEvent.ACTION_UP -> {
                                                }
                                            }
                                        },
                                    value = musicViewModel.sliderPosition.floatValue,
                                    onValueChange = {
                                        musicViewModel.sliderPosition.floatValue =
                                            it.roundToLong().toFloat()
                                    },
                                    valueRange = 0f..musicViewModel.currentDuration.longValue.toFloat(),
                                    steps = 100,
                                    onValueChangeFinished = {
                                        val bundle = Bundle()
                                        bundle.putLong(
                                            "position",
                                            musicViewModel.sliderPosition.floatValue.toLong()
                                        )
                                        musicViewModel.mediaBrowser?.sendCustomAction(
                                            ACTION_SEEK_TO,
                                            bundle,
                                            null
                                        )
                                        musicViewModel.sliderTouching = false
                                    },
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = Utils.formatTime(musicViewModel.sliderPosition.floatValue.toLong()),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = Utils.formatTime(musicViewModel.currentDuration.longValue),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            } else {
                                if (musicViewModel.currentPlay.value != null) {
                                    Text(
                                        text = stringResource(R.string.get_duration_failed),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                            ConstraintLayout(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                            ) {
                                val (playIndicator, playIndicator2, playIndicator3) = createRefs()
                                Row(
                                    modifier = Modifier
                                        .constrainAs(playIndicator) {
                                            bottom.linkTo(anchor = parent.bottom, margin = 0.dp)
                                            start.linkTo(anchor = parent.start, margin = 0.dp)
                                            top.linkTo(anchor = parent.top, margin = 0.dp)
                                        }
                                        .height(60.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    key(musicViewModel.enableShuffleModel.value) {
                                        IconButton(onClick = {
                                            musicViewModel.enableShuffleModel.value =
                                                !musicViewModel.enableShuffleModel.value
                                            val bundle = Bundle()
                                            bundle.putBoolean(
                                                "enable",
                                                musicViewModel.enableShuffleModel.value
                                            )
                                            musicViewModel.mediaBrowser?.sendCustomAction(
                                                ACTION_SWITCH_SHUFFLE,
                                                bundle,
                                                object : MediaBrowserCompat.CustomActionCallback() {
                                                    override fun onResult(
                                                        action: String?,
                                                        extras: Bundle?,
                                                        resultData: Bundle?
                                                    ) {
                                                        super.onResult(action, extras, resultData)
                                                        if (ACTION_SWITCH_SHUFFLE == action && resultData != null) {
                                                            val qList =
                                                                resultData.getParcelableArrayList<MusicItem>(
                                                                    "list"
                                                                )
                                                            val qIndex =
                                                                resultData.getInt("index", -1)
                                                            if (qList != null && qIndex != -1) {
                                                                musicViewModel.musicQueue.clear()
                                                                musicViewModel.musicQueue.addAll(
                                                                    qList
                                                                )
                                                                if (musicViewModel.currentPlayQueueIndex.intValue == -1) {
                                                                    musicViewModel.currentPlayQueueIndex.intValue =
                                                                        qIndex
                                                                    musicViewModel.currentPlay.value =
                                                                        musicViewModel.musicQueue[qIndex]
                                                                    musicViewModel.currentCaptionList.clear()
                                                                    musicViewModel.currentMusicCover.value =
                                                                        null
                                                                    musicViewModel.currentPlay.value =
                                                                        musicViewModel.musicQueue[qIndex]
                                                                    musicViewModel.sliderPosition.floatValue =
                                                                        0f
                                                                    musicViewModel.currentDuration.longValue =
                                                                        musicViewModel.currentPlay.value?.duration
                                                                            ?: 0
                                                                    musicViewModel.dealLyrics(
                                                                        context,
                                                                        musicViewModel.musicQueue[qIndex]
                                                                    )
                                                                }
                                                                if (musicViewModel.enableShuffleModel.value && music != null && SharedPreferencesUtils.getAutoToTopRandom(
                                                                        context
                                                                    )
                                                                ) {
                                                                    TracksUtils.currentPlayToTop(
                                                                        musicViewModel.mediaBrowser!!,
                                                                        musicViewModel.musicQueue,
                                                                        music,
                                                                        qIndex
                                                                    )
                                                                }

                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }) {
                                            Icon(
                                                imageVector = if (musicViewModel.enableShuffleModel.value) Icons.Outlined.Shuffle else Icons.Outlined.Shuffle,
                                                contentDescription = "shuffle model",
                                                modifier = Modifier
                                                    .width(50.dp)
                                                    .height(50.dp)
                                                    .padding(5.dp),
                                                tint = if (musicViewModel.enableShuffleModel.value) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(
                                                    alpha = 0.5f
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.constrainAs(playIndicator2) {
                                        bottom.linkTo(anchor = parent.bottom, margin = 0.dp)
                                        start.linkTo(anchor = parent.start, margin = 0.dp)
                                        end.linkTo(anchor = parent.end, margin = 0.dp)
                                        top.linkTo(anchor = parent.top, margin = 0.dp)
                                    }
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.play_previous_song),
                                        contentDescription = "play previous song",
                                        modifier = Modifier
                                            .clickable {
                                                musicViewModel.mediaController?.transportControls?.skipToPrevious()
                                            }
                                            .width(50.dp)
                                            .height(50.dp)
                                            .padding(10.dp),
                                        colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                                    )
                                    Image(
                                        painter = painterResource(
                                            if (musicViewModel.playStatus.value) {
                                                R.drawable.pause
                                            } else {
                                                R.drawable.play
                                            }
                                        ),
                                        contentDescription = "Pause",
                                        modifier = Modifier
                                            .clickable {
                                                val pbState =
                                                    musicViewModel.mediaController?.playbackState?.state
                                                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                                                    musicViewModel.mediaController?.transportControls?.pause()
                                                } else {
                                                    musicViewModel.mediaController?.transportControls?.play()
                                                }
                                            }
                                            .width(60.dp)
                                            .height(60.dp)
                                            .padding(5.dp),
                                        colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                                    )
                                    Image(
                                        painter = painterResource(R.drawable.play_next_song),
                                        contentDescription = "Play next song",
                                        modifier = Modifier
                                            .clickable {
                                                musicViewModel.mediaController?.transportControls?.skipToNext()
                                            }
                                            .width(50.dp)
                                            .height(50.dp)
                                            .padding(10.dp),
                                        colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                                    )
                                }
                                Icon(
                                    imageVector = when (repeatModel) {

                                        Player.REPEAT_MODE_ALL -> {
                                            Icons.Outlined.Repeat
                                        }

                                        Player.REPEAT_MODE_ONE -> {
                                            Icons.Outlined.RepeatOne
                                        }

                                        else -> {
                                            Icons.Outlined.Repeat
                                        }
                                    },
                                    contentDescription = "Repeat model",
                                    modifier = Modifier
                                        .clickable {
                                            when (repeatModel) {
                                                Player.REPEAT_MODE_ALL -> {
                                                    repeatModel = Player.REPEAT_MODE_ONE
                                                    musicViewModel.repeatModel.intValue =
                                                        Player.REPEAT_MODE_ONE
                                                    musicViewModel.mediaController?.transportControls?.setRepeatMode(
                                                        PlaybackStateCompat.REPEAT_MODE_ONE
                                                    )
                                                }

                                                Player.REPEAT_MODE_ONE -> {
                                                    repeatModel = Player.REPEAT_MODE_OFF
                                                    musicViewModel.repeatModel.intValue =
                                                        Player.REPEAT_MODE_OFF
                                                    musicViewModel.mediaController?.transportControls?.setRepeatMode(
                                                        PlaybackStateCompat.REPEAT_MODE_NONE
                                                    )
                                                }

                                                else -> {
                                                    repeatModel = Player.REPEAT_MODE_ALL
                                                    musicViewModel.repeatModel.intValue =
                                                        Player.REPEAT_MODE_ALL
                                                    musicViewModel.mediaController?.transportControls?.setRepeatMode(
                                                        PlaybackStateCompat.REPEAT_MODE_ALL
                                                    )
                                                }
                                            }
                                        }
                                        .width(50.dp)
                                        .height(50.dp)
                                        .padding(10.dp)
                                        .constrainAs(playIndicator3) {
                                            bottom.linkTo(anchor = parent.bottom, margin = 0.dp)
                                            end.linkTo(anchor = parent.end, margin = 0.dp)
                                            top.linkTo(anchor = parent.top, margin = 0.dp)
                                        },
                                    tint = if (repeatModel == Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onBackground.copy(
                                        alpha = 0.5f
                                    ) else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                )

            }
    )

}

