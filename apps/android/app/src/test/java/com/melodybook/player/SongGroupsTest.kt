package com.melodybook.player

import com.melodybook.player.data.SongGroups
import com.melodybook.player.data.SongMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class SongGroupsTest {

    @Test
    fun `artists are grouped and unknown goes last`() {
        val songs = listOf(
            song(1, artist = SongMetadata.UNKNOWN_ARTIST, artistPinyin = "weizhi"),
            song(2, artist = "b", artistPinyin = "b"),
            song(3, artist = "a", artistPinyin = "a"),
            song(4, artist = "b", artistPinyin = "b"),
        )
        val groups = SongGroups.byArtist(songs)
        assertEquals(listOf("a", "b", SongMetadata.UNKNOWN_ARTIST), groups.map { it.title })
        assertEquals(listOf(2L, 4L), groups[1].songs.map { it.id })
        assertEquals("2 首", groups[1].subtitle)
    }

    @Test
    fun `albums with the same name but different ids stay separate`() {
        val songs = listOf(
            song(1, artist = "x", album = "Greatest", albumId = 10),
            song(2, artist = "y", album = "Greatest", albumId = 20),
            song(3, artist = "x", album = "Greatest", albumId = 10),
        )
        val groups = SongGroups.byAlbum(songs)
        assertEquals(2, groups.size)
        assertEquals(listOf(1L, 3L), groups.first { it.key == "album:id:10" }.songs.map { it.id })
    }

    @Test
    fun `album shows the artist, or various artists`() {
        val songs = listOf(
            song(1, artist = "x", album = "Solo", albumId = 1),
            song(2, artist = "x", album = "Mix", albumId = 2),
            song(3, artist = "y", album = "Mix", albumId = 2),
        )
        val byTitle = SongGroups.byAlbum(songs).associateBy { it.title }
        assertEquals("x · 1 首", byTitle.getValue("Solo").subtitle)
        assertEquals("多位歌手 · 2 首", byTitle.getValue("Mix").subtitle)
    }

    @Test
    fun `songs without album id are grouped by name and unknown album goes last`() {
        val songs = listOf(
            song(1, album = SongMetadata.UNKNOWN_ALBUM, albumId = -1),
            song(2, album = "Zed", albumId = -1),
            song(3, album = "Zed", albumId = -1),
        )
        val groups = SongGroups.byAlbum(songs)
        assertEquals(listOf("Zed", SongMetadata.UNKNOWN_ALBUM), groups.map { it.title })
        assertEquals(2, groups[0].songs.size)
    }
}
