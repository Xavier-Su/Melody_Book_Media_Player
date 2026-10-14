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
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 查找歌词，按顺序尝试：
 * 1. 音频文件内嵌歌词（MP3 USLT、FLAC LYRICS）；
 * 2. 用户在设置里选的“歌词文件夹”（不需要额外权限，用系统文件选择器授权）；
 * 3. 与歌曲同目录、同名的 .lrc（通过媒体库查找；Android 13+ 媒体库可能不收录 .lrc，所以第 2 步更可靠）。
 */
class LyricsRepository(context: Context, private val online: LrclibClient = LrclibClient()) {

    private val resolver: ContentResolver = context.applicationContext.contentResolver
    private val cache = LruCache<String, Result>(64)

    /** 联网下载的歌词存成文件，下次不再联网。手动选的放 manual，优先级高于内嵌和自动下载的。 */
    private val manualDir = File(context.applicationContext.filesDir, "lyrics/manual")
    private val autoDir = File(context.applicationContext.filesDir, "lyrics/auto")

    /** 结果里带上来源，便于界面提示。 */
    data class Result(val lyrics: Lyrics?, val source: String?)

    /**
     * [allowOnline] 为 false 时不会发任何网络请求，但之前下载保存的歌词仍会显示（那已经是本地文件）。
     * 缓存键带上联网开关，开关切换后不会读到旧状态下的“没有歌词”。
     */
    suspend fun load(song: Song, lyricsFolder: String?, allowOnline: Boolean): Result =
        withContext(Dispatchers.IO) {
            val key = cacheKey(song.id, allowOnline)
            cache.get(key)?.let { return@withContext it }
            val result = find(song, lyricsFolder, allowOnline)
            // 联网失败（网络错误）不缓存，换首歌再回来可以重试
            if (result.lyrics != null || !allowOnline || result.source != FAILED) cache.put(key, result)
            result
        }

    /** 用户更换歌词文件夹后清空缓存。 */
    fun invalidate() {
        cache.evictAll()
        treeCache = null
    }

    /** 手动搜索，阻塞网络调用已放在 IO 线程。网络或解析失败抛 [java.io.IOException]。 */
    suspend fun search(query: String): List<LrclibItem> = withContext(Dispatchers.IO) { online.search(query) }

    /** 保存用户手动选的歌词。内容无法解析时返回 false。 */
    suspend fun saveManual(song: Song, item: LrclibItem): Boolean = withContext(Dispatchers.IO) {
        val text = item.text ?: return@withContext false
        if (LrcParser.parse(text) == null) return@withContext false
        write(File(manualDir, "${song.id}.lrc"), text)
        cache.remove(cacheKey(song.id, true))
        cache.remove(cacheKey(song.id, false))
        true
    }

    private fun cacheKey(id: Long, allowOnline: Boolean) = "$id/$allowOnline"

    private fun find(song: Song, lyricsFolder: String?, allowOnline: Boolean): Result {
        readSaved(File(manualDir, "${song.id}.lrc"))?.let { return Result(it, "手动选择的歌词") }

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

        val autoFile = File(autoDir, "${song.id}.lrc")
        readSaved(autoFile)?.let { return Result(it, "在线歌词") }

        if (!allowOnline) return Result(null, null)
        return try {
            val artist = song.artist.takeIf { it.isNotBlank() && it != SongMetadata.UNKNOWN_ARTIST }
            val item = online.lookup(song.title, artist, song.durationMs)
            val lyrics = item?.text?.let(LrcParser::parse)
            if (lyrics != null) {
                write(autoFile, item.text!!)
                Result(lyrics, "在线歌词")
            } else {
                Result(null, null)
            }
        } catch (e: IOException) {
            Result(null, FAILED)
        }
    }

    private fun readSaved(file: File): Lyrics? =
        if (file.isFile) runCatching { LrcParser.parse(file.readText()) }.getOrNull() else null

    /** 先写临时文件再改名，避免中途失败留下半个文件。 */
    private fun write(file: File, text: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }

    private companion object {
        /** 内部标记：联网请求失败（区别于“查到了但没有”）。 */
        const val FAILED = "__failed__"
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
