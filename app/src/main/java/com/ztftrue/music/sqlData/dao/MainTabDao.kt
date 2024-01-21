package com.ztftrue.music.sqlData.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ztftrue.music.sqlData.model.MainTab

@Dao
interface MainTabDao {

    @Query("SELECT * FROM main_tab ORDER BY priority ASC")
    fun findAllMainTabSortByPriority(): List<MainTab>?

    @Query("SELECT * FROM main_tab WHERE isShow = 1 ORDER BY priority ASC")
    fun findAllIsShowMainTabSortByPriority(): List<MainTab>?

    @Insert
    fun insertAll(list: List<MainTab>)

    @Insert
    fun insert(main: MainTab)

    @Update
    fun update(main: MainTab)

    @Update
    fun updateAll(list: List<MainTab>)
}