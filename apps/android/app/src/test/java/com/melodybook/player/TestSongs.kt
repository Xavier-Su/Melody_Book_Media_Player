package com.melodybook.player

import com.melodybook.player.data.Song

fun song(
    id: Long,
    title: String = "Song $id",
    artist: String = "Artist",
    pinyin: String = title.lowercase(),
    initials: String = "",
    artistPinyin: String = artist.lowercase(),
    durationMs: Long = 200_000,
    dateAdded: Long = id,
    hidden: Boolean = false,
) = Song(
    id = id,
    uri = "content://media/external/audio/media/$id",
    title = title,
    artist = artist,
    album = "Album",
    albumId = 1,
    durationMs = durationMs,
    folder = "Music",
    format = "mp3",
    sizeBytes = 1,
    dateAddedSec = dateAdded,
    pinyin = pinyin,
    initials = initials,
    artistPinyin = artistPinyin,
    hidden = hidden,
)
