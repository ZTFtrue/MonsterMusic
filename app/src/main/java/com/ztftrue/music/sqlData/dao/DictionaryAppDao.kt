package com.ztftrue.music.sqlData.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ztftrue.music.sqlData.model.DictionaryApp

@Dao
interface DictionaryAppDao {

    @Query("SELECT * FROM dictionary_app")
    fun findAllDictionaryApp(): List<DictionaryApp>?

    @Insert
    fun insertAll(list: List<DictionaryApp>)

    @Query("DELETE FROM dictionary_app")
    fun deleteAll()
}