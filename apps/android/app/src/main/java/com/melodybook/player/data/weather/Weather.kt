package com.melodybook.player.data.weather

/** 用户设置的天气城市。坐标由城市名查询一次后保存，不使用定位权限。 */
data class WeatherPlace(val name: String, val lat: Double, val lon: Double)

data class Weather(val tempC: Int, val code: Int) {
    /** 主页副标题里的文字，例如 "多云 23°C"。 */
    val display: String
        get() = "${WeatherCodes.describe(code)} ${tempC}°C"
}

/** WMO 天气代码 -> 中文，纯函数。代码表见 Open-Meteo 文档。 */
object WeatherCodes {
    fun describe(code: Int): String = when (code) {
        0, 1 -> "晴"
        2 -> "多云"
        3 -> "阴"
        45, 48 -> "雾"
        51, 53, 55 -> "毛毛雨"
        56, 57, 66, 67 -> "冻雨"
        61 -> "小雨"
        63 -> "中雨"
        65 -> "大雨"
        71 -> "小雪"
        73 -> "中雪"
        75 -> "大雪"
        77 -> "雪粒"
        80, 81 -> "阵雨"
        82 -> "暴雨"
        85, 86 -> "阵雪"
        95 -> "雷阵雨"
        96, 99 -> "雷暴冰雹"
        else -> "天气未知"
    }
}
