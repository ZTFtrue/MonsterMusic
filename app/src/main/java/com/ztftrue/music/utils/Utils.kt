package com.ztftrue.music.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.text.TextUtils
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.content.FileProvider
import androidx.media3.common.util.UnstableApi
import com.ztftrue.music.MainActivity
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.ACTION_AddPlayQueue
import com.ztftrue.music.play.ACTION_GET_TRACKS
import com.ztftrue.music.play.ACTION_PlayLIST_CHANGE
import com.ztftrue.music.sqlData.model.DictionaryApp
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.ui.play.Lyrics
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.trackManager.PlaylistManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

enum class OperateTypeInActivity {
    DeletePlayList,
    InsertTrackToPlaylist,
    ModifyTrackFromPlayList,//sort and remove tracks
    RenamePlaylist,
    RemoveTrackFromStorage,
    EditTrackInfo,
}

enum class PlayListType {
    Songs,
    PlayLists,
    Queue,
    Albums,
    Artists,
    Genres,
    Folders,
    None
}

fun enumToStringForPlayListType(myEnum: PlayListType): String {
    return myEnum.name
}

fun stringToEnumForPlayListType(enumString: String): PlayListType {
    return try {
        PlayListType.valueOf(enumString)
    } catch (e: IllegalArgumentException) {
        PlayListType.Songs
    }
}


enum class OperateType {
    AddToQueue,
    RemoveFromQueue,
    PlayNext,
    AddToPlaylist,
    RemoveFromPlaylist,
    AddToFavorite,
    Artist,
    Album,
    EditMusicInfo,
    DeleteFromStorage,
    No,
    RenamePlayList,
    DeletePlayList,
    ShowArtist,
    ClearQueue,
    SaveQueueToPlayList,
    IgnoreFolder,
    QueueSwipeSort
}

enum class ScrollDirectionType {
    GRID_HORIZONTAL,
    GRID_VERTICAL,
    LIST_VERTICAL,
}

data class CheckLyricsData(val path: String, val type: LyricsType)

@Suppress("deprecation")
object Utils {
    val translateMap = HashMap<String, Int>().apply {
        put("Songs", R.string.tab_songs)
        put("PlayLists", R.string.tab_playLists)
        put("Queue", R.string.tab_queue)
        put("Albums", R.string.tab_albums)
        put("Artists", R.string.tab_artists)
        put("Genres", R.string.tab_genres)
        put("Folders", R.string.tab_folders)
        put("Cover", R.string.tab_cover)
        put("Lyrics", R.string.tab_lyrics)
        put("Equalizer", R.string.tab_equalizer)
        put("Effect", R.string.effect)
        put("Custom", R.string.custom)
    }

    val items = listOf(
        R.string.theme_follow_system,
        R.string.theme_light,
        R.string.theme_dark,
        R.string.theme_follow_music_cover,
        R.string.theme_material_you,
    )
    var bandsCenter = doubleArrayOf(
        31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0
        //60.0,170.0,310.0,600.0,1000.0,3000.0,6000.0,12000.0,14000.0,16000.0
    )
    var qs = doubleArrayOf(
        1.5,
        1.5,
        1.5,
        1.5,
        1.5,
        1.5,
        1.5,
        1.5,
        1.5,
        1.5,
    )
    var kThirdBW = doubleArrayOf(
        //15.0, 30.0, 60.0, 120.0, 240.0, 480.0, 960.0, 1920.0, 3840.0, 7680.0
        60.0, 170.0, 310.0, 600.0, 1000.0, 3000.0, 6000.0, 12000.0, 14000.0, 16000.0
    )

