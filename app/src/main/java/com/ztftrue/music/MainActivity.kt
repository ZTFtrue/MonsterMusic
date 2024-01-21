package com.ztftrue.music

import android.Manifest
import android.content.ComponentName
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ztftrue.music.play.ACTION_PlayLIST_CHANGE
import com.ztftrue.music.play.ACTION_TRACKS_DELETE
import com.ztftrue.music.play.ACTION_TRACKS_UPDATE
import com.ztftrue.music.play.EVENT_DATA_READY
import com.ztftrue.music.play.EVENT_MEDIA_ITEM_Change
import com.ztftrue.music.play.EVENT_MEDIA_METADATA_Change
import com.ztftrue.music.play.EVENT_SLEEP_TIME_Change
import com.ztftrue.music.play.EVENT_changePlayQueue
import com.ztftrue.music.play.PlayService
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.ui.home.AlbumGridView
import com.ztftrue.music.ui.home.ArtistsGridView
import com.ztftrue.music.ui.home.FolderListView
import com.ztftrue.music.ui.home.GenreGridView
import com.ztftrue.music.ui.home.PlayListView
import com.ztftrue.music.ui.other.DrawMenu
import com.ztftrue.music.ui.other.EditTrackPage
import com.ztftrue.music.ui.other.SearchPage
import com.ztftrue.music.ui.other.SettingsPage
import com.ztftrue.music.ui.other.TracksSelectPage
import com.ztftrue.music.ui.play.PlayingPage
import com.ztftrue.music.ui.public.Bottom
import com.ztftrue.music.ui.public.CreatePlayListDialog
import com.ztftrue.music.ui.public.QueuePage
import com.ztftrue.music.ui.public.SleepTimeDialog
import com.ztftrue.music.ui.public.TracksListPage
import com.ztftrue.music.ui.public.TracksListView
import com.ztftrue.music.ui.theme.MusicPitchTheme
import com.ztftrue.music.utils.AnyListBase
import com.ztftrue.music.utils.OperateTypeInActivity
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.PlaylistManager.removeTrackFromM3U
import com.ztftrue.music.utils.stringToEnumForPlayListType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream


@Suppress("DEPRECATION")
@UnstableApi
class MainActivity : ComponentActivity() {

    private var mediaBrowser: MediaBrowserCompat? = null
    private val musicViewModel: MusicViewModel by viewModels()
    private var jobSeek: Job? = null
    private val scopeMain = CoroutineScope(Dispatchers.Main)
    private var lyricsPath = ""


    val bundle = Bundle()

