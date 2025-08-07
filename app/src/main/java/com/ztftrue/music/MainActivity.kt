package com.ztftrue.music

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.LibraryResult.RESULT_ERROR_IO
import androidx.media3.session.LibraryResult.RESULT_ERROR_PERMISSION_DENIED
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.ztftrue.music.play.MediaItemUtils
import com.ztftrue.music.play.PlayService
import com.ztftrue.music.play.PlayService.Companion.COMMAND_GET_INITIALIZED_DATA
import com.ztftrue.music.play.PlayService.Companion.COMMAND_TRACK_DELETE
import com.ztftrue.music.sqlData.model.ARTIST_TYPE
import com.ztftrue.music.sqlData.model.GENRE_TYPE
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.sqlData.model.SortFiledData
import com.ztftrue.music.sqlData.model.StorageFolder
import com.ztftrue.music.ui.home.BaseLayout
import com.ztftrue.music.ui.theme.MusicPitchTheme
import com.ztftrue.music.utils.OperateTypeInActivity
import com.ztftrue.music.utils.SharedPreferencesUtils
import com.ztftrue.music.utils.TracksUtils
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.deleteTrackUpdate
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.trackManager.PlaylistManager
import com.ztftrue.music.utils.trackManager.SongsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock


@Suppress("DEPRECATION")
@UnstableApi
class MainActivity : ComponentActivity() {

    private val musicViewModel: MusicViewModel by viewModels()
    private var jobSeek: Job? = null
    private val scopeMain = CoroutineScope(Dispatchers.Main)
    private var lyricsPath = ""
    val bundle = Bundle()

    private lateinit var browserFuture: ListenableFuture<MediaBrowser>
//    private val browser: MediaBrowser?
//        get() = if (browserFuture.isDone && !browserFuture.isCancelled) {
//            browserFuture.get()
//        } else {
//            null
//        }

    // ... 其他代码 ...

