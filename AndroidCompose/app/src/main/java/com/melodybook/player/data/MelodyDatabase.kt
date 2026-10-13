package com.melodybook.player.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Song::class], version = 2, exportSchema = true)
abstract class MelodyDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        /** v2：新增 fileName 列。保留原有数据（包括“已移除”标记），下次扫描时填上文件名。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN fileName TEXT NOT NULL DEFAULT ''")
            }
        }

        fun create(context: Context): MelodyDatabase =
            Room.databaseBuilder(context, MelodyDatabase::class.java, "melody.db")
                .addMigrations(MIGRATION_1_2)
                // 兜底：万一迁移失败，曲库可以重新扫描得到
                .fallbackToDestructiveMigration()
                .build()
    }
}
