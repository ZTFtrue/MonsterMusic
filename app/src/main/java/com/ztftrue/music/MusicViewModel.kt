package com.ztftrue.music

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.style.TextAlign
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import androidx.navigation.NavHostController
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.model.ARTIST_TYPE
import com.ztftrue.music.sqlData.model.DictionaryApp
import com.ztftrue.music.sqlData.model.GENRE_TYPE
import com.ztftrue.music.sqlData.model.LYRICS_TYPE
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.ui.play.Lyrics
import com.ztftrue.music.utils.CaptionUtils
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
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.concurrent.locks.ReentrantLock


var SongsPlayList = AnyListBase(-1, PlayListType.Songs)
var QueuePlayList = AnyListBase(-1, PlayListType.Queue)


class MusicViewModel : ViewModel() {

    var db: MusicDatabase? = null
    val refreshPlayList = mutableStateOf(false)
    val refreshAlbum = mutableStateOf(false)
    val refreshArtist = mutableStateOf(false)
    val refreshGenre = mutableStateOf(false)
    val refreshFolder = mutableStateOf(false)
    var navController: NavHostController? = null
    var themeSelected = mutableIntStateOf(0)
    var editTrackEnable = mutableStateOf(false)

    //    val albumItemsCount = mutableIntStateOf(2)
//    val genreItemsCount = mutableIntStateOf(2)
    var mediaController: MediaControllerCompat? = null
    var mediaBrowser: MediaBrowserCompat? = null

//    val albumScrollDirection = mutableStateOf(ScrollDirectionType.GRID_VERTICAL)
//    val artistScrollDirection = mutableStateOf(ScrollDirectionType.GRID_VERTICAL)
//    val genreScrollDirection = mutableStateOf(ScrollDirectionType.GRID_VERTICAL)

    // 当前播放的列表，应该换数据结构存储，每个列表设置变量 播放状态，album和 genres 也是，艺术家跳转到 album， 然后在下一步处理
    // 每次播放仅设置当前列表的状态

    var musicQueue = mutableStateListOf<MusicItem>()

    var currentMusicCover = mutableStateOf<Bitmap?>(null)
    var currentPlay = mutableStateOf<MusicItem?>(null)

    var currentPlayQueueIndex = mutableIntStateOf(-1)
    var songsList = mutableStateListOf<MusicItem>()
    var playListCurrent = mutableStateOf<AnyListBase?>(null)

    var delayTime = mutableFloatStateOf(0.5f)
    var decay = mutableFloatStateOf(1f)
    var echoFeedBack = mutableStateOf(false)
    var enableShuffleModel = mutableStateOf(false)
    var pitch = mutableFloatStateOf(1f)
    var speed = mutableFloatStateOf(1f)
    var currentDuration = mutableLongStateOf(0)

    var mainTabList = mutableStateListOf<MainTab>()