    @JvmField
    val modifyMediaLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val resolver: ContentResolver = contentResolver
                val action = bundle.getString("action")
                if (OperateTypeInActivity.DeletePlayList.name == action) {
                    val u = bundle.getParcelable<Uri>("uri")
                    if (u != null) {
                        resolver.delete(u, null, null)
                        resolver.notifyChange(u, object : ContentObserver(null) {
                            override fun onChange(selfChange: Boolean) {
                                super.onChange(selfChange)
                                SongsUtils.refreshPlaylist(musicViewModel)
                            }
                        })
                    }
                } else if (OperateTypeInActivity.InsertTrackToPlaylist.name == action) {
                    val u = bundle.getParcelable<Uri>("uri")
                    val v = bundle.getParcelableArrayList<MusicItem>("values")
                    val id = bundle.getLong("id")
                    val removeDuplicate = bundle.getBoolean("removeDuplicate")
                    if (u != null && v != null) {
//                        resolver.bulkInsert(u, v.toTypedArray())
                        if (PlaylistManager.addMusicsToPlaylist(
                                this@MainActivity,
                                id,
                                v,
                                removeDuplicate,
                                true
                            )
                        ) {
                            resolver.notifyChange(u, object : ContentObserver(null) {
                                override fun onChange(selfChange: Boolean) {
                                    super.onChange(selfChange)
                                    SongsUtils.refreshPlaylist(musicViewModel)
                                }
                            })
                        }
                    }
                } else if (OperateTypeInActivity.RenamePlaylist.name == action) {
                    val u = bundle.getParcelable<Uri>("uri")
                    val v = bundle.getParcelable<ContentValues>("values")
                    if (u != null && v != null) {
                        resolver.update(u, v, null, null)
                    }
                } else if (OperateTypeInActivity.ModifyTrackFromPlayList.name == action) {
                    val playListPath = bundle.getString("playListPath", "")
                    val uri = bundle.getString("uri", "")
                    val arrayList: ArrayList<MusicItem> =
                        bundle.getParcelableArrayList("list") ?: ArrayList()
                    if (playListPath.isNotEmpty()) {
                        SongsUtils.resortOrRemoveTrackFromM3U(
                            this@MainActivity,
                            uri.toUri(),
                            playListPath,
                            arrayList
                        )
                        MediaScannerConnection.scanFile(
                            this@MainActivity,
                            arrayOf<String>(playListPath),
                            arrayOf("*/*"),
                            object : MediaScannerConnectionClient {
                                override fun onMediaScannerConnected() {}
                                override fun onScanCompleted(path: String, uri: Uri) {
                                    SongsUtils.refreshPlaylist(musicViewModel)
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
                        val futureResult: ListenableFuture<SessionResult>? =
                            musicViewModel.browser?.sendCustomCommand(
                                COMMAND_TRACK_DELETE,
                                bundleTemp
                            )
                        futureResult?.addListener({
                            try {
                                // a. 获取 SessionResult
                                val sessionResult = futureResult.get()
                                // b. 检查操作是否成功
                                if (sessionResult.resultCode == SessionResult.RESULT_SUCCESS) {
                                    deleteTrackUpdate(musicViewModel, sessionResult.extras)
                                }
                            } catch (e: Exception) {
                                // 处理在获取结果过程中可能发生的异常 (如 ExecutionException)
                                Log.e("Client", "Failed to toggle favorite status", e)
                            }
                        }, ContextCompat.getMainExecutor(this@MainActivity)) // 或者使用主线程的 Executor
                    }
                    return@registerForActivityResult
                } else if (OperateTypeInActivity.EditTrackInfo.name == action) {
                    musicViewModel.editTrackEnable.value = true
                }
                SongsUtils.refreshPlaylist(musicViewModel)
                bundle.clear()
            } else {
                bundle.clear()
                Toast.makeText(this@MainActivity, "Need permission", Toast.LENGTH_SHORT).show()
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
                        lyricsPath += if (name.lowercase().endsWith(".lrc")) {
                            "lrc"
                        } else if (name.lowercase().endsWith(".srt")) {
                            "srt"
                        } else if (name.lowercase().endsWith(".vtt")) {
                            "vtt"
                        } else if (name.lowercase().endsWith(".txt")) {
                            "txt"
                        } else {
                            return@registerForActivityResult
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
    private val roseImagePickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedFileUri: Uri? = result.data?.data
                if (selectedFileUri != null) {
                    val inputStream = contentResolver.openInputStream(selectedFileUri)
                    if (inputStream != null) {
                        try {
                            //  read file name
                            val fileName =
                                Utils.getFileNameFromUri(this@MainActivity, selectedFileUri)
                                    ?: "cover.jpg"
                            val fileExtension = if (fileName.contains('.')) {
                                fileName.substringAfterLast('.').lowercase()
                            } else {
                                "" // 没有后缀
                            }
                            val folderPath = "cover"
                            val folder = this@MainActivity.getExternalFilesDir(
                                folderPath
                            )
                            folder?.mkdirs()
                            val tempPath: String? =
                                this@MainActivity.getExternalFilesDir(folderPath)?.absolutePath
                            // 创建目标文件
                            val targetFile = File(tempPath, "track_cover.$fileExtension")
                            musicViewModel.customMusicCover.value = targetFile.absolutePath
                            SharedPreferencesUtils.setTrackCoverData(
                                this@MainActivity,
                                targetFile.absolutePath
                            )
                            val outputStream = FileOutputStream(targetFile)
                            // 复制内容
                            inputStream.copyTo(outputStream)

                            // 关闭流
                            inputStream.close()
                            outputStream.close()
                            getSharedPreferences("Widgets", MODE_PRIVATE).getBoolean(
                                "enable",
                                false
                            )
                                .let {
                                    if (it) {
                                        val intent =
                                            Intent(this@MainActivity, PlayMusicWidget::class.java)
                                        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                        intent.putExtra("source", this@MainActivity.packageName)
                                        val ids = AppWidgetManager.getInstance(
                                            application
                                        ).getAppWidgetIds(
                                            ComponentName(
                                                application,
                                                PlayMusicWidget::class.java
                                            )
                                        )
                                        intent.putExtra(
                                            "playingStatus",
                                            musicViewModel.playStatus.value
                                        )
                                        intent.putExtra(
                                            "title",
                                            musicViewModel.currentPlay.value?.name ?: ""
                                        )
                                        intent.putExtra(
                                            "author",
                                            musicViewModel.currentPlay.value?.artist ?: ""
                                        )
                                        intent.putExtra(
                                            "path",
                                            musicViewModel.currentPlay.value?.path ?: ""
                                        )
                                        intent.putExtra(
                                            "id",
                                            musicViewModel.currentPlay.value?.id ?: 0L
                                        )
                                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                                        sendBroadcast(intent)
                                    }
                                }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

            }
        }
    private val coverImagePickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                if (result.data != null) {
                    val selectedFileUri: Uri? = result.data!!.data
                    if (selectedFileUri != null) {
                        val inputStream =
                            this@MainActivity.contentResolver.openInputStream(selectedFileUri)
                        val outputStream = ByteArrayOutputStream()
                        if (inputStream != null) {
                            try {
                                coverBitmap?.value = BitmapFactory.decodeStream(inputStream)
//                                val cache=File(externalCacheDir,"cache1.jpg")
//                                if(cache.exists()){
//                                    cache.delete()
//                                }
//                                cache.createNewFile()
//                                coverBitmap?.value!!.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(cache))
//                                coverBitmap?.value = BitmapFactory.decodeFile(cache.absolutePath)
//                           Bitmap.createBitmap(coverBitmap.value!!.width,coverBitmap.value!!.height,Bitmap.Config.ARGB_8888)
//                                    .compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
//                                BitmapFactory.decodeByteArray(
//                                    outputStream.toByteArray(),
//                                    0,
//                                    outputStream.size()
//                                )
                                inputStream.close()
                                outputStream.close()
                            } catch (e: Exception) {
                                inputStream.close()
                                outputStream.close()
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    val folderPickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val treeUri = result.data?.data
                if (treeUri != null) {
                    contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        musicViewModel.getDb(this@MainActivity).StorageFolderDao().insert(
                            StorageFolder(null, treeUri.toString())
                        )
                        val c = musicViewModel.currentPlay.value
                        if (c != null)
                            musicViewModel.dealLyrics(
                                this@MainActivity,
                                c
                            )
                    }

                }
            }
        }
    val genreFolderPickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val treeUri = result.data?.data
                if (treeUri != null) {
                    contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        musicViewModel.getDb(this@MainActivity).StorageFolderDao().insert(
                            StorageFolder(null, treeUri.toString(), GENRE_TYPE)
                        )
                        musicViewModel.genreCover.clear()
                        musicViewModel.prepareArtistAndGenreCover(this@MainActivity)
                    }
                }
            }
        }
    val artistFolderPickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val treeUri = result.data?.data
                if (treeUri != null) {
                    contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        musicViewModel.getDb(this@MainActivity).StorageFolderDao().insert(
                            StorageFolder(null, treeUri.toString(), ARTIST_TYPE)
                        )
                        musicViewModel.artistCover.clear()
                        musicViewModel.prepareArtistAndGenreCover(this@MainActivity)
                    }
                }
            }
        }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.setData(Uri.fromParts("package", packageName, null))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            finish()
        }
    }


    fun openFilePicker(filePath: String) {
        lyricsPath = filePath
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*" // Specify the MIME type for text files
//        intent.addCategory(Intent.CATEGORY_OPENABLE)
//        intent.type = "text/plain|application/octet-stream";
        filePickerLauncher.launch(intent)
    }

    fun roseImagPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "image/*" // Specify the MIME type for text files
