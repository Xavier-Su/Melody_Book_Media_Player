package com.melodybook.player.data

import kotlinx.coroutines.flow.Flow

data class ScanReport(val total: Int, val added: Int, val removed: Int)

class LibraryRepository(
    private val dao: SongDao,
    private val scanner: MediaScanner,
) {
    val songs: Flow<List<Song>> = dao.observeVisible()

    suspend fun scan(filter: ScanFilter, restoreHidden: Boolean): ScanReport {
        val scanned = scanner.scan(filter)
        val result = mergeScan(dao.getAll(), scanned, restoreHidden, scope = filter.extensions)
        dao.applyMerge(result)
        return ScanReport(total = scanned.size, added = result.addedCount, removed = result.deletedIds.size)
    }

    suspend fun isEmpty(): Boolean = dao.count() == 0

    suspend fun getById(id: Long): Song? = dao.getByIds(listOf(id)).firstOrNull()

    /** 从列表移除（不删除本地文件）。 */
    suspend fun hide(id: Long) = dao.setHidden(id, true)

    /** 按传入顺序返回歌曲，找不到的（文件已删除）会被跳过。 */
    suspend fun getByIds(ids: List<Long>): List<Song> {
        if (ids.isEmpty()) return emptyList()
        val found = HashMap<Long, Song>(ids.size * 2)
        ids.distinct().chunked(500).forEach { chunk ->
            dao.getByIds(chunk).forEach { found[it.id] = it }
        }
        return ids.mapNotNull { found[it] }
    }
}
