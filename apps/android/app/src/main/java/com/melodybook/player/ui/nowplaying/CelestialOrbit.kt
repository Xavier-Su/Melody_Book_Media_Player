package com.melodybook.player.ui.nowplaying

import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.melodybook.player.orbit.OrbitConfig
import com.melodybook.player.orbit.OrbitMath
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.flow.first

/** 轨道外圈留给天体和光晕的边距，布局时要给 Canvas 加上这一圈。 */
val OrbitHaloMargin = 16.dp

private const val TICK_COUNT = 24
private const val TICK_MAJOR_EVERY = 6
private const val SUN_SMOOTH_SEC = 0.25f

private val SunColor = Color(0xFFFFB74D)

/**
 * 日月轨道：只负责画。位置来自 [cycle]（0..1），不碰播放器。
 * [cycle] 只在绘制阶段读取，周期变化只触发重绘，不触发重组。
 * Canvas 是正方形，轨道是以中心为圆心的圆，半径 = 边长 / 2 - [OrbitHaloMargin]。
 */
@Composable
fun CelestialOrbit(
    cycle: () -> Float,
    config: OrbitConfig,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val ringColor = scheme.onSurface.copy(alpha = 0.16f)
    val tickColor = scheme.onSurface.copy(alpha = 0.28f)
    val dayArcColor = SunColor.copy(alpha = 0.40f)
    val moonColor = scheme.onSurfaceVariant

    val startDeg = OrbitMath.startDegFor(config)
    val window = OrbitMath.sunWindow(config.switchRule, config.mode)
    val wrap = OrbitMath.wraps(config.mode)
    val dir = if (config.reverse) -1f else 1f

    // 日 / 月的权重做一次限速平滑：seek、切歌、循环回到起点时权重会突变，这里把它缓成淡入淡出。
    // 周期连续变化时目标每帧只动一点点，限速追得上，不会产生延迟。
    val sunWeight = remember(window, wrap, cycle) {
        mutableFloatStateOf(Snapshot.withoutReadObservation { OrbitMath.sunWeight(cycle(), window, wrap) })
    }
    LaunchedEffect(window, wrap, cycle) {
        var last = 0L
        while (true) {
            val target = OrbitMath.sunWeight(cycle(), window, wrap)
            val shown = sunWeight.floatValue
            if (target == shown) {
                last = 0L
                snapshotFlow { OrbitMath.sunWeight(cycle(), window, wrap) }.first { it != sunWeight.floatValue }
                continue
            }
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1_000_000_000f
                last = now
                val step = dt / SUN_SMOOTH_SEC
                val t = OrbitMath.sunWeight(cycle(), window, wrap)
                val s = sunWeight.floatValue
                sunWeight.floatValue = when {
                    t > s -> (s + step).coerceAtMost(t)
                    else -> (s - step).coerceAtLeast(t)
                }
            }
        }
    }

    // 刻度的方向只和起点 / 方向有关，预先算好，绘制时不再做三角函数
    val tickDirs = remember(startDeg, config.reverse) {
        Array(TICK_COUNT) {
            val a = OrbitMath.angleRad(it / TICK_COUNT.toFloat(), startDeg, config.reverse)
            Offset(cos(a), sin(a))
        }
    }
    // 单位月牙：半径 1 的圆减去错开的另一个圆，绘制时只做缩放平移
    val crescent = remember {
        val outer = Path().apply { addOval(Rect(Offset.Zero, 1f)) }
        val cut = Path().apply { addOval(Rect(Offset(0.42f, -0.12f), 0.86f)) }
        Path.combine(PathOperation.Difference, outer, cut)
    }

    Canvas(modifier.fillMaxSize()) {
        val margin = OrbitHaloMargin.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - margin
        if (radius <= 0f) return@Canvas
        val bodyR = (margin * 0.42f).coerceAtMost(radius * 0.08f)
        val hairline = 1.dp.toPx()

        // 主轨道
        drawCircle(ringColor, radius, center, style = Stroke(hairline))

        // 白天那一段稍亮一点，只是提示，不是真实日出日落
        drawArc(
            color = dayArcColor,
            startAngle = startDeg + dir * 360f * window.start,
            sweepAngle = dir * 360f * window.length,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(hairline * 1.5f),
        )

        if (config.showTicks) {
            for (i in 0 until TICK_COUNT) {
                val major = i % TICK_MAJOR_EVERY == 0
                val len = if (major) bodyR * 0.9f else bodyR * 0.5f
                val d = tickDirs[i]
                drawLine(
                    tickColor,
                    Offset(center.x + d.x * (radius + hairline * 2), center.y + d.y * (radius + hairline * 2)),
                    Offset(center.x + d.x * (radius + hairline * 2 + len), center.y + d.y * (radius + hairline * 2 + len)),
                    strokeWidth = hairline,
                )
            }
        }

        val angle = OrbitMath.angleRad(cycle(), startDeg, config.reverse)
        val x = OrbitMath.orbitX(center.x, radius, angle)
        val y = OrbitMath.orbitY(center.y, radius, angle)
        val sun = sunWeight.floatValue
        val moon = 1f - sun

        if (sun > 0.001f) {
            drawCircle(SunColor.copy(alpha = 0.10f * sun), bodyR * 2.6f, Offset(x, y))
            drawCircle(SunColor.copy(alpha = 0.18f * sun), bodyR * 1.7f, Offset(x, y))
            drawCircle(SunColor.copy(alpha = sun), bodyR, Offset(x, y))
        }
        if (moon > 0.001f) {
            drawCircle(moonColor.copy(alpha = 0.08f * moon), bodyR * 2.4f, Offset(x, y))
            withTransform({
                translate(x, y)
                scale(bodyR * 1.1f, bodyR * 1.1f, Offset.Zero)
            }) {
                drawPath(crescent, moonColor, alpha = moon)
            }
        }
    }
}
