package com.ztftrue.music.sqlData.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ztftrue.music.sqlData.model.StorageFolder

@Dao
interface StorageFolderDao {


    @Query("SELECT * FROM storage_folder WHERE id = :id")
    fun findById(id: Long): StorageFolder?

    @Query("SELECT * FROM storage_folder ORDER BY id ASC LIMIT 1")
    fun findFirstFolder(): StorageFolder?

    @Query("SELECT * FROM storage_folder")
    fun findAll(): List<StorageFolder>

    @Query("SELECT * FROM storage_folder where type = :type ORDER BY id ASC")
    fun findAllByType(type: Int): List<StorageFolder>

    @Insert
    fun insert(folder: StorageFolder)

    @Update
    fun update(folder: StorageFolder)

    @Query("DELETE FROM storage_folder WHERE id = :id")
    fun deleteById(id: Int)
}