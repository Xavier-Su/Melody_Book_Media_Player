package com.melodybook.player

import com.melodybook.player.data.lyrics.LrclibItem
import com.melodybook.player.data.lyrics.LrclibPick
import com.melodybook.player.data.weather.WeatherCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LrclibPickTest {

    private fun item(
        id: Long,
        durationSec: Int,
        synced: String? = "[00:01.00]a",
        plain: String? = "a",
        instrumental: Boolean = false,
    ) = LrclibItem(id, "t", "a", "al", durationSec, instrumental, synced, plain)

    @Test
    fun rejectsDurationOutsideTolerance() {
        assertNull(LrclibPick.pick(listOf(item(1, 200)), durationMs = 190_000))
    }

    @Test
    fun prefersSyncedOverCloserPlain() {
        val plain = item(1, 180, synced = null)
        val synced = item(2, 182)
        assertEquals(2L, LrclibPick.pick(listOf(plain, synced), 180_000)?.id)
    }

    @Test
    fun prefersClosestDurationAmongSynced() {
        val far = item(1, 182)
        val near = item(2, 180)
        assertEquals(2L, LrclibPick.pick(listOf(far, near), 180_000)?.id)
    }

    @Test
    fun skipsInstrumentalAndEmpty() {
        val items = listOf(item(1, 180, instrumental = true), item(2, 180, synced = null, plain = null))
        assertNull(LrclibPick.pick(items, 180_000))
    }

    @Test
    fun weatherCodes() {
        assertEquals("晴", WeatherCodes.describe(0))
        assertEquals("小雨", WeatherCodes.describe(61))
        assertEquals("天气未知", WeatherCodes.describe(1234))
    }
}
