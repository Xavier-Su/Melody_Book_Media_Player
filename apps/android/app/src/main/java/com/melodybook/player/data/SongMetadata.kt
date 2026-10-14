package com.melodybook.player.data

import java.util.Locale

/** 元数据清洗，纯函数。 */
object SongMetadata {

    const val UNKNOWN_ARTIST = "未知歌手"
    const val UNKNOWN_ALBUM = "未知专辑"

    private val dashSeparator = Regex("\\s+[-–—]\\s+")

    /**
     * 很多下载来的歌曲没有写标签，文件名形如“周杰伦 - 青花瓷.mp3”。
     * 当 MediaStore 给出的歌手是空或 <unknown> 时，尝试从标题/文件名里拆出歌手。
     */
    fun normalize(rawTitle: String, rawArtist: String?, displayName: String): Pair<String, String> {
        val artistKnown = !rawArtist.isNullOrBlank() && rawArtist != "<unknown>"
        if (artistKnown) return rawTitle.trim() to rawArtist!!.trim()

        val base = rawTitle.ifBlank { displayName.substringBeforeLast('.') }.trim()
        val parts = base.split(dashSeparator, limit = 2)
        return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            parts[1].trim() to parts[0].trim()
        } else {
            base to UNKNOWN_ARTIST
        }
    }

    /**
     * 计算展示用的目录。
     * @param raw Android 10+ 为 RELATIVE_PATH（如 "Music/Jay/"），更老系统为完整文件路径。
     */
    fun folderOf(raw: String, isRelative: Boolean): String {
        if (raw.isBlank()) return ""
        if (isRelative) return raw.trimEnd('/')
        val dir = raw.substringBeforeLast('/', "")
        val storageRoot = Regex("^/storage/[^/]+/(\\d+/)?|^/sdcard/")
        return dir.replaceFirst(storageRoot, "").trimEnd('/')
    }

    /** 毫秒 -> "3:05" / "1:02:03"。旧版没有补零，会显示成 "3:5"。 */
    fun formatDuration(ms: Long): String {
        val totalSec = (ms.coerceAtLeast(0) / 1000)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        // Locale.ROOT：避免阿拉伯语等系统语言下显示成非拉丁数字
        return if (h > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.ROOT, "%d:%02d", m, s)
        }
    }
}
