package com.melodybook.player.data

import androidx.compose.runtime.Immutable

/** 分类页里的一组歌：一个歌手，或一张专辑。 */
@Immutable
data class SongGroup(
    /** 稳定的标识，用来记住“当前打开的是哪一组”。 */
    val key: String,
    val title: String,
    val subtitle: String,
    val songs: List<Song>,
)

/** 按歌手 / 专辑归类。纯 Kotlin，方便单元测试。组内顺序沿用传入列表的顺序。 */
object SongGroups {

    private class Keyed(val group: SongGroup, val sortKey: String)

    fun byArtist(songs: List<Song>): List<SongGroup> =
        songs.groupBy { it.artist }
            .map { (artist, list) ->
                Keyed(SongGroup("artist:$artist", artist, "${list.size} 首", list), list.first().artistPinyin)
            }
            .sortedWith(compareBy<Keyed>({ it.group.title == SongMetadata.UNKNOWN_ARTIST }, { it.sortKey }))
            .map { it.group }

    /**
     * 有专辑 id 的按 id 分（同名但不同歌手的两张专辑不会混在一起），没有的按专辑名分。
     * [sortKey] 用来排序专辑名，界面层传入拼音转换，这里默认只转小写。
     */
    fun byAlbum(songs: List<Song>, sortKey: (String) -> String = { it.lowercase() }): List<SongGroup> =
        songs.groupBy { if (it.albumId > 0) "id:${it.albumId}" else "name:${it.album}" }
            .map { (key, list) ->
                val album = list.first().album
                val artists = list.map { it.artist }.distinct()
                val who = if (artists.size == 1) artists[0] else "多位歌手"
                Keyed(SongGroup("album:$key", album, "$who · ${list.size} 首", list), sortKey(album))
            }
            .sortedWith(compareBy<Keyed>({ it.group.title == SongMetadata.UNKNOWN_ALBUM }, { it.sortKey }))
            .map { it.group }
}
