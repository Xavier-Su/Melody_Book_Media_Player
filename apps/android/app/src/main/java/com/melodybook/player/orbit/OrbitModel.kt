package com.melodybook.player.orbit

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** 驱动日月轨道的时间来源。 */
enum class OrbitTimeMode(val label: String) {
    /** 歌曲进度：一首歌走完一整圈。 */
    TRACK_PROGRESS("歌曲进度"),

    /** 现实时间：24 小时一圈，与播放无关。 */
    REAL_TIME("现实时间"),

    /** 快速预览：按固定周期自转，调试用，与播放无关。 */
    PREVIEW("快速预览"),
}

/** 太阳 / 月亮谁是主角的判定规则。只影响显示，不影响天体位置。 */
enum class OrbitSwitchRule(val label: String) {
    /** 现实时间用 [DAYLIGHT]，其余模式用 [HALF_CYCLE]。 */
    AUTO("自动"),

    /** 前半圈是太阳，后半圈是月亮。 */
    HALF_CYCLE("前半日后半月"),

    /** 06:00 到 18:00 是太阳，其余是月亮（简化的昼夜，不是真实日出日落）。 */
    DAYLIGHT("6 点到 18 点为日"),
}

/**
 * 日月轨道的全部可配置项。
 *
 * 角度约定（全文件统一）：屏幕坐标，0° 在 3 点钟方向，正方向为屏幕上的顺时针（y 轴向下）。
 * 所以 180° 在左侧，沿顺时针先经过上方：左（日出）→ 上（正午）→ 右（日落）→ 下（午夜）。
 */
data class OrbitConfig(
    val enabled: Boolean = true,
    val mode: OrbitTimeMode = OrbitTimeMode.TRACK_PROGRESS,
    /** 预览模式走一圈的秒数。 */
    val previewPeriodSec: Int = 30,
    /** 歌曲 / 预览模式下，周期 0 对应的角度。现实时间模式固定用 [OrbitMath.REAL_TIME_START_DEG]。 */
    val startDeg: Float = 180f,
    /** true 为逆时针。 */
    val reverse: Boolean = false,
    val switchRule: OrbitSwitchRule = OrbitSwitchRule.AUTO,
    /** 轨道相对唱片的大小倍数，范围 [SCALE_MIN]..[SCALE_MAX]。 */
    val scale: Float = 1f,
    val showTicks: Boolean = true,
) {
    val previewPeriodMs: Long get() = previewPeriodSec.coerceAtLeast(1) * 1000L

    companion object {
        const val SCALE_MIN = 0.95f
        const val SCALE_MAX = 1.3f
        val PREVIEW_PERIODS_SEC = listOf(10, 30, 60)
    }
}

/** 太阳作为主角的周期区间：[start, start + 0.5)，周期取值 0..1。 */
data class SunWindow(val start: Float, val end: Float) {
    val length: Float get() = end - start
}

/** 所有位置 / 显隐计算，纯函数，不依赖 Android。 */
object OrbitMath {

    const val TWO_PI = (2.0 * PI).toFloat()

    /** 现实时间模式：00:00 放在正下方（90°），这样正午在上方，06:00 在左，18:00 在右。 */
    const val REAL_TIME_START_DEG = 90f

    /** 日月交接时，淡入淡出占整圈的比例。 */
    const val FADE = 0.06f

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** 歌曲进度 -> 周期 0..1。时长未知、进度无效时返回 0。 */
    fun trackCycle(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L) return 0f
        return (positionMs.coerceIn(0L, durationMs).toDouble() / durationMs).toFloat()
    }

    /** 当天已过的毫秒数 -> 周期 0..1（00:00 为 0，12:00 为 0.5）。 */
    fun timeOfDayCycle(msOfDay: Long): Float =
        (Math.floorMod(msOfDay, DAY_MS).toDouble() / DAY_MS).toFloat()

    /** 预览时钟前进 [dtNanos]，结果回绕到 0..1。周期无效时原地不动。 */
    fun advance(cycle: Float, dtNanos: Long, periodMs: Long): Float {
        if (periodMs <= 0L || dtNanos <= 0L) return cycle
        val next = cycle + (dtNanos / 1_000_000.0 / periodMs).toFloat()
        return next - floor(next)
    }

    fun startDegFor(config: OrbitConfig): Float =
        if (config.mode == OrbitTimeMode.REAL_TIME) REAL_TIME_START_DEG else config.startDeg

    /** 周期 -> 弧度。无效输入（NaN / 无穷）按周期 0 处理。 */
    fun angleRad(cycle: Float, startDeg: Float, reverse: Boolean): Float {
        val c = if (cycle.isFinite()) cycle else 0f
        val dir = if (reverse) -1f else 1f
        return Math.toRadians(startDeg.toDouble()).toFloat() + dir * TWO_PI * c
    }

    fun orbitX(centerX: Float, radiusX: Float, angleRad: Float): Float = centerX + radiusX * cos(angleRad)

    fun orbitY(centerY: Float, radiusY: Float, angleRad: Float): Float = centerY + radiusY * sin(angleRad)

    fun resolveRule(rule: OrbitSwitchRule, mode: OrbitTimeMode): OrbitSwitchRule = when (rule) {
        OrbitSwitchRule.AUTO ->
            if (mode == OrbitTimeMode.REAL_TIME) OrbitSwitchRule.DAYLIGHT else OrbitSwitchRule.HALF_CYCLE
        else -> rule
    }

    fun sunWindow(rule: OrbitSwitchRule, mode: OrbitTimeMode): SunWindow =
        when (resolveRule(rule, mode)) {
            OrbitSwitchRule.DAYLIGHT -> SunWindow(0.25f, 0.75f)
            else -> SunWindow(0f, 0.5f)
        }

    /** 歌曲模式的周期是 0..1 不回绕（终点和下一首的起点不连续）；其余模式首尾相接。 */
    fun wraps(mode: OrbitTimeMode): Boolean = mode != OrbitTimeMode.TRACK_PROGRESS

    /**
     * 太阳的显示权重 0..1，月亮是 1 - 权重。
     *
     * 在区间两端各有一段宽 [fade] 的平滑过渡，中心正好落在区间端点上。
     * [wrap] 为 true 时周期首尾相接，端点处两侧连续，不会出现跳变；
     * 为 false 时（歌曲模式）周期 0 和 1 是真实的起止点，起点在区间开头就直接是满权重。
     */
    fun sunWeight(cycle: Float, window: SunWindow, wrap: Boolean, fade: Float = FADE): Float {
        val c = if (cycle.isFinite()) cycle else 0f
        val len = window.length
        val half = fade / 2f
        val v: Float
        val rise: Float
        if (wrap) {
            var u = c - window.start
            u -= floor(u)
            // 把夜晚从中点切开：前半归到区间之前（负数），这样两端的过渡都是连续的
            v = if (u > (1f + len) / 2f) u - 1f else u
            rise = smoothstep(-half, half, v)
        } else {
            v = c - window.start
            rise = if (window.start <= 0f) 1f else smoothstep(-half, half, v)
        }
        val fall = 1f - smoothstep(len - half, len + half, v)
        return (rise * fall).coerceIn(0f, 1f)
    }

    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        if (edge1 <= edge0) return if (x < edge0) 0f else 1f
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** 轨道半径相对唱片半径的倍数。 */
    fun ringRatio(scale: Float): Float =
        1.12f * scale.coerceIn(OrbitConfig.SCALE_MIN, OrbitConfig.SCALE_MAX)
}
