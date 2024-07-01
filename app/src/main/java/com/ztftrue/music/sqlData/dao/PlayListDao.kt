package com.ztftrue.music.sqlData.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ztftrue.music.sqlData.model.PlayListData
import com.ztftrue.music.sqlData.model.PlayListTable

@Dao
interface PlayListDao {
    @Query("SELECT * FROM play_list WHERE id = :id")
    fun findPlayListById(id: Long): PlayListTable?

    @Insert
    fun insertPlayListArray(playListTable: ArrayList<PlayListTable>)

    @Insert
    fun insert(playListTable: PlayListTable)

    @Update
    fun update(playListTable: PlayListTable)

    @Query("SELECT * FROM play_list ORDER BY :sortOrder")
    fun findPlayListAll(sortOrder: String?): List<PlayListTable>?

    //---------------------------------//

    @Query("SELECT * FROM play_list_data where playListId = :playListId ORDER BY :sortOrder")
    fun findPlayListTracks(playListId: Long, sortOrder: String?): List<PlayListData>?

    @Query("DELETE FROM play_list_data where playListId = :playListId ")
    fun deleteAllData(playListId: Long)

    @Update
    fun updatePlayListData(list: ArrayList<PlayListData>)

    @Insert
    fun addTracksToPlayList(list: ArrayList<PlayListData>)
}