package com.melodybook.player.ui.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.melodybook.player.playback.SpectrumAnalyzer
import com.melodybook.player.playback.SpectrumBus
import com.melodybook.player.ui.theme.MelodyTokens
import kotlinx.coroutines.flow.first

/** 音频输出到扬声器还要一点时间，画面再晚一点点才和耳朵里听到的对得上。 */
private const val OUTPUT_LATENCY_NS = 40_000_000L

/** 条的下落速度：每秒最多落掉整根的多少。上升是即时的，所以有“跳一下”的节奏感。 */
private const val FALL_PER_SECOND = 2.5f

/** 低于这个值就认为已经落到底，可以停止逐帧刷新。 */
private const val REST_LEVEL = 0.01f

/**
 * 频谱条。只在播放页可见、且开关打开时才存在于界面里，
 * 离开时通知 [SpectrumBus] 停止分析，处理器就不再耗 CPU。
 * 动画只在播放时逐帧刷新，暂停并落到底之后就不再重绘。
 */
@Composable
fun SpectrumView(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val sizes = MelodyTokens.sizes
    val spacing = MelodyTokens.spacing
    val color = MaterialTheme.colorScheme.primary

    val levels = remember { FloatArray(SpectrumAnalyzer.BANDS) }
    var tick by remember { mutableLongStateOf(0L) }
    val playing by rememberUpdatedState(isPlaying)

    DisposableEffect(Unit) {
        SpectrumBus.setActive(true)
        onDispose { SpectrumBus.setActive(false) }
    }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = (now - last) / 1_000_000_000f
            last = now

            val target = if (playing) {
                SpectrumBus.frameAt(System.nanoTime() - OUTPUT_LATENCY_NS)
            } else {
                SpectrumBus.clear()
                null
            }

            var anyUp = false
            for (i in levels.indices) {
                val t = target?.get(i) ?: 0f
                levels[i] = if (t >= levels[i]) t else (levels[i] - FALL_PER_SECOND * dt).coerceAtLeast(t)
                if (levels[i] > REST_LEVEL) anyUp = true
            }
            tick = now

            if (!playing && !anyUp) {
                levels.fill(0f)
                // 暂停且已落到底：等到重新播放再继续逐帧刷新
                snapshotFlow { playing }.first { it }
                last = withFrameNanos { it }
            }
        }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(sizes.spectrumHeight)
            .clearAndSetSemantics { },
    ) {
        // 读一下 tick，让每次刷新都触发重绘
        @Suppress("UNUSED_VARIABLE") val redraw = tick

        val gap = spacing.xs.toPx()
        val count = levels.size
        val barWidth = (size.width - gap * (count - 1)) / count
        val minHeight = barWidth
        val corner = CornerRadius(barWidth / 2)
        for (i in 0 until count) {
            val h = minHeight + (size.height - minHeight) * levels[i]
            drawRoundRect(
                color = color.copy(alpha = 0.8f),
                topLeft = Offset(i * (barWidth + gap), size.height - h),
                size = Size(barWidth, h),
                cornerRadius = corner,
            )
        }
    }
}
