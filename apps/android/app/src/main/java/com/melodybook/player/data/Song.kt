package com.melodybook.player.data

import android.content.ContentUris
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一首本地歌曲。
 *
 * 主键直接使用 MediaStore 的 _ID，而不是歌名（旧版用歌名当键，同名歌曲会互相覆盖）。
 * 音频表和视频表共用 MediaStore 的 files 表 _ID，所以两者不会冲突。
 * [hidden] 表示用户“从列表移除”，重新扫描时会保留这个标记，除非用户选择恢复。
 */
@Immutable
@Entity(tableName = "songs", indices = [Index("hidden")])
data class Song(
    @PrimaryKey val id: Long,
    /** content:// Uri，播放一律走它，不再直接读文件路径。 */
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    /** 文件所在目录（仅展示用），例如 "Music/周杰伦/"。 */
    val folder: String,
    /** 小写扩展名，例如 "flac"。 */
    val format: String,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    /** 全拼，例如“周杰伦” -> "zhoujielun"，用于搜索和排序。 */
    val pinyin: String,
    /** 拼音首字母，例如 "zjl"。 */
    val initials: String,
    /** 歌手全拼，用于“按歌手”排序。 */
    val artistPinyin: String,
    val hidden: Boolean = false,
    /**
     * 原始文件名，例如“周杰伦 - 青花瓷.mp3”，用于匹配同名 .lrc 歌词。数据库 v2 新增。
     * defaultValue 必须和迁移语句里的 DEFAULT '' 一致，否则 Room 校验迁移结果时会报错。
     */
    @ColumnInfo(defaultValue = "''")
    val fileName: String = "",
) {
    companion object {
        // 不要写成静态字段：伴生对象的属性会在 Song 类加载时初始化，
        // 纯 JVM 单元测试里调用 Uri.parse 会直接报错。
        fun albumArtUriOf(albumId: Long): Uri =
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
    }
}

/**
 * 封面 Uri。写成扩展属性而不是实体里的属性，避免 Room 把它当成一列。
 * 视频文件（MP4 听声）没有专辑封面，albumId 记为 -1。
 */
val Song.artworkUri: Uri?
    get() = if (albumId > 0) Song.albumArtUriOf(albumId) else null

/**
 * 给人看的文件位置，例如 "Music/周杰伦/青花瓷.mp3"，方便在文件管理器里找到它。
 * 目录来自 MediaStore（Android 10+ 是相对存储根目录的路径），不含存储卷名。
 * 老数据没有文件名时只显示目录。
 */
val Song.displayPath: String
    get() = when {
        folder.isBlank() -> fileName
        fileName.isBlank() -> folder
        else -> "$folder/$fileName"
    }
