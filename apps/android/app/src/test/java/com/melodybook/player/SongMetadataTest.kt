package com.melodybook.player

import com.melodybook.player.data.PinyinText
import com.melodybook.player.data.SongMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class SongMetadataTest {

    @Test fun `duration is zero padded`() {
        // 旧版会显示成 "3:5"
        assertEquals("3:05", SongMetadata.formatDuration(185_000))
        assertEquals("0:00", SongMetadata.formatDuration(-5))
        assertEquals("1:02:03", SongMetadata.formatDuration(3_723_000))
    }

    @Test fun `known artist is kept`() {
        assertEquals("青花瓷" to "周杰伦", SongMetadata.normalize("青花瓷", "周杰伦", "x.mp3"))
    }

    @Test fun `artist parsed from file name when tag missing`() {
        assertEquals("青花瓷" to "周杰伦", SongMetadata.normalize("周杰伦 - 青花瓷", "<unknown>", "周杰伦 - 青花瓷.mp3"))
        assertEquals("晴天" to "周杰伦", SongMetadata.normalize("", null, "周杰伦 — 晴天.flac"))
    }

    @Test fun `hyphenated title without spaces is not split`() {
        assertEquals("A-ha" to SongMetadata.UNKNOWN_ARTIST, SongMetadata.normalize("A-ha", null, "A-ha.mp3"))
    }

    @Test fun `folder from relative and absolute paths`() {
        assertEquals("Music/Jay", SongMetadata.folderOf("Music/Jay/", isRelative = true))
        assertEquals("Music/Jay", SongMetadata.folderOf("/storage/emulated/0/Music/Jay/a.mp3", isRelative = false))
        assertEquals("", SongMetadata.folderOf("", isRelative = true))
    }

    @Test fun `pinyin text split into full and initials`() {
        assertEquals("zhoujielun" to "zjl", PinyinText.split("zhou jie lun"))
        assertEquals("qinghuaci" to "qhc", PinyinText.split("Qing Hua Ci"))
        assertEquals("" to "", PinyinText.split("  - "))
    }
}
