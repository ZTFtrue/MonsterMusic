package com.ztftrue.music.sqlData

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ztftrue.music.sqlData.dao.AuxDao
import com.ztftrue.music.sqlData.dao.CurrentListDao
import com.ztftrue.music.sqlData.dao.MainTabDao
import com.ztftrue.music.sqlData.dao.PlayConfigDao
import com.ztftrue.music.sqlData.dao.QueueDao
import com.ztftrue.music.sqlData.model.Auxr
import com.ztftrue.music.sqlData.model.CurrentList
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.sqlData.model.PlayConfig

@Database(entities = [Auxr::class, CurrentList::class,MainTab::class, PlayConfig::class,MusicItem::class], version = 1, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun AuxDao(): AuxDao
    abstract fun CurrentListDao(): CurrentListDao
    abstract fun PlayConfigDao(): PlayConfigDao
    abstract fun MainTabDao(): MainTabDao
    abstract fun QueueDao(): QueueDao
}