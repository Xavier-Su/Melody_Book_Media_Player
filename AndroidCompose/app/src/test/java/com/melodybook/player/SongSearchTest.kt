package com.melodybook.player

import com.melodybook.player.data.SongSearch
import com.melodybook.player.data.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongSearchTest {

    private val qhc = song(1, title = "青花瓷", artist = "周杰伦", pinyin = "qinghuaci", initials = "qhc", artistPinyin = "zhoujielun")
    private val dk = song(2, title = "稻香", artist = "周杰伦", pinyin = "daoxiang", initials = "dx", artistPinyin = "zhoujielun")
    private val en = song(3, title = "Yesterday", artist = "The Beatles", pinyin = "yesterday", initials = "y", artistPinyin = "thebeatles")

    @Test fun `matches original text case-insensitively`() {
        assertTrue(SongSearch.matches(qhc, "青花"))
        assertTrue(SongSearch.matches(en, "YESTER"))
        assertTrue(SongSearch.matches(en, "beatles"))
    }

    @Test fun `matches full pinyin and initials`() {
        assertTrue(SongSearch.matches(qhc, "qinghua"))
        assertTrue(SongSearch.matches(qhc, "qhc"))
        assertTrue(SongSearch.matches(qhc, "zhoujie"))
        assertTrue(SongSearch.matches(qhc, "qing hua"))
        assertFalse(SongSearch.matches(qhc, "hc")) // 首字母只按前缀匹配，避免短查询噪音太多
    }

    @Test fun `blank query matches everything`() {
        assertTrue(SongSearch.matches(qhc, "  "))
    }

    @Test fun `sort by title uses pinyin order`() {
        val sorted = SongSearch.filterAndSort(listOf(qhc, en, dk), "", SortOrder.TITLE)
        assertEquals(listOf(2L, 1L, 3L), sorted.map { it.id })
    }

    @Test fun `filter then sort`() {
        val sorted = SongSearch.filterAndSort(listOf(qhc, en, dk), "周杰伦", SortOrder.TITLE)
        assertEquals(listOf(2L, 1L), sorted.map { it.id })
    }

    @Test fun `index letter falls back to hash`() {
        assertEquals('Q', SongSearch.indexLetter(qhc))
        assertEquals('#', SongSearch.indexLetter(song(9, pinyin = "123")))
    }
}
