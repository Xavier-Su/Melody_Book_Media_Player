package com.melodybook.player

import com.melodybook.player.data.card.CardContent
import com.melodybook.player.data.card.CardText
import com.melodybook.player.data.card.LunarInfo
import com.melodybook.player.data.card.Quote
import com.melodybook.player.data.card.QuoteCategory
import com.melodybook.player.data.card.Zodiac
import com.melodybook.player.data.card.cardShareText
import com.melodybook.player.data.card.hitokotoUrl
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LunarInfoTest {

    @Test
    fun springFestival2024() {
        val info = LunarInfo.of(LocalDate.of(2024, 2, 10))
        assertTrue(info.lunarLine, info.lunarLine.startsWith("甲辰年 正月初一"))
        assertEquals("2月10日 星期六", info.solarLine)
    }

    @Test
    fun yiJiAreCappedAndNeverEmpty() {
        val info = LunarInfo.of(LocalDate.of(2024, 5, 1))
        assertTrue(info.yi.isNotEmpty() && info.yi.size <= 6)
        assertTrue(info.ji.isNotEmpty() && info.ji.size <= 6)
    }

    @Test
    fun quoteSourceHandlesMissingParts() {
        assertEquals("刘禹锡 《秋词》", Quote("x", "秋词", "刘禹锡").source)
        assertEquals("《秋词》", Quote("x", "秋词", null).source)
        assertEquals("", Quote("x", null, null).source)
    }

    @Test
    fun shareTextWithoutLunar() {
        val words = CardContent.Words(Quote("一句话", null, "某人"))
        assertEquals("一句话\n—— 某人", cardShareText(words, null))
    }

    @Test
    fun stickShareTextHasNoYiJi() {
        val lunar = LunarInfo.of(LocalDate.of(2024, 2, 10))
        val text = cardShareText(CardContent.Stick("辛戊中吉签", listOf("上句", "下句"), "解签"), lunar)
        assertTrue(text.startsWith("辛戊中吉签\n上句\n下句\n\n解签"))
        assertTrue(!text.contains("宜："))
    }

    @Test
    fun cleanStripsWatermark() {
        assertEquals("整体运势平稳。", CardText.clean("整体运势平稳。星O座O屋"))
        assertEquals("无水印", CardText.clean("  无水印 "))
    }

    @Test
    fun starsAreClamped() {
        assertEquals("★★★☆☆", CardText.stars(3))
        assertEquals("★★★★★", CardText.stars(9))
        assertEquals("☆☆☆☆☆", CardText.stars(-1))
    }

    @Test
    fun hitokotoUrlCarriesTheChosenCategory() {
        val urls = QuoteCategory.entries.map { hitokotoUrl(it) }
        assertEquals(QuoteCategory.entries.size, urls.distinct().size)
        assertTrue(!hitokotoUrl(QuoteCategory.ANY).contains("c="))
        assertTrue(hitokotoUrl(QuoteCategory.POEM).endsWith("&c=i"))
        assertTrue(hitokotoUrl(QuoteCategory.LITERATURE).endsWith("&c=d"))
    }

    @Test
    fun zodiacFallsBackToAries() {
        assertEquals(Zodiac.LEO, Zodiac.fromCode("leo"))
        assertEquals(Zodiac.ARIES, Zodiac.fromCode("nope"))
        assertEquals(Zodiac.ARIES, Zodiac.fromCode(null))
        assertEquals(12, Zodiac.entries.map { it.code }.distinct().size)
    }
}
