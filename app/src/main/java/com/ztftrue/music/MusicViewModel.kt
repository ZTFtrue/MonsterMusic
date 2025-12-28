package com.ztftrue.music

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.ztftrue.music.play.AudioDataRepository
import com.ztftrue.music.play.CustomMetadataKeys
import com.ztftrue.music.play.manager.MediaCommands
import com.ztftrue.music.play.MediaItemUtils
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.model.ARTIST_TYPE
import com.ztftrue.music.sqlData.model.DictionaryApp
import com.ztftrue.music.sqlData.model.GENRE_TYPE
import com.ztftrue.music.sqlData.model.LYRICS_TYPE
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.sqlData.model.StorageFolder
import com.ztftrue.music.ui.play.Lyrics
import com.ztftrue.music.utils.CaptionUtils
import com.ztftrue.music.utils.CaptionUtils.getLyricsTypeFromExtension
import com.ztftrue.music.utils.CaptionUtils.splitStringIntoWordsAndSymbols
import com.ztftrue.music.utils.LyricsSettings.FIRST_EMBEDDED_LYRICS
import com.ztftrue.music.utils.LyricsType
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.SharedPreferencesName.LYRICS_SETTINGS
import com.ztftrue.music.utils.Utils
import com.ztftrue.music.utils.Utils.getCover
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.model.Caption
import com.ztftrue.music.utils.model.EqualizerBand
import com.ztftrue.music.utils.model.ListStringCaption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.locks.ReentrantLock


var SongsPlayList = AnyListBase(-1, PlayListType.Songs)
var QueuePlayList = AnyListBase(-1, PlayListType.Queue)

sealed class ImageSource {
    data class Resource(@DrawableRes val id: Int) : ImageSource()
    data class FilePath(val path: String) : ImageSource()
    data class BitmapFile(val bitmap: Bitmap) : ImageSource()

    fun asModel(): Any = when (this) {
        is Resource -> id
        is FilePath -> File(path)
        is BitmapFile -> bitmap
    }
}

class MusicViewModel : ViewModel() {

    var db: MusicDatabase? = null
    val refreshPlayList = mutableStateOf(false)
    val refreshAlbum = mutableStateOf(false)
    val refreshArtist = mutableStateOf(false)
    val refreshGenre = mutableStateOf(false)
    val refreshFolder = mutableStateOf(false)
    lateinit var navController: SnapshotStateList<Any>
    var themeSelected = mutableIntStateOf(0)
    var editTrackEnable = mutableStateOf(false)
    private val _visualizationData = MutableLiveData<List<Float>>()
    val visualizationData: LiveData<List<Float>> = _visualizationData
    var loadingTracks = mutableStateOf(false)


    val folderViewTree = mutableStateOf(false)
    val folderViewShowPath = mutableStateOf(false)

    //    val albumItemsCount = mutableIntStateOf(2)
//    val genreItemsCount = mutableIntStateOf(2)
//    var mediaBrowser: MediaBrowserCompat? = null
    var browser: MediaBrowser? = null
    var currentInputFormat =
        mutableStateMapOf<String, String>() //mutableStateOf<LinkedHashMap<String, String>>(java.util.LinkedHashMap())

    //    val albumScrollDirection = mutableStateOf(ScrollDirectionType.GRID_VERTICAL)
//    val artistScrollDirection = mutableStateOf(ScrollDirectionType.GRID_VERTICAL)
//    val genreScrollDirection = mutableStateOf(ScrollDirectionType.GRID_VERTICAL)
    var musicVisualizationEnable = mutableStateOf(false)
    var showMusicCover = mutableStateOf(false)
    var customMusicCover = mutableStateOf<Any>(R.drawable.songs_thumbnail_cover)

    // 当前播放的列表，应该换数据结构存储，每个列表设置变量 播放状态，album和 genres 也是，艺术家跳转到 album， 然后在下一步处理
    // 每次播放仅设置当前列表的状态
    var musicQueue = mutableStateListOf<MusicItem>()

    var currentMusicCover =
        mutableStateOf<ImageSource>(ImageSource.Resource(R.drawable.songs_thumbnail_cover))
    var currentPlay = mutableStateOf<MusicItem?>(null)
    var needRefreshTheme = mutableStateOf(false)

    //    var currentPlayQueueIndex = mutableIntStateOf(-1)
    var songsList = mutableStateListOf<MusicItem>()
    var playListCurrent = mutableStateOf<AnyListBase?>(null)

