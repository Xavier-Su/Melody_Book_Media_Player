package com.melodybook.player.data.card

import com.nlf.calendar.Solar
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Hitokoto 的分类参数，null 表示不限。 */
enum class QuoteCategory(val label: String, val code: String?) {
    ANY("随机", null),
    POEM("诗词", "i"),
    ANIME("动画", "a"),
    MUSIC("网易云", "j"),
    LITERATURE("文学", "d"),
}

/** Hitokoto 请求地址。分类为“随机”时不带 c 参数。 */
fun hitokotoUrl(category: QuoteCategory): String =
    "https://v1.hitokoto.cn/?encode=json&charset=utf-8" + (category.code?.let { "&c=$it" } ?: "")

/** 抽什么：一句美句、一支灵签，还是今日星座运势。 */
enum class CardKind(val label: String) { QUOTE("美句"), STICK("灵签"), HOROSCOPE("星座") }

/** [code] 是接口要求的英文名。 */
enum class Zodiac(val label: String, val code: String) {
    ARIES("白羊座", "aries"),
    TAURUS("金牛座", "taurus"),
    GEMINI("双子座", "gemini"),
    CANCER("巨蟹座", "cancer"),
    LEO("狮子座", "leo"),
    VIRGO("处女座", "virgo"),
    LIBRA("天秤座", "libra"),
    SCORPIO("天蝎座", "scorpio"),
    SAGITTARIUS("射手座", "sagittarius"),
    CAPRICORN("摩羯座", "capricorn"),
    AQUARIUS("水瓶座", "aquarius"),
    PISCES("双鱼座", "pisces");

    companion object {
        fun fromCode(code: String?): Zodiac = entries.firstOrNull { it.code == code } ?: ARIES
    }
}

/** 一句美句。[from] 是出处，[author] 是作者，都可能为空。 */
data class Quote(val text: String, val from: String?, val author: String?) {
    /** 例如 "刘禹锡 《秋词》"。 */
    val source: String
        get() = listOfNotNull(author, from?.let { "《$it》" }).joinToString(" ")
}

/** 卡片正文，三种来源各有各的版式。 */
sealed interface CardContent {
    data class Words(val quote: Quote) : CardContent

    /** [poem] 已按句拆好，[explain] 是解签。 */
    data class Stick(val title: String, val poem: List<String>, val explain: String) : CardContent

    /** [stars] 是综合运势 0~5 星。其余字段接口可能不给，缺了就不显示。 */
    data class Horoscope(
        val sign: String,
        val stars: Int,
        val shortComment: String?,
        val text: String?,
        val luckyColor: String?,
        val luckyNumber: String?,
        val luckyConstellation: String?,
        val yi: String?,
        val ji: String?,
    ) : CardContent
}

/** 接口返回文字的清理，纯函数。 */
object CardText {
    /** 星座接口会在正文里夹带这个广告水印。 */
    private const val WATERMARK = "星O座O屋"

    fun clean(raw: String): String = raw.replace(WATERMARK, "").trim()

    /** 3 -> "★★★☆☆"。 */
    fun stars(n: Int): String {
        val k = n.coerceIn(0, 5)
        return "★".repeat(k) + "☆".repeat(5 - k)
    }
}

/** 某一天的农历信息，由本机算法得出。 */
data class LunarInfo(
    /** 例如 "10月14日 星期三"。 */
    val solarLine: String,
    /** 例如 "丙午年 九月初五 · 马"。 */
    val lunarLine: String,
    val yi: List<String>,
    val ji: List<String>,
) {
    companion object {
        /** 宜忌条目很多，卡片上只放得下前几项。 */
        private const val MAX_ITEMS = 6

        fun of(date: LocalDate): LunarInfo {
            val lunar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
            val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)
            return LunarInfo(
                solarLine = "${date.monthValue}月${date.dayOfMonth}日 $weekday",
                lunarLine = "${lunar.yearInGanZhi}年 ${lunar.monthInChinese}月${lunar.dayInChinese} · ${lunar.yearShengXiao}",
                yi = lunar.dayYi.take(MAX_ITEMS).ifEmpty { listOf("无") },
                ji = lunar.dayJi.take(MAX_ITEMS).ifEmpty { listOf("无") },
            )
        }
    }
}

/** 分享出去的纯文本。农历宜忌只跟美句一起出现，灵签和星座有自己的内容。 */
fun cardShareText(content: CardContent, lunar: LunarInfo?): String = buildString {
    when (content) {
        is CardContent.Words -> {
            append(content.quote.text)
            if (content.quote.source.isNotEmpty()) append("\n—— ").append(content.quote.source)
        }
        is CardContent.Stick -> {
            append(content.title).append('\n').append(content.poem.joinToString("\n"))
            append("\n\n").append(content.explain)
        }
        is CardContent.Horoscope -> {
            append(content.sign).append(" 今日运势 ").append(CardText.stars(content.stars))
            content.shortComment?.let { append('\n').append(it) }
            content.text?.let { append("\n\n").append(it) }
            val lucky = listOfNotNull(
                content.luckyColor?.let { "幸运色 $it" },
                content.luckyNumber?.let { "幸运数字 $it" },
                content.luckyConstellation?.let { "速配 $it" },
            )
            if (lucky.isNotEmpty()) append("\n\n").append(lucky.joinToString("  "))
            listOfNotNull(content.yi, content.ji).forEach { append('\n').append(it) }
        }
    }
    if (lunar != null) {
        append("\n\n").append(lunar.solarLine).append("  ").append(lunar.lunarLine)
        if (content is CardContent.Words) {
            append("\n宜：").append(lunar.yi.joinToString(" "))
            append("\n忌：").append(lunar.ji.joinToString(" "))
        }
    }
}
