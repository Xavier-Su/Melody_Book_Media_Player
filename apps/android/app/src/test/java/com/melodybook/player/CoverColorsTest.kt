package com.melodybook.player

import com.melodybook.player.ui.theme.CoverColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverColorsTest {

    private fun hueOf(color: Int): Float {
        val hsl = FloatArray(3)
        CoverColors.rgbToHsl((color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF, hsl)
        return hsl[0]
    }

    private fun lightnessOf(color: Int): Float {
        val hsl = FloatArray(3)
        CoverColors.rgbToHsl((color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF, hsl)
        return hsl[2]
    }

    private fun image(vararg parts: Pair<Int, Int>): IntArray =
        parts.flatMap { (color, count) -> List(count) { color } }.toIntArray()

    @Test fun `black and white cover gives no seeds`() {
        val pixels = image(0xFF000000.toInt() to 500, 0xFFFFFFFF.toInt() to 400, 0xFF808080.toInt() to 100)
        assertNull(CoverColors.extract(pixels))
    }

    @Test fun `empty image gives no seeds`() {
        assertNull(CoverColors.extract(IntArray(0)))
    }

    @Test fun `dominant color becomes primary`() {
        // 大片蓝色 + 一小块橙色
        val pixels = image(0xFF2050C0.toInt() to 700, 0xFFE08020.toInt() to 300)
        val seeds = CoverColors.extract(pixels)
        assertNotNull(seeds)
        seeds!!
        assertEquals(hueOf(0xFF2050C0.toInt()), hueOf(seeds.primary), 8f)
        assertEquals(hueOf(0xFFE08020.toInt()), hueOf(seeds.accent), 8f)
    }

    @Test fun `single color cover uses primary as accent`() {
        val seeds = CoverColors.extract(image(0xFF30A060.toInt() to 1000))!!
        assertEquals(seeds.primary, seeds.accent)
    }

    @Test fun `very dark cover is brightened to a readable primary`() {
        val seeds = CoverColors.extract(image(0xFF301008.toInt() to 1000))!!
        assertTrue(lightnessOf(seeds.primary) >= 0.34f)
    }

    @Test fun `hsl round trip keeps color`() {
        val c = 0xFF3F6FB5.toInt()
        val hsl = FloatArray(3)
        CoverColors.rgbToHsl(0x3F, 0x6F, 0xB5, hsl)
        val back = CoverColors.hslToRgb(hsl)
        for (shift in intArrayOf(16, 8, 0)) {
            assertEquals((c shr shift) and 0xFF, (back shr shift) and 0xFF, 1)
        }
    }

    private fun assertEquals(expected: Int, actual: Int, tolerance: Int) {
        assertTrue("expected $expected±$tolerance but was $actual", kotlin.math.abs(expected - actual) <= tolerance)
    }
}