    var delayTime = mutableFloatStateOf(0.5f)
    var decay = mutableFloatStateOf(1f)
    var echoFeedBack = mutableStateOf(false)
    var enableShuffleModel = mutableStateOf(false)
    var pitch = mutableFloatStateOf(1f)
    var equalizerQ = mutableFloatStateOf(Utils.Q)
    var speed = mutableFloatStateOf(1f)

    var virtualStrength = mutableIntStateOf(0)
    var enableVirtual = mutableStateOf(false)

    var mainTabList = mutableStateListOf<MainTab>()

    var sliderPosition = mutableFloatStateOf(0F)
    var sliderTouching = false
    var autoScroll = mutableStateOf(true)
    var autoHighLight = mutableStateOf(true)

    var dictionaryAppList = mutableStateListOf<DictionaryApp>()

    var enableEqualizer = mutableStateOf(false)
    var enableEcho = mutableStateOf(false)

    var playStatus = mutableStateOf(false)
    var equalizerBands = mutableStateListOf<EqualizerBand>()
    var showIndicatorMap = mutableStateMapOf<String, Boolean>()

    var artistCover = mutableStateMapOf<String, Uri>()
    var genreCover = mutableStateMapOf<String, Uri>()

    // lyrics
    var itemDuration: Long = 1
    var lyricsType: LyricsType = LyricsType.TEXT
    var currentCaptionList = mutableStateListOf<ListStringCaption>()
    var currentCaptionListLoading = mutableStateOf(false)
    var isEmbeddedLyrics = mutableStateOf(false)
    var fontSize = mutableIntStateOf(18)
    var textAlign = mutableStateOf(TextAlign.Center)

    var showSlideIndicators = mutableStateOf(false)

    // sleep time
    var sleepTime = mutableLongStateOf(0L)
    var remainTime = mutableLongStateOf(0L)
    var playCompleted = mutableStateOf(false)
    var volume = mutableIntStateOf(100)
    var repeatModel = mutableIntStateOf(Player.REPEAT_MODE_ALL)
    fun reset() {
        if (currentPlay.value != null) {
            currentCaptionList.clear()
        }
        currentMusicCover.value = ImageSource.Resource(R.drawable.songs_thumbnail_cover)
        currentPlay.value = null
        playListCurrent.value = null
        musicQueue.clear()
        songsList.clear()
        mainTabList.clear()
        sliderPosition.floatValue = 0f
        sleepTime.longValue = 0
        remainTime.longValue = 0
        playCompleted.value = false
        repeatModel.intValue = Player.REPEAT_MODE_ALL
    }

    private fun subscribeToVisualizationData() {
        viewModelScope.launch {
            // a. 订阅 (collect) 来自 Repository 的数据流
            AudioDataRepository.visualizationDataFlow
                // b. (可选) 使用 .flowOn(Dispatchers.Default) 可以在后台线程处理数据转换
                // c. (可选) 使用 .buffer() 进一步增强背压处理
                .collectLatest { fftArray -> // collectLatest 确保我们只处理最新的数据
                    // d. 当收到新数据时，这个代码块会被执行
                    //    我们在这里将 FloatArray 转换为 List<Float> 并更新 LiveData
                    _visualizationData.postValue(fftArray.toList())
                }
        }
    }

    init {
        subscribeToVisualizationData()
        val band = ArrayList<EqualizerBand>()
        for (index in 0 until Utils.bandsCenter.size) {
            val hz = if (Utils.bandsCenter[index] < 1000) {
                "${Utils.bandsCenter[index]}"
            } else {
                "${Utils.bandsCenter[index] / 1000}k"
            }
            band.add(EqualizerBand(index, hz, 0))
        }
        equalizerBands.addAll(band)
    }

    fun getDb(context: Context): MusicDatabase {
        if (db == null) {
            db = MusicDatabase.getDatabase(context)
        }
        return db!!
    }

    val tags = mutableStateMapOf<String, String>()
    private val lock = ReentrantLock()
    private var lyricsJob: Job? = null
    private var dealCurrentPlayJob: Job? = null

    fun scheduleDealCurrentPlay(context: Context, index: Int, mediaItem: MediaItem?, reason: Int) {
        currentCaptionListLoading.value = true
        dealCurrentPlayJob?.cancel()
        dealCurrentPlayJob = viewModelScope.launch {
            // when reason is Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
            // sometimes index is 0 but this is not the real index, because User click the item is not 0
            if (index == 0 && reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                delay(100)
            }

            sliderPosition.floatValue = 0f
            currentCaptionList.clear()
            if (mediaItem != null) {
                val music = MediaItemUtils.mediaItemToMusicItem(mediaItem)
                currentPlay.value = music
                if (music != null) {
                    dealLyrics(context, music)
                    getCurrentMusicCover(context)
                }
            }
        }
    }

