package com.ztftrue.music.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.ACTION_AddPlayQueue
import com.ztftrue.music.play.ACTION_GET_TRACKS
import com.ztftrue.music.play.ACTION_PlayLIST_CHANGE
import com.ztftrue.music.sqlData.model.DictionaryApp
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.model.AnyListBase
import com.ztftrue.music.utils.trackManager.PlaylistManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

enum class OperateTypeInActivity {
    DeletePlayList,
    InsertTrackToPlaylist,
    RemoveTrackFromPlayList,
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
    IgnoreFolder
}

enum class ScrollDirectionType {
    GRID_HORIZONTAL,
    GRID_VERTICAL,
    LIST_VERTICAL,
}


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
    }

    val items = listOf(
        R.string.theme_follow_system,
        R.string.theme_light,
        R.string.theme_dark,
        R.string.theme_follow_music_cover,
        R.string.theme_material_you,
    )
    var kThirdOct = doubleArrayOf(
        31.5, 63.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0
    )
    var kThirdBW = doubleArrayOf(
        20.0, 40.0, 80.0, 160.0, 320.0, 640.0, 1280.0, 2560.0, 5120.0, 10240.0
    )
    var equalizerMax = 15
    var equalizerMin = -24
    var eqPreset = HashMap<String, IntArray>().apply {
        put("Classical", intArrayOf(0, 0, 0, 0, 0, 0, -40, -40, -40, -50))
        put("Club", intArrayOf(0, 0, 20, 30, 30, 30, 20, 0, 0, 0))
        put("Dance", intArrayOf(50, 35, 10, 0, 0, -30, -40, -40, 0, 0))
        put("FullBass", intArrayOf(70, 70, 70, 40, 20, -45, -50, -55, -55, -55))
        put("FullTreble", intArrayOf(-50, -50, -50, -25, 15, 55, 80, 80, 80, 85))
        put("FullBass+Treble", intArrayOf(35, 30, 0, -40, -25, 10, 45, 55, 60, 60))
        put("Laptop/Headphones", intArrayOf(25, 50, 25, -20, 0, -30, -40, -40, 0, 0))
        put("LargeHall", intArrayOf(50, 50, 30, 30, 0, -25, -25, -25, 0, 0))
        put("Live", intArrayOf(-25, 0, 20, 25, 30, 30, 20, 15, 15, 10))
        put("Party", intArrayOf(35, 35, 0, 0, 0, 0, 0, 0, 35, 35))
        put("Pop", intArrayOf(-10, 25, 35, 40, 25, -5, -15, -15, -10, -10))
        put("Reggae", intArrayOf(0, 0, -5, -30, 0, -35, -35, 0, 0, 0))
        put("Rock", intArrayOf(40, 25, -30, -40, -20, 20, 45, 55, 55, 55))
        put("Soft", intArrayOf(25, 10, -5, -15, -5, 20, 45, 50, 55, 60))
        put("Ska", intArrayOf(-15, -25, -25, -5, 20, 30, 45, 50, 55, 50))
        put("SoftRock", intArrayOf(20, 20, 10, -5, -25, -30, -20, -5, 15, 45))
        put("Techno", intArrayOf(40, 30, 0, -30, -25, 0, 40, 50, 50, 45))
    }

    /**
     * B≈2×fc Q=fc/B
     */
    var qFactors = doubleArrayOf(
        0.67, 0.67, 0.67, 0.67, 0.67, 0.67, 0.67, 0.67, 0.67, 0.67
    )


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
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        ) else String.format("%02d:%02d", minutes, seconds)
    }


    fun openFile(path: String, minType: String = "text/plain", context: Context) {
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
                                                musicViewModel.refreshList.value =
                                                    !musicViewModel.refreshList.value
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
                                        musicViewModel.refreshList.value =
                                            !musicViewModel.refreshList.value
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


}