package com.melodybook.player.data

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 扫描范围，对应旧版“请选择扫描歌曲类型”的选项。 */
enum class ScanFilter(val label: String, val extensions: Set<String>, val includeVideo: Boolean) {
    ALL("我全都要", setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "ape"), false),
    MP3("仅 MP3", setOf("mp3"), false),
    WAV("仅 WAV", setOf("wav"), false),
    FLAC("仅 FLAC", setOf("flac"), false),
    M4A("仅 M4A", setOf("m4a"), false),
    MP4("隐藏选项：MP4 听声", setOf("mp4"), true),
}

/**
 * 通过 MediaStore 读取本地音频。
 *
 * 旧版递归遍历整个外部存储，在 Android 11+ 的分区存储下基本拿不到文件，
 * 且后缀判断区分大小写。MediaStore 由系统维护索引，速度快，还能直接拿到
 * 歌手、专辑、时长等元数据。
 */
class MediaScanner(private val resolver: ContentResolver) {

    suspend fun scan(filter: ScanFilter, minDurationMs: Long = DEFAULT_MIN_DURATION_MS): List<Song> =
        withContext(Dispatchers.IO) {
            val rows = if (filter.includeVideo) queryVideo() else queryAudio()
            rows.filter { it.format in filter.extensions && it.durationMs >= minDurationMs }
        }

    private val isQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun queryAudio(): List<Song> {
        val collection: Uri = if (isQ) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = baseColumns() + MediaStore.Audio.Media.ALBUM_ID
        // 排除铃声、通知音、闹钟等系统声音
        val selection = "${MediaStore.Audio.Media.IS_ALARM} = 0 AND " +
            "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
            "${MediaStore.Audio.Media.IS_RINGTONE} = 0"
        return resolver.query(collection, projection, selection, null, null)
            ?.use { readRows(it, collection, hasAlbumId = true) }
            .orEmpty()
    }

    private fun queryVideo(): List<Song> {
        val collection: Uri = if (isQ) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        return resolver.query(collection, baseColumns(), null, null, null)
            ?.use { readRows(it, collection, hasAlbumId = false) }
            .orEmpty()
    }

    @Suppress("DEPRECATION")
    private fun baseColumns(): Array<String> = arrayOf(
        BaseColumns._ID,
        MediaStore.MediaColumns.TITLE,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.Audio.AudioColumns.ARTIST,
        MediaStore.Audio.AudioColumns.ALBUM,
        MediaStore.Audio.AudioColumns.DURATION,
        // Android 10+ 用 RELATIVE_PATH；更老的系统只有 DATA（完整路径）
        if (isQ) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA,
    )

    private fun readRows(c: Cursor, collection: Uri, hasAlbumId: Boolean): List<Song> {
        val iId = c.getColumnIndexOrThrow(BaseColumns._ID)
        val iTitle = c.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE)
        val iName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val iSize = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val iAdded = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
        val iArtist = c.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)
        val iAlbum = c.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)
        val iDuration = c.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)
        @Suppress("DEPRECATION")
        val iFolder = c.getColumnIndexOrThrow(
            if (isQ) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA,
        )
        val iAlbumId = if (hasAlbumId) c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID) else -1

        val out = ArrayList<Song>(c.count)
        while (c.moveToNext()) {
            val id = c.getLong(iId)
            val displayName = c.getString(iName).orEmpty()
            val format = displayName.substringAfterLast('.', "").lowercase()
            val rawTitle = c.getString(iTitle).orEmpty()
            val rawArtist = if (iArtist >= 0) c.getString(iArtist) else null
            val (title, artist) = SongMetadata.normalize(rawTitle, rawArtist, displayName)
            val album = (if (iAlbum >= 0) c.getString(iAlbum) else null)
                ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                ?: SongMetadata.UNKNOWN_ALBUM
            val (py, initials) = Pinyin.of(title)

            out += Song(
                id = id,
                uri = Uri.withAppendedPath(collection, id.toString()).toString(),
                title = title,
                artist = artist,
                album = album,
                albumId = if (iAlbumId >= 0) c.getLong(iAlbumId) else -1L,
                durationMs = if (iDuration >= 0) c.getLong(iDuration) else 0L,
                folder = SongMetadata.folderOf(c.getString(iFolder).orEmpty(), isRelative = isQ),
                format = format,
                sizeBytes = c.getLong(iSize),
                dateAddedSec = c.getLong(iAdded),
                pinyin = py,
                initials = initials,
                artistPinyin = Pinyin.of(artist).first,
                fileName = displayName,
            )
        }
        return out
    }

    companion object {
        /** 过滤掉 10 秒以下的短音频（提示音、录音碎片等）。 */
        const val DEFAULT_MIN_DURATION_MS = 10_000L
    }
}