    fun getDocumentFileFromPath(
        context: Context,
        treeUri: Uri,
        relativePath: String
    ): DocumentFile? {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val targetDocumentId = "$treeDocumentId/$relativePath"
        val targetFileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, targetDocumentId)
        return DocumentFile.fromSingleUri(context, targetFileUri)
    }

    fun matchedExtension(fileName: String, musicName: String, ext: String): String? {
        val lowerName = fileName.lowercase()
        val lowerMusic = musicName.lowercase()
        val lowerExt = ext.lowercase()

        // 必须以扩展名结尾
        if (!lowerName.endsWith(lowerExt)) return null

        val baseName = lowerName.removeSuffix(lowerExt)

        // 完全匹配
        if (baseName == lowerMusic) return lowerExt

        // 带语言标记
        val regex = Regex("^${Regex.escape(lowerMusic)}\\.[a-z0-9]{2,8}(-[a-z0-9]{2,8})?$")
        return if (regex.matches(baseName)) lowerExt else null
    }

    fun loadLyrics(
        context: Context,
        storageFolder: StorageFolder,
        musicName: String,
        fileLyrics: MutableList<ListStringCaption>
    ): Boolean {
        val treeUri = storageFolder.uri.toUri()

        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is SecurityException) {
                storageFolder.id?.let { getDb(context).StorageFolderDao().deleteById(it) }
            }
        }

        // 这里假设 pickedDir 是 musicName 所在的目录
        // 我们直接构造出可能的歌词文件名并尝试匹配
        val extensions = listOf(".lrc", ".srt", ".vtt", ".txt")
        for (ext in extensions) {
            val relativePath = "$musicName$ext"
            val targetFile = getDocumentFileFromPath(context, treeUri, relativePath)
            if (targetFile != null && targetFile.isFile && targetFile.canRead()) {
                val type = getLyricsTypeFromExtension(ext)
                fileLyrics.addAll(
                    fileRead(targetFile.uri, context, type)
                )
                return true
            }
        }
        // for vtt any language
        val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
        pickedDir?.listFiles()?.forEach { file ->
            if (!file.isFile || !file.canRead()) return@forEach
            val name = file.name ?: return@forEach
            val match = matchedExtension(name, musicName, ".vtt")
            if (match != null) {
                fileLyrics.addAll(fileRead(file.uri, context, LyricsType.VTT))
                return true
            }
        }
        return false
    }

    fun dealLyrics(context: Context, currentPlay: MusicItem) {
        lock.lock()
        currentCaptionListLoading.value = true
        currentCaptionList.clear()
        lyricsType = LyricsType.TEXT
        if (lyricsJob != null && lyricsJob?.isActive == true) {
            lyricsJob?.cancel()
        }
        lyricsJob = CoroutineScope(Dispatchers.IO).launch {
            val regexPattern = Regex("[<>\"/~'{}?,+=)(^&*%!@#$]")
            val artistsFolder = currentPlay.artist.replace(
                regexPattern,
                "_"
            )
            val folderPath = "$Lyrics/$artistsFolder"
            val folder = context.getExternalFilesDir(
                folderPath
            )
            folder?.mkdirs()
            val id = currentPlay.name.replace(regexPattern, "_")
            val path = "${context.getExternalFilesDir(folderPath)?.absolutePath}/$id"
            val firstEmbeddedLyrics =
                context.getSharedPreferences(LYRICS_SETTINGS, Context.MODE_PRIVATE)
                    .getBoolean(FIRST_EMBEDDED_LYRICS, false)
            val embeddedLyrics = arrayListOf<ListStringCaption>()
            val fileLyrics = arrayListOf<ListStringCaption>()
            tags.clear()
            // if embedded lyrics is prior, first import lyrics
            if (firstEmbeddedLyrics) {
                embeddedLyrics.addAll(
                    CaptionUtils.getEmbeddedLyrics(
                        currentPlay.path,
                        context,
                        tags
                    )
                )
            }
            if (embeddedLyrics.isNotEmpty()) {
                lyricsType = LyricsType.TEXT
            } else {
                // import lyrics
                val fileInternal = Utils.checkLyrics(path)
                if (fileInternal != null) {
                    lyricsType = fileInternal.type
                    fileLyrics.addAll(
                        readCaptions(
                            File(fileInternal.path).bufferedReader(),
                            fileInternal.type,
                            context
                        )
                    )
                }
                // Same as tracks file
                if (fileLyrics.isEmpty()) {
                    val fileR = Utils.checkLyrics(
                        currentPlay.path.substring(
                            0,
                            currentPlay.path.lastIndexOf("."),
                        )
                    )
                    if (fileR != null) {
                        lyricsType = fileR.type
                        fileLyrics.addAll(
                            readCaptions(
                                File(fileR.path).bufferedReader(),
                                fileR.type,
                                context
                            )
                        )
                    }
                }
                if (fileLyrics.isEmpty()) {
                    val musicName: String = try {
                        currentPlay.path.substring(
                            currentPlay.path.lastIndexOf("/") + 1,
                            currentPlay.path.lastIndexOf(".")
                        )
                    } catch (_: Exception) {
                        ""
                    }
                    val files = getDb(context).StorageFolderDao().findAllByType(LYRICS_TYPE)
                    outer@ for (storageFolder in files) {
                        try {
                            if (loadLyrics(context, storageFolder, musicName, fileLyrics)) {
                                break@outer
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            getDb(context).StorageFolderDao().deleteById(storageFolder.id!!)
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(
                                    context,
                                    "There has error, can't read some lyrics. Most of times, this occur after you reinstall app.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    }
                }
            }
            if (fileLyrics.isEmpty() && !firstEmbeddedLyrics) {
                embeddedLyrics.addAll(
                    CaptionUtils.getEmbeddedLyrics(
                        currentPlay.path,
                        context,
                        tags
                    )
                )
            }
            if (fileLyrics.isNotEmpty()) {
                isEmbeddedLyrics.value = false
                currentCaptionList.addAll(fileLyrics)
            } else if (embeddedLyrics.isNotEmpty()) {
                isEmbeddedLyrics.value = true
                currentCaptionList.addAll(embeddedLyrics)
            }
            val duration = currentPlay.duration
            // every lyrics line duration
            itemDuration =
                duration / if (currentCaptionList.isEmpty()) 1 else currentCaptionList.size
            currentCaptionListLoading.value = false
        }
        lock.unlock()

    }

    private fun fileRead(
        uri: Uri,
        context: Context,
        captionType: LyricsType
    ): ArrayList<ListStringCaption> {
        lyricsType = captionType
        val inputStream =
            context.contentResolver.openInputStream(uri)
        val i = InputStreamReader(inputStream)
        val reader =
            BufferedReader(i)
        val r = readCaptions(reader, lyricsType, context)
        reader.close()
        i.close()
        inputStream?.close()
        return r
    }

    private fun readCaptions(
        bufferedReader: BufferedReader,
        captionType: LyricsType,
        context: Context
    ): ArrayList<ListStringCaption> {
        val captions = arrayListOf<Caption>()
        when (captionType) {
            LyricsType.SRT -> {
                captions.addAll(CaptionUtils.parseSrtFile(bufferedReader))
            }

            LyricsType.VTT -> {
                captions.addAll(CaptionUtils.parseVttFile(bufferedReader))
            }

            LyricsType.LRC -> {
                captions.addAll(CaptionUtils.parseLrcFile(bufferedReader, context))
            }

            LyricsType.TEXT -> {
                captions.addAll(CaptionUtils.parseTextFile(bufferedReader, context))
            }
        }
        val arrayList = arrayListOf<ListStringCaption>()
        captions.forEach {
            val an = ListStringCaption(
                text = splitStringIntoWordsAndSymbols(it.text), //ArrayList(it.text.split(Regex("[\\n\\r\\s]+"))),
                timeStart = it.timeStart,
                timeEnd = it.timeEnd
            )
            arrayList.add(an)
        }
        bufferedReader.close()
        return arrayList
    }


    fun getCurrentMusicCover(context: Context): ImageSource {
        val v = currentPlay.value
        if (v != null) {
            currentMusicCover.value = getCover(this@MusicViewModel, context, v.id, v.path)
        }
        if (themeSelected.intValue == 3) {
            needRefreshTheme.value = !needRefreshTheme.value
        }
        return currentMusicCover.value
    }

    suspend fun getAlbumCover(id: Long, context: Context): Any {
        return withContext(Dispatchers.IO) {
            try {
                val folder = File(context.externalCacheDir, "album_cover")
                folder.mkdirs()
                val coverPath = File(folder, "$id.jpg")
                if (coverPath.exists()) {
                    Log.d("AlbumCover", "Loaded from cache: ${coverPath.path}")
                    return@withContext coverPath.path
                }
                try {
                    val albumUri = ContentUris.withAppendedId(
                        "content://media/external/audio/albumart".toUri(),
                        id
                    )
                    context.contentResolver.openInputStream(albumUri)?.use { s ->
                        Files.copy(s, coverPath.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        Log.d("AlbumCover", "Loaded from MediaStore: ${coverPath.path}")
                        return@withContext coverPath.path
                    }
                } catch (e: Exception) {
                    Log.e(
                        "AlbumCover",
                        "Error getting album art from MediaStore for ID $id: ${e.message}",
                        e
                    )
                }
                val defaultCoverResId = customMusicCover.value
                if (defaultCoverResId is Int) {
                    val bm = BitmapFactory.decodeResource(context.resources, defaultCoverResId)
                    FileOutputStream(coverPath).use { outStream ->
                        bm.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
                    }
                    bm.recycle()
                    return@withContext defaultCoverResId
                } else if (defaultCoverResId is String) {
                    val sourceFile = File(defaultCoverResId)
                    if (sourceFile.exists()) {
                        sourceFile.copyTo(coverPath, true)
                        return@withContext defaultCoverResId
                    }
//                    Files.copy(sourceFile.toPath(), coverPath.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                return@withContext R.drawable.songs_thumbnail_cover
            } catch (e: Exception) {
                Log.e("AlbumCover", "Unexpected error in getAlbumCover for ID $id: ${e.message}", e)
                return@withContext R.drawable.songs_thumbnail_cover // 替换为你的默认封面资源
            }
        }
    }

    fun prepareArtistAndGenreCover(context: Context) {
        if (genreCover.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val files = getDb(context).StorageFolderDao().findAllByType(GENRE_TYPE)
                outer@ for (storageFolder in files) {
                    try {
                        val treeUri = storageFolder.uri.toUri()
                        context.contentResolver.takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
                        val d = pickedDir?.listFiles()
                        if (d != null) {
                            for (it in d) {
                                if (it != null && it.isFile && it.canRead() && it.name != null
                                    && (it.name?.endsWith(".jpg") == true
                                            || it.name?.endsWith(".jpeg") == true
                                            || it.name?.endsWith(".png") == true)
                                ) {

                                    genreCover[it.name!!.lowercase().replace(".jpg", "")
                                        .replace(".jpeg", "")
                                        .replace(".png", "")
                                        .trimIndent().trimEnd()] = it.uri
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        if (artistCover.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val files = getDb(context).StorageFolderDao().findAllByType(ARTIST_TYPE)
                outer@ for (storageFolder in files) {
                    try {
                        val treeUri = storageFolder.uri.toUri()
                        context.contentResolver.takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
                        val d = pickedDir?.listFiles()
                        if (d != null) {
                            for (it in d) {
                                if (it != null && it.isFile && it.canRead() && it.name != null
                                    && (it.name?.endsWith(".jpg") == true
                                            || it.name?.endsWith(".jpeg") == true
                                            || it.name?.endsWith(".png") == true)
                                ) {
                                    artistCover[it.name!!.lowercase().replace(".jpg", "")
                                        .replace(".jpeg", "")
                                        .replace(".png", "")
                                        .trimIndent().trimEnd()] = it.uri
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun scanAndRefreshPlaylist(context: Context, playListPath: String) {
        viewModelScope.launch {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(playListPath),
                arrayOf("*/*"),
                object : MediaScannerConnection.MediaScannerConnectionClient {
                    override fun onMediaScannerConnected() {}
                    override fun onScanCompleted(path: String, uri: Uri) {
                        viewModelScope.launch(Dispatchers.Main) {
                            val futureResult: ListenableFuture<SessionResult>? =
                                browser?.sendCustomCommand(
                                    MediaCommands.COMMAND_PlAY_LIST_CHANGE,
                                    Bundle().apply {
                                        putString(
                                            CustomMetadataKeys.KEY_PATH,
                                            playListPath
                                        )
                                    },
                                )
                            futureResult?.addListener({
                                try {
                                    val sessionResult = futureResult.get()
                                    if (sessionResult.resultCode == SessionResult.RESULT_SUCCESS) {
                                        refreshPlayList.value =
                                            !refreshPlayList.value
                                    }
                                } catch (e: Exception) {
                                    Log.e("Client", "Failed to toggle favorite status", e)
                                }
                            }, ContextCompat.getMainExecutor(context))
                        }

                    }
                })
        }

    }

    fun playShuffled(playListType: PlayListType, playListId: Long) {
        val browser = this.browser ?: return
        val args = Bundle().apply {
            putBoolean("queue", false)
            putBoolean("enable", true)
            putBoolean("autoPlay", true)
            putString("playListType", playListType.name)
            putLong("playListId", playListId)
        }
        browser.sendCustomCommand(MediaCommands.COMMAND_SMART_SHUFFLE, args)
    }
}
