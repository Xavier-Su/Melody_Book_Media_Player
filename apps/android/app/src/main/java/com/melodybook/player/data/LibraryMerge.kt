package com.melodybook.player.data

data class MergeResult(
    val upserts: List<Song>,
    val deletedIds: List<Long>,
    val addedCount: Int,
)

/**
 * 把一次扫描结果合并进现有曲库（纯函数，便于单元测试）。
 *
 * 旧版每次扫描都清空整张表；这里改为增量合并：
 * - 扫描到的歌曲 upsert，并保留原来的“已移除”标记（除非 [restoreHidden]）；
 * - 只在本次扫描覆盖的格式范围（[scope]）内清理没扫到的歌曲（文件已被删除）。
 *   比如“仅 FLAC”扫描不会把曲库里的 MP3 删掉。
 */
fun mergeScan(
    existing: List<Song>,
    scanned: List<Song>,
    restoreHidden: Boolean,
    scope: Set<String>? = null,
): MergeResult {
    val existingById = existing.associateBy { it.id }
    val scannedIds = HashSet<Long>(scanned.size * 2)

    var added = 0
    val upserts = scanned.map { song ->
        scannedIds += song.id
        val old = existingById[song.id]
        if (old == null) added++
        song.copy(hidden = if (restoreHidden) false else old?.hidden ?: false)
    }
    val deleted = existing.asSequence()
        .filter { scope == null || it.format in scope }
        .map { it.id }
        .filterNot { it in scannedIds }
        .toList()
    return MergeResult(upserts = upserts, deletedIds = deleted, addedCount = added)
}
