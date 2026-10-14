package com.melodybook.player.ui.nowplaying

import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.melodybook.player.orbit.OrbitConfig
import com.melodybook.player.orbit.OrbitMath
import com.melodybook.player.orbit.OrbitTimeMode
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** 用户刚刚拖动进度条 seek 到的位置。轨道据此立即跳转，不用等下一次进度轮询（最长 500ms）。 */
data class SeekHint(val positionMs: Long, val stamp: Long)

/**
 * 快速预览模式的时钟。放在 ViewModel 里，所以暂停 / 进度在旋转屏幕后还在，
 * 设置弹层也能控制它。进度以“圈”为单位，改周期时位置保持连续。
 */
@Stable
class OrbitPreviewClock {
    var running by mutableStateOf(true)
        private set
    var cycle by mutableFloatStateOf(0f)
        private set

    fun toggle() {
        running = !running
    }

    fun reset() {
        cycle = 0f
    }

    /** 暂停时不请求任何帧；离开组合树时随协程取消。 */
    suspend fun run(periodMs: Long) {
        snapshotFlow { running }.collectLatest { on ->
            if (!on) return@collectLatest
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    // 切到后台再回来时帧时钟会停，限制单帧步长，免得跳一大截
                    if (last != 0L) cycle = OrbitMath.advance(cycle, (now - last).coerceAtMost(MAX_STEP_NANOS), periodMs)
                    last = now
                }
            }
        }
    }

    private companion object {
        const val MAX_STEP_NANOS = 100_000_000L
    }
}

/**
 * 当前时间模式下的周期（0..1）。返回的是 lambda：只在绘制阶段读取，
 * 周期每帧变化也不会让播放页重组。三种模式共用同一个绘制组件。
 */
@Composable
fun rememberOrbitCycle(
    config: OrbitConfig,
    trackKey: Long?,
    durationMs: Long,
    isPlaying: Boolean,
    position: State<Long>,
    seekHint: State<SeekHint?>,
    clock: OrbitPreviewClock,
): () -> Float = when (config.mode) {
    OrbitTimeMode.TRACK_PROGRESS -> rememberTrackCycle(trackKey, durationMs, isPlaying, position, seekHint)
    OrbitTimeMode.REAL_TIME -> rememberRealTimeCycle()
    OrbitTimeMode.PREVIEW -> {
        LaunchedEffect(clock, config.previewPeriodMs) { clock.run(config.previewPeriodMs) }
        remember(clock) { { clock.cycle } }
    }
}

/** 播放进度的锚点：某个时刻的位置，播放中就按流逝的时间外推。 */
private class PositionAnchor {
    var key: Long? = null
    var hasKey = false
    var posMs = 0L
    var atNanos = 0L
    var playing = false

    fun estimate(nowNanos: Long): Long =
        if (playing) posMs + (nowNanos - atNanos) / 1_000_000L else posMs

    fun set(positionMs: Long, nowNanos: Long) {
        posMs = positionMs
        atNanos = nowNanos
    }
}

/**
 * 歌曲进度：以播放器的真实进度为准。播放器每 500ms 才上报一次，
 * 中间用单调时钟外推，让天体匀速移动；每次上报或 seek 都重新对齐锚点，所以不会累积偏差。
 * 暂停时锚点保持在暂停瞬间的估计值，天体停在原位，不会回跳到上一次轮询的值。
 */
@Composable
private fun rememberTrackCycle(
    trackKey: Long?,
    durationMs: Long,
    isPlaying: Boolean,
    position: State<Long>,
    seekHint: State<SeekHint?>,
): () -> Float {
    val anchor = remember { PositionAnchor() }
    val cycle = remember { mutableFloatStateOf(0f) }
    val duration by rememberUpdatedState(durationMs)

    LaunchedEffect(trackKey, isPlaying) {
        val now = System.nanoTime()
        when {
            !anchor.hasKey -> anchor.set(position.value, now)
            anchor.key != trackKey -> anchor.set(0L, now) // 切歌：新歌从头开始，旧歌的进度不能带过来
            else -> anchor.set(anchor.estimate(now), now)
        }
        anchor.hasKey = true
        anchor.key = trackKey
        anchor.playing = isPlaying

        fun publish() {
            cycle.floatValue = OrbitMath.trackCycle(anchor.estimate(System.nanoTime()), duration)
        }
        publish()

        // snapshotFlow 先发一次当前值，丢掉它：那可能是切歌 / 暂停瞬间还没刷新的旧进度
        launch {
            snapshotFlow { position.value }.drop(1).collect {
                anchor.set(it, System.nanoTime())
                publish()
            }
        }
        launch {
            snapshotFlow { seekHint.value }.drop(1).collect { hint ->
                if (hint != null) {
                    anchor.set(hint.positionMs, System.nanoTime())
                    publish()
                }
            }
        }
        if (isPlaying) {
            while (true) withFrameNanos { publish() }
        }
    }
    return remember { { cycle.floatValue } }
}

/**
 * 现实时间：每次都重新读系统时钟，所以时区、夏令时、手动改时间、跨天都会在下一次刷新时自动生效。
 * 视觉上 15 秒内移动不到 0.2°，不需要更高频率。不联网、不用定位。
 */
@Composable
private fun rememberRealTimeCycle(): () -> Float {
    val cycle = remember { mutableFloatStateOf(OrbitMath.timeOfDayCycle(msOfDayNow())) }
    LaunchedEffect(Unit) {
        while (true) {
            cycle.floatValue = OrbitMath.timeOfDayCycle(msOfDayNow())
            delay(REAL_TIME_TICK_MS)
        }
    }
    return remember { { cycle.floatValue } }
}

private const val REAL_TIME_TICK_MS = 15_000L

private fun msOfDayNow(): Long {
    val c = Calendar.getInstance()
    return ((c.get(Calendar.HOUR_OF_DAY) * 60L + c.get(Calendar.MINUTE)) * 60L + c.get(Calendar.SECOND)) * 1000L +
        c.get(Calendar.MILLISECOND)
}
