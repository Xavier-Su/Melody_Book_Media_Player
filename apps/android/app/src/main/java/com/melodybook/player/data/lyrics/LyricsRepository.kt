package com.melodybook.player.data.lyrics

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.LruCache
import com.melodybook.player.data.Song
import com.melodybook.player.data.SongMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 查找歌词，按顺序尝试：
 * 1. 音频文件内嵌歌词（MP3 USLT、FLAC LYRICS）；
 * 2. 用户在设置里选的“歌词文件夹”（不需要额外权限，用系统文件选择器授权）；
 * 3. 与歌曲同目录、同名的 .lrc（通过媒体库查找；Android 13+ 媒体库可能不收录 .lrc，所以第 2 步更可靠）。
 */
class LyricsRepository(context: Context) {

    private val resolver: ContentResolver = context.applicationContext.contentResolver
    private val cache = LruCache<Long, Result>(64)

    /** 结果里带上来源，便于界面提示。 */
    data class Result(val lyrics: Lyrics?, val source: String?)

    suspend fun load(song: Song, lyricsFolder: String?): Result = withContext(Dispatchers.IO) {
        cache.get(song.id)?.let { return@withContext it }
        val result = find(song, lyricsFolder)
        cache.put(song.id, result)
        result
    }

    /** 用户更换歌词文件夹后清空缓存。 */
    fun invalidate() {
        cache.evictAll()
        treeCache = null
    }

    private fun find(song: Song, lyricsFolder: String?): Result {
        EmbeddedLyrics.read(song.format) { resolver.openInputStream(Uri.parse(song.uri)) }
            ?.let(LrcParser::parse)
            ?.let { return Result(it, "内嵌歌词") }

        val candidates = LyricsMatch.candidates(
            fileName = song.fileName,
            title = song.title,
            artist = song.artist,
            unknownArtist = SongMetadata.UNKNOWN_ARTIST,
        )

        if (lyricsFolder != null) {
            runCatching { findInTree(Uri.parse(lyricsFolder), candidates) }.getOrNull()
                ?.let { return Result(it, "歌词文件夹") }
        }

        runCatching { findBesideSong(song, candidates) }.getOrNull()
            ?.let { return Result(it, "同名 LRC") }

        return Result(null, null)
    }

    private fun readLrc(uri: Uri): Lyrics? {
        val bytes = resolver.openInputStream(uri)?.use { input ->
            // 歌词文件不会很大，超过 1MB 的不是歌词
            val buf = input.readBytes()
            if (buf.size > 1_048_576) null else buf
        } ?: return null
        return LrcParser.parse(TextDecoding.decode(bytes))
    }

    // ---------------------------------------------------------------- 歌词文件夹（SAF）

    private fun findInTree(treeUri: Uri, candidates: List<String>): Lyrics? {
        val index = treeIndex(treeUri)
        for (c in candidates) {
            index[c]?.let { uri -> readLrc(uri)?.let { return it } }
        }
        return null
    }

    @Volatile private var treeCache: Pair<Uri, Map<String, Uri>>? = null

    /** 列出文件夹（含一层子文件夹）里所有 .lrc，按规范化文件名建索引。 */
    private fun treeIndex(treeUri: Uri): Map<String, Uri> {
        treeCache?.let { (u, map) -> if (u == treeUri) return map }
        val map = HashMap<String, Uri>()
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val subDirs = ArrayList<String>()
        listChildren(treeUri, rootId) { docId, name, isDir ->
            if (isDir) subDirs += docId else addLrc(map, treeUri, docId, name)
        }
        subDirs.take(50).forEach { dir ->
            listChildren(treeUri, dir) { docId, name, isDir ->
                if (!isDir) addLrc(map, treeUri, docId, name)
            }
        }
        treeCache = treeUri to map
        return map
    }

    private fun addLrc(map: HashMap<String, Uri>, treeUri: Uri, docId: String, name: String) {
        if (!name.endsWith(".lrc", ignoreCase = true)) return
        val key = LyricsMatch.normalize(LyricsMatch.baseName(name))
        map.putIfAbsent(key, DocumentsContract.buildDocumentUriUsingTree(treeUri, docId))
    }

    private inline fun listChildren(
        treeUri: Uri,
        parentId: String,
        onEach: (docId: String, name: String, isDir: Boolean) -> Unit,
    ) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        resolver.query(children, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1).orEmpty()
                val isDir = c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                onEach(id, name, isDir)
            }
        }
    }

    // ---------------------------------------------------------------- 同目录同名 .lrc（媒体库）

    @Suppress("DEPRECATION")
    private fun findBesideSong(song: Song, candidates: List<String>): Lyrics? {
        val files = MediaStore.Files.getContentUri("external")
        val isQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val nameCol = MediaStore.MediaColumns.DISPLAY_NAME
        val (selection, args) = if (isQ) {
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND $nameCol LIKE ?" to
                arrayOf("${song.folder}/", "%.lrc")
        } else {
            "${MediaStore.MediaColumns.DATA} LIKE ?" to arrayOf("%/${song.folder}/%.lrc")
        }
        val found = HashMap<String, Uri>()
        resolver.query(files, arrayOf(BaseColumns._ID, nameCol), selection, args, null)?.use { c ->
            while (c.moveToNext()) {
                val key = LyricsMatch.normalize(LyricsMatch.baseName(c.getString(1).orEmpty()))
                found.putIfAbsent(key, android.content.ContentUris.withAppendedId(files, c.getLong(0)))
            }
        }
        for (cand in candidates) {
            found[cand]?.let { uri -> readLrc(uri)?.let { return it } }
        }
        return null
    }
}
