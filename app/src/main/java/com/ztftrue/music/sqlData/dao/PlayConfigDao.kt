package com.ztftrue.music.sqlData.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.ztftrue.music.sqlData.model.PlayConfig

@Dao
interface PlayConfigDao {

    @Query("SELECT * FROM playconfig ORDER BY CASE WHEN trackEffectEnabled = 1 THEN 0 ELSE 1 END, id ASC LIMIT 1")
    fun findConfig(): PlayConfig?

    @Query("DELETE FROM playconfig WHERE id != 0")
    fun cleanupDuplicateConfigs()

    @Insert
    fun insert(playConfig: PlayConfig)

    @Update
    fun update(playConfig: PlayConfig)

    @Upsert
    fun upsert(playConfig: PlayConfig)
}