    @OptIn(ExperimentalFoundationApi::class)
    var currentPageState: PagerState? = null

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
    var isEmbeddedLyrics = mutableStateOf(false)
    var fontSize = mutableIntStateOf(18)
    var textAlign = mutableStateOf(TextAlign.Center)

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
        mediaBrowser = null
        mediaController = null
        currentPlay.value = null
        currentMusicCover.value = null
        playListCurrent.value = null
        musicQueue.clear()
        songsList.clear()
        mainTabList.clear()
        sliderPosition.floatValue = 0f
        sleepTime.longValue = 0
        remainTime.longValue = 0
        currentPlayQueueIndex.intValue = -1
        playCompleted.value = false
        repeatModel.intValue = Player.REPEAT_MODE_ALL
    }

    init {
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
    fun dealLyrics(context: Context, currentPlay: MusicItem) {
        lock.lock()
        currentCaptionList.clear()
        lyricsType = LyricsType.TEXT
        if (lyricsJob != null && lyricsJob?.isActive == true) {
            lyricsJob?.cancel()
        }
        lyricsJob = CoroutineScope(Dispatchers.IO).launch {
            val regexPattern = Regex("[<>\"/~'{}?,+=)(^&*%!@#\$]")
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
            embeddedLyrics.addAll(CaptionUtils.getEmbeddedLyrics(currentPlay.path, context, tags))
            if (firstEmbeddedLyrics && embeddedLyrics.isNotEmpty()) {
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
                    } catch (e: Exception) {
                        ""
                    }
                    val files = getDb(context).StorageFolderDao().findAllByType(LYRICS_TYPE)
                    outer@ for (storageFolder in files) {
                        try {
                            val treeUri = Uri.parse(storageFolder.uri)
                            if (treeUri != null) {
                                context.contentResolver.takePersistableUriPermission(
                                    treeUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )
                                val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
                                val d = pickedDir?.listFiles()
                                if (d != null) {
                                    for (it in d) {
                                        if (it.isFile && it.canRead()
                                        ) {
                                            val fileNameWithSuffix =
                                                it.name?.lowercase() ?: ""
                                            val type =
                                                if (fileNameWithSuffix.endsWith(".lrc")) {
                                                    LyricsType.LRC
                                                } else if (fileNameWithSuffix.endsWith(".srt")) {
                                                    LyricsType.SRT
                                                } else if (fileNameWithSuffix.endsWith(".vtt")) {
                                                    LyricsType.VTT
                                                } else if (fileNameWithSuffix.endsWith(".txt")) {
                                                    LyricsType.TEXT
                                                } else {
                                                    continue
                                                }
                                            val fileName = try {
                                                fileNameWithSuffix.substring(
                                                    0,
                                                    fileNameWithSuffix.indexOf(".")
                                                )
                                            } catch (e: Exception) {
                                                ""
                                            }
                                            if (fileName.trim()
                                                    .lowercase() == musicName.trim()
                                                    .lowercase()
                                            ) {
                                                fileLyrics.addAll(
                                                    fileRead(
                                                        it.uri,
                                                        context,
                                                        type
                                                    )
                                                )
                                                break@outer
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
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
                duration / if (currentCaptionList.size == 0) 1 else currentCaptionList.size
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
                text = ArrayList(it.text.split(Regex("[\\n\\r\\s]+"))),
                timeStart = it.timeStart,
                timeEnd = it.timeEnd
            )
            arrayList.add(an)
        }
        bufferedReader.close()
        return arrayList
    }


    fun getCurrentMusicCover(context: Context): Bitmap? {
        if (currentMusicCover.value != null) {
            return currentMusicCover.value
        }
        val v = currentPlay.value
        if (v != null) {
            currentMusicCover.value = getCover(context, v.id, v.path)
            return currentMusicCover.value
        }
        return null
    }

    fun getAlbumCover(id: Long, context: Context): Any? {
        try {
            var result: Any? = null
            val folder = File(context.externalCacheDir, "album_cover")
            folder.mkdirs()
            val coverPath = File(folder, "$id.jpg")
            if (coverPath.exists()) {
                return coverPath.path
            } else {
                //            coverPath.createNewFile()
            }
            try {
                val albumUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    id
                )
                val s = context.contentResolver.openInputStream(
                    albumUri
                )
                if (s == null) {
                    val bm =
                        BitmapFactory.decodeResource(
                            context.resources,
                            R.drawable.songs_thumbnail_cover
                        )
                    val outStream = FileOutputStream(coverPath)

                    bm.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
                    outStream.flush()
                    outStream.close()
                    bm.recycle()
                    result =
                        R.drawable.songs_thumbnail_cover
                } else {
                    s.use { input ->
                        Files.copy(input, coverPath.toPath())
                    }
                    result = coverPath.path
                    s.close()
                }

            } catch (e: Exception) {
                //                        e.printStackTrace()
            }
            if (result == null) {
                val bm =
                    BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.songs_thumbnail_cover
                    )
                val outStream = FileOutputStream(coverPath)
                bm.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
                outStream.flush()
                outStream.close()
                bm.recycle()
                return R.drawable.songs_thumbnail_cover
            }
            return result
        } catch (_: Exception) {

        }
        return R.drawable.songs_thumbnail_cover
    }

    fun prepareArtistAndGenreCover(context: Context) {
        if (genreCover.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val files = getDb(context).StorageFolderDao().findAllByType(GENRE_TYPE)
                outer@ for (storageFolder in files) {
                    try {
                        val treeUri = Uri.parse(storageFolder.uri)
                        if (treeUri != null) {
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
                        val treeUri = Uri.parse(storageFolder.uri)
                        if (treeUri != null) {
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
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

    }


}
