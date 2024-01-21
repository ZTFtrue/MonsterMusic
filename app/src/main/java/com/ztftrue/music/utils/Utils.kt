package com.ztftrue.music.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import com.ztftrue.music.sqlData.model.MusicItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern


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
    Folders
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
    ShowArtist
}

enum class ScrollDirectionType {
    GRID_HORIZONTAL,
    GRID_VERTICAL,
    LIST_VERTICAL,
}
@Suppress("deprecation")
object Utils {
    val items = listOf("Follow System", "Light", "Dark", "Follow Music Cover")
    var kThirdOct = doubleArrayOf(
        31.5, 63.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0
    )
    var equalizerMax = 5
    var equalizerMin = -8

    /**
     * B≈2×fc Q=fc/B
     */
    var qFactors = doubleArrayOf(
        0.707, 0.707, 0.707, 0.707, 0.707, 0.707, 0.707, 0.707, 0.707, 0.707
    )

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

    /**
     * timeStr minutes:seconds.milliseconds
     */
    private fun parseTime(timeStr: String?): Long {
        if (timeStr.isNullOrEmpty()) {
            return 0L
        }
        val timeParts = timeStr.split(":".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val minutes = timeParts[0].toInt()
        val secondsParts = timeParts[1].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val seconds = secondsParts[0].toInt()
        val milliseconds = secondsParts[1].toInt()
        return (minutes * 60 * 1000L + seconds * 1000 + milliseconds)
    }

    // other
    // [al:专辑名]
    // [ar:歌手名]
    // [au:歌词作者-作曲家]
    // [by:此LRC文件的创建者]
    // [offset:+/- 时间补偿值，以毫秒为单位，正值表示加快，负值表示延后]
    // [re:创建此LRC文件的播放器或编辑器]
    // [ti:歌词(歌曲)的标题]
    // [ve:程序的版本]
    private var RString: Map<String, Int> = mapOf(
        "al" to R.string.al,
        "ar" to R.string.ar,
        "au" to R.string.au,
        "by" to R.string.by,
        "re" to R.string.re,
        "ti" to R.string.ti,
        "ve" to R.string.ve
    )

    private fun parseLyricOtherMessage(message: String, context: Context): String {
        val pattern: Pattern = Pattern.compile("\\[([a-z]+):(.*)]")
        val matcher: Matcher = pattern.matcher(message)
        if (matcher.matches()) {
            val name = matcher.group(1)
            val text = matcher.group(2)
            if (name != null) {
                val id = RString[name]
                if (id != null) {
                    return context.getString(id) + text
                }
            }

        }
        return message.replace("[", "").replace("]", "").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.getDefault()
            ) else it.toString()
        }
    }

    /**
     *  [00:07.04][00:57.36]I don't want a lot for Christmas
     *  or
     *  [00:07.04]I don't want a lot for Christmas
     *  or
     *  I don't want a lot for Christmas
     */
    fun parseLyricLine(line: String, context: Context): Lyrics {
        // time
        val s = line.replace("\r", "")
        val pattern: Pattern = Pattern.compile("\\[([0-9]+:[0-9]+\\.[0-9]+)](.*)")
        val matcher: Matcher = pattern.matcher(s)
        if (matcher.matches()) {
            val timeStr = matcher.group(1)
            val text = matcher.group(2)
            val time = parseTime(timeStr)
            return Lyrics(text ?: "", time)
        }
        return Lyrics(parseLyricOtherMessage(s, context), 0)
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
                Toast.makeText(context, "创建失败", Toast.LENGTH_SHORT)
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
                Toast.makeText(context, "创建失败", Toast.LENGTH_SHORT)
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
        index: Int
    ) {
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

    fun isColorDark(color: Int): Boolean {
        // 计算相对亮度
        val darkness: Double =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        // 根据阈值判断是深色还是浅色
        return darkness >= 0.5
    }
}
