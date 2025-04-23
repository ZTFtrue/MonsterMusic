package com.ztftrue.music.sqlData.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ztftrue.music.sqlData.model.SortFiledData

@Dao
interface SortFiledDao {

    @Query("SELECT * FROM sort_filed_data WHERE type = :type")
    fun findSortByType(type: String): SortFiledData?

    @Query("SELECT * FROM sort_filed_data")
    fun findSortAll(): List<SortFiledData>

    @Query("SELECT * FROM sort_filed_data WHERE type LIKE '%' || :substring")
    fun findSortAllTracksData(substring:String="@Tracks"): List<SortFiledData>

    @Insert
    fun insert(sortFiledData: SortFiledData)

    @Update
    fun update(sortFiledData: SortFiledData)
}