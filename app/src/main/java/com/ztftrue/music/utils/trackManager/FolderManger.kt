package com.ztftrue.music.utils.trackManager

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.model.FolderList


object FolderManger {
    fun getMusicFolders(context: Context): HashMap<Long, FolderList> {
        val contentResolver: ContentResolver = context.getContentResolver()
        val musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.BUCKET_ID,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Audio.Media.NUM_TRACKS
        )

        val cursor = contentResolver.query(musicUri, projection, null, null, null)

        if (cursor != null && cursor.moveToFirst()) {
            val musicFolders: HashMap<Long, FolderList> = HashMap()
            do {
                val bucketId =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID))
                val bucketName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME))
                val tracksNumber =
                    cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.NUM_TRACKS))
                musicFolders.putIfAbsent(
                    bucketId,
                    FolderList(
                        bucketName ?: "/",
                        bucketId,
                        tracksNumber,
                        type = PlayListType.Folders
                    )
                )
            } while (cursor.moveToNext())
            cursor.close()
            return musicFolders
        }
        return HashMap<Long, FolderList>()
    }
}