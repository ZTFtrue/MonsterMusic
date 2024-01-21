package com.ztftrue.music.sqlData.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ztftrue.music.sqlData.model.MusicItem

@Dao
interface QueueDao {

    @Query("SELECT * FROM queue")
    fun findQueue(): List<MusicItem>?

    @Insert
    fun insert(main: MusicItem)

    @Insert
    fun insertAll(main: List<MusicItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllOrReplace(main: List<MusicItem>)

    @Update
    fun update(main: MusicItem)

    @Query("DELETE FROM queue")
    fun deleteAllQueue()

    @Query("DELETE FROM queue WHERE id = :id")
    fun deleteById(id: Long)
}