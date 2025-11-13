package com.ztftrue.music.utils.trackManager

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import com.ztftrue.music.utils.PlayListType
import com.ztftrue.music.utils.model.FolderList
import java.io.File


object FolderManger {
    fun getMusicFolders(context: Context): HashMap<Long, FolderList> {
        val contentResolver: ContentResolver = context.contentResolver
        val musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.BUCKET_ID,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Audio.Media.NUM_TRACKS,
            MediaStore.Audio.Media.RELATIVE_PATH
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
                val relativePath =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH))
                musicFolders.putIfAbsent(
                    bucketId,
                    FolderList(
                        children = ArrayList(),
                        path = relativePath ?: "/",
                        name = bucketName ?: "/",
                        id = bucketId,
                        trackNumber = tracksNumber,
                        type = PlayListType.Folders
                    )
                )
            } while (cursor.moveToNext())
            cursor.close()
            return musicFolders
        }
        return HashMap()
    }


    /**
     * 根据一个包含预定义 FolderList 对象的列表，构建一个文件夹的树状结构。
     * 只有在 allItemRelativePaths 中明确存在的文件夹及其必要的父路径才会被包含在树中。
     *
     * @param initialFolderList 一个包含 FolderList 对象的列表，每个对象代表一个需要加入树的文件夹。
     *                          这些 FolderList 已经包含了 path, name, id, trackNumber 等信息。
     * @return 包含所有文件夹的根 FolderList 对象。
     */
    fun buildFolderTreeFromPaths(initialFolderList: List<FolderList>): ArrayList<FolderList> {
        val hashMapFolder = HashMap<String, FolderList>()
        // 遍历 initialFolderList，为每个传入的文件夹及其所有父路径做标记
        for (folder in initialFolderList) {
            var currentPath = folder.path
            if (currentPath.isNotEmpty() && currentPath.endsWith(File.separator)) {
                currentPath = currentPath.dropLast(1)
            }
            // 从当前传入的文件夹路径逐级向上，提取所有父文件夹路径并添加到 allEffectivePathsToBuild
            var tempPath = currentPath
            var tempFolder: FolderList? = hashMapFolder.get(tempPath)
            if (tempFolder != null) {
                tempFolder.children.add(folder)
            } else {
                tempFolder = folder
                hashMapFolder[folder.path] = tempFolder
                while (true) {
                    val lastSeparatorIndex = tempPath.lastIndexOf(File.separator)
                    if (lastSeparatorIndex != -1) {
                        tempPath = tempPath.take(lastSeparatorIndex)
                        val temptttFolder: FolderList? = hashMapFolder.get(tempPath)
                        if (temptttFolder != null && tempFolder != null) {
                            temptttFolder.children.add(tempFolder)
                            tempFolder.parent = temptttFolder
                            break
                        }
                        val tf = FolderList(
                            children = ArrayList(),
                            path = tempPath,
                            name = tempPath.substring(tempPath.lastIndexOf(File.separator) + 1),
                            id = tempPath.hashCode().toLong(),
                            trackNumber = 0,
                        )
                        hashMapFolder[tempPath] = tf
                        tempFolder?.let {
                            tf.children.add(it)
                            it.parent = tf
                        }
                        tempFolder = tf
                    } else {
                        break
                    }
                }
            }
        }
        val parentsFolders = ArrayList<FolderList>()
        hashMapFolder.entries.forEach { (_, value) ->
            if (value.parent == null) {
                parentsFolders.add(value)
            }
        }
        val needArray = ArrayList<FolderList>()
        val newParent = ArrayList<FolderList>()
        parentsFolders.forEach {
            if (it.children.size == 1 && it.trackNumber == 0) {
                needArray.add(it)
                var pp = it.children[0]
                pp.parent = null
                newParent.add(pp)
                while (true) {
                    if (pp.children.size == 1 && pp.trackNumber == 0) {
                        newParent.remove(pp)
                        pp = pp.children[0]
                        pp.parent = null
                        newParent.add(pp)
                    } else {
                        break
                    }
                }
            }
        }
        parentsFolders.removeAll(needArray.toSet())
        parentsFolders.addAll(newParent)
//        parentsFolders.forEach {
//            val stack = ArrayDeque<FolderList>()
//            stack.addFirst(it)
//            while (stack.isNotEmpty()) {
//                val temp = stack.removeLast()
//                val pt = temp.parent
//                if (pt != null && temp.children.size == 1 && temp.trackNumber == 0) {
//                    pt.children.remove(temp)
//                    pt.children.addAll(temp.children)
//                    // 实际只有一个
//                    temp.children.forEach { it2 ->
//                        it2.parent = pt
//                    }
//                }
//                temp.children.forEach { it2 ->
//                    stack.addFirst(it2)
//                }
//            }
//        }
        return if (parentsFolders.size == 1) {
            parentsFolders[0].children
        } else {
            parentsFolders
        }
    }
}