//        intent.addCategory(Intent.CATEGORY_OPENABLE)
//        intent.type = "text/plain|application/octet-stream";
        roseImagePickerLauncher.launch(intent)
    }

    private var coverBitmap: MutableState<Bitmap?>? = null

    /**
     * for edit tracks cover
     */
    fun openImagePicker(bitmap: MutableState<Bitmap?>) {
        coverBitmap = bitmap
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "image/*" // Specify the MIME type for text files
//        intent.addCategory(Intent.CATEGORY_OPENABLE)
//        intent.type = "text/plain|application/octet-stream";
        coverImagePickerLauncher.launch(intent)
    }

    @OptIn(ExperimentalFoundationApi::class)
    private val audioPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            val i = Intent(this@MainActivity, PlayService::class.java)
            startService(i)  // Start the service explicitly
            setContent {
                MusicPitchTheme(musicViewModel) {
                    BaseLayout(musicViewModel, this@MainActivity)
                }
                initializeAndConnect()
            }
        } else {
            compatSplashScreen?.setKeepOnScreenCondition { false }
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
                            text = stringResource(R.string.can_t_find_any_audio_file_i_need_permission_click_here_to_open_settings),
                            fontSize = 20.sp,
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }

    private var compatSplashScreen: SplashScreen? = null
    override fun attachBaseContext(newBase: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            super.attachBaseContext(newBase)
        } else {
            val languageCode = SharedPreferencesUtils.getCurrentLanguage(newBase)
            super.attachBaseContext(newBase.wrapInLocale(languageCode))
        }
    }

    fun Context.wrapInLocale(language: String?): Context {
        if (language.isNullOrEmpty()) {
            return this
        }
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        return createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        val languageCode = SharedPreferencesUtils.getCurrentLanguage(context = this)
//        if (!languageCode.isNullOrEmpty()) {
//            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
//            val localeList = LocaleListCompat.forLanguageTags("zh-CN")
//            AppCompatDelegate.setApplicationLocales(localeList)
//        }
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        compatSplashScreen = installSplashScreen()
        compatSplashScreen?.setKeepOnScreenCondition { musicViewModel.mainTabList.isEmpty() }
        Utils.initSettingsData(musicViewModel, this)
        musicViewModel.prepareArtistAndGenreCover(this@MainActivity)
        val customMusicCoverPath = SharedPreferencesUtils.getTrackCoverData(this@MainActivity)

        musicViewModel.customMusicCover.value = customMusicCoverPath?.takeIf {
            File(it).exists()
        } ?: R.drawable.songs_thumbnail_cover


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {

            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val i = Intent(this@MainActivity, PlayService::class.java)
                startService(i)  // Start the service explicitly
                setContent {
                    MusicPitchTheme(musicViewModel) {
                        BaseLayout(musicViewModel, this@MainActivity)
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
                    BaseLayout(musicViewModel, this@MainActivity)
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

                initializeAndConnect()
            }
        } else if (ActivityCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initializeAndConnect()
        }
    }

    private val browserListener = object : MediaBrowser.Listener {
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            Log.e("DEBUG_LISTENER", ">>>>>>> onCustomCommand RECEIVED! Action: ${command.customAction}")
            if (command.customAction == PlayService.COMMAND_SLEEP_STATE_UPDATE.customAction) {
                val remainTime = args.getLong("remaining")
                musicViewModel.remainTime.longValue = remainTime
                if (remainTime == 0L) {
                    musicViewModel.sleepTime.longValue = 0
                }
            } else if (command.customAction == PlayService.COMMAND_VISUALIZATION_DATA.customAction) {
                val magnitude = args.getFloatArray("magnitude")?.toList()
                if (magnitude != null) {
                    musicViewModel.musicVisualizationData.clear()
                    musicViewModel.musicVisualizationData.addAll(magnitude)
                }
            }
            return super.onCustomCommand(controller, command, args)
        }

        override fun onExtrasChanged(
            controller: MediaController,
            extras: Bundle
        ) {
            super.onExtrasChanged(controller, extras)
        }
    }

    private fun initializeAndConnect() {
        if (::browserFuture.isInitialized) return // 防止重复初始化
        // 1. 创建 SessionToken，指定要连接的 Service
        val sessionToken = SessionToken(this, ComponentName(this, PlayService::class.java))
        // 2. 使用 Builder 构建 MediaBrowser，这是一个异步过程
        browserFuture = MediaBrowser.Builder(this, sessionToken).setListener(browserListener)
            .buildAsync()
        // 3. (关键!) 添加一个监听器来处理连接的结果（成功或失败）
        browserFuture.addListener({
            // 这个代码块会在连接过程完成后（无论成功或失败）在主线程上执行
            try {
                // 检查连接是否成功，并获取 browser 实例
                val connectedBrowser = browserFuture.get()
                if (connectedBrowser != null) {
                    val rootResult: ListenableFuture<LibraryResult<MediaItem>> =
                        connectedBrowser.getLibraryRoot(null)
                    val result: LibraryResult<MediaItem>? = rootResult.get()
                    if (result == null || result.resultCode != LibraryResult.RESULT_SUCCESS) {
                        Log.e(
                            "MyMusicActivity",
                            "Error getting MediaBrowser root${result?.resultCode}"
                        )

                        return@addListener
                    }
                    Log.e(
                        "MyMusicActivity",
                        "${RESULT_ERROR_PERMISSION_DENIED},${RESULT_ERROR_IO},${SessionResult.RESULT_ERROR_NOT_SUPPORTED},${SessionResult.RESULT_ERROR_SESSION_DISCONNECTED},                            ${SessionResult.RESULT_ERROR_SESSION_AUTHENTICATION_EXPIRED},                            ${SessionResult.RESULT_ERROR_SESSION_PREMIUM_ACCOUNT_REQUIRED},                            ${SessionResult.RESULT_ERROR_SESSION_CONCURRENT_STREAM_LIMIT},${SessionResult.RESULT_ERROR_SESSION_PARENTAL_CONTROL_RESTRICTED},${SessionResult.RESULT_ERROR_SESSION_NOT_AVAILABLE_IN_REGION},${SessionResult.RESULT_ERROR_SESSION_SKIP_LIMIT_REACHED},${SessionResult.RESULT_ERROR_SESSION_SETUP_REQUIRED},"
                    )
                    rootResult.addListener({
                        musicViewModel.browser = connectedBrowser
                        onBrowserConnected(connectedBrowser)
                    }, ContextCompat.getMainExecutor(this))
                } else {
                    // 连接失败或被取消
                    onBrowserConnectionFailed()
                }
            } catch (e: Exception) {
                // 在获取 browser 实例时可能抛出异常 (如 ExecutionException)
                Log.e("MyMusicActivity", "Error getting MediaBrowser", e)
                onBrowserConnectionFailed()
            }
        }, ContextCompat.getMainExecutor(this)) // 确保监听器在主线程执行
    }

    public override fun onResume() {
        super.onResume()
        getSeek()
        volumeControlStream = AudioManager.STREAM_MUSIC
        musicViewModel.browser?.let {
            updateUiWithCurrentState(it)
        }
    }

    override fun onPause() {
        jobSeek?.cancel()
        super.onPause()
    }

    public override fun onStop() {
        super.onStop()
        if (::browserFuture.isInitialized) {
            musicViewModel.browser?.removeListener(playerListener)
            MediaBrowser.releaseFuture(browserFuture)
        }
    }

    private fun onBrowserConnected(browser: MediaBrowser) {
        browser.addListener(playerListener) // playerListener 是你定义的 Player.Listener 实例
        updateUiWithCurrentState(browser)

        val futureResult: ListenableFuture<SessionResult>? =
            musicViewModel.browser?.sendCustomCommand(
                COMMAND_GET_INITIALIZED_DATA,
                Bundle().apply { },
            )
        futureResult?.addListener({
            try {
                val sessionResult = futureResult.get()
                if (sessionResult.resultCode == SessionResult.RESULT_SUCCESS) {
                    getInitData(sessionResult.extras)
                }
            } catch (e: Exception) {
                Log.e("Client", "Failed to toggle favorite status", e)
            }
        }, ContextCompat.getMainExecutor(this@MainActivity))
        fetchRootChildren(browser)
        SharedPreferencesUtils.getEnableShuffle(this@MainActivity).also {
            musicViewModel.enableShuffleModel.value = it
        }
    }

    private fun onBrowserConnectionFailed() {
        Log.e("MyMusicActivity", "MediaBrowser connection failed.")
        // Toast.makeText(this, "无法连接到播放服务", Toast.LENGTH_SHORT).show()
    }

    // 示例：获取根目录的子项
    private fun fetchRootChildren(browser: MediaBrowser) {

    }

    // 示例：定义 Player.Listener
    private val playerListener = object : Player.Listener {
        override fun onTimelineChanged(
            timeline: Timeline,
            reason: Int
        ) {
            if (!timeline.isEmpty && TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED == reason) {
                val newQueue = mutableListOf<MediaItem>()
                for (i in 0 until timeline.windowCount) {
                    // a. 获取指定索引的窗口信息，为了效率，我们重用一个 Window 对象
                    val window = timeline.getWindow(i, Timeline.Window())
                    val mediaItem = window.mediaItem
                    newQueue.add(mediaItem)
                }
                val qList: Collection<MusicItem> =
                    (newQueue.map { MediaItemUtils.mediaItemToMusicItem(it) }).filterNotNull()
                val qIndex = musicViewModel.browser?.currentMediaItemIndex ?: 0

                if (qIndex >= qList.size) {
                    Log.e("MusicViewModel", "onTimelineChanged: qIndex >= qList.size")
                    return
                }
                musicViewModel.currentPlayQueueIndex.intValue = qIndex
                musicViewModel.musicQueue.clear()
                musicViewModel.musicQueue.addAll(qList)
                val music = musicViewModel.musicQueue[qIndex]
                if (musicViewModel.enableShuffleModel.value && SharedPreferencesUtils.getAutoToTopRandom(
                        this@MainActivity
                    )
                ) {
                    if (qIndex == 0) return
                    musicViewModel.musicQueue.remove(music)
                    musicViewModel.musicQueue.add(0, music)
                    TracksUtils.currentPlayToTop(
                        musicViewModel.browser!!,
                        musicViewModel.musicQueue,
                        music,
                        qIndex
                    )
                }
            }


            super.onTimelineChanged(timeline, reason)
        }


        override fun onVolumeChanged(volume: Float) {
            val volumeInt = (volume * 100).toInt()
            musicViewModel.volume.intValue = volumeInt
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            musicViewModel.playStatus.value = isPlaying
            if (isPlaying) {
                getSeek()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // 更新歌曲标题和封面
            val index: Int = musicViewModel.browser?.currentMediaItemIndex ?: 0
            val reason = reason
            if (index >= 0 && musicViewModel.musicQueue.size > index) {
                musicViewModel.currentPlay.value = musicViewModel.musicQueue[index]
                musicViewModel.currentPlayQueueIndex.intValue = index
                musicViewModel.currentDuration.longValue = musicViewModel.musicQueue[index].duration
                musicViewModel.scheduleDealCurrentPlay(this@MainActivity, index, reason)
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            super.onTracksChanged(tracks)
            val formatMap = HashMap<String, String>()
            val audioTrackGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
            for (trackGroup in audioTrackGroups) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    formatMap["Codec"] = format.codecs ?: ""
                    formatMap["SampleRate"] = format.sampleRate.toString()
                    formatMap["ChannelCount"] = format.channelCount.toString()
                    formatMap["Bitrate"] = format.bitrate.toString()
                    break
                }
            }
            musicViewModel.currentInputFormat.clear()
            formatMap.forEach { formatItem ->
                musicViewModel.currentInputFormat[formatItem.key] = formatItem.value
            }
        }

    }

    override fun onDestroy() {
        // should invoke in onStop()
        musicViewModel.reset()
        super.onDestroy()
    }

    private val lock = ReentrantLock()
    fun getSeek() {
        lock.lock()
        if (musicViewModel.playStatus.value) {
            jobSeek = scopeMain.launch {
                // i debug it, when i set it while(true),
                // this code is working (when it invoke cancel) also. who can tell me,this is why?
                while (isActive) {
                    delay(1000)
                    val f = musicViewModel.browser?.currentPosition ?: 0
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
        lock.unlock()
    }


    fun getInitData(resultData: Bundle) {
        resultData.getParcelableArrayList<SortFiledData>("showIndicatorList")?.onEach { sort ->
            musicViewModel.showIndicatorMap[sort.type.replace("@Tracks", "")] =
                sort.filedName == "Alphabetical"
        }
        resultData.getParcelableArrayList<MainTab>("mainTabList")?.also {
            musicViewModel.mainTabList.clear()
            musicViewModel.mainTabList.addAll(it)
        }
        resultData.getSerializable("playListCurrent")?.also {
            musicViewModel.playListCurrent.value = it as AnyListBase
        }
//        resultData.getParcelableArrayList<MusicItem>("songsList")?.also {
//            musicViewModel.songsList.clear()
//            musicViewModel.songsList.addAll(it)
//        }
//        val index = resultData.getInt("index")
//        if (index >= 0 && musicViewModel.musicQueue.size > index && index != musicViewModel.currentPlayQueueIndex.intValue
//        ) {
//            musicViewModel.scheduleDealCurrentPlay(
//                this,
//                index,
//                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
//            )
//        }
        val pitch = resultData.getFloat("pitch", 1f)
        val speed = resultData.getFloat("speed", 1f)
        val Q = resultData.getFloat("Q", Utils.Q)
        musicViewModel.Q.floatValue = Q
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
        musicViewModel.sliderPosition.floatValue = resultData.getLong("position", 0L).toFloat()
        // SleepTime wait when play next
        musicViewModel.playCompleted.value =
            resultData.getBoolean("play_completed")
        musicViewModel.volume.intValue = resultData.getInt("volume", 100)
        getSeek()
    }

    /**
     * 根据播放器当前的状态，一次性更新所有相关的UI组件。
     *
     * @param player MediaBrowser 实例，它实现了 Player 接口。
     */
    private fun updateUiWithCurrentState(player: Player) {
        // 1. 更新元数据 (歌曲信息和封面)
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem != null && musicViewModel.musicQueue.isNotEmpty() && musicViewModel.musicQueue.size > player.currentMediaItemIndex
            && player.currentMediaItemIndex >= 0
        ) {
            // &&player.currentMediaItemIndex!=musicViewModel.currentPlayQueueIndex.intValue 如果是和当前相同就不解析
            musicViewModel.currentPlayQueueIndex.intValue = player.currentMediaItemIndex
            musicViewModel.currentPlay.value =
                musicViewModel.musicQueue[musicViewModel.currentPlayQueueIndex.intValue]
            // val uri = currentMediaItem.mediaMetadata.artworkUri
            // if (uri != null) {
            //     albumArtImageView.setImageURI(uri)
            // }
            // val metadata = currentMediaItem.mediaMetadata
            // songTitleTextView.text = metadata.title ?: "未知标题"
            // songArtistTextView.text = metadata.artist ?: "未知艺术家"
        } else {
            musicViewModel.currentPlayQueueIndex.intValue = -1
            musicViewModel.currentPlay.value = null
        }

        // 2. 更新播放/暂停按钮的状态
        musicViewModel.playStatus.value = player.isPlaying
        // 3. 更新进度条和时间
        // 获取总时长 (如果是直播流，可能为 C.TIME_UNSET)
        val duration = if (player.duration == C.TIME_UNSET) 0 else player.duration
        musicViewModel.currentDuration.longValue = duration

        // 获取当前播放位置
        val currentPosition = player.currentPosition
        musicViewModel.currentDuration.longValue = currentPosition

        // 4. 更新随机播放按钮的状态
        musicViewModel.enableShuffleModel.value = player.shuffleModeEnabled
        // 5. 更新重复模式按钮的状态
        musicViewModel.repeatModel.intValue = player.repeatMode
    }


}
