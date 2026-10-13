package com.melodybook.player.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.melodybook.player.data.LibraryRepository
import com.melodybook.player.data.PlayMode
import com.melodybook.player.data.SettingsRepository
import com.melodybook.player.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/** 界面需要的播放状态快照（不含进度，进度单独按需轮询）。 */
data class PlayerUiState(
    val connected: Boolean = false,
    val currentId: Long? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUri: Uri? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0,
    val playMode: PlayMode = PlayMode.LIST_LOOP,
) {
    val hasMedia: Boolean get() = currentId != null
}

/**
 * 界面一侧与播放服务的连接。
 *
 * 旧版通过 Binder + 全局广播 + 静态变量在 Activity 和 Service 之间同步状态，
 * 两边各自维护“当前第几首”，容易不一致。这里只有一个真相来源：服务里的播放器。
 * 界面通过 MediaController 发指令，并监听播放器事件刷新 [state]。
 */
class PlayerConnection(
    context: Context,
    private val scope: CoroutineScope,
    private val library: LibraryRepository,
    private val settings: SettingsRepository,
) {
    private val appContext = context.applicationContext
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh(player)
    }

    init {
        scope.launch {
            val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
            val c = runCatching { MediaController.Builder(appContext, token).buildAsync().await() }
                .getOrNull() ?: return@launch
            controller = c
            c.addListener(listener)
            if (c.mediaItemCount == 0) restoreLastSession(c)
            refresh(c)
        }
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    /** 播放进度。只在界面收集时轮询，App 退到后台就停（旧版的 Handler 会一直跑）。 */
    fun positionFlow(intervalMs: Long = 500): Flow<Long> = flow {
        while (true) {
            emit(controller?.currentPosition ?: 0L)
            delay(intervalMs)
        }
    }.distinctUntilChanged()

    // ---------------------------------------------------------------- 指令

    fun playQueue(queue: List<Song>, startIndex: Int) {
        val c = controller ?: return
        if (queue.isEmpty()) return
        c.setMediaItems(queue.map { it.toMediaItem() }, startIndex.coerceIn(0, queue.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        when {
            c.mediaItemCount == 0 -> Unit
            c.isPlaying -> c.pause()
            else -> {
                if (c.playbackState == Player.STATE_IDLE) c.prepare()
                if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition()
                c.play()
            }
        }
    }

    fun next() {
        controller?.seekToNext()
    }

    /** 播放超过 3 秒时回到本曲开头，否则上一首（与系统播放器习惯一致）。 */
    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun setPlayMode(mode: PlayMode) {
        controller?.applyPlayMode(mode)
    }

    fun cyclePlayMode(): PlayMode? {
        val c = controller ?: return null
        val next = c.currentPlayMode().next()
        c.applyPlayMode(next)
        return next
    }

    /**
     * 插到当前歌曲后面播放；队列为空时直接开始播放。
     * @return 是否真的改动了队列（正在播放的就是这首时返回 false）。
     */
    fun playNext(song: Song): Boolean {
        val c = controller ?: return false
        if (c.mediaItemCount == 0) {
            playQueue(listOf(song), 0)
            return true
        }
        if (c.currentMediaItem?.mediaId == song.id.toString()) return false
        // 已在队列里的先移除，避免重复（MediaController 会立即更新本地索引）
        removeFromQueue(song.id)
        c.addMediaItem((c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount), song.toMediaItem())
        return true
    }

    /** 歌曲被从列表移除时，同步把它从播放队列里拿掉。 */
    fun removeFromQueue(songId: Long) {
        val c = controller ?: return
        val idStr = songId.toString()
        for (i in c.mediaItemCount - 1 downTo 0) {
            if (c.getMediaItemAt(i).mediaId == idStr) c.removeMediaItem(i)
        }
    }

    // ---------------------------------------------------------------- 内部

    private suspend fun restoreLastSession(c: MediaController) {
        val snap = settings.loadSnapshot() ?: return
        val songs = library.getByIds(snap.queueIds)
        if (songs.isEmpty()) return
        val currentId = snap.queueIds[snap.index]
        val index = songs.indexOfFirst { it.id == currentId }
        c.setMediaItems(
            songs.map { it.toMediaItem() },
            index.coerceAtLeast(0),
            if (index >= 0) snap.positionMs else 0L,
        )
        c.applyPlayMode(settings.current().playMode)
        // 只准备，不自动播放
        c.prepare()
    }

    private fun refresh(p: Player) {
        val item = p.currentMediaItem
        val md: MediaMetadata = p.mediaMetadata
        _state.value = PlayerUiState(
            connected = true,
            currentId = item?.mediaId?.toLongOrNull(),
            title = md.title?.toString().orEmpty(),
            artist = md.artist?.toString().orEmpty(),
            album = md.albumTitle?.toString().orEmpty(),
            artworkUri = md.artworkUri,
            isPlaying = p.isPlaying,
            isBuffering = p.playbackState == Player.STATE_BUFFERING,
            durationMs = p.duration.takeIf { it > 0 } ?: 0L,
            playMode = p.currentPlayMode(),
        )
    }
}
