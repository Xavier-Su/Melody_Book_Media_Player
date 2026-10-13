package com.melodybook.player

import com.melodybook.player.data.lyrics.EmbeddedLyrics
import com.melodybook.player.data.lyrics.LrcParser
import com.melodybook.player.data.lyrics.LyricLine
import com.melodybook.player.data.lyrics.Lyrics
import com.melodybook.player.data.lyrics.LyricsMatch
import com.melodybook.player.data.lyrics.TextDecoding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class LyricsTest {

    private fun synced(raw: String): List<LyricLine> = (LrcParser.parse(raw) as Lyrics.Synced).lines

    @Test fun `parses basic lrc and skips id tags`() {
        val lines = synced(
            """
            [ti:青花瓷]
            [ar:周杰伦]
            [00:01.50]素胚勾勒出青花笔锋浓转淡
            [00:05.2]瓶身描绘的牡丹一如你初妆
            """.trimIndent(),
        )
        assertEquals(listOf(1_500L, 5_200L), lines.map { it.timeMs })
        assertEquals("素胚勾勒出青花笔锋浓转淡", lines[0].text)
    }

    @Test fun `multiple time tags on one line`() {
        val lines = synced("[00:10.00][01:20.00]副歌\n[00:30.00]主歌")
        assertEquals(listOf(10_000L, 30_000L, 80_000L), lines.map { it.timeMs })
        assertEquals("副歌", lines[2].text)
    }

    @Test fun `same timestamp becomes translation`() {
        val lines = synced("[00:01.00]Hello\n[00:01.00]你好\n[00:02.00]World")
        assertEquals("Hello", lines[0].text)
        assertEquals("你好", lines[0].translation)
        assertEquals(2, lines.size)
    }

    @Test fun `offset shifts lines earlier`() {
        val lines = synced("[offset:500]\n[00:02.00]a")
        assertEquals(1_500L, lines.single().timeMs)
    }

    @Test fun `word level tags are stripped`() {
        val lines = synced("[00:01.00]<00:01.00>你<00:01.30>好")
        assertEquals("你好", lines.single().text)
    }

    @Test fun `three digit milliseconds`() {
        assertEquals(1_234L, synced("[00:01.234]x").single().timeMs)
    }

    @Test fun `plain text lyrics`() {
        val l = LrcParser.parse("第一行\n第二行")
        assertTrue(l is Lyrics.Plain)
        assertEquals(listOf("第一行", "第二行"), (l as Lyrics.Plain).lines)
    }

    @Test fun `empty or tag-only input yields null`() {
        assertNull(LrcParser.parse(""))
        assertNull(LrcParser.parse("[ti:x]\n[ar:y]"))
    }

    @Test fun `active index by binary search`() {
        val lines = listOf(LyricLine(1_000, "a"), LyricLine(2_000, "b"), LyricLine(3_000, "c"))
        assertEquals(-1, LrcParser.activeIndex(lines, 500))
        assertEquals(0, LrcParser.activeIndex(lines, 1_000))
        assertEquals(1, LrcParser.activeIndex(lines, 2_999))
        assertEquals(2, LrcParser.activeIndex(lines, 99_000))
    }

    @Test fun `lrc file name candidates`() {
        val c = LyricsMatch.candidates("周杰伦 - 青花瓷.mp3", "青花瓷", "周杰伦", "未知歌手")
        assertEquals("周杰伦 - 青花瓷", c.first())
        assertTrue("青花瓷" in c)
        // 不同的分隔写法规范化后一致
        assertEquals(LyricsMatch.normalize("周杰伦-青花瓷"), LyricsMatch.normalize("周杰伦 －  青花瓷"))
    }

    @Test fun `decodes gbk and utf8`() {
        val text = "青花瓷"
        assertEquals(text, TextDecoding.decode(text.toByteArray(Charsets.UTF_8)))
        assertEquals(text, TextDecoding.decode(text.toByteArray(charset("GBK"))))
    }

    @Test fun `reads id3v23 uslt frame`() {
        val lyric = "[00:01.00]你好"
        val body = ByteArrayOutputStream().apply {
            write(3) // UTF-8
            write("chi".toByteArray())
            write(0) // 空描述
            write(lyric.toByteArray(Charsets.UTF_8))
        }.toByteArray()
        val frame = ByteArrayOutputStream().apply {
            write("USLT".toByteArray())
            write(int32(body.size))
            write(byteArrayOf(0, 0))
            write(body)
        }.toByteArray()
        val tag = ByteArrayOutputStream().apply {
            write("ID3".toByteArray())
            write(byteArrayOf(3, 0, 0))
            write(syncSafe(frame.size))
            write(frame)
        }.toByteArray()
        val read = EmbeddedLyrics.read("mp3") { ByteArrayInputStream(tag) }
        assertEquals(lyric, read)
    }

    @Test fun `reads flac vorbis lyrics`() {
        val comment = "LYRICS=[00:02.00]flac"
        val block = ByteArrayOutputStream().apply {
            write(le32(0)) // vendor length
            write(le32(1)) // comment count
            write(le32(comment.toByteArray().size))
            write(comment.toByteArray())
        }.toByteArray()
        val file = ByteArrayOutputStream().apply {
            write("fLaC".toByteArray())
            // STREAMINFO（非最后一块，34 字节）
            write(byteArrayOf(0, 0, 0, 34))
            write(ByteArray(34))
            // VORBIS_COMMENT（最后一块）
            val len = block.size
            write(byteArrayOf((0x80 or 4).toByte(), (len shr 16).toByte(), (len shr 8).toByte(), len.toByte()))
            write(block)
        }.toByteArray()
        assertEquals("[00:02.00]flac", EmbeddedLyrics.read("flac") { ByteArrayInputStream(file) })
    }

    @Test fun `garbage input does not crash`() {
        assertNull(EmbeddedLyrics.read("mp3") { ByteArrayInputStream(ByteArray(3)) })
        assertNull(EmbeddedLyrics.read("flac") { ByteArrayInputStream("fLaC".toByteArray()) })
        assertNull(EmbeddedLyrics.read("ogg") { ByteArrayInputStream(ByteArray(0)) })
    }

    private fun int32(v: Int) = byteArrayOf((v shr 24).toByte(), (v shr 16).toByte(), (v shr 8).toByte(), v.toByte())
    private fun le32(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())
    private fun syncSafe(v: Int) = byteArrayOf(
        ((v shr 21) and 0x7F).toByte(),
        ((v shr 14) and 0x7F).toByte(),
        ((v shr 7) and 0x7F).toByte(),
        (v and 0x7F).toByte(),
    )
}
