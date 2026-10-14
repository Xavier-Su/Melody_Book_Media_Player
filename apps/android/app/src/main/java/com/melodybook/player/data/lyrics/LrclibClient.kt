package com.melodybook.player.data.lyrics

import com.melodybook.player.data.net.Http
import java.io.IOException
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** LRCLIB（lrclib.net）：免费、无需 key。所有方法会阻塞，必须在 IO 线程调用；网络或解析失败抛 [IOException]。 */
class LrclibClient {

    /**
     * 按歌名（和已知的歌手）找最合适的一条。
     * 先带歌手精确搜，没有再只用歌名搜；每次都要求时长接近，避免配错版本。
     */
    fun lookup(title: String, artist: String?, durationMs: Long): LrclibItem? {
        val queries = buildList {
            if (!artist.isNullOrBlank()) add("track_name=${Http.encode(title)}&artist_name=${Http.encode(artist)}")
            add("q=${Http.encode(title)}")
        }
        for (q in queries) {
            LrclibPick.pick(request(q), durationMs)?.let { return it }
        }
        return null
    }

    /** 手动搜索，关键词会同时匹配歌名、歌手、专辑。只保留有歌词的记录。 */
    fun search(query: String): List<LrclibItem> =
        request("q=${Http.encode(query)}").filter { !it.instrumental && it.text != null }.take(MAX_RESULTS)

    private fun request(params: String): List<LrclibItem> {
        val body = Http.get("$BASE?$params") ?: return emptyList()
        return try {
            parse(JSONArray(body))
        } catch (e: JSONException) {
            throw IOException(e)
        }
    }

    private fun parse(arr: JSONArray): List<LrclibItem> = (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        LrclibItem(
            id = o.optLong("id"),
            title = o.optString("trackName"),
            artist = o.optString("artistName"),
            album = o.optString("albumName"),
            durationSec = o.optDouble("duration", 0.0).roundToInt(),
            instrumental = o.optBoolean("instrumental"),
            synced = o.text("syncedLyrics"),
            plain = o.text("plainLyrics"),
        )
    }

    /** JSON 里的 null 用 optString 会变成字符串 "null"，这里统一处理成真正的 null。 */
    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else getString(key).takeIf { it.isNotBlank() }

    private companion object {
        const val BASE = "https://lrclib.net/api/search"
        const val MAX_RESULTS = 20
    }
}
