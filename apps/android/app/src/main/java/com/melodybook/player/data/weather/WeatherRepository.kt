package com.melodybook.player.data.weather

import com.melodybook.player.data.net.Http
import java.io.IOException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/** Open-Meteo：无需 key。城市名 -> 坐标（地理编码），坐标 -> 当前天气。 */
class WeatherRepository {

    private data class Cached(val place: WeatherPlace, val at: Long, val weather: Weather)

    @Volatile private var cached: Cached? = null

    /** 城市名查坐标。找不到返回 null；网络问题抛 [IOException]。 */
    suspend fun geocode(name: String): WeatherPlace? = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=${Http.encode(name)}&count=1&language=zh&format=json"
        val body = Http.get(url) ?: return@withContext null
        try {
            val hit = JSONObject(body).optJSONArray("results")?.optJSONObject(0)
                ?: return@withContext null
            WeatherPlace(hit.optString("name", name), hit.getDouble("latitude"), hit.getDouble("longitude"))
        } catch (e: JSONException) {
            throw IOException(e)
        }
    }

    /** 当前天气，失败返回 null（界面上就是不显示）。10 分钟内重复调用直接用缓存。 */
    suspend fun current(place: WeatherPlace): Weather? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cached?.let { if (it.place == place && now - it.at < CACHE_MS) return@withContext it.weather }
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${place.lat}&longitude=${place.lon}" +
                "&current=temperature_2m,weather_code&timezone=auto"
            val body = Http.get(url) ?: return@withContext null
            val cur = JSONObject(body).getJSONObject("current")
            val w = Weather(cur.getDouble("temperature_2m").roundToInt(), cur.getInt("weather_code"))
            cached = Cached(place, now, w)
            w
        } catch (e: IOException) {
            null
        } catch (e: JSONException) {
            null
        }
    }

    private companion object {
        const val CACHE_MS = 10 * 60_000L
    }
}