    @JvmField
    val modifyMediaLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result != null && result.resultCode == RESULT_OK) {
                val resolver: ContentResolver = contentResolver
                val action = bundle.getString("action")
                if (OperateTypeInActivity.DeletePlayList.name == action) {
                    val u = bundle.getParcelable<Uri>("uri")
                    if (u != null) {
                        resolver.delete(u, null, null)
                    }
                } else if (OperateTypeInActivity.InsertTrackToPlaylist.name == action) {
                    val u = bundle.getParcelable<Uri>("uri")
                    val v = bundle.getParcelableArrayList<ContentValues>("values")
                    if (u != null && v != null) {
                        resolver.bulkInsert(u, v.toTypedArray())
                    }
                } else if (OperateTypeInActivity.RenamePlaylist.name == action) {
                    val u = bundle.getParcelable<Uri>("uri")
                    val v = bundle.getParcelable<ContentValues>("values")
                    if (u != null && v != null) {
                        resolver.update(u, v, null, null)
                    }
                } else if (OperateTypeInActivity.RemoveTrackFromPlayList.name == action) {
                    val playListPath = bundle.getString("playListPath", "")
                    val trackIndex = bundle.getInt("trackIndex", -1)
                    if (playListPath.isNotEmpty() && trackIndex != -1) {
                        removeTrackFromM3U(playListPath, trackIndex)
                        MediaScannerConnection.scanFile(
                            this@MainActivity,
                            arrayOf<String>(playListPath),
                            arrayOf("*/*"),
                            object : MediaScannerConnectionClient {
                                override fun onMediaScannerConnected() {}
                                override fun onScanCompleted(path: String, uri: Uri) {
                                    musicViewModel.mediaBrowser?.sendCustomAction(
                                        ACTION_PlayLIST_CHANGE,
                                        null,
                                        object : MediaBrowserCompat.CustomActionCallback() {
                                            override fun onResult(
                                                action: String?,
                                                extras: Bundle?,
                                                resultData: Bundle?
                                            ) {
                                                super.onResult(action, extras, resultData)
                                                if (ACTION_PlayLIST_CHANGE == action) {
                                                    musicViewModel.refreshList.value =
                                                        !musicViewModel.refreshList.value
                                                }
                                            }
                                        }
                                    )
                                }
                            })
                    }
                    return@registerForActivityResult
                } else if (OperateTypeInActivity.RemoveTrackFromStorage.name == action) {
                    val u = bundle.getParcelable<Uri>("uri")
                    val id = bundle.getLong("musicId")
                    if (u != null) {
                        contentResolver.delete(u, null, null)
                        val bundleTemp = Bundle()
                        bundleTemp.putLong("id", id)
                        musicViewModel.mediaBrowser?.sendCustomAction(
                            ACTION_TRACKS_DELETE,
                            bundleTemp,
                            object : MediaBrowserCompat.CustomActionCallback() {
                                override fun onResult(
                                    action: String?,
                                    extras: Bundle?,
                                    resultData: Bundle?
                                ) {
                                    super.onResult(action, extras, resultData)
                                    if (ACTION_TRACKS_DELETE == action) {
                                        musicViewModel.refreshList.value =
                                            !musicViewModel.refreshList.value
                                    }
                                }
                            }
                        )
                    }
                    return@registerForActivityResult
                } else if (OperateTypeInActivity.EditTrackInfo.name == action) {
                    val u = bundle.getParcelable<Uri>("uri")
                    val v = bundle.getParcelable<ContentValues>("value")
                    val id = bundle.getLong("id")
                    if (u != null && v != null) {
                        // TODO this is disable,  because cant  updating real file in storage
//                        values.put(MediaStore.Audio.Media.IS_PENDING, 1)
//                        contentResolver.update(uri, values, null, null)
                        resolver.update(u, v, null, null)
                        val bundleTemp = Bundle()
                        bundleTemp.putLong("id", id)
                        musicViewModel.mediaBrowser?.sendCustomAction(
                            ACTION_TRACKS_UPDATE,
                            bundleTemp,
                            object : MediaBrowserCompat.CustomActionCallback() {
                                override fun onResult(
                                    action: String?,
                                    extras: Bundle?,
                                    resultData: Bundle?
                                ) {
                                    super.onResult(action, extras, resultData)
                                    if (ACTION_TRACKS_UPDATE == action) {
                                        musicViewModel.refreshList.value =
                                            !musicViewModel.refreshList.value
                                    }
                                }
                            }
                        )
                    }

                }

                musicViewModel.mediaBrowser?.sendCustomAction(
                    ACTION_PlayLIST_CHANGE,
                    null,
                    object : MediaBrowserCompat.CustomActionCallback() {
                        override fun onResult(
                            action: String?,
                            extras: Bundle?,
                            resultData: Bundle?
                        ) {
                            super.onResult(action, extras, resultData)
                            if (ACTION_PlayLIST_CHANGE == action) {
                                musicViewModel.refreshList.value = !musicViewModel.refreshList.value
                            }
                        }
                    }
                )
            } else {
                bundle.clear()
            }
        }


    private val filePickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                if (result.data != null) {
                    val selectedFileUri: Uri? = result.data!!.data
                    if (selectedFileUri != null) {
                        val cursor = this@MainActivity.contentResolver.query(
                            selectedFileUri,
                            null,
                            null,
                            null,
                            null
                        ) ?: return@registerForActivityResult
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        val name = cursor.getString(nameIndex)
                        lyricsPath += if (name.endsWith(".txt")) {
                            "txt"
                        } else {
                            "lrc"
                        }
                        val inputStream =
                            this@MainActivity.contentResolver.openInputStream(selectedFileUri)
                        if (inputStream != null) {
                            val outputStream = FileOutputStream(File(lyricsPath))
                            val buffer = ByteArray(1024)
                            var length: Int
                            while (inputStream.read(buffer).also { length = it } > 0) {
                                outputStream.write(buffer, 0, length)
                            }
                            inputStream.close()
                            outputStream.close()
                        }
                        if (musicViewModel.currentPlay.value != null) {
                            musicViewModel.dealLyrics(
                                this@MainActivity,
                                musicViewModel.currentPlay.value!!
                            )
                        }
                        lyricsPath = ""
                    }
                }
            }
        }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.setData(Uri.fromParts("package", packageName, null))
        // Check if there is an activity that can handle this intent
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            finish()
        }
    }


    fun openFilePicker(filePath: String) {
        lyricsPath = filePath
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/plain" // Specify the MIME type for text files
        intent.addCategory(Intent.CATEGORY_OPENABLE)
//        intent.type = "text/plain|application/octet-stream";
        filePickerLauncher.launch(intent)
    }

    @OptIn(ExperimentalFoundationApi::class)
    private val audioPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            val i = Intent(this@MainActivity, PlayService::class.java)
            startService(i)  // Start the service explicitly
            mediaBrowser = MediaBrowserCompat(
                this,
                ComponentName(this, PlayService::class.java),
                connectionCallbacks,
                null // optional Bundle
            )
            setContent {
                MusicPitchTheme(musicViewModel) {
                    BaseLayout()
                }
                mediaBrowser?.connect()
            }
        } else {
            setContent {
                MusicPitchTheme(musicViewModel) {
                    // A surface container using the 'background' color from the theme
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .combinedClickable {
                                openAppSettings()
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Can't find any audio file\n I need permission\nClick here to open settings",
                            fontSize = 20.sp,
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val i = Intent(this@MainActivity, PlayService::class.java)
        // startService does not work in there.
//        startService(i)  // Start the service explicitly
        musicViewModel.themeSelected.intValue = getSharedPreferences(
            "SelectedTheme",
            Context.MODE_PRIVATE
        ).getInt("SelectedTheme", 0)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val i = Intent(this@MainActivity, PlayService::class.java)
                startService(i)  // Start the service explicitly
                setContent {
                    MusicPitchTheme(musicViewModel) {
                        BaseLayout()
                    }
                }
            } else {
                audioPermissionRequest.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else if (ActivityCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val i = Intent(this@MainActivity, PlayService::class.java)
            startService(i)  // Start the service explicitly
            setContent {
                MusicPitchTheme(musicViewModel) {
                    BaseLayout()
                }
            }
        } else {
            audioPermissionRequest.launch(Manifest.permission.READ_MEDIA_AUDIO)
        }
    }

    public override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mediaBrowser = MediaBrowserCompat(
                    this,
                    ComponentName(this, PlayService::class.java),
                    connectionCallbacks,
                    null // optional Bundle
                )
                mediaBrowser?.connect()
            }
        } else if (ActivityCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mediaBrowser = MediaBrowserCompat(
                this,
                ComponentName(this, PlayService::class.java),
                connectionCallbacks,
                null // optional Bundle
            )
            mediaBrowser?.connect()
        }
    }

    public override fun onResume() {
        super.onResume()
        getSeek()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onPause() {
        jobSeek?.cancel()
        super.onPause()
    }

    public override fun onStop() {
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(callback)
        musicViewModel.mediaController = null
        mediaBrowser?.disconnect()
        super.onStop()
    }

    override fun onDestroy() {
        // should invoke in onStop()
        musicViewModel.reset()
        super.onDestroy()
    }

    fun getSeek() {
        if (musicViewModel.playStatus.value) {
            jobSeek = scopeMain.launch {
                // i debug it, when i set it while(true),
                // this code is working (when it invoke cancel) also. who can tell me,this is why?
                while (isActive) {
                    delay(1000)
                    val f = mediaController.playbackState?.position ?: 0
                    if (f < 0) {
                        continue
                    }
                    if (!musicViewModel.sliderTouching) {
                        musicViewModel.sliderPosition.floatValue = f.toFloat()
                    }
                }
            }
        } else {
            jobSeek?.cancel()
        }
    }

    val callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
//            musicViewModel.currentPlay.value?.isPlaying = options.getBoolean("isPlaying")
            if (state != null) {
                musicViewModel.playStatus.value = state.state == PlaybackStateCompat.STATE_PLAYING
                getSeek()
            }
        }

        override fun onExtrasChanged(extras: Bundle?) {
            super.onExtrasChanged(extras)
            extras?.let {
                if (it.getInt("type") == EVENT_changePlayQueue) {
//                    val playList = it.getParcelable<MusicPlayList>("playList")
//                    musicViewModel.musicQueue.value = musicViewModel.musicListMap[playList?.id]
                } else if (it.getInt("type") == EVENT_MEDIA_ITEM_Change) {
                    // before switch to another music, must clear lyrics
                    musicViewModel.currentLyricsList.clear()
                    val index = it.getInt("index")
                    if (index >= 0 && musicViewModel.musicQueue.size > index) {
                        musicViewModel.currentMusicCover.value = null
                        musicViewModel.currentPlay.value =
                            musicViewModel.musicQueue[index]
                        musicViewModel.currentPlayQueueIndex.intValue = index
                        musicViewModel.currentDuration.longValue =
                            musicViewModel.currentPlay.value?.duration ?: 0
                        musicViewModel.dealLyrics(
                            this@MainActivity,
                            musicViewModel.musicQueue[index]
                        )

                    }
                } else if (it.getInt("type") == EVENT_MEDIA_METADATA_Change) {
                    val cover = it.getByteArray("cover")
                    if (cover != null)
                        musicViewModel.currentMusicCover.value = BitmapFactory.decodeByteArray(
                            cover, 0,
                            cover.size
                        )
                } else if (it.getInt("type") == EVENT_SLEEP_TIME_Change) {
                    val remainTime = it.getLong("remaining")
                    musicViewModel.remainTime.longValue = remainTime
                    if (remainTime == 0L) {
                        musicViewModel.sleepTime.longValue = 0
                    }
                } else if (it.getInt("type") == EVENT_DATA_READY) {
                    getInitData(it)
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
        }

        override fun onQueueTitleChanged(title: CharSequence?) {
            super.onQueueTitleChanged(title)
        }

        override fun onAudioInfoChanged(info: MediaControllerCompat.PlaybackInfo?) {
            super.onAudioInfoChanged(info)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            when (repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_NONE -> {
                    musicViewModel.repeatModel.intValue = Player.REPEAT_MODE_OFF
                }

                PlaybackStateCompat.REPEAT_MODE_ALL -> {
                    musicViewModel.repeatModel.intValue = Player.REPEAT_MODE_ALL
                }

                PlaybackStateCompat.REPEAT_MODE_ONE -> {
                    musicViewModel.repeatModel.intValue = Player.REPEAT_MODE_ONE
                }

                PlaybackStateCompat.REPEAT_MODE_GROUP -> {
                }

                PlaybackStateCompat.REPEAT_MODE_INVALID -> {
                }
            }
        }
    }
    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            // Get the token for the MediaSession
            mediaBrowser?.sessionToken.also { token ->
                // Create a MediaControllerCompat
                val mediaController = token?.let {
                    MediaControllerCompat(
                        this@MainActivity, // Context
                        it
                    )
                }
                musicViewModel.mediaBrowser = mediaBrowser
                musicViewModel.mediaController = mediaController
                val extras = mediaBrowser?.extras
                if (extras != null) {
                    getInitData(extras)
                }
                MediaControllerCompat.setMediaController(this@MainActivity, mediaController)
                mediaController?.registerCallback(callback)
            }
        }

        override fun onConnectionSuspended() {
        }

        override fun onConnectionFailed() {
            // The Service has refused our connection
        }

    }

    fun getInitData(resultData: Bundle) {

        resultData.getParcelableArrayList<MusicItem>("musicQueue")?.also {
            musicViewModel.musicQueue.clear()
            musicViewModel.musicQueue.addAll(it)
        }
        resultData.getParcelableArrayList<MusicItem>("songsList")?.also {
            Log.i("TAG", "getInitData: ${it.size}")
            musicViewModel.songsList.clear()
            musicViewModel.songsList.addAll(it)
        }
        resultData.getParcelableArrayList<MainTab>("mainTabList")?.also {
            musicViewModel.mainTabList.clear()
            musicViewModel.mainTabList.addAll(it)
        }
        resultData.getSerializable("playListCurrent")?.also {
            musicViewModel.playListCurrent.value = it as AnyListBase
        }
        val isPlaying = resultData.getBoolean("isPlaying")
        musicViewModel.playStatus.value = isPlaying
        val index = resultData.getInt("index")
        if (index >= 0 && musicViewModel.musicQueue.size > index
        ) {
            musicViewModel.currentMusicCover.value = null
            musicViewModel.currentPlay.value =
                musicViewModel.musicQueue[index]
            musicViewModel.currentPlayQueueIndex.intValue = index
            musicViewModel.currentDuration.longValue =
                musicViewModel.currentPlay.value?.duration ?: 0
            if (musicViewModel.currentPlay.value != null) {
                musicViewModel.dealLyrics(
                    this@MainActivity,
                    musicViewModel.currentPlay.value!!
                )
            }
        }
        val pitch = resultData.getFloat("pitch", 1f)
        val speed = resultData.getFloat("speed", 1f)
        musicViewModel.pitch.floatValue = pitch
        musicViewModel.speed.floatValue = speed
        musicViewModel.sleepTime.longValue =
            resultData.getLong("sleepTime", 0)
        musicViewModel.remainTime.longValue =
            resultData.getLong("remaining")

        musicViewModel.enableEqualizer.value =
            resultData.getBoolean("equalizerEnable")
        val equalizerValue =
            resultData.getIntArray("equalizerValue") ?: intArrayOf(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0
            )
        musicViewModel.equalizerBands.forEachIndexed { index1, _ ->
            musicViewModel.equalizerBands[index1].value =
                equalizerValue[index1]
        }
        musicViewModel.delayTime.floatValue = resultData.getFloat("delayTime")
        musicViewModel.decay.floatValue = resultData.getFloat("decay")
        musicViewModel.enableEcho.value = resultData.getBoolean("echoActive")
        musicViewModel.echoFeedBack.value = resultData.getBoolean("echoFeedBack")
        musicViewModel.repeatModel.intValue = resultData.getInt("repeat", Player.REPEAT_MODE_ALL)
        musicViewModel.playCompleted.value =
            resultData.getBoolean("play_completed")
        getSeek()
    }


    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun BaseLayout(
    ) {
        val navController = rememberNavController()
        musicViewModel.navController = navController
        val pagerState = rememberPagerState { musicViewModel.mainTabList.size }
        NavHost(
            navController = navController, startDestination = Router.MainView.route,
        ) {
            composable(route = Router.MainView.route) {
                key(Router.MainView.route) {
                    if (musicViewModel.mainTabList.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter =
                                painterResource(id = R.drawable.launcher_image),
                                contentDescription = "launching",
                                modifier = Modifier
                                    .padding(0.dp)
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
                                alignment = Alignment.BottomEnd,
                                contentScale = ContentScale.FillBounds
                            )
                        }
                    } else {
                        MainView(navController, pagerState)
                    }
                }
            }
            composable(
                route = Router.MusicPlayerView.route,
            ) { _ ->
                key(Router.MusicPlayerView.route) {
                    PlayingPage(navController, viewModel = musicViewModel)
                }
            }
            composable(
                route = Router.PlayListView.withArgs("{id}", "{itemType}"), arguments = listOf(),
            ) { backStackEntry ->
                val arg = backStackEntry.arguments

                key(Unit) {
                    if (arg != null) {
                        TracksListPage(
                            musicViewModel = musicViewModel,
                            navController,
                            stringToEnumForPlayListType(arg.getString("itemType") ?: ""),
                            arg.getString("id")?.toLong() ?: 0
                        )
                    }
                }
            }
            composable(
                route = Router.TracksSelectPage.withArgs("{id}", "{name}"), arguments = listOf(),
            ) { backStackEntry ->
                val arg = backStackEntry.arguments
                key(Unit) {
                    if (arg != null) {
                        TracksSelectPage(
                            musicViewModel = musicViewModel,
                            navController,
                            arg.getString("name"),
                            arg.getString("id")?.toLong()
                        )
                    }
                }
            }
            composable(
                route = Router.EditTrackPage.withArgs("{id}"), arguments = listOf(),
            ) { backStackEntry ->
                val arg = backStackEntry.arguments
                key(Unit) {
                    if (arg != null) {
                        val id = arg.getString("id")?.toLong()
                        if (id != null) {
                            EditTrackPage(
                                musicViewModel = musicViewModel,
                                navController,
                                id
                            )
                        }
                    }
                }
            }
            composable(
                route = Router.SettingsPage.route, arguments = listOf(),
            ) { _ ->
                key(Unit) {
                    SettingsPage(
                        musicViewModel = musicViewModel,
                        navController,
                    )
                }
            }
            composable(
                route = Router.QueuePage.route, arguments = listOf(),
            ) { _ ->
                key(Unit) {
                    QueuePage(
                        musicViewModel = musicViewModel,
                        navController,
                    )
                }
            }
            composable(
                route = Router.SearchPage.route, arguments = listOf(),
            ) { _ ->
                key(Unit) {
                    SearchPage(
                        musicViewModel = musicViewModel,
                        navController,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MainView(navController: NavHostController, pagerState: PagerState) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        key(
            musicViewModel, navController, drawerState, pagerState
        ) {
            BackHandler(enabled = drawerState.isOpen) {
                if (drawerState.isOpen) {
                    scope.launch {
                        drawerState.close()
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
            ) {
                Row {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            DrawMenu(
                                pagerState,
                                drawerState,
                                navController,
                                musicViewModel,
                                this@MainActivity
                            )
                        },
                    ) {
                        Scaffold(modifier = Modifier,
                            topBar = {
                                MainTopBar(drawerState, pagerState, navController)
                            }, bottomBar = {
                                Bottom(
                                    musicViewModel, navController
                                )
                            }, content = {
                                HorizontalPager(
                                    state = pagerState,
                                    Modifier
                                        .fillMaxSize()
                                        .padding(it),
                                ) { it1 ->
                                    key(it1) {
                                        when (musicViewModel.mainTabList[it1].type) {
                                            PlayListType.Songs -> {
                                                TracksListView(
                                                    Modifier.fillMaxHeight(),
                                                    musicViewModel,
                                                    SongsPlayList,
                                                    musicViewModel.songsList
                                                )
                                            }

                                            PlayListType.PlayLists -> {
                                                PlayListView(
                                                    Modifier.fillMaxHeight(),
                                                    musicViewModel,
                                                    navController,
                                                )
                                            }

                                            PlayListType.Queue -> {
                                                TracksListView(
                                                    Modifier.fillMaxHeight(),
                                                    musicViewModel,
                                                    QueuePlayList,
                                                    tracksList = musicViewModel.musicQueue
                                                )
                                            }

                                            PlayListType.Albums -> {
                                                AlbumGridView(
                                                    Modifier.fillMaxHeight(),
                                                    musicViewModel,
                                                    navController
                                                )
                                            }

                                            PlayListType.Artists -> {
                                                ArtistsGridView(
                                                    Modifier.fillMaxHeight(),
                                                    musicViewModel,
                                                    navController,
                                                )
                                            }

                                            PlayListType.Genres -> {
                                                GenreGridView(
                                                    Modifier.fillMaxHeight(),
                                                    musicViewModel,
                                                    navController,
                                                )
                                            }

                                            PlayListType.Folders -> {
                                                FolderListView(
                                                    Modifier.fillMaxHeight(),
                                                    musicViewModel,
                                                    navController,
                                                )
                                            }

                                            else -> {

                                            }
                                        }
                                    }

                                }

                            })
                    }
                }
            }
        }

    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MainTopBar(
        drawerState: DrawerState,
        pagerState: PagerState,
        navController: NavHostController
    ) {
        val scope = rememberCoroutineScope()
        var showDialog by remember { mutableStateOf(false) }
        var showCreatePlayListDialog by remember { mutableStateOf(false) }
        val timerIcon: Int = if (musicViewModel.remainTime.longValue == 0L) {
            R.drawable.set_timer
        } else {
            R.drawable.setted_timer
        }

        Column {
            if (showDialog) {
                SleepTimeDialog(musicViewModel, onDismiss = {
                    showDialog = false
                })
            }
            if (showCreatePlayListDialog) {
                CreatePlayListDialog(musicViewModel, onDismiss = {
                    showCreatePlayListDialog = false
                    if (!it.isNullOrEmpty()) {
                        navController.navigate(
                            Router.TracksSelectPage.withArgs("-1", it)
                        )
                    }
                })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    scope.launch {
                        drawerState.apply {
                            if (isClosed) open() else close()
                        }
                    }
                }) {
                    Icon(Icons.Filled.Menu, contentDescription = "menu")
                }
                Row {
                    if (musicViewModel.mainTabList[pagerState.currentPage].type == PlayListType.PlayLists) {
                        IconButton(
                            modifier = Modifier.semantics {
                                contentDescription = "Add PlayList"
                            }, onClick = {
                                showCreatePlayListDialog = true
                            }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add PlayList",
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape),
                            )
                        }
                    }
                    IconButton(modifier = Modifier.semantics {
                        contentDescription = "Set sleep time"
                    }, onClick = {
                        showDialog = true
                    }) {
                        Image(
                            painter = painterResource(timerIcon),
                            contentDescription = "Sleeper time",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape),
                            colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
                        )
                    }
                    IconButton(modifier = Modifier.semantics {
                        contentDescription = "Search"
                    }, onClick = {
                        navController.navigate(
                            Router.SearchPage.route
                        )
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            }
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        TabRowDefaults.Indicator(
                            Modifier
                                .height(3.0.dp)
                                .tabIndicatorOffset(tabPositions[pagerState.currentPage]),
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
                musicViewModel.mainTabList.forEachIndexed { index, item ->
                    Tab(selected = pagerState.currentPage == index, onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
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

}
