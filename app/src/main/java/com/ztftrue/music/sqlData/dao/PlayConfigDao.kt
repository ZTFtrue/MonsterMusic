package com.ztftrue.music.sqlData.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ztftrue.music.sqlData.model.PlayConfig

@Dao
interface PlayConfigDao {

    @Query("SELECT * FROM playconfig ORDER BY id ASC LIMIT 1")
    fun findConfig(): PlayConfig?


    @Insert
    fun insert(playConfig: PlayConfig)

    @Update
    fun update(playConfig: PlayConfig)
}