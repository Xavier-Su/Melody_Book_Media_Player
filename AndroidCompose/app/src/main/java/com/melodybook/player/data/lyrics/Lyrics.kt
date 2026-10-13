package com.melodybook.player.data.lyrics

import androidx.compose.runtime.Immutable

/** 一行带时间的歌词。双语 LRC 里同一时间的第二行会作为 [translation]。 */
@Immutable
data class LyricLine(val timeMs: Long, val text: String, val translation: String? = null)

@Immutable
sealed interface Lyrics {
    /** 带时间轴，可以跟随播放滚动。 */
    @Immutable
    data class Synced(val lines: List<LyricLine>) : Lyrics

    /** 纯文本歌词，没有时间轴。 */
    @Immutable
    data class Plain(val lines: List<String>) : Lyrics
}

/** LRC 解析，纯函数。 */
object LrcParser {
    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    /** 逐字歌词里的 <mm:ss.xx> 标记，直接去掉。 */
    private val wordTag = Regex("""<\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?>""")
    private val offsetTag = Regex("""\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)
    /** [ti:xxx] [ar:xxx] [offset:xxx] 这类信息标签。 */
    private val idTag = Regex("""^\[[A-Za-z#]+:[^\]]*]$""")

    private val BOM: Char = Char(0xFEFF)

    fun hasTimeTags(text: String): Boolean = timeTag.containsMatchIn(text)

    fun parse(raw: String): Lyrics? {
        // LRC 规范：offset 为正表示歌词整体提前
        val offset = offsetTag.find(raw)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val timed = ArrayList<LyricLine>()
        val plain = ArrayList<String>()

        for (rawLine in raw.lineSequence()) {
            // 去掉可能残留的 BOM（U+FEFF）
            val line = rawLine.trim().trimStart(BOM)
            if (line.isEmpty()) {
                plain += ""
                continue
            }
            // 一行开头可以有多个时间标签：[00:12.00][01:30.00]副歌
            var idx = 0
            val times = ArrayList<Long>(2)
            while (true) {
                val m = timeTag.find(line, idx) ?: break
                if (m.range.first != idx) break
                times += toMillis(m)
                idx = m.range.last + 1
                while (idx < line.length && line[idx] == ' ') idx++
            }
            if (times.isNotEmpty()) {
                val text = wordTag.replace(line.substring(idx), "").trim()
                times.forEach { t -> timed += LyricLine((t - offset).coerceAtLeast(0), text) }
            } else if (!idTag.matches(line)) {
                plain += wordTag.replace(line, "").trim()
            }
        }

        val synced = groupSameTime(timed.sortedBy { it.timeMs })
        if (synced.any { it.text.isNotBlank() }) return Lyrics.Synced(synced)

        val text = plain.dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }
        return if (text.any { it.isNotBlank() }) Lyrics.Plain(text) else null
    }

    /** 当前播放位置对应的行，还没到第一行时返回 -1。 */
    fun activeIndex(lines: List<LyricLine>, positionMs: Long): Int {
        var lo = 0
        var hi = lines.lastIndex
        var ans = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                ans = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return ans
    }

    private fun toMillis(m: MatchResult): Long {
        val min = m.groupValues[1].toLong()
        val sec = m.groupValues[2].toLong()
        val frac = m.groupValues[3]
        val fracMs = when (frac.length) {
            0 -> 0L
            1 -> frac.toLong() * 100
            2 -> frac.toLong() * 10
            else -> frac.toLong()
        }
        return min * 60_000 + sec * 1_000 + fracMs
    }

    /** 同一时间的多行合并：第一行是原文，其余作为翻译。连续的空行只保留一个。 */
    private fun groupSameTime(lines: List<LyricLine>): List<LyricLine> {
        val merged = ArrayList<LyricLine>(lines.size)
        var i = 0
        while (i < lines.size) {
            var j = i
            while (j + 1 < lines.size && lines[j + 1].timeMs == lines[i].timeMs) j++
            val texts = (i..j).map { lines[it].text }.filter { it.isNotBlank() }
            merged += LyricLine(
                timeMs = lines[i].timeMs,
                text = texts.firstOrNull().orEmpty(),
                translation = texts.drop(1).joinToString("\n").ifEmpty { null },
            )
            i = j + 1
        }
        // 空行用来结束上一句的高亮（间奏），但开头的和连续的空行没有意义
        return merged.filterIndexed { k, l -> l.text.isNotBlank() || (k > 0 && merged[k - 1].text.isNotBlank()) }
    }
}

/** 歌曲与 .lrc 文件名的匹配规则，纯函数。 */
object LyricsMatch {
    private val spaces = Regex("[\\s_]+")
    private val dash = Regex("\\s*[-–—－]\\s*")

    fun normalize(name: String): String =
        name.lowercase().replace(spaces, " ").replace(dash, " - ").trim()

    fun baseName(fileName: String): String = fileName.substringBeforeLast('.', fileName)

    /** 按优先级给出可能的歌词文件名（已规范化）。 */
    fun candidates(fileName: String, title: String, artist: String, unknownArtist: String): List<String> {
        val names = ArrayList<String>(4)
        if (fileName.isNotBlank()) names += baseName(fileName)
        if (artist.isNotBlank() && artist != unknownArtist) {
            names += "$artist - $title"
            names += "$title - $artist"
        }
        names += title
        return names.map(::normalize).filter { it.isNotEmpty() }.distinct()
    }
}