    var order = 40
    var equalizerMax = 13
    var equalizerMin = -13
    var custom = "Custom"
    var eqPreset = LinkedHashMap<String, IntArray>().apply {
        put("Zero", intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
        put("Classical", intArrayOf(0, 0, 0, 0, 0, 0, -9, -9, -9, -12))
        put("Club", intArrayOf(0, 0, 2, 3, 3, 3, 2, 0, 0, 0))
        put("Dance", intArrayOf(6, 4, 1, 0, 0, -7, -9, -9, 0, 0))
        put("FullBass", intArrayOf(8, 8, 8, 4, 2, -10, -12, -13, -13, -13))
        put("FullTreble", intArrayOf(-12, -12, -12, -6, 1, 6, 9, 9, 9, 10))
        put("FullBass+Treble", intArrayOf(4, 3, 0, -9, -6, 1, 5, 6, 7, 7))
        put("Laptop/Headphones", intArrayOf(3, 6, 3, -4, 0, -7, -9, -9, 0, 0))
        put("LargeHall", intArrayOf(6, 6, 3, 3, 0, -6, -6, -6, 0, 0))
        put("Live", intArrayOf(-6, 0, 2, 3, 3, 3, 2, 1, 1, 1))
        put("Party", intArrayOf(4, 4, 0, 0, 0, 0, 0, 0, 4, 4))
        put("Pop", intArrayOf(-2, 3, 4, 4, 3, -1, -3, -3, -2, -2))
        put("Reggae", intArrayOf(0, 0, -1, -7, 0, -8, -8, 0, 0, 0))
        put("Rock", intArrayOf(4, 3, -7, -9, -4, 2, 5, 6, 6, 6))
        put("Soft", intArrayOf(3, 1, -1, -3, -1, 2, 5, 6, 6, 7))
        put("Ska", intArrayOf(-3, -6, -6, -1, 2, 3, 5, 6, 6, 6))
        put("SoftRock", intArrayOf(2, 2, 1, -1, -6, -7, -4, -1, 1, 5))
        put("Techno", intArrayOf(4, 3, 0, -7, -6, 0, 4, 6, 6, 5))
    }

    fun initSettingsData(musicViewModel: MusicViewModel, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {

            musicViewModel.themeSelected.intValue = context.getSharedPreferences(
                "SelectedTheme",
                Context.MODE_PRIVATE
            ).getInt("SelectedTheme", 0)
            musicViewModel.textAlign.value =
                SharedPreferencesUtils.getDisplayAlign(context)
            musicViewModel.fontSize.intValue = SharedPreferencesUtils.getFontSize(context)
            musicViewModel.autoScroll.value =
                SharedPreferencesUtils.getAutoScroll(context)
            musicViewModel.autoHighLight.value =
                SharedPreferencesUtils.getAutoHighLight(context)
            val dicApps = musicViewModel.getDb(context).DictionaryAppDao().findAllDictionaryApp()
            if (dicApps.isNullOrEmpty()) {
                val list = ArrayList<DictionaryApp>()
                getAllDictionaryActivity(context)
                    .forEachIndexed { index, it ->
                        list.add(
                            DictionaryApp(
                                index,
                                it.activityInfo.name,
                                it.activityInfo.packageName,
                                it.loadLabel(context.packageManager).toString(),
                                isShow = true,
                                autoGo = false
                            )
                        )
                    }
                musicViewModel.dictionaryAppList.addAll(list)
            } else {
                musicViewModel.dictionaryAppList.addAll(dicApps)
            }
        }
    }

    fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val seconds = totalSeconds % 60
        var minutes = totalSeconds / 60
        val hours = minutes / 60
        minutes %= 60
        return if (hours > 0) String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        ) else String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }


    private fun openFile(path: String, minType: String = "text/plain", context: Context) {
        val fileUri: Uri
        val outputImage = File(path)
        fileUri =
            FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                outputImage
            )
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (TextUtils.isEmpty(minType)) {
            intent.data = fileUri
        } else {
            intent.setDataAndType(fileUri, minType)
        }
        context.startActivity(intent)
    }

    private val retriever = MediaMetadataRetriever()
    fun getCover(context: Context, musicId: Long, path: String): Bitmap? {
//        try {
//            var uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//            uri = ContentUris.withAppendedId(uri, musicId)
//            val thumbnail =
//                context.contentResolver.loadThumbnail(uri, Size(512, 512), null);
//            return thumbnail
//        } catch (_: IOException) {
//        }
        try {
            retriever.setDataSource(path)
            val coverT = retriever.embeddedPicture
            if (coverT != null) {
                return BitmapFactory.decodeByteArray(coverT, 0, coverT.size)
            }
        } catch (_: Exception) {
        }
        return null
    }

    @Suppress("unused")
    fun copyFile(sourceFile: File, destinationFile: File) {
        try {
            val inputStream = FileInputStream(sourceFile)
            val outputStream = FileOutputStream(destinationFile)

            val buffer = ByteArray(1024)
            var length: Int

            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            inputStream.close()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun openBrowser(link: String, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        context.startActivity(intent)
    }

    fun sendEmail(recipient: String, subject: String, context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        context.startActivity(intent)
    }

    fun createPlayListAddTracks(
        name: String,
        context: Context,
        type: PlayListType,
        id: Long,
        musicViewModel: MusicViewModel
    ) {
        if (name.isNotEmpty()) {
            val idPlayList = PlaylistManager.createPlaylist(context, name)
            if (idPlayList != -1L) {
                val bundle = Bundle()
                bundle.putString("type", type.name)
                bundle.putLong("id", id)
                musicViewModel.mediaBrowser?.sendCustomAction(
                    ACTION_GET_TRACKS,
                    bundle,
                    object : MediaBrowserCompat.CustomActionCallback() {
                        override fun onResult(
                            action: String?,
                            extras: Bundle?,
                            resultData: Bundle?
                        ) {
                            super.onResult(action, extras, resultData)
                            if (ACTION_GET_TRACKS == action && resultData != null) {
                                val tracksList =
                                    resultData.getParcelableArrayList<MusicItem>("list")
                                if (tracksList != null) {
                                    val tIds = ArrayList(tracksList.map { item -> item.id })
                                    PlaylistManager.addMusicsToPlaylist(context, idPlayList, tIds)
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
                                                musicViewModel.refreshPlayList.value =
                                                    !musicViewModel.refreshPlayList.value
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.create_failed),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    fun createPlayListAddTrack(
        name: String,
        context: Context,
        item: MusicItem,
        musicViewModel: MusicViewModel
    ) {
        if (name.isNotEmpty()) {
            val idPlayList = PlaylistManager.createPlaylist(context, name)
            if (idPlayList != -1L) {
                PlaylistManager.addMusicToPlaylist(context, idPlayList, item.id)
                musicViewModel.mediaBrowser?.sendCustomAction(
                    ACTION_PlayLIST_CHANGE, null, null
                )
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.create_failed),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    fun addTracksToPlayList(
        playListId: Long,
        context: Context,
        type: PlayListType,
        id: Long,
        musicViewModel: MusicViewModel
    ) {
        val bundle = Bundle()
        bundle.putString("type", type.name)
        bundle.putLong("id", id)
        musicViewModel.mediaBrowser?.sendCustomAction(
            ACTION_GET_TRACKS,
            bundle,
            object : MediaBrowserCompat.CustomActionCallback() {
                override fun onResult(
                    action: String?,
                    extras: Bundle?,
                    resultData: Bundle?
                ) {
                    super.onResult(action, extras, resultData)
                    if (ACTION_GET_TRACKS == action && resultData != null) {
                        val tracksList =
                            resultData.getParcelableArrayList<MusicItem>("list")
                        if (tracksList != null) {
                            val tIds = ArrayList(tracksList.map { item -> item.id })
                            PlaylistManager.addMusicsToPlaylist(context, playListId, tIds)
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
                                        musicViewModel.refreshPlayList.value =
                                            !musicViewModel.refreshPlayList.value
                                    }
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    fun operateDialogDeal(
        operateType: OperateType,
        item: AnyListBase,
        musicViewModel: MusicViewModel
    ) {
        when (operateType) {
            OperateType.AddToQueue -> {
                val bundle = Bundle()
                bundle.putString("type", item.type.name)
                bundle.putLong("id", item.id)
                musicViewModel.mediaBrowser?.sendCustomAction(
                    ACTION_GET_TRACKS,
                    bundle,
                    object : MediaBrowserCompat.CustomActionCallback() {
                        override fun onResult(
                            action: String?,
                            extras: Bundle?,
                            resultData: Bundle?
                        ) {
                            super.onResult(action, extras, resultData)
                            if (ACTION_GET_TRACKS == action && resultData != null) {
                                val tracksList =
                                    resultData.getParcelableArrayList<MusicItem>("list")
                                if (tracksList != null) {
                                    musicViewModel.musicQueue.addAll(tracksList)
                                    val bundleAddTracks = Bundle()
                                    bundleAddTracks.putParcelableArrayList("musicItems", tracksList)
                                    musicViewModel.mediaBrowser?.sendCustomAction(
                                        ACTION_AddPlayQueue,
                                        bundleAddTracks,
                                        null
                                    )
                                }
                            }
                        }
                    }
                )

            }

            OperateType.PlayNext -> {
                val bundle = Bundle()
                bundle.putString("type", item.type.name)
                bundle.putLong("id", item.id)
                musicViewModel.mediaBrowser?.sendCustomAction(
                    ACTION_GET_TRACKS,
                    bundle,
                    object : MediaBrowserCompat.CustomActionCallback() {
                        override fun onResult(
                            action: String?,
                            extras: Bundle?,
                            resultData: Bundle?
                        ) {
                            super.onResult(action, extras, resultData)
                            if (ACTION_GET_TRACKS == action && resultData != null) {
                                val tracksList =
                                    resultData.getParcelableArrayList<MusicItem>("list")
                                if (tracksList != null) {
                                    addTracksToQueue(
                                        musicViewModel,
                                        tracksList,
                                        musicViewModel.currentPlayQueueIndex.intValue + 1
                                    )
                                }

                            }
                        }
                    }
                )
            }


            else -> {

            }
        }
    }

    fun addTracksToQueue(
        musicViewModel: MusicViewModel,
        tracksList: ArrayList<MusicItem>,
        indexTemp: Int
    ) {
        var index = indexTemp
        if (musicViewModel.musicQueue.isEmpty()) {
            index = 0
        }
        musicViewModel.musicQueue.addAll(
            index,
            tracksList
        )
        val bundleAddTracks = Bundle()
        bundleAddTracks.putParcelableArrayList("musicItems", tracksList)
        bundleAddTracks.putInt(
            "index",
            index
        )
        musicViewModel.mediaBrowser?.sendCustomAction(
            ACTION_AddPlayQueue,
            bundleAddTracks,
            null
        )
    }

    fun deleteTrackUpdate(musicViewModel: MusicViewModel, resultData: Bundle?) {
        musicViewModel.refreshPlayList.value =
            !musicViewModel.refreshPlayList.value
        musicViewModel.refreshAlbum.value =
            !musicViewModel.refreshAlbum.value
        musicViewModel.refreshArtist.value =
            !musicViewModel.refreshArtist.value
        musicViewModel.refreshGenre.value =
            !musicViewModel.refreshGenre.value
        musicViewModel.refreshFolder.value =
            !musicViewModel.refreshFolder.value
        musicViewModel.playListCurrent.value = null
        if (resultData != null) {
            resultData.getParcelableArrayList<MusicItem>(
                "songsList"
            )?.also {
                musicViewModel.songsList.clear()
                musicViewModel.songsList.addAll(it)
            }
            resultData.getLong("id", -1).also {
                if (it == -1L) return
                musicViewModel.musicQueue.removeAll { mIt ->
                    mIt.id == it
                }
                if (it == musicViewModel.currentPlay.value?.id) {
                    musicViewModel.currentPlayQueueIndex.intValue = -1
                    musicViewModel.currentPlay.value = null
                } else {
                    resultData.getInt(
                        "playIndex"
                    ).also { indexPlay ->
                        if (indexPlay > -1)
                            musicViewModel.currentPlayQueueIndex.intValue = indexPlay
                    }
                }
            }

        }
    }

    fun getAllDictionaryActivity(context: Context): List<ResolveInfo> {
        val shareIntent = Intent(Intent.ACTION_PROCESS_TEXT)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, "")
        val resolveInfoList: List<ResolveInfo> =
            context.packageManager.queryIntentActivities(shareIntent, 0)
        for (resolveInfo in resolveInfoList) {
            val activityInfo = resolveInfo.activityInfo
            val packageName = activityInfo.packageName
            val className = activityInfo.name
            val label = resolveInfo.loadLabel(context.packageManager).toString()
            println("Package Name: $packageName, Class Name: $className, Label: $label")
        }
        return resolveInfoList
    }

    fun clearAlbumCoverCache(context: Context) {
        val folder = File(context.externalCacheDir, "album_cover")
        folder.mkdirs()
        folder.listFiles()?.forEach {
            it.delete()
        }
    }

    @OptIn(UnstableApi::class)
    fun setLyricsFolder(context: Context) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        (context as MainActivity).folderPickerLauncher.launch(intent)
    }
    @OptIn(UnstableApi::class)
    fun setArtistFolder(context: Context) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        (context as MainActivity).artistFolderPickerLauncher.launch(intent)
    }
    @OptIn(UnstableApi::class)
    fun setGenreFolder(context: Context) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        (context as MainActivity).genreFolderPickerLauncher.launch(intent)
    }
    @OptIn(UnstableApi::class)
    fun setLyricsFile(musicViewModel: MusicViewModel, context: Context) {
        if (musicViewModel.currentPlay.value != null) {
            val regexPattern = Regex("[<>\"/~'{}?,+=)(^&*%!@#\$]")
            val artistsFolder = musicViewModel.currentPlay.value?.artist
                ?.replace(
                    regexPattern,
                    "_"
                )
            val folderPath = "$Lyrics/$artistsFolder"
            val folder = context.getExternalFilesDir(
                folderPath
            )
            folder?.mkdirs()
            val id =
                musicViewModel.currentPlay.value?.name?.replace(regexPattern, "_")
            val pathLyrics: String =
                context.getExternalFilesDir(folderPath)?.absolutePath + "/$id.lrc"
            val path: String =
                context.getExternalFilesDir(folderPath)?.absolutePath + "/$id.txt"
            val lyrics = File(pathLyrics)
            val text = File(path)
            if (lyrics.exists()) {
                openFile(lyrics.path, context = context)
            } else if (text.exists()) {
                openFile(text.path, context = context)
            } else {
                val tempPath: String =
                    context.getExternalFilesDir(folderPath)?.absolutePath + "/$id."
                (context as MainActivity).openFilePicker(tempPath)
            }
        }
    }

    fun checkLyrics(path: String): CheckLyricsData? {
        if (File("$path.lrc").exists()) {
            return CheckLyricsData("$path.lrc", LyricsType.LRC)
        } else if (File("$path.txt").exists()) {
            return CheckLyricsData("$path.txt", LyricsType.TEXT)
        } else if (File("$path.srt").exists()) {
            return CheckLyricsData("$path.srt", LyricsType.SRT)
        } else if (File("$path.vtt").exists()) {
            return CheckLyricsData("$path.vtt", LyricsType.VTT)
        }
        return null
    }
}