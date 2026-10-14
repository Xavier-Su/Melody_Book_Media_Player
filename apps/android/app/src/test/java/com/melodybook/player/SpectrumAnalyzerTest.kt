package com.melodybook.player

import com.melodybook.player.playback.SpectrumAnalyzer
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumAnalyzerTest {

    private val rate = 44_100

    /** 送入 n 个采样，返回最后一帧。 */
    private fun feed(a: SpectrumAnalyzer, n: Int, sample: (Int) -> Float): FloatArray? {
        var last: FloatArray? = null
        for (i in 0 until n) a.push(sample(i))?.let { last = it }
        return last
    }

    @Test
    fun `no frame until the window is full`() {
        val a = SpectrumAnalyzer(rate)
        assertNull(feed(a, SpectrumAnalyzer.SIZE - 1) { 0f })
    }

    @Test
    fun `silence gives all zero`() {
        val a = SpectrumAnalyzer(rate)
        val out = feed(a, SpectrumAnalyzer.SIZE * 2) { 0f }
        assertNotNull(out)
        assertTrue(out!!.all { it == 0f })
    }

    @Test
    fun `1kHz tone peaks in the middle bands and not at the extremes`() {
        val a = SpectrumAnalyzer(rate)
        val out = feed(a, SpectrumAnalyzer.SIZE * 3) { sin(2.0 * PI * 1000.0 * it / rate).toFloat() }
        assertNotNull(out)
        val peak = out!!.indices.maxByOrNull { out[it] }!!
        // 50Hz~16kHz 对数分 24 段，1kHz 约落在第 12 段
        assertTrue("peak band was $peak", peak in 10..14)
        assertTrue(out[peak] > 0.5f)
        assertEquals(0f, out[0], 0.05f)
    }

    @Test
    fun `reset forgets buffered samples`() {
        val a = SpectrumAnalyzer(rate)
        feed(a, SpectrumAnalyzer.SIZE) { 0.5f }
        a.reset()
        assertNull(feed(a, SpectrumAnalyzer.SIZE - 1) { 0.5f })
    }
}
