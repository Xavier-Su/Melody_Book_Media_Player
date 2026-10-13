package com.melodybook.player.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SongDao {

    @Query("SELECT * FROM songs WHERE hidden = 0")
    abstract fun observeVisible(): Flow<List<Song>>

    @Query("SELECT * FROM songs")
    abstract suspend fun getAll(): List<Song>

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    abstract suspend fun getByIds(ids: List<Long>): List<Song>

    @Query("SELECT COUNT(*) FROM songs")
    abstract suspend fun count(): Int

    @Upsert
    abstract suspend fun upsert(songs: List<Song>)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    abstract suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE songs SET hidden = :hidden WHERE id = :id")
    abstract suspend fun setHidden(id: Long, hidden: Boolean)

    /** 在一个事务里应用扫描结果，避免界面看到“删了一半”的中间状态。 */
    @Transaction
    open suspend fun applyMerge(result: MergeResult) {
        // SQLite 单条语句的参数上限是 999，分批处理
        result.deletedIds.chunked(500).forEach { deleteByIds(it) }
        result.upserts.chunked(500).forEach { upsert(it) }
    }
}
