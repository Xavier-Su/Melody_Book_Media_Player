package com.melodybook.player

import com.melodybook.player.playback.SleepTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerTest {

    private var now = 1_000L
    private val timer = SleepTimer(clock = { now })

    @Test fun `remaining time counts down from deadline`() {
        timer.start(10)
        assertEquals(600_000L, timer.remainingMs())
        now += 60_000
        assertEquals(540_000L, timer.remainingMs())
    }

    @Test fun `remaining never goes negative`() {
        timer.start(1)
        now += 120_000
        assertEquals(0L, timer.remainingMs())
    }

    @Test fun `cancel and after-current states`() {
        timer.stopAfterCurrent()
        assertTrue(timer.state.value is SleepTimer.State.AfterCurrent)
        assertEquals(0L, timer.remainingMs())
        timer.cancel()
        assertEquals(SleepTimer.State.Off, timer.state.value)
    }
}
