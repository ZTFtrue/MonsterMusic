package com.ztftrue.music.utils.trackManager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.ztftrue.music.MainActivity
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.play.PlayService.Companion.COMMAND_PlAY_LIST_CHANGE
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.model.MusicPlayList
import java.io.File
import java.io.FileOutputStream

@OptIn(UnstableApi::class)
object SongsUtils {
    fun sortSongs(
        list: ArrayList<MusicItem>,
        field: String?,
        order: String?
    ): ArrayList<MusicItem> {
        val comparator: Comparator<MusicItem> = when (field) {
            MediaStore.Audio.Media.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            MediaStore.Audio.Media.ALBUM -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.album }
            MediaStore.Audio.Media.ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
            MediaStore.Audio.Media.DURATION -> compareBy { it.duration }
            MediaStore.Audio.Media.YEAR -> compareBy { it.year }
            else -> {
                return list
            }
        }
        val sortedList = when (order?.uppercase()) {
            "ASC" -> list.sortedWith(comparator)
            "DESC" -> list.sortedWith(comparator.reversed())
            else -> return list
        }
        return ArrayList(sortedList)
    }

    fun sortPlayList(
        list: ArrayList<MusicPlayList>,
        field: String?,
        order: String?
    ) {
        val comparator: Comparator<MusicPlayList> = when (field) {
            MediaStore.Audio.Playlists.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            else -> {
                return
            }
        }
        when (order?.uppercase()) {
            "ASC" -> list.sortWith(comparator)
            "DESC" -> list.sortWith(comparator.reversed())
            else -> return
        }
    }

    fun sendRequest(uri: Uri, context: MainActivity) {
        try {
            val pendingIntent = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
            val intentSenderRequest: IntentSenderRequest =
                IntentSenderRequest.Builder(pendingIntent.intentSender)
                    .setFlags(
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    .build()
            context.modifyMediaLauncher.launch(intentSenderRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resortOrRemoveTrackFromM3U(
        context: Context, uri: Uri, m3uPath: String,
        arrayList: ArrayList<MusicItem>,
    ) {
        val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
        val file = (File(m3uPath).parent ?: "") + "/"
        val stringBu = StringBuilder("#EXTM3U").append("\n")
        arrayList.forEach {
            val p = it.path.replace(file, "")
            stringBu.append(p).append("\n")
        }
        if (pfd != null) {
            val pfdWT = context.contentResolver.openFileDescriptor(uri, "wt")
            if (pfdWT != null) {
                val fileOutputStream = FileOutputStream(pfdWT.fileDescriptor)
                val a = stringBu.toString().toByteArray()
                fileOutputStream.write(a)
                fileOutputStream.flush()
                fileOutputStream.close()
                pfdWT.close()
            }
            pfd.close()
        }
    }

    fun refreshPlaylist(musicViewModel: MusicViewModel) {
        val futureResult: ListenableFuture<SessionResult>? =
            musicViewModel.browser?.sendCustomCommand(
                COMMAND_PlAY_LIST_CHANGE,
                Bundle().apply {

                },
            )
        futureResult?.addListener({
            try {
                val sessionResult = futureResult.get()
                if (sessionResult.resultCode == SessionResult.RESULT_SUCCESS) {
                    musicViewModel.refreshPlayList.value =
                        !musicViewModel.refreshPlayList.value
                }
            } catch (e: Exception) {
                Log.e("Client", "Failed to toggle favorite status", e)
            }
        }, MoreExecutors.directExecutor())
    }
}