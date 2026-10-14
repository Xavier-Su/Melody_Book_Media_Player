package com.melodybook.player.ui.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.melodybook.player.playback.SpectrumBus

/** 只看最低的几个频段（鼓、贝斯）。频段按对数间隔，前 4 个大约在 200Hz 以下。 */
private const val BASS_BANDS = 4

/** 平均值的跟随速度（每秒）。越小越“记得”之前的响度，鼓点越突出。 */
private const val AVERAGE_RATE = 2f

/** 鼓点高出平均值多少算满格。 */
private const val ONSET_GAIN = 3f

/** 呼吸回落速度：每秒最多落掉整格的多少。 */
private const val PULSE_FALL_PER_SECOND = 1.8f

/**
 * 背景“呼吸”的强度，0~1。取低频能量相对最近平均值的突起，
 * 所以持续的低音不会让背景一直鼓着，只有鼓点那一下会亮一下。
 *
 * 只在 [active] 时向 [SpectrumBus] 要数据，暂停或离开播放页就停止分析、强度归零。
 */
@Composable
fun rememberBeatPulse(active: Boolean): State<Float> {
    val pulse = remember { mutableFloatStateOf(0f) }

    DisposableEffect(active) {
        if (active) SpectrumBus.acquire()
        onDispose { if (active) SpectrumBus.release() }
    }

    LaunchedEffect(active) {
        if (!active) {
            pulse.floatValue = 0f
            return@LaunchedEffect
        }
        var average = 0f
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = (now - last) / 1_000_000_000f
            last = now

            val levels = SpectrumBus.frameAt(System.nanoTime() - OUTPUT_LATENCY_NS) ?: continue
            var bass = 0f
            for (i in 0 until BASS_BANDS) bass += levels[i]
            bass /= BASS_BANDS

            average += (bass - average) * (AVERAGE_RATE * dt).coerceAtMost(1f)
            val target = ((bass - average) * ONSET_GAIN).coerceIn(0f, 1f)
            val current = pulse.floatValue
            pulse.floatValue = if (target >= current) target
            else (current - PULSE_FALL_PER_SECOND * dt).coerceAtLeast(target)
        }
    }
    return pulse
}
