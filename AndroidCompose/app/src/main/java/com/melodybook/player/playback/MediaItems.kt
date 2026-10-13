package com.melodybook.player.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.melodybook.player.data.PlayMode
import com.melodybook.player.data.Song
import com.melodybook.player.data.artworkUri
import androidx.media3.common.Player

fun Song.toMediaItem(): MediaItem {
    val playUri = Uri.parse(uri)
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(playUri)
        // 跨进程传给 MediaSession 时 localConfiguration 可能被裁掉，
        // 把 Uri 同时放进 RequestMetadata，服务端据此恢复。
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(playUri).build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build(),
        )
        .build()
}

/** 是否为本机媒体库地址（content://media/...）。 */
fun Uri.isLocalMedia(): Boolean = scheme == "content" && authority == "media"

/** 确保 MediaItem 带有可播放的 Uri。 */
fun MediaItem.withResolvedUri(): MediaItem {
    if (localConfiguration != null) return this
    val uri = requestMetadata.mediaUri ?: return this
    return buildUpon().setUri(uri).build()
}

fun Player.applyPlayMode(mode: PlayMode) {
    when (mode) {
        PlayMode.LIST_LOOP -> {
            repeatMode = Player.REPEAT_MODE_ALL
            shuffleModeEnabled = false
        }
        PlayMode.SINGLE_LOOP -> {
            repeatMode = Player.REPEAT_MODE_ONE
            shuffleModeEnabled = false
        }
        PlayMode.SHUFFLE -> {
            repeatMode = Player.REPEAT_MODE_ALL
            shuffleModeEnabled = true
        }
    }
}

fun Player.currentPlayMode(): PlayMode = when {
    shuffleModeEnabled -> PlayMode.SHUFFLE
    repeatMode == Player.REPEAT_MODE_ONE -> PlayMode.SINGLE_LOOP
    else -> PlayMode.LIST_LOOP
}

fun PlayMode.next(): PlayMode = when (this) {
    PlayMode.LIST_LOOP -> PlayMode.SINGLE_LOOP
    PlayMode.SINGLE_LOOP -> PlayMode.SHUFFLE
    PlayMode.SHUFFLE -> PlayMode.LIST_LOOP
}
