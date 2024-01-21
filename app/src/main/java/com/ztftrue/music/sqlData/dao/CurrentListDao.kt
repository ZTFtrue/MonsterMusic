package com.ztftrue.music.sqlData.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ztftrue.music.sqlData.model.CurrentList

@Dao
interface CurrentListDao {

    @Query("SELECT * FROM currentlist ORDER BY id ASC LIMIT 1")
    fun findCurrentList(): CurrentList?

    @Insert
    fun insert(currentList: CurrentList)

    @Update
    fun update(currentList: CurrentList)

    @Query("DELETE FROM currentlist")
    fun delete()
}