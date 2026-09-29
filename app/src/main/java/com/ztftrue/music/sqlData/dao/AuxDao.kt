package com.ztftrue.music.sqlData.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.ztftrue.music.sqlData.model.Auxr

@Dao
interface AuxDao {

    @Query("SELECT * FROM aux WHERE id = :id")
    fun findAuxById(id: Long): Auxr?

    @Query("SELECT * FROM aux WHERE id = 0")
    fun findGlobalAux(): Auxr?

    @Query("SELECT * FROM aux WHERE id = 0 LIMIT 1")
    fun findFirstAux(): Auxr?

    @Query("DELETE FROM aux WHERE id = :id")
    fun deleteAuxById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(auxr: Auxr)

    @Update
    fun update(auxr: Auxr)

    @Upsert
    fun upsert(auxr: Auxr)
}