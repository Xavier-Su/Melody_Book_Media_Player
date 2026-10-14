package com.melodybook.player.data.card

import com.melodybook.player.data.net.Http
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * 抽卡用到的联网接口，都无需 key，全部走 HTTPS：
 * - Hitokoto：随机美句；
 * - xxapi.cn：文昌帝君灵签、星座运势；
 * - picsum.photos：背景图（只拼地址，由 Coil 去加载）。
 * 失败抛 [IOException]。
 */
class CardRepository {

    suspend fun quote(category: QuoteCategory): CardContent.Words = withContext(Dispatchers.IO) {
        val url = hitokotoUrl(category)
        retry {
            val body = Http.get(url) ?: throw IOException("empty response")
            try {
                val o = JSONObject(body)
                CardContent.Words(Quote(o.getString("hitokoto"), o.text("from"), o.text("from_who")))
            } catch (e: JSONException) {
                throw IOException(e)
            }
        }
    }

    suspend fun stick(): CardContent.Stick = withContext(Dispatchers.IO) {
        retry {
            val d = xxapi("https://v2.xxapi.cn/api/wenchangdijunrandom")
            try {
                CardContent.Stick(
                    title = CardText.clean(d.getString("title")),
                    // 上下两句之间是 tab
                    poem = CardText.clean(d.getString("poem")).split('\t', '\n').map { it.trim() }.filter { it.isNotEmpty() },
                    explain = CardText.clean(d.getString("content")),
                )
            } catch (e: JSONException) {
                throw IOException(e)
            }
        }
    }

    suspend fun horoscope(zodiac: Zodiac): CardContent.Horoscope = withContext(Dispatchers.IO) {
        retry {
            val d = xxapi("https://v2.xxapi.cn/api/horoscope?type=${zodiac.code}&time=today")
            val todo = d.optJSONObject("todo")
            CardContent.Horoscope(
                sign = d.text("title") ?: zodiac.label,
                stars = d.optJSONObject("fortune")?.optInt("all") ?: 0,
                shortComment = d.text("shortcomment"),
                text = d.optJSONObject("fortunetext")?.text("all"),
                luckyColor = d.text("luckycolor"),
                luckyNumber = d.text("luckynumber"),
                luckyConstellation = d.text("luckyconstellation"),
                yi = todo?.text("yi"),
                ji = todo?.text("ji"),
            )
        }
    }

    /** 背景图地址。每次用不同的 seed，否则 picsum 会一直给同一张。 */
    fun imageUrl(): String = "https://picsum.photos/seed/${System.nanoTime()}/800/1200"

    /** xxapi 统一返回 {"code":200,"data":{...}}，code 不是 200 就当失败。 */
    private fun xxapi(url: String): JSONObject {
        val body = Http.get(url) ?: throw IOException("empty response")
        val root = try {
            JSONObject(body)
        } catch (e: JSONException) {
            throw IOException(e)
        }
        if (root.optInt("code") != 200) throw IOException("api code ${root.optInt("code")}")
        return root.optJSONObject("data") ?: throw IOException("no data")
    }

    /** 网络偶尔抖一下，失败再试一次。 */
    private inline fun <T> retry(block: () -> T): T {
        try {
            return block()
        } catch (e: IOException) {
            return block()
        }
    }

    /** JSON 里的 null 用 optString 会变成字符串 "null"；这里统一处理成真正的 null，并清掉水印。 */
    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else CardText.clean(getString(key)).takeIf { it.isNotEmpty() }
}
