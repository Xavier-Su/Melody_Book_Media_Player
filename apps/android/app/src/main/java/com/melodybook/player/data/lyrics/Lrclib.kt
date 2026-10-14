package com.melodybook.player.data.lyrics

import kotlin.math.abs

/** LRCLIB 返回的一条歌词记录。[durationSec] 是它记录的歌曲时长，用来判断是不是同一个版本。 */
data class LrclibItem(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val instrumental: Boolean,
    val synced: String?,
    val plain: String?,
) {
    /** 优先带时间轴的版本。 */
    val text: String? get() = synced ?: plain
}

/** 自动匹配：从搜索结果里挑最可信的一条，纯函数。 */
object LrclibPick {
    const val DEFAULT_TOLERANCE_MS = 3_000L

    /** 时长差在容差内才算同一首；有时间轴的优先，其次时长最接近的。 */
    fun pick(items: List<LrclibItem>, durationMs: Long, toleranceMs: Long = DEFAULT_TOLERANCE_MS): LrclibItem? =
        items
            .filter { !it.instrumental && it.text != null }
            .filter { abs(it.durationSec * 1000L - durationMs) <= toleranceMs }
            .sortedWith(
                compareByDescending<LrclibItem> { it.synced != null }
                    .thenBy { abs(it.durationSec * 1000L - durationMs) },
            )
            .firstOrNull()
}
