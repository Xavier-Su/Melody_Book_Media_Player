package com.melodybook.player.data

enum class SortOrder(val label: String) {
    TITLE("按歌名"),
    ARTIST("按歌手"),
    DATE_ADDED("最近添加"),
    DURATION("按时长"),
}

/**
 * 列表搜索与排序，纯函数。
 *
 * 支持：原文包含（歌名 / 歌手 / 专辑 / 文件夹）、全拼包含、首字母前缀。
 * 例如“青花瓷”可以用 "青花"、"qinghua"、"qhc" 搜到。
 */
object SongSearch {

    fun matches(song: Song, rawQuery: String): Boolean {
        val q = rawQuery.trim().lowercase()
        if (q.isEmpty()) return true
        if (song.title.lowercase().contains(q)) return true
        if (song.artist.lowercase().contains(q)) return true
        if (song.album.lowercase().contains(q)) return true
        if (song.folder.lowercase().contains(q)) return true

        val compact = q.filter { it.isLetterOrDigit() }
        if (compact.isEmpty()) return false
        if (song.pinyin.contains(compact)) return true
        if (song.artistPinyin.contains(compact)) return true
        return song.initials.startsWith(compact)
    }

    fun filterAndSort(songs: List<Song>, query: String, order: SortOrder): List<Song> {
        val filtered = if (query.isBlank()) songs else songs.filter { matches(it, query) }
        return when (order) {
            SortOrder.TITLE -> filtered.sortedWith(compareBy({ it.pinyin }, { it.title }))
            SortOrder.ARTIST -> filtered.sortedWith(compareBy({ it.artistPinyin }, { it.pinyin }))
            SortOrder.DATE_ADDED -> filtered.sortedByDescending { it.dateAddedSec }
            SortOrder.DURATION -> filtered.sortedByDescending { it.durationMs }
        }
    }

    /** 列表侧边索引用的分组字母：A-Z，其他归入 "#"。 */
    fun indexLetter(song: Song): Char {
        val c = song.pinyin.firstOrNull()?.uppercaseChar() ?: return '#'
        return if (c in 'A'..'Z') c else '#'
    }
}
