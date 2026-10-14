package com.melodybook.player.data.net

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** 极简 HTTPS GET。联网功能只有两个 JSON 接口，不值得为此引入网络库。 */
object Http {
    private const val USER_AGENT = "MelodyBook/2.0.0"
    private const val TIMEOUT_MS = 8_000

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    /** 返回响应正文；404 返回 null，其它非 2xx 抛 [IOException]。必须在 IO 线程调用。 */
    fun get(url: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code == 404) return null
            if (code !in 200..299) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
