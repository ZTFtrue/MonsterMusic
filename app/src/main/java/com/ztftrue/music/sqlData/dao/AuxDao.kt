package com.ztftrue.music.sqlData.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ztftrue.music.sqlData.model.Auxr

@Dao
interface AuxDao {


    @Query("SELECT * FROM aux WHERE id = :id")
    fun findAuxById(id: Long): Auxr?

    @Query("SELECT * FROM aux ORDER BY id ASC LIMIT 1")
    fun findFirstAux(): Auxr?

    @Insert
    fun insert(auxr: Auxr)

    @Update
    fun update(auxr: Auxr)
}