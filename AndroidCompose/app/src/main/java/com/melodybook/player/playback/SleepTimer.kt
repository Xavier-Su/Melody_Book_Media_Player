package com.melodybook.player.playback

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 定时关闭。
 *
 * 只保存“截止时刻”，不再像旧版那样每秒递减一个计数器：
 * 剩余时间由界面按需计算，播放服务只需要在截止时刻暂停一次。
 * 使用 elapsedRealtime，不受用户修改系统时间影响。
 */
class SleepTimer(private val clock: () -> Long = SystemClock::elapsedRealtime) {

    sealed interface State {
        data object Off : State
        data class Counting(val endAtElapsedMs: Long, val totalMs: Long) : State
        /** 播完当前这首就停。 */
        data object AfterCurrent : State
    }

    private val _state = MutableStateFlow<State>(State.Off)
    val state: StateFlow<State> = _state.asStateFlow()

    fun start(minutes: Int) {
        require(minutes > 0)
        val total = minutes * 60_000L
        _state.value = State.Counting(clock() + total, total)
    }

    fun stopAfterCurrent() {
        _state.value = State.AfterCurrent
    }

    fun cancel() {
        _state.value = State.Off
    }

    fun remainingMs(state: State = _state.value): Long = when (state) {
        is State.Counting -> (state.endAtElapsedMs - clock()).coerceAtLeast(0)
        else -> 0
    }

    companion object {
        val PRESET_MINUTES = listOf(5, 10, 15, 20, 30, 45, 60, 90)
    }
}
