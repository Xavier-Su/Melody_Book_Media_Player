package com.melodybook.player.ui.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import com.melodybook.player.data.SongMetadata
import com.melodybook.player.ui.theme.MelodyTokens

/**
 * 自绘进度条。
 *
 * 之前用 Material 的 Slider 自定义 thumb/track，两者各自在 Slider 内部布局，
 * 圆点和轨道的垂直中心对不齐。这里把轨道和圆点画在同一个 Canvas 里，
 * 共用同一条水平中线 [cy]，所以轨道一定从圆点正中穿过。
 *
 * 拖动时只改本地值，松手才 seek；拖动期间不会被播放进度“抢回去”。
 * 整条的触控高度是 48dp。
 */
@Composable
fun SeekBar(durationMs: Long, position: State<Long>, onSeek: (Long) -> Unit) {
    val sizes = MelodyTokens.sizes
    val scheme = MaterialTheme.colorScheme
    var dragging by remember { mutableStateOf<Float?>(null) }

    val enabled = durationMs > 0
    val duration = durationMs.coerceAtLeast(1)
    val fraction = dragging ?: (position.value.toFloat() / duration).coerceIn(0f, 1f)
    val shownMs = (fraction * duration).toLong()

    // pointerInput 的 lambda 只在 key 变化时重建，用 rememberUpdatedState 拿最新值
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentDuration by rememberUpdatedState(duration)

    val activeColor = if (enabled) scheme.primary else scheme.outline
    val trackColor = scheme.outlineVariant

    Column(Modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(sizes.touchTarget)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    // 圆点中心的可移动范围：两端各让出一个圆点半径
                    val pad = (sizes.seekThumb / 2).toPx()
                    awaitEachGesture {
                        fun fractionOf(x: Float): Float =
                            ((x - pad) / (size.width - 2 * pad).coerceAtLeast(1f)).coerceIn(0f, 1f)

                        val down = awaitFirstDown()
                        dragging = fractionOf(down.position.x)
                        drag(down.id) { change ->
                            dragging = fractionOf(change.position.x)
                            change.consume()
                        }
                        dragging?.let { currentOnSeek((it * currentDuration).toLong()) }
                        dragging = null
                    }
                }
                .semantics {
                    contentDescription = "播放进度"
                    stateDescription = SongMetadata.formatDuration(shownMs)
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    if (enabled) {
                        setProgress { target ->
                            currentOnSeek((target.coerceIn(0f, 1f) * currentDuration).toLong())
                            true
                        }
                    }
                },
        ) {
            val radius = sizes.seekThumb.toPx() / 2
            val trackHeight = sizes.seekTrack.toPx()
            val cy = size.height / 2
            val thumbX = radius + (size.width - 2 * radius) * fraction
            val corner = CornerRadius(trackHeight / 2)
            val top = cy - trackHeight / 2

            drawRoundRect(trackColor, Offset(0f, top), Size(size.width, trackHeight), corner)
            drawRoundRect(activeColor, Offset(0f, top), Size(thumbX, trackHeight), corner)
            drawCircle(activeColor, radius, Offset(thumbX, cy))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                SongMetadata.formatDuration(shownMs),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            Text(
                SongMetadata.formatDuration(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
