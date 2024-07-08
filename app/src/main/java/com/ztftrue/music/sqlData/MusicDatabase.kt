package com.ztftrue.music.sqlData

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ztftrue.music.sqlData.dao.AuxDao
import com.ztftrue.music.sqlData.dao.CurrentListDao
import com.ztftrue.music.sqlData.dao.DictionaryAppDao
import com.ztftrue.music.sqlData.dao.MainTabDao
import com.ztftrue.music.sqlData.dao.PlayConfigDao
import com.ztftrue.music.sqlData.dao.QueueDao
import com.ztftrue.music.sqlData.dao.SortFiledDao
import com.ztftrue.music.sqlData.dao.StorageFolderDao
import com.ztftrue.music.sqlData.model.Auxr
import com.ztftrue.music.sqlData.model.CurrentList
import com.ztftrue.music.sqlData.model.DictionaryApp
import com.ztftrue.music.sqlData.model.MainTab
import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.sqlData.model.PlayConfig
import com.ztftrue.music.sqlData.model.SortFiledData
import com.ztftrue.music.sqlData.model.StorageFolder
import kotlin.concurrent.Volatile

const val MUSIC_DATABASE_NAME = "default_data.db"


@Database(
    entities = [Auxr::class, CurrentList::class, MainTab::class, PlayConfig::class, MusicItem::class,
        DictionaryApp::class, StorageFolder::class,SortFiledData::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 3, to = 4)
    ]
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun AuxDao(): AuxDao
    abstract fun CurrentListDao(): CurrentListDao
    abstract fun PlayConfigDao(): PlayConfigDao
    abstract fun MainTabDao(): MainTabDao
    abstract fun QueueDao(): QueueDao
    abstract fun DictionaryAppDao(): DictionaryAppDao
    abstract fun StorageFolderDao(): StorageFolderDao
    abstract fun SortFiledDao(): SortFiledDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null
        fun getDatabase(context: Context): MusicDatabase {
            if (INSTANCE == null) {
                synchronized(MusicDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = databaseBuilder(
                            context.applicationContext,
                            MusicDatabase::class.java, MUSIC_DATABASE_NAME
                        )
                            .addMigrations(MIGRATION_1_2) // Add migration path from version 1 to version 2
                            .addMigrations(MIGRATION_2_3)
                            .addMigrations(MIGRATION_3_4)
                            .build()
                    }
                }
            }
            return INSTANCE!!
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Perform the necessary database schema changes for migration from version 1 to version 2
                // You may need to alter tables, add new columns, etc.
                // For example:
                // database.execSQL("ALTER TABLE your_entity ADD COLUMN new_column_name TEXT");
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS dictionary_app (\n" +
                            "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                            "    name TEXT NOT NULL,\n" +
                            "    package_name TEXT NOT NULL,\n" +
                            "    autoGo INTEGER NOT NULL,\n" +
                            "    label TEXT NOT NULL,\n" +
                            "    isShow INTEGER NOT NULL\n" +
                            ");\n"
                )
            }
        }
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS storage_folder (\n" +
                            "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                            "    uri TEXT NOT NULL\n" +
                            ");\n"
                )
            }
        }
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS sort_filed_data (
                type TEXT NOT NULL,
                filed TEXT NOT NULL,
                method TEXT NOT NULL,
                methodName TEXT NOT NULL,
                filedName TEXT NOT NULL,
                PRIMARY KEY(type)
            )
        """.trimIndent()
                )
            }
        }
    }

}