package com.melodybook.player

import com.melodybook.player.orbit.OrbitConfig
import com.melodybook.player.orbit.OrbitMath
import com.melodybook.player.orbit.OrbitSwitchRule
import com.melodybook.player.orbit.OrbitTimeMode
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrbitMathTest {

    private val eps = 1e-4f

    @Test fun `track cycle clamps and handles unknown duration`() {
        assertEquals(0f, OrbitMath.trackCycle(5_000, 0), 0f)
        assertEquals(0f, OrbitMath.trackCycle(-10, 100), 0f)
        assertEquals(1f, OrbitMath.trackCycle(500, 100), 0f)
        assertEquals(0.25f, OrbitMath.trackCycle(25, 100), eps)
    }

    @Test fun `time of day maps 6h to quarter and wraps`() {
        assertEquals(0f, OrbitMath.timeOfDayCycle(0), eps)
        assertEquals(0.25f, OrbitMath.timeOfDayCycle(6 * 3_600_000L), eps)
        assertEquals(0.5f, OrbitMath.timeOfDayCycle(12 * 3_600_000L), eps)
        assertEquals(0f, OrbitMath.timeOfDayCycle(24 * 3_600_000L), eps)
        assertEquals(0.75f, OrbitMath.timeOfDayCycle(-6 * 3_600_000L), eps)
    }

    @Test fun `advance wraps and ignores bad input`() {
        assertEquals(0.5f, OrbitMath.advance(0f, 5_000_000_000L, 10_000), eps)
        assertEquals(0.1f, OrbitMath.advance(0.9f, 2_000_000_000L, 10_000), eps)
        assertEquals(0.3f, OrbitMath.advance(0.3f, 1_000_000L, 0), 0f)
        assertEquals(0.3f, OrbitMath.advance(0.3f, -5L, 1000), 0f)
    }

    @Test fun `angle starts at start deg and full cycle is one turn`() {
        assertEquals(Math.PI.toFloat(), OrbitMath.angleRad(0f, 180f, false), eps)
        val full = OrbitMath.angleRad(1f, 180f, false) - OrbitMath.angleRad(0f, 180f, false)
        assertEquals(OrbitMath.TWO_PI, full, eps)
        assertEquals(-OrbitMath.TWO_PI, OrbitMath.angleRad(1f, 0f, true), eps)
        assertEquals(0f, OrbitMath.angleRad(Float.NaN, 0f, false), 0f)
    }

    @Test fun `real time puts noon on top and 6h on the left`() {
        val noon = OrbitMath.angleRad(0.5f, OrbitMath.REAL_TIME_START_DEG, false)
        assertEquals(100f, OrbitMath.orbitX(100f, 50f, noon), 1e-3f)
        assertEquals(50f, OrbitMath.orbitY(100f, 50f, noon), 1e-3f)
        val six = OrbitMath.angleRad(0.25f, OrbitMath.REAL_TIME_START_DEG, false)
        assertEquals(50f, OrbitMath.orbitX(100f, 50f, six), 1e-3f)
    }

    @Test fun `auto rule resolves per mode`() {
        assertEquals(OrbitSwitchRule.DAYLIGHT, OrbitMath.resolveRule(OrbitSwitchRule.AUTO, OrbitTimeMode.REAL_TIME))
        assertEquals(OrbitSwitchRule.HALF_CYCLE, OrbitMath.resolveRule(OrbitSwitchRule.AUTO, OrbitTimeMode.PREVIEW))
        assertEquals(OrbitSwitchRule.DAYLIGHT, OrbitMath.resolveRule(OrbitSwitchRule.DAYLIGHT, OrbitTimeMode.PREVIEW))
    }

    @Test fun `track mode is sun at start and moon at end`() {
        val w = OrbitMath.sunWindow(OrbitSwitchRule.AUTO, OrbitTimeMode.TRACK_PROGRESS)
        assertEquals(1f, OrbitMath.sunWeight(0f, w, wrap = false), eps)
        assertEquals(1f, OrbitMath.sunWeight(0.25f, w, wrap = false), eps)
        assertEquals(0.5f, OrbitMath.sunWeight(0.5f, w, wrap = false), eps)
        assertEquals(0f, OrbitMath.sunWeight(0.75f, w, wrap = false), eps)
        assertEquals(0f, OrbitMath.sunWeight(1f, w, wrap = false), eps)
    }

    @Test fun `daylight window is day at noon and night at midnight`() {
        val w = OrbitMath.sunWindow(OrbitSwitchRule.AUTO, OrbitTimeMode.REAL_TIME)
        assertEquals(1f, OrbitMath.sunWeight(0.5f, w, wrap = true), eps)
        assertEquals(0f, OrbitMath.sunWeight(0f, w, wrap = true), eps)
        assertEquals(0.5f, OrbitMath.sunWeight(0.25f, w, wrap = true), eps)
        assertEquals(0.5f, OrbitMath.sunWeight(0.75f, w, wrap = true), eps)
    }

    @Test fun `sun weight is continuous across the wrap seam`() {
        for (rule in listOf(OrbitSwitchRule.HALF_CYCLE, OrbitSwitchRule.DAYLIGHT)) {
            val w = OrbitMath.sunWindow(rule, OrbitTimeMode.PREVIEW)
            var prev = OrbitMath.sunWeight(0f, w, wrap = true)
            var c = 0f
            while (c < 1.5f) {
                c += 0.0005f
                val cur = OrbitMath.sunWeight(c, w, wrap = true)
                assertTrue("jump at $c for $rule", abs(cur - prev) < 0.05f)
                assertTrue(cur in 0f..1f)
                prev = cur
            }
        }
    }

    @Test fun `ring ratio clamps scale`() {
        assertEquals(OrbitMath.ringRatio(OrbitConfig.SCALE_MAX), OrbitMath.ringRatio(5f), 0f)
        assertEquals(OrbitMath.ringRatio(OrbitConfig.SCALE_MIN), OrbitMath.ringRatio(0f), 0f)
    }
}
