package com.melodybook.player

import com.melodybook.player.data.mergeScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryMergeTest {

    @Test fun `new songs are added and missing songs deleted`() {
        val existing = listOf(song(1), song(2))
        val scanned = listOf(song(2), song(3))
        val r = mergeScan(existing, scanned, restoreHidden = false)
        assertEquals(listOf(2L, 3L), r.upserts.map { it.id })
        assertEquals(listOf(1L), r.deletedIds)
        assertEquals(1, r.addedCount)
    }

    @Test fun `hidden flag survives rescan`() {
        val r = mergeScan(listOf(song(1, hidden = true)), listOf(song(1)), restoreHidden = false)
        assertTrue(r.upserts.single().hidden)
    }

    @Test fun `restore clears hidden flag`() {
        val r = mergeScan(listOf(song(1, hidden = true)), listOf(song(1)), restoreHidden = true)
        assertFalse(r.upserts.single().hidden)
    }

    @Test fun `narrow scan only cleans its own formats`() {
        val mp3 = song(1) // format = mp3
        val flac = song(2).copy(format = "flac")
        // “仅 FLAC”扫描什么都没扫到：FLAC 被清理，MP3 保留
        val r = mergeScan(listOf(mp3, flac), emptyList(), restoreHidden = false, scope = setOf("flac"))
        assertEquals(listOf(2L), r.deletedIds)
    }

    @Test fun `songs with same title stay distinct`() {
        // 旧版用歌名做主键，同名歌曲会互相覆盖
        val r = mergeScan(emptyList(), listOf(song(1, title = "晴天"), song(2, title = "晴天")), false)
        assertEquals(2, r.upserts.size)
        assertEquals(2, r.addedCount)
    }
}
