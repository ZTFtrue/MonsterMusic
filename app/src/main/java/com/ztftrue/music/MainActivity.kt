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
import android.os.Looper
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
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.ztftrue.music.play.MediaCommands
import com.ztftrue.music.play.MediaItemUtils
import com.ztftrue.music.play.PlayService
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
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.deleteTrackUpdate
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.trackManager.PlaylistManager
import com.ztftrue.music.utils.trackManager.SongsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale


@Suppress("DEPRECATION")
@UnstableApi
class MainActivity : ComponentActivity() {

    private val musicViewModel: MusicViewModel by viewModels()
    private var jobSeek: Job? = null
    private var lyricsPath = ""
    val bundle = Bundle()

    private lateinit var browserFuture: ListenableFuture<MediaBrowser>


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
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        SongsUtils.refreshPlaylist(musicViewModel)

                                    }
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
                                MediaCommands.COMMAND_TRACK_DELETE,
                                bundleTemp
                            )
                        futureResult?.addListener({
                            try {
                                // a. 获取 SessionResult
                                val sessionResult = futureResult.get()
                                // b. 检查操作是否成功
                                if (sessionResult.resultCode == SessionResult.RESULT_SUCCESS) {
                                    deleteTrackUpdate(musicViewModel)
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
                            musicViewModel.getCurrentMusicCover(this@MainActivity)
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
                        if (c != null){
                            musicViewModel.dealLyrics(
                                this@MainActivity,
                                c
                            )
                        }

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
        intent.data = Uri.fromParts("package", packageName, null)
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
            if (command.customAction == MediaCommands.COMMAND_SLEEP_STATE_UPDATE.customAction) {
                val remainTime = args.getLong("remaining")
                musicViewModel.remainTime.longValue = remainTime
                if (remainTime == 0L) {
                    musicViewModel.sleepTime.longValue = 0
                }
            }
            return super.onCustomCommand(controller, command, args)
        }

    }

    private fun initializeAndConnect() {
        if (musicViewModel.browser != null || (::browserFuture.isInitialized && !browserFuture.isDone)) {
            return
        }
        val sessionToken = SessionToken(this, ComponentName(this, PlayService::class.java))
        browserFuture = MediaBrowser.Builder(this, sessionToken).setListener(browserListener)
            .setApplicationLooper(Looper.getMainLooper())
            .buildAsync()
        browserFuture.addListener({
            try {
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
//                    Log.e(
//                        "MyMusicActivity",
//                        "${RESULT_ERROR_PERMISSION_DENIED},${RESULT_ERROR_IO},${SessionResult.RESULT_ERROR_NOT_SUPPORTED},${SessionResult.RESULT_ERROR_SESSION_DISCONNECTED},                            ${SessionResult.RESULT_ERROR_SESSION_AUTHENTICATION_EXPIRED},                            ${SessionResult.RESULT_ERROR_SESSION_PREMIUM_ACCOUNT_REQUIRED},                            ${SessionResult.RESULT_ERROR_SESSION_CONCURRENT_STREAM_LIMIT},${SessionResult.RESULT_ERROR_SESSION_PARENTAL_CONTROL_RESTRICTED},${SessionResult.RESULT_ERROR_SESSION_NOT_AVAILABLE_IN_REGION},${SessionResult.RESULT_ERROR_SESSION_SKIP_LIMIT_REACHED},${SessionResult.RESULT_ERROR_SESSION_SETUP_REQUIRED},"
//                    )
                    rootResult.addListener({
                        musicViewModel.browser = connectedBrowser
                        onBrowserConnected(connectedBrowser)
                    }, ContextCompat.getMainExecutor(this))
                } else {
                    onBrowserConnectionFailed()
                }
            } catch (e: Exception) {
                Log.e("MyMusicActivity", "Error getting MediaBrowser", e)
                onBrowserConnectionFailed()
            }
        }, ContextCompat.getMainExecutor(this)) // 确保监听器在主线程执行
    }

    public override fun onResume() {
        super.onResume()
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
            musicViewModel.browser = null
        }
    }

    private fun onBrowserConnected(browser: MediaBrowser) {
        browser.addListener(playerListener) // playerListener 是你定义的 Player.Listener 实例
        val futureResult: ListenableFuture<SessionResult>? =
            musicViewModel.browser?.sendCustomCommand(
                MediaCommands.COMMAND_GET_INITIALIZED_DATA,
                Bundle().apply { },
            )
        futureResult?.addListener({
            try {
                val sessionResult = futureResult.get()
                if (sessionResult.resultCode == SessionResult.RESULT_SUCCESS) {
                    getInitData(sessionResult.extras)
                    updateUiWithCurrentState(browser)
                }
            } catch (e: Exception) {
                Log.e("Client", "Failed to toggle favorite status", e)
            }
        }, ContextCompat.getMainExecutor(this@MainActivity))
        fetchRootChildren(browser)
    }

    private fun onBrowserConnectionFailed() {
        Log.e("MyMusicActivity", "MediaBrowser connection failed.")
        // Toast.makeText(this, "无法连接到播放服务", Toast.LENGTH_SHORT).show()
    }

    private fun fetchRootChildren(browser: MediaBrowser) {

    }

    private val playerListener = object : Player.Listener {
        override fun onTimelineChanged(
            timeline: Timeline,
            reason: Int
        ) {
            if (!timeline.isEmpty && TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED == reason) {
                val newQueue = mutableListOf<MediaItem>()
                for (i in 0 until timeline.windowCount) {
                    val window = timeline.getWindow(i, Timeline.Window())
                    val mediaItem = window.mediaItem
                    newQueue.add(mediaItem)
                }
                val qList: ArrayList<MusicItem> =
                    ArrayList((newQueue.map { MediaItemUtils.mediaItemToMusicItem(it) }).filterNotNull())
                musicViewModel.musicQueue.clear()
                musicViewModel.musicQueue.addAll(qList)
            }
            super.onTimelineChanged(timeline, reason)
        }


        override fun onVolumeChanged(volume: Float) {
            val volumeInt = (volume * 100).toInt()
            musicViewModel.volume.intValue = volumeInt
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            musicViewModel.playStatus.value = isPlaying
            getSeek()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index: Int = musicViewModel.browser?.currentMediaItemIndex ?: 0
            val reason = reason
            if (index >= 0 && musicViewModel.musicQueue.size > index) {
                musicViewModel.scheduleDealCurrentPlay(this@MainActivity, index, mediaItem, reason)
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

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            @Player.DiscontinuityReason reason: Int
        ) {
            super.onPositionDiscontinuity(oldPosition, newPosition, reason)

            // 我们通常只关心由 seek 引起的跳转
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                val actualNewPositionMs = newPosition.positionMs // 获取精确的新位置
//                val actualNewWindowIndex = newPosition.windowIndex // 获取精确的新窗口索引
                musicViewModel.sliderPosition.floatValue = actualNewPositionMs.toFloat()
                // (可选) 广播给 ViewModel，更新 UI 进度条的 LiveData
                // (this@PlayService.application as? Application)?.let { app ->
                //     val viewModel = ViewModelProvider(app).get(MusicPlayerViewModel::class.java)
                //     viewModel.onPositionDiscontinuity(actualNewPositionMs) // 假设 ViewModel 有这个方法
                // }
            }
            // 您也可以在其他 reason (如 MEDIA_ITEM_TRANSITION) 中处理位置更新，
            // 但 DISCONTINUITY_REASON_SEEK 是最明确的。
        }
    }

    override fun onDestroy() {
        musicViewModel.reset()
        super.onDestroy()
    }

    private val mutex = Mutex()
    private val seekScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun getSeek() {
        seekScope.launch {
            mutex.withLock {
                if (musicViewModel.playStatus.value) {
                    if (jobSeek?.isActive == true) return@withLock
                    jobSeek = launch {
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
                    jobSeek = null
                }
            }
        }
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
        val pitch = resultData.getFloat("pitch", 1f)
        val speed = resultData.getFloat("speed", 1f)
        val q = resultData.getFloat("Q", Utils.Q)
        musicViewModel.equalizerQ.floatValue = q
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
        musicViewModel.volume.intValue = resultData.getInt("volume", 100)
    }

    private fun updateUiWithCurrentState(player: MediaBrowser) {
        musicViewModel.musicQueue.clear()
        musicViewModel.musicQueue.addAll(getCurrentPlaylist(player).mapNotNull {
            MediaItemUtils.mediaItemToMusicItem(
                it
            )
        })
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem != null && musicViewModel.musicQueue.isNotEmpty() && musicViewModel.musicQueue.size > player.currentMediaItemIndex
            && player.currentMediaItemIndex >= 0
        ) {
            val futureResult: ListenableFuture<SessionResult>? =
                musicViewModel.browser?.sendCustomCommand(
                    MediaCommands.COMMAND_GET_CURRENT_PLAYLIST,
                    Bundle().apply { },
                )
            futureResult?.addListener({
                try {
                    val sessionResult = futureResult.get()
                    if (sessionResult.resultCode == SessionResult.RESULT_SUCCESS) {
                        musicViewModel.playListCurrent.value =
                            sessionResult.extras.getParcelable("playList")
                    }
                } catch (e: Exception) {
                    Log.e("Client", "Failed to toggle favorite status", e)
                }
            }, ContextCompat.getMainExecutor(this@MainActivity))
            val currentIndex = player.currentMediaItemIndex
            val currentMusic = musicViewModel.currentPlay.value
            if (musicViewModel.musicQueue.size > currentIndex) {
                if (currentMusic == null || currentMusic.id != musicViewModel.musicQueue[currentIndex].id) {
                    musicViewModel.currentCaptionList.clear()
                    musicViewModel.currentPlay.value =
                        musicViewModel.musicQueue[currentIndex]
                    musicViewModel.getCurrentMusicCover(this@MainActivity)
                    musicViewModel.dealLyrics(
                        this@MainActivity,
                        musicViewModel.musicQueue[player.currentMediaItemIndex]
                    )
                }
                getSeek()
            }
        } else {
            musicViewModel.currentPlay.value = null
        }
        musicViewModel.playStatus.value = player.isPlaying
        val currentPosition = player.currentPosition
        musicViewModel.sliderPosition.floatValue = currentPosition.toFloat()
        musicViewModel.enableShuffleModel.value = player.shuffleModeEnabled
        musicViewModel.repeatModel.intValue = player.repeatMode
    }

    fun getCurrentPlaylist(browser: MediaBrowser): List<MediaItem> {
        val timeline: Timeline = browser.currentTimeline
        if (timeline.isEmpty) {
            return emptyList()
        }
        val playlist = mutableListOf<MediaItem>()
        val window = Timeline.Window() // 创建一个可重用的 Window 对象以提高效率
        for (i in 0 until timeline.windowCount) {
            timeline.getWindow(i, window)
            playlist.add(window.mediaItem)
        }
        return playlist
    }
}
