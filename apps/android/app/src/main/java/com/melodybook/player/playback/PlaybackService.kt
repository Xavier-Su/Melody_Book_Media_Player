package com.melodybook.player.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.melodybook.player.MainActivity
import androidx.media3.common.PlaybackException
import com.melodybook.player.R
import com.melodybook.player.appContainer
import com.melodybook.player.data.PlaybackSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 后台播放服务。
 *
 * 对比旧版 MyService，这里交给 Media3 处理的事情：
 * - 系统媒体通知、锁屏控制、蓝牙/耳机按键、车机（MediaSession）；
 * - 音频焦点（来电、其他 App 播放时自动让步）；
 * - 拔出耳机自动暂停；
 * - 播完自动切下一首 / 单曲循环 / 随机（不再靠每秒比对时间字符串）；
 * - 前台服务的启动与停止。
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private val container by lazy { appContainer }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var session: MediaSession? = null
    private var progressSaver: Job? = null

    override fun onCreate() {
        super.onCreate()

        // 在默认音频链后面插一个只“旁听”的频谱处理器，声音本身不变
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf(SpectrumProcessor()))
                .build()
        }

        val player = ExoPlayer.Builder(this, renderers)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(PlayerEvents(player))

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(SessionCallback())
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.notification_channel_name)
                .build()
                .apply { setSmallIcon(R.drawable.ic_notification) },
        )

        scope.launch {
            player.applyPlayMode(container.settings.current().playMode)
        }
        observeSleepTimer(player)
    }

    private companion object {
        const val MAX_SKIP_ON_ERROR = 10
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** 用户从最近任务里划掉 App：没在播放就顺手停掉服务，在播放则继续后台播放。 */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.let { s ->
            saveSnapshot(s.player)
            s.player.release()
            s.release()
        }
        session = null
        scope.cancel()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- 定时关闭

    private fun observeSleepTimer(player: ExoPlayer) {
        scope.launch {
            container.sleepTimer.state.collectLatest { state ->
                player.pauseAtEndOfMediaItems = state is SleepTimer.State.AfterCurrent
                if (state is SleepTimer.State.Counting) {
                    val remaining = container.sleepTimer.remainingMs(state)
                    if (remaining > 0) delay(remaining)
                    try {
                        fadeOutAndPause(player)
                    } finally {
                        // 淡出途中用户取消或改了定时，也要把音量恢复
                        player.volume = 1f
                    }
                    container.sleepTimer.cancel()
                }
            }
        }
    }

    /** 3 秒淡出再暂停，比戛然而止更适合睡前听。 */
    private suspend fun fadeOutAndPause(player: ExoPlayer) {
        val steps = 15
        for (i in steps downTo 1) {
            player.volume = i / steps.toFloat()
            delay(200)
        }
        player.pause()
    }

    // ---------------------------------------------------------------- 播放进度记忆

    private fun saveSnapshot(player: Player) {
        if (player.mediaItemCount == 0) return
        val ids = (0 until player.mediaItemCount).mapNotNull {
            player.getMediaItemAt(it).mediaId.toLongOrNull()
        }
        val snapshot = PlaybackSnapshot(ids, player.currentMediaItemIndex, player.currentPosition)
        // 用进程级作用域，保证服务销毁时也能写完
        container.appScope.launch { container.settings.saveSnapshot(snapshot) }
    }

    private inner class PlayerEvents(private val player: ExoPlayer) : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            progressSaver?.cancel()
            if (isPlaying) {
                progressSaver = scope.launch {
                    while (isActive) {
                        delay(10_000)
                        saveSnapshot(player)
                    }
                }
            } else {
                saveSnapshot(player)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = saveSnapshot(player)

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            // “播完当前这首就停”触发了
            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                container.sleepTimer.cancel()
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) = persistPlayMode()
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = persistPlayMode()

        private fun persistPlayMode() {
            val mode = player.currentPlayMode()
            container.appScope.launch { container.settings.setPlayMode(mode) }
        }

        private var consecutiveErrors = 0

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) consecutiveErrors = 0
        }

        /**
         * 文件被删除或格式不支持时跳到下一首，而不是卡住。
         * 列表循环模式下 hasNextMediaItem 恒为真，所以加个上限，避免整个队列都坏时无限重试。
         */
        override fun onPlayerError(error: PlaybackException) {
            consecutiveErrors++
            if (consecutiveErrors < player.mediaItemCount.coerceAtMost(MAX_SKIP_ON_ERROR) &&
                player.hasNextMediaItem()
            ) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            }
        }
    }

    // ---------------------------------------------------------------- Session 回调

    private inner class SessionCallback : MediaSession.Callback {

        /**
         * 控制器传来的 MediaItem 跨进程后可能丢了 Uri，这里补回来。
         * 同时只放行本机媒体库的 content:// 地址，外部 App 不能借我们的服务播放任意网络地址。
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(
                mediaItems.map { it.withResolvedUri() }
                    .filter { it.localConfiguration?.uri?.isLocalMedia() == true }
                    .toMutableList(),
            )

        /** 系统“继续播放”入口（如 Android 13+ 媒体面板、蓝牙重连）使用上次的播放现场。 */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaItemsWithStartPosition> = scope.future {
            val snap = container.settings.loadSnapshot()
            val songs = snap?.let { container.library.getByIds(it.queueIds) }.orEmpty()
            if (snap == null || songs.isEmpty()) {
                // Media3 约定：没有可恢复的内容时返回失败
                throw UnsupportedOperationException("没有可恢复的播放记录")
            } else {
                val currentId = snap.queueIds[snap.index]
                val index = songs.indexOfFirst { it.id == currentId }
                MediaItemsWithStartPosition(
                    songs.map { it.toMediaItem() },
                    index.coerceAtLeast(0),
                    if (index >= 0) snap.positionMs else 0,
                )
            }
        }
    }
